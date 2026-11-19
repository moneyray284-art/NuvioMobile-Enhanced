package com.nuvio.app.features.debrid

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ln
import kotlin.math.pow

internal enum class TorboxPlan(
    val displayName: String,
    val allowanceBytes: Long,
) {
    FREE("Free", 200L * 1024 * 1024 * 1024),
    ESSENTIAL("Essential", 5L * 1024 * 1024 * 1024 * 1024),
    STANDARD("Standard", 10L * 1024 * 1024 * 1024 * 1024),
    PRO("Pro", 30L * 1024 * 1024 * 1024 * 1024);

    companion object {
        fun fromCode(code: Int): TorboxPlan = when (code) {
            1 -> ESSENTIAL
            2 -> PRO
            3 -> STANDARD
            else -> FREE
        }
    }
}

internal data class TorboxAccountUsage(
    val email: String,
    val plan: TorboxPlan,
    val isSubscribed: Boolean,
    val premiumExpiresAt: String?,
    val daysUntilRenewal: Long?,
    val cooldownUntil: String?,
    val todayDownloadedBytes: Long?,
    val monthlyDownloadedBytes: Long?,
    val totalDownloadedBytes: Long,
) {
    val monthlyUsageRatio: Float?
        get() = monthlyDownloadedBytes?.let {
            it.toFloat() / plan.allowanceBytes.coerceAtLeast(1L).toFloat()
        }
}

internal object TorboxUsageRepository {
    suspend fun fetchAccountUsage(apiKey: String): TorboxAccountUsage {
        val normalizedApiKey = apiKey.trim().takeIf { it.isNotEmpty() }
            ?: error("TorBox account is not linked.")

        return coroutineScope {
            val userDeferred = async {
                TorboxApiClient.getUserMe(normalizedApiKey).requireData("Unable to load TorBox account.")
            }
            val statsDeferred = async {
                try {
                    TorboxApiClient.getUserStats(normalizedApiKey)
                        .takeIf { it.isSuccessful }
                        ?.body
                        ?.takeIf { it.success != false }
                        ?.data
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }

            val user = userDeferred.await()
            val stats = statsDeferred.await()
            val now = Clock.System.now()
            val todayUtc = now.toLocalDateTime(TimeZone.UTC).date.toString()
            val buckets = stats?.bandwidthUsage.orEmpty()
            val todayBytes = stats?.let {
                buckets.asSequence()
                    .filter { bucket -> bucket.timeGroup.startsWith(todayUtc) }
                    .sumOf(TorboxBandwidthUsageBucketDto::downloadedBytes)
            }
            val monthlyBytes = stats?.let {
                if (buckets.isNotEmpty()) {
                    buckets.sumOf(TorboxBandwidthUsageBucketDto::downloadedBytes)
                } else {
                    it.totalDownloadedBandwidth
                }
            }
            val daysUntilRenewal = if (user.isSubscribed) {
                user.premiumExpiresAt
                    ?.let { expiry -> runCatching { Instant.parse(expiry) }.getOrNull() }
                    ?.let { expiry -> ((expiry - now).inWholeDays).coerceAtLeast(0L) }
            } else {
                null
            }

            TorboxAccountUsage(
                email = user.email,
                plan = TorboxPlan.fromCode(user.plan),
                isSubscribed = user.isSubscribed,
                premiumExpiresAt = user.premiumExpiresAt,
                daysUntilRenewal = daysUntilRenewal,
                cooldownUntil = user.cooldownUntil,
                todayDownloadedBytes = todayBytes,
                monthlyDownloadedBytes = monthlyBytes,
                totalDownloadedBytes = user.totalDownloaded,
            )
        }
    }
}

private fun <T> DebridApiResponse<TorboxEnvelopeDto<T>>.requireData(fallback: String): T {
    val envelope = body
    if (!isSuccessful || envelope == null || envelope.success == false) {
        error(envelope?.detail ?: envelope?.error ?: "$fallback (HTTP $status).")
    }
    return envelope.data ?: error(envelope.detail ?: envelope.error ?: fallback)
}

internal fun formatTorboxBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB", "PB")
    val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(exponent)
    val formatted = when {
        value >= 100 -> value.toInt().toString()
        value >= 10 -> ((value * 10).toInt() / 10.0).toString()
        else -> ((value * 100).toInt() / 100.0).toString()
    }
    return "$formatted ${units[exponent]}"
}
