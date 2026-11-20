package com.nuvio.app.features.debrid

import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val DECIMAL_TB_BYTES = 1_000_000_000_000L
private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val USAGE_WINDOW_DAYS = 30
private const val USAGE_STORAGE_DAYS = 60
private const val MIN_REFRESH_INTERVAL_MS = 60_000L

internal enum class TorboxPlan(
    val displayName: String,
    val monthlyAllowanceBytes: Long,
) {
    FREE("Free", 5L * DECIMAL_TB_BYTES),
    ESSENTIAL("Essential", 10L * DECIMAL_TB_BYTES),
    STANDARD("Standard", 20L * DECIMAL_TB_BYTES),
    PRO("Pro", 30L * DECIMAL_TB_BYTES),
    ;

    companion object {
        fun fromCode(code: Int?): TorboxPlan = when (code) {
            1 -> ESSENTIAL
            2 -> PRO
            3 -> STANDARD
            else -> FREE
        }

        fun fromName(name: String?): TorboxPlan? {
            val normalized = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return TorboxPlan.entries.firstOrNull { it.displayName.equals(normalized, ignoreCase = true) }
        }
    }
}

internal data class TorboxUsageUiState(
    val isLoading: Boolean = false,
    val email: String? = null,
    val plan: TorboxPlan? = null,
    val isSubscribed: Boolean = false,
    val expiryEpochMs: Long? = null,
    val cooldownEpochMs: Long? = null,
    val daysLeft: Long? = null,
    val todayDownloadedBytes: Long = 0L,
    val monthlyDownloadedBytes: Long = 0L,
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
)

internal object TorboxUsageRepository {
    private val _uiState = MutableStateFlow(TorboxUsageUiState())
    val uiState: StateFlow<TorboxUsageUiState> = _uiState.asStateFlow()

    private val refreshMutex = Mutex()
    private val stateMutex = Mutex()
    private var hasLoadedLocal = false
    private var dailyBytes: Map<String, Long> = emptyMap()
    private var statsBytes: Map<String, Long> = emptyMap()
    private var statsLocalBaselineBytes: Map<String, Long> = emptyMap()

    private var plan: TorboxPlan? = null
    private var email: String? = null
    private var isSubscribed = false
    private var expiryEpochMs: Long? = null
    private var cooldownEpochMs: Long? = null
    private var lastRefreshAtMs = 0L
    private var lastRefreshApiKey: String? = null
    private var remoteLoaded = false

    fun ensureLoaded() {
        if (hasLoadedLocal) return
        val stored = runCatching {
            decodeDailyBytes(DebridSettingsStorage.loadTorboxUsageDailyBytes())
        }.getOrDefault(emptyMap())
        val merged = stored.toMutableMap()
        dailyBytes.forEach { (dateKey, bytes) ->
            merged[dateKey] = maxOf(merged[dateKey] ?: 0L, bytes)
        }
        dailyBytes = merged
        hasLoadedLocal = true
        publish()
    }

    fun onProfileChanged() {
        hasLoadedLocal = false
        dailyBytes = emptyMap()
        statsBytes = emptyMap()
        statsLocalBaselineBytes = emptyMap()
        plan = null
        email = null
        isSubscribed = false
        expiryEpochMs = null
        cooldownEpochMs = null
        lastRefreshAtMs = 0L
        lastRefreshApiKey = null
        remoteLoaded = false
        _uiState.value = TorboxUsageUiState()
        ensureLoaded()
    }

    suspend fun refresh(apiKey: String, force: Boolean = false) {
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isBlank()) return
        ensureLoaded()

        refreshMutex.withLock {
            if (lastRefreshApiKey != normalizedApiKey) {
                stateMutex.withLock {
                    lastRefreshApiKey = normalizedApiKey
                    plan = null
                    email = null
                    isSubscribed = false
                    expiryEpochMs = null
                    cooldownEpochMs = null
                    statsBytes = emptyMap()
                    statsLocalBaselineBytes = emptyMap()
                    lastRefreshAtMs = 0L
                    remoteLoaded = false
                    publish(loading = false, failed = false, error = null)
                }
            }

            val startedAtMs = nowMs()
            if (!force && remoteLoaded && startedAtMs - lastRefreshAtMs in 0L until MIN_REFRESH_INTERVAL_MS) return
            lastRefreshAtMs = startedAtMs
            _uiState.value = _uiState.value.copy(isLoading = true, loadFailed = false, errorMessage = null)
            val localBytesAtRequestStart = stateMutex.withLock { dailyBytes.toMap() }

            // Network calls happen outside the state lock so usage recording never waits on them.
            suspend fun <T> safeCall(block: suspend () -> T): Pair<T?, Throwable?> = try {
                block() to null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null to error
            }

            var requestError: Throwable? = null
            val responses = try {
                coroutineScope {
                    val user = async { safeCall { TorboxApiClient.fetchUser(normalizedApiKey) } }
                    val stats = async { safeCall { TorboxApiClient.fetchUserStats(normalizedApiKey) } }
                    user.await() to stats.await()
                }
            } catch (error: CancellationException) {
                stateMutex.withLock { publish(loading = false) }
                throw error
            } catch (error: Exception) {
                requestError = error
                null
            }

            stateMutex.withLock {
                var anySuccess = false
                var userError: String? = null
                var statsError: String? = null
                if (responses != null) {
                    val (userResult, statsResult) = responses
                    val (userResponse, userException) = userResult
                    val (statsResponse, statsException) = statsResult
                    val userEnvelope = userResponse?.body
                        ?.takeIf { userResponse.isSuccessful && it.success != false }
                    val statsEnvelope = statsResponse?.body
                        ?.takeIf { statsResponse.isSuccessful && it.success != false }

                    userEnvelope?.data?.let { data ->
                        if (parseUser(data)) {
                            anySuccess = true
                        } else {
                            userError = "Unable to parse TorBox account response."
                        }
                    }
                    statsEnvelope?.data?.let { data ->
                        statsBytes = parseBandwidthBytes(data)
                        statsLocalBaselineBytes = localBytesAtRequestStart
                        anySuccess = true
                    }
                    userError = userError ?: torboxResponseError(userResponse, userException, normalizedApiKey)
                    statsError = torboxResponseError(statsResponse, statsException, normalizedApiKey)
                } else {
                    userError = requestError?.let { safeDebridFailureMessage("TorBox", it, normalizedApiKey) }
                    statsError = userError
                }

                val error = userError ?: statsError
                remoteLoaded = anySuccess && error == null
                publish(loading = false, failed = error != null, error = error)
            }
        }
    }

    suspend fun recordStreamedBytes(bytes: Long) {
        if (bytes <= 0L) return
        val nowMs = nowMs()
        stateMutex.withLock {
            val stored = runCatching {
                decodeDailyBytes(DebridSettingsStorage.loadTorboxUsageDailyBytes())
            }.getOrDefault(emptyMap())
            val next = stored.toMutableMap()
            dailyBytes.forEach { (dateKey, value) ->
                next[dateKey] = maxOf(next[dateKey] ?: 0L, value)
            }
            val dateKey = localDate(nowMs).toString()
            next[dateKey] = (next[dateKey] ?: 0L).safeAdd(bytes)
            dailyBytes = pruneDailyBytes(next, nowMs)
            hasLoadedLocal = true
            runCatching {
                DebridSettingsStorage.saveTorboxUsageDailyBytes(encodeDailyBytes(dailyBytes))
            }
            publish()
        }
    }

    private fun parseUser(element: JsonElement): Boolean {
        val user = when (element) {
            is JsonObject -> element
            is JsonArray -> element.firstOrNull() as? JsonObject
            else -> null
        } ?: return false

        email = user["email"]?.stringValueOrNull()?.takeIf { it.isNotBlank() }

        val planElement = user["plan"]
        val planCode = planElement?.intValueOrNull()
        val planName = planElement?.stringValueOrNull()
        when {
            planCode != null -> plan = TorboxPlan.fromCode(planCode)
            !planName.isNullOrBlank() -> TorboxPlan.fromName(planName)?.let { plan = it }
        }

        isSubscribed = user["is_subscribed"]?.booleanValueOrNull()
            ?: user["isSubscribed"]?.booleanValueOrNull()
            ?: false

        expiryEpochMs = timestampMs(user["premium_expires_at"])
            ?: timestampMs(user["subscription_expires_at"])

        cooldownEpochMs = timestampMs(user["cooldown_until"])
            ?: timestampMs(user["cooldownUntil"])
        return true
    }

    private fun parseBandwidthBytes(element: JsonElement): Map<String, Long> {
        val parsed = mutableMapOf<String, Long>()
        val root = element as? JsonObject ?: return emptyMap()
        val buckets = root.firstValue("bandwidth_usage", "bandwidthUsage", "bandwidth", "daily_usage", "usage_by_day")
            ?: return emptyMap()

        fun collect(value: JsonElement, inheritedDate: String? = null) {
            when (value) {
                is JsonArray -> value.forEach { collect(it, inheritedDate) }
                is JsonObject -> {
                    val dateKey = value.firstDate("time_group", "timeGroup", "date", "day", "timestamp", "created_at")
                        ?.toLocalDateKey()
                        ?: inheritedDate
                    val bytes = value.bandwidthBytes()
                    if (dateKey != null && bytes != null) {
                        parsed[dateKey] = (parsed[dateKey] ?: 0L).safeAdd(bytes)
                    } else {
                        value.forEach { (key, child) ->
                            collect(child, parseLocalDate(key)?.toString() ?: inheritedDate)
                        }
                    }
                }
                is JsonPrimitive -> {
                    val bytes = value.longValueOrNull()
                    if (inheritedDate != null && bytes != null) {
                        parsed[inheritedDate] = (parsed[inheritedDate] ?: 0L).safeAdd(bytes)
                    }
                }
                else -> Unit
            }
        }
        collect(buckets)
        return parsed
    }

    private fun publish(
        loading: Boolean = _uiState.value.isLoading,
        failed: Boolean = _uiState.value.loadFailed,
        error: String? = _uiState.value.errorMessage,
    ) {
        val nowMs = nowMs()
        val keys = rollingUsageKeys(nowMs)
        val todayKey = keys.first()

        val todayUsage = combinedUsageFor(todayKey)
        val monthlyUsage = keys.fold(0L) { total, dateKey ->
            total.safeAdd(combinedUsageFor(dateKey))
        }

        _uiState.value = TorboxUsageUiState(
            isLoading = loading,
            email = email,
            plan = plan,
            isSubscribed = isSubscribed,
            expiryEpochMs = expiryEpochMs,
            cooldownEpochMs = cooldownEpochMs,
            daysLeft = expiryEpochMs?.let { daysLeftFrom(it, nowMs) },
            todayDownloadedBytes = todayUsage,
            monthlyDownloadedBytes = monthlyUsage,
            loadFailed = failed,
            errorMessage = error,
        )
    }

    private fun combinedUsageFor(dateKey: String): Long {
        val localBytes = dailyBytes[dateKey] ?: 0L
        val localBytesAtSnapshot = statsLocalBaselineBytes[dateKey] ?: 0L
        val baselineBytes = maxOf(statsBytes[dateKey] ?: 0L, localBytesAtSnapshot)
        val localBytesSinceSnapshot = (localBytes - localBytesAtSnapshot).coerceAtLeast(0L)
        return baselineBytes.safeAdd(localBytesSinceSnapshot)
    }

    private fun rollingUsageKeys(nowMs: Long): List<String> {
        val today = localDate(nowMs)
        return (0 until USAGE_WINDOW_DAYS).map { offset ->
            today.minus(DatePeriod(days = offset)).toString()
        }
    }

    private fun pruneDailyBytes(values: Map<String, Long>, nowMs: Long): Map<String, Long> {
        val today = localDate(nowMs)
        val oldestKey = today.minus(DatePeriod(days = USAGE_STORAGE_DAYS)).toString()
        return values.filterKeys { it in oldestKey..today.toString() }
    }

    private fun decodeDailyBytes(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { DebridApiJson.json.parseToJsonElement(raw) }
            .getOrNull() as? JsonObject
            ?: return emptyMap()
        return root.mapNotNull { (dateKey, value) ->
            if (!IsoDateRegex.matches(dateKey)) return@mapNotNull null
            val bytes = (value as? JsonPrimitive)?.longValueOrNull() ?: return@mapNotNull null
            dateKey to bytes
        }.toMap()
    }

    private fun encodeDailyBytes(values: Map<String, Long>): String =
        buildJsonObject {
            values.entries.sortedBy { it.key }.forEach { (dateKey, bytes) -> put(dateKey, bytes) }
        }.toString()

    private fun daysLeftFrom(expiryMs: Long, nowMs: Long): Long {
        val remaining = expiryMs - nowMs
        if (remaining <= 0L) return 0L
        return ceil(remaining.toDouble() / DAY_MS.toDouble()).toLong()
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    suspend fun refreshUsageWindow() {
        ensureLoaded()
        stateMutex.withLock { publish() }
    }
}

private val IsoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val ZoneSuffixRegex = Regex("""([zZ]|[+-]\d{2}:?\d{2})$""")

private fun torboxResponseError(
    response: DebridApiResponse<TorboxEnvelopeDto<JsonElement>>?,
    exception: Throwable?,
    apiKey: String,
): String? {
    if (response == null) {
        return exception?.let { safeDebridFailureMessage("TorBox", it, apiKey) }
            ?: "TorBox network request failed."
    }
    if (!response.isSuccessful) return response.errorMessage ?: "HTTP ${response.status}"
    val envelope = response.body ?: return response.errorMessage ?: "Unable to parse TorBox response."
    if (envelope.success == false) {
        return safeDebridProviderMessage(envelope.error ?: envelope.detail, apiKey)
            ?: response.errorMessage
            ?: "TorBox rejected the request."
    }
    if (envelope.data == null) return response.errorMessage ?: "TorBox response did not include account data."
    return response.errorMessage
}

private fun localDate(epochMs: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun JsonElement.toLocalDateKey(): String? = stringValueOrNull()?.let(::parseLocalDate)?.toString()

private fun parseLocalDate(raw: String): LocalDate? {
    val value = raw.trim()
    if (IsoDateRegex.matches(value)) return runCatching { LocalDate.parse(value) }.getOrNull()
    val epochMs = parseTimestampMs(value) ?: return null
    return localDate(epochMs)
}

private fun JsonObject.firstValue(vararg keys: String): JsonElement? =
    keys.firstNotNullOfOrNull { this[it] }

private fun JsonObject.firstDate(vararg keys: String): JsonElement? = firstValue(*keys)

private fun JsonObject.bandwidthBytes(): Long? {
    val components = listOf("cdn", "webdav", "zips")
        .mapNotNull { this[it].longValueOrNull() } +
        listOfNotNull(firstValue("uncached_torrents", "uncachedTorrents", "uncached_torrent")?.longValueOrNull())
    if (components.isNotEmpty()) return components.fold(0L) { total, bytes -> total.safeAdd(bytes) }
    return firstValue(
        "total_download",
        "total_downloaded_bandwidth",
        "downloaded_bytes",
        "bytes_downloaded",
        "total_downloaded_bytes",
        "total_bytes",
        "bytes",
        "download",
    )?.longValueOrNull()
}

private fun timestampMs(element: JsonElement?): Long? {
    val primitive = element as? JsonPrimitive ?: return null
    val raw = primitive.content.trim()
    if (raw.isBlank()) return null
    return parseTimestampMs(raw)
}

private fun parseTimestampMs(rawValue: String): Long? {
    val raw = rawValue.trim()
    if (raw.isBlank()) return null

    val numeric = raw.toDoubleOrNull()
    if (numeric != null) {
        if (!numeric.isFinite() || numeric <= 0.0) return null
        val value = numeric.toLong()
        return if (value < 100_000_000_000L) value * 1000L else value
    }

    val candidate = if (IsoDateRegex.matches(raw)) "${raw}T00:00:00Z" else raw.replace(' ', 'T')
    val zoned = if (ZoneSuffixRegex.containsMatchIn(candidate)) candidate else "${candidate}Z"
    return runCatching { Instant.parse(candidate) }
        .recoverCatching { Instant.parse(zoned) }
        .getOrNull()
        ?.toEpochMilliseconds()
}

private fun JsonElement.intValueOrNull(): Int? =
    (this as? JsonPrimitive)?.content?.trim()?.toIntOrNull()

private fun JsonElement.stringValueOrNull(): String? =
    (this as? JsonPrimitive)?.content

private fun JsonElement.booleanValueOrNull(): Boolean? =
    (this as? JsonPrimitive)?.content?.trim()?.toBooleanStrictOrNull()

private fun JsonElement?.longValueOrNull(): Long? {
    val raw = (this as? JsonPrimitive)?.content?.trim() ?: return null
    val number = raw.toDoubleOrNull() ?: return null
    if (!number.isFinite() || number < 0.0) return null
    return number.toLong()
}

private fun Long.safeAdd(delta: Long): Long {
    if (delta <= 0L) return this
    return if (Long.MAX_VALUE - this < delta) Long.MAX_VALUE else this + delta
}
