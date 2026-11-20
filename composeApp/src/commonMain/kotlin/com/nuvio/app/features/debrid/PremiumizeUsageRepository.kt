package com.nuvio.app.features.debrid

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val USAGE_WINDOW_DAYS = 30
private const val USAGE_STORAGE_DAYS = 60
private const val MIN_REFRESH_INTERVAL_MS = 60_000L

internal data class PremiumizeUsageUiState(
    val isLoading: Boolean = false,
    val customerId: String? = null,
    val isActive: Boolean = false,
    val isPremium: Boolean = false,
    val isExpired: Boolean = false,
    val premiumUntilEpochMs: Long? = null,
    val daysLeft: Long? = null,
    val limitUsed: Double? = null,
    val pointsUsed: Int = 0,
    val pointsRemaining: Int = 1000,
    val boosterPoints: Int = 0,
    val todayDownloadedBytes: Long = 0L,
    val monthlyDownloadedBytes: Long = 0L,
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
)

internal object PremiumizeUsageRepository {
    private val _uiState = MutableStateFlow(PremiumizeUsageUiState())
    val uiState: StateFlow<PremiumizeUsageUiState> = _uiState.asStateFlow()

    private val refreshMutex = Mutex()
    private val stateMutex = Mutex()
    private var hasLoadedLocal = false
    private var dailyBytes: Map<String, Long> = emptyMap()

    private var customerId: String? = null
    private var isActive = false
    private var isPremium = false
    private var isExpired = false
    private var premiumUntilEpochMs: Long? = null
    private var limitUsed: Double? = null
    private var pointsUsed = 0
    private var pointsRemaining = 1000
    private var boosterPoints = 0
    private var lastRefreshAtMs = 0L
    private var lastRefreshApiKey: String? = null
    private var remoteLoaded = false

    fun ensureLoaded() {
        if (hasLoadedLocal) return
        val stored = runCatching {
            decodeDailyBytes(DebridSettingsStorage.loadPremiumizeUsageDailyBytes())
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
        customerId = null
        isActive = false
        isPremium = false
        isExpired = false
        premiumUntilEpochMs = null
        limitUsed = null
        pointsUsed = 0
        pointsRemaining = 1000
        boosterPoints = 0
        lastRefreshAtMs = 0L
        lastRefreshApiKey = null
        remoteLoaded = false
        _uiState.value = PremiumizeUsageUiState()
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
                    customerId = null
                    isActive = false
                    isPremium = false
                    isExpired = false
                    premiumUntilEpochMs = null
                    limitUsed = null
                    pointsUsed = 0
                    pointsRemaining = 1000
                    boosterPoints = 0
                    lastRefreshAtMs = 0L
                    remoteLoaded = false
                    publish(loading = false, failed = false, error = null)
                }
            }

            val startedAtMs = nowMs()
            if (!force && remoteLoaded && startedAtMs - lastRefreshAtMs in 0L until MIN_REFRESH_INTERVAL_MS) return
            lastRefreshAtMs = startedAtMs
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(isLoading = true, loadFailed = false, errorMessage = null)
            }

            var requestError: Throwable? = null
            val response = try {
                PremiumizeApiClient.accountInfo(normalizedApiKey)
            } catch (cancelled: CancellationException) {
                stateMutex.withLock { publish(loading = false) }
                throw cancelled
            } catch (error: Exception) {
                requestError = error
                null
            }

            stateMutex.withLock {
                val data = response?.body
                val isSuccess = response?.isSuccessful == true &&
                    data?.status?.trim().equals("success", ignoreCase = true)
                val parsed = isSuccess && data != null && parseAccount(data)
                remoteLoaded = parsed
                val errorMessage = when {
                    response == null -> requestError?.let {
                        safeDebridFailureMessage("Premiumize", it, normalizedApiKey)
                    } ?: "Premiumize network request failed."
                    !response.isSuccessful -> response.errorMessage ?: "HTTP ${response.status}"
                    data == null -> response.errorMessage ?: "Unable to parse Premiumize account response."
                    !isSuccess -> safeDebridProviderMessage(data?.message, normalizedApiKey)
                        ?: data?.status?.takeIf { it.isNotBlank() }
                        ?: "Premiumize did not report a successful account response."
                    !parsed -> "Unable to parse Premiumize account information."
                    else -> null
                }
                publish(loading = false, failed = errorMessage != null, error = errorMessage)
            }
        }
    }

    suspend fun recordStreamedBytes(bytes: Long) {
        if (bytes <= 0L) return
        val nowMs = nowMs()
        stateMutex.withLock {
            val stored = runCatching {
                decodeDailyBytes(DebridSettingsStorage.loadPremiumizeUsageDailyBytes())
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
                DebridSettingsStorage.savePremiumizeUsageDailyBytes(encodeDailyBytes(dailyBytes))
            }
            publish()
        }
    }

    private fun parseAccount(data: PremiumizeAccountInfoDto): Boolean {
        val premiumUntilSeconds = data.premiumUntil
        val now = Clock.System.now()
        val expiryInstant = premiumUntilSeconds?.let { seconds ->
            runCatching { Instant.fromEpochSeconds(seconds) }.getOrNull()
        }
        if (premiumUntilSeconds != null && expiryInstant == null) return false
        val expiryEpochMs = expiryInstant?.let { expiry ->
            runCatching { expiry.toEpochMilliseconds() }.getOrNull()
        }
        if (expiryInstant != null && expiryEpochMs == null) return false

        customerId = data.customerId?.trim()?.takeIf { it.isNotBlank() }
        premiumUntilEpochMs = expiryEpochMs
        isActive = expiryInstant != null && expiryInstant > now
        isPremium = expiryInstant != null
        isExpired = expiryInstant != null && !isActive

        val clampedLimit = data.limitUsed
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0.0, 1.0)
        limitUsed = clampedLimit
        pointsUsed = clampedLimit?.let { (it * 1000.0).roundToInt() } ?: 0
        pointsRemaining = clampedLimit?.let { 1000 - (it * 1000.0).roundToInt() } ?: 1000
        boosterPoints = (data.boosterPoints ?: 0).coerceAtLeast(0)
        return true
    }

    private fun publish(
        loading: Boolean = _uiState.value.isLoading,
        failed: Boolean = _uiState.value.loadFailed,
        error: String? = _uiState.value.errorMessage,
    ) {
        val nowMs = nowMs()
        val keys = rollingUsageKeys(nowMs)
        val todayKey = keys.first()

        val localToday = dailyBytes[todayKey] ?: 0L
        val localMonthly = keys.sumOf { dailyBytes[it] ?: 0L }

        val computedDaysLeft = premiumUntilEpochMs?.let { daysLeftFrom(it, nowMs) }

        _uiState.value = PremiumizeUsageUiState(
            isLoading = loading,
            customerId = customerId,
            isActive = isActive,
            isPremium = isPremium,
            isExpired = isExpired,
            premiumUntilEpochMs = premiumUntilEpochMs,
            daysLeft = computedDaysLeft,
            limitUsed = limitUsed,
            pointsUsed = pointsUsed,
            pointsRemaining = pointsRemaining,
            boosterPoints = boosterPoints,
            todayDownloadedBytes = localToday,
            monthlyDownloadedBytes = localMonthly,
            loadFailed = failed,
            errorMessage = error,
        )
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

private fun localDate(epochMs: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date

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
