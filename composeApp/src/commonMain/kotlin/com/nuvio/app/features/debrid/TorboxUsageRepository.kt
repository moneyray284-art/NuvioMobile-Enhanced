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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.parseToJsonElement
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
    val plan: TorboxPlan? = null,
    val isSubscribed: Boolean = false,
    val expiryEpochMs: Long? = null,
    val cooldownEpochMs: Long? = null,
    val daysLeft: Long? = null,
    val todayDownloadedBytes: Long = 0L,
    val monthlyDownloadedBytes: Long = 0L,
    val loadFailed: Boolean = false,
)

internal object TorboxUsageRepository {
    private val _uiState = MutableStateFlow(TorboxUsageUiState())
    val uiState: StateFlow<TorboxUsageUiState> = _uiState.asStateFlow()

    private val refreshMutex = Mutex()
    private val stateMutex = Mutex()
    private var hasLoadedLocal = false
    private var dailyBytes: Map<String, Long> = emptyMap()
    private var statsBytes: Map<String, Long> = emptyMap()

    private var plan: TorboxPlan? = null
    private var isSubscribed = false
    private var expiryEpochMs: Long? = null
    private var cooldownEpochMs: Long? = null
    private var lastRefreshAtMs = 0L
    private var remoteLoaded = false

    fun ensureLoaded() {
        if (hasLoadedLocal) return
        hasLoadedLocal = true
        dailyBytes = decodeDailyBytes(DebridSettingsStorage.loadTorboxUsageDailyBytes())
        publish()
    }

    fun onProfileChanged() {
        hasLoadedLocal = false
        dailyBytes = emptyMap()
        statsBytes = emptyMap()
        plan = null
        isSubscribed = false
        expiryEpochMs = null
        cooldownEpochMs = null
        lastRefreshAtMs = 0L
        remoteLoaded = false
        _uiState.value = TorboxUsageUiState()
        ensureLoaded()
    }

    suspend fun refresh(apiKey: String, force: Boolean = false) {
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isBlank()) return
        ensureLoaded()

        if (!force && remoteLoaded && nowMs() - lastRefreshAtMs in 0L until MIN_REFRESH_INTERVAL_MS) return

        refreshMutex.withLock {
            val startedAtMs = nowMs()
            if (!force && remoteLoaded && startedAtMs - lastRefreshAtMs in 0L until MIN_REFRESH_INTERVAL_MS) return
            lastRefreshAtMs = startedAtMs
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Network calls happen outside the state lock so usage recording never waits on them.
            val responses = try {
                coroutineScope {
                    val user = async { TorboxApiClient.fetchUser(normalizedApiKey) }
                    val stats = async { TorboxApiClient.fetchUserStats(normalizedApiKey) }
                    user.await() to stats.await()
                }
            } catch (error: CancellationException) {
                stateMutex.withLock { publish(loading = false) }
                throw error
            } catch (_: Exception) {
                null
            }

            stateMutex.withLock {
                var anySuccess = false
                if (responses != null) {
                    val (userResponse, statsResponse) = responses
                    val userEnvelope = userResponse.body
                        ?.takeIf { userResponse.isSuccessful && it.success != false }
                    val statsEnvelope = statsResponse.body
                        ?.takeIf { statsResponse.isSuccessful && it.success != false }

                    userEnvelope?.data?.let { data ->
                        parseUser(data)
                        anySuccess = true
                    }
                    statsEnvelope?.data?.let { data ->
                        statsBytes = parseBandwidthBytes(data)
                        anySuccess = true
                    }
                }

                if (anySuccess) {
                    remoteLoaded = true
                }
                publish(loading = false, failed = !anySuccess)
            }
        }
    }

    suspend fun recordStreamedBytes(bytes: Long) {
        if (bytes <= 0L) return
        ensureLoaded()
        val nowMs = nowMs()
        stateMutex.withLock {
            val dateKey = utcDateKey(nowMs)
            val next = dailyBytes.toMutableMap()
            next[dateKey] = (next[dateKey] ?: 0L).safeAdd(bytes)
            dailyBytes = pruneDailyBytes(next, nowMs)
            DebridSettingsStorage.saveTorboxUsageDailyBytes(encodeDailyBytes(dailyBytes))
            publish()
        }
    }

    private fun parseUser(element: JsonElement) {
        val user = when (element) {
            is JsonObject -> element
            is JsonArray -> element.firstOrNull() as? JsonObject
            else -> null
        } ?: return

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
    }

    private fun parseBandwidthBytes(element: JsonElement): Map<String, Long> {
        val stats = element as? JsonObject ?: return emptyMap()
        val rows = stats["bandwidth"] as? JsonArray ?: return emptyMap()
        val parsed = mutableMapOf<String, Long>()
        rows.forEach { row ->
            val rowObject = row as? JsonObject ?: return@forEach
            val dateKey = rowObject["date"]?.toUtcDateKey() ?: return@forEach
            val bytes = rowObject["bytes_downloaded"]?.longValueOrNull() ?: return@forEach
            parsed[dateKey] = maxOf(parsed[dateKey] ?: 0L, bytes)
        }
        return parsed
    }

    private fun publish(
        loading: Boolean = _uiState.value.isLoading,
        failed: Boolean = _uiState.value.loadFailed,
    ) {
        val nowMs = nowMs()
        val keys = rollingUsageKeys(nowMs)
        val todayKey = keys.first()

        val localToday = dailyBytes[todayKey] ?: 0L
        val localMonthly = keys.sumOf { dailyBytes[it] ?: 0L }
        val remoteToday = statsBytes[todayKey] ?: 0L
        val remoteMonthly = keys.sumOf { statsBytes[it] ?: 0L }

        _uiState.value = TorboxUsageUiState(
            isLoading = loading,
            plan = plan,
            isSubscribed = isSubscribed,
            expiryEpochMs = expiryEpochMs,
            cooldownEpochMs = cooldownEpochMs,
            daysLeft = expiryEpochMs?.let { daysLeftFrom(it, nowMs) },
            todayDownloadedBytes = maxOf(localToday, remoteToday),
            monthlyDownloadedBytes = maxOf(localMonthly, remoteMonthly),
            loadFailed = failed,
        )
    }

    private fun rollingUsageKeys(nowMs: Long): List<String> =
        (0 until USAGE_WINDOW_DAYS).map { offset -> utcDateKey(nowMs - offset * DAY_MS) }

    private fun pruneDailyBytes(values: Map<String, Long>, nowMs: Long): Map<String, Long> {
        val oldestKey = utcDateKey(nowMs - USAGE_STORAGE_DAYS * DAY_MS)
        return values.filterKeys { it >= oldestKey }
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
}

private val IsoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val ZoneSuffixRegex = Regex("""([zZ]|[+-]\d{2}:?\d{2})$""")

private fun utcDateKey(epochMs: Long): String =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC).date.toString()

private fun JsonElement.toUtcDateKey(): String? {
    val raw = stringValueOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (IsoDateRegex.matches(raw)) return raw
    val epochMs = parseTimestampMs(raw) ?: return null
    return utcDateKey(epochMs)
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

private fun JsonElement.longValueOrNull(): Long? {
    val raw = (this as? JsonPrimitive)?.content?.trim() ?: return null
    val number = raw.toDoubleOrNull() ?: return null
    if (!number.isFinite() || number < 0.0) return null
    return number.toLong()
}

private fun Long.safeAdd(delta: Long): Long {
    if (delta <= 0L) return this
    return if (Long.MAX_VALUE - this < delta) Long.MAX_VALUE else this + delta
}
