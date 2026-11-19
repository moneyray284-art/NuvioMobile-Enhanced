package com.nuvio.app.features.debrid

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Instant

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
    val planCode: Int,
    val isSubscribed: Boolean,
    val premiumExpiresAt: String?,
    val hasFuturePremiumExpiry: Boolean,
    val daysUntilRenewal: Long?,
    val cooldownUntil: String?,
    val todayDownloadedBytes: Long?,
    val monthlyDownloadedBytes: Long?,
    val totalDownloadedBytes: Long?,
    val totalDownloads: Long,
    val usageErrorMessage: String?,
) {
    val planDisplayName: String
        get() = when {
            planCode == 0 && hasFuturePremiumExpiry -> "Active"
            planCode > 0 && plan == TorboxPlan.FREE -> "Plan $planCode"
            else -> plan.displayName
        }

    val isActivePlan: Boolean
        get() = planCode > 0 || hasFuturePremiumExpiry

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
                TorboxApiClient.getUserMe(normalizedApiKey)
                    .requireData("Unable to load TorBox account.")
            }
            val statsDeferred = async { fetchStats(normalizedApiKey) }

            val user = userDeferred.await()
            val statsResult = statsDeferred.await()
            val now = Clock.System.now()
            val todayUtc = now.toLocalDateTime(TimeZone.UTC).date.toString()
            val parsedStats = statsResult.stats?.toUsage(todayUtc)
            val needsCloudFallback = parsedStats?.let {
                it.todayBytes == null || it.monthlyBytes == null || it.totalBytes == null
            } ?: true
            val cloudFallback = if (needsCloudFallback) {
                fetchCloudFallback(normalizedApiKey, now, todayUtc)
            } else {
                null
            }

            val premiumExpiry = user.premiumExpiresAt?.let(::parseInstant)
            val hasFutureExpiry = premiumExpiry?.let { it > now } == true
            val daysUntilRenewal = premiumExpiry
                ?.takeIf { it > now }
                ?.let { expiry ->
                    ((expiry.toEpochMilliseconds() - now.toEpochMilliseconds()) / MILLIS_PER_DAY)
                        .coerceAtLeast(0L)
                }
            val usageErrors = listOfNotNull(
                statsResult.errorMessage,
                parsedStats?.errorMessage,
                cloudFallback?.errorMessage,
            ).distinct()

            TorboxAccountUsage(
                email = user.email,
                plan = TorboxPlan.fromCode(user.plan),
                planCode = user.plan,
                isSubscribed = user.isSubscribed,
                premiumExpiresAt = user.premiumExpiresAt,
                hasFuturePremiumExpiry = hasFutureExpiry,
                daysUntilRenewal = daysUntilRenewal,
                cooldownUntil = user.cooldownUntil,
                todayDownloadedBytes = parsedStats?.todayBytes ?: cloudFallback?.todayBytes,
                monthlyDownloadedBytes = parsedStats?.monthlyBytes ?: cloudFallback?.monthlyBytes,
                totalDownloadedBytes = parsedStats?.totalBytes ?: cloudFallback?.totalBytes,
                totalDownloads = user.totalDownloaded,
                usageErrorMessage = usageErrors.takeIf { it.isNotEmpty() }?.joinToString(" "),
            )
        }
    }

    private suspend fun fetchStats(apiKey: String): StatsFetchResult = try {
        val response = TorboxApiClient.getUserStats(apiKey)
        val stats = response.requireData("Unable to load TorBox usage statistics.")
        StatsFetchResult(stats = stats)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        StatsFetchResult(
            errorMessage = error.message?.takeIf(String::isNotBlank)
                ?: "TorBox usage statistics could not be loaded.",
        )
    }

    private suspend fun fetchCloudFallback(
        apiKey: String,
        now: Instant,
        todayUtc: String,
    ): CloudFallbackResult = coroutineScope {
        val torrentsDeferred = async { fetchCloudList("torrents") { TorboxApiClient.listCloudTorrents(apiKey) } }
        val usenetDeferred = async { fetchCloudList("usenet") { TorboxApiClient.listCloudUsenet(apiKey) } }
        val webDownloadsDeferred = async {
            fetchCloudList("web downloads") { TorboxApiClient.listCloudWebDownloads(apiKey) }
        }
        val results = listOf(
            torrentsDeferred.await(),
            usenetDeferred.await(),
            webDownloadsDeferred.await(),
        )
        val items = results.flatMap { it.items.orEmpty() }
        val hasAnySuccessfulList = results.any { it.items != null }
        val cutoffMillis = now.toEpochMilliseconds() - THIRTY_DAYS_MILLIS
        val sizedItems = items.mapNotNull { item ->
            item.byteSize()?.let { size -> item to size }
        }
        val datedItems = sizedItems.mapNotNull { (item, size) ->
            item.createdAt.asInstantOrNull()?.let { createdAt ->
                DatedCloudSize(size = size, createdAt = createdAt)
            }
        }
        val errors = results.mapNotNull(CloudListResult::errorMessage)

        CloudFallbackResult(
            todayBytes = if (hasAnySuccessfulList && datedItems.isNotEmpty()) {
                datedItems.asSequence()
                    .filter { it.createdAt.toLocalDateTime(TimeZone.UTC).date.toString() == todayUtc }
                    .sumOf(DatedCloudSize::size)
            } else {
                null
            },
            monthlyBytes = if (hasAnySuccessfulList && datedItems.isNotEmpty()) {
                datedItems.asSequence()
                    .filter {
                        it.createdAt.toEpochMilliseconds() in cutoffMillis..now.toEpochMilliseconds()
                    }
                    .sumOf(DatedCloudSize::size)
            } else {
                null
            },
            totalBytes = if (hasAnySuccessfulList && sizedItems.isNotEmpty()) {
                sizedItems.sumOf { (_, size) -> size }
            } else {
                null
            },
            errorMessage = errors.takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "Cloud history fallback failed: ", separator = "; "),
        )
    }

    private suspend fun fetchCloudList(
        listName: String,
        fetch: suspend () -> DebridApiResponse<TorboxEnvelopeDto<List<TorboxCloudItemDto>>>,
    ): CloudListResult = try {
        CloudListResult(items = fetch().requireData("Unable to load TorBox $listName history."))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        CloudListResult(
            errorMessage = error.message?.takeIf(String::isNotBlank)
                ?: "Unable to load TorBox $listName history.",
        )
    }
}

private data class StatsFetchResult(
    val stats: TorboxUserStatsDto? = null,
    val errorMessage: String? = null,
)

private data class ParsedStatsUsage(
    val todayBytes: Long?,
    val monthlyBytes: Long?,
    val totalBytes: Long?,
    val errorMessage: String? = null,
)

private data class CloudListResult(
    val items: List<TorboxCloudItemDto>? = null,
    val errorMessage: String? = null,
)

private data class CloudFallbackResult(
    val todayBytes: Long?,
    val monthlyBytes: Long?,
    val totalBytes: Long?,
    val errorMessage: String?,
)

private data class BandwidthBucket(
    val dateUtc: String?,
    val bytes: Long,
)

private data class DatedCloudSize(
    val size: Long,
    val createdAt: Instant,
)

private fun TorboxUserStatsDto.toUsage(todayUtc: String): ParsedStatsUsage {
    val buckets = parseBandwidthBuckets(bandwidthUsage)
    val datedBuckets = buckets.filter { it.dateUtc != null }
    val validBucketBytes = buckets.takeIf { it.isNotEmpty() }?.sumOf(BandwidthBucket::bytes)
    val todayBytes = if (buckets.isNotEmpty() && datedBuckets.size == buckets.size) {
        datedBuckets.asSequence()
            .filter { it.dateUtc == todayUtc }
            .sumOf(BandwidthBucket::bytes)
    } else {
        null
    }
    val monthlyBytes = validBucketBytes
        ?: totalDownloadedBandwidth.asBytesOrNull()
    val totalBytes = totalDownload.asBytesOrNull()
        ?: totalDownloadedBandwidth.asBytesOrNull()

    return ParsedStatsUsage(
        todayBytes = todayBytes,
        monthlyBytes = monthlyBytes,
        totalBytes = totalBytes,
        errorMessage = if (buckets.isEmpty() && monthlyBytes == null && totalBytes == null) {
            "TorBox returned no recognized byte usage fields."
        } else {
            null
        },
    )
}

private fun parseBandwidthBuckets(element: JsonElement?): List<BandwidthBucket> =
    when (element) {
        is JsonArray -> element.flatMap { parseBandwidthBuckets(it, fallbackDateUtc = null) }
        is JsonObject -> parseBandwidthBuckets(element, fallbackDateUtc = null)
        else -> emptyList()
    }

private fun parseBandwidthBuckets(
    element: JsonElement,
    fallbackDateUtc: String?,
): List<BandwidthBucket> = when (element) {
    is JsonArray -> element.flatMap { parseBandwidthBuckets(it, fallbackDateUtc) }
    is JsonObject -> {
        val bytes = element.readBucketBytes()
        if (bytes != null) {
            listOf(BandwidthBucket(dateUtc = element.readBucketDateUtc() ?: fallbackDateUtc, bytes = bytes))
        } else {
            element.entries.flatMap { (key, value) ->
                parseBandwidthBuckets(value, parseUtcDate(key) ?: fallbackDateUtc)
            }
        }
    }
    is JsonPrimitive -> {
        val bytes = element.asBytesOrNull()
        val date = fallbackDateUtc
        if (bytes != null && date != null) listOf(BandwidthBucket(dateUtc = date, bytes = bytes)) else emptyList()
    }
    else -> emptyList()
}

private fun JsonObject.readBucketBytes(): Long? {
    val categoryKeys = listOf(
        "cdn",
        "storage",
        "webdav",
        "zips",
        "uncached_torrents",
        "uncached_torrent",
        "seeding",
        "cloud_uploads",
        "third_party_clouds",
        "upload",
    )
    val categoryValues = categoryKeys.mapNotNull { key -> this[key].asBytesOrNull() }
    if (categoryValues.isNotEmpty()) return categoryValues.sum()

    val directByteKeys = listOf(
        "total_download",
        "total_downloaded_bandwidth",
        "downloaded_bytes",
        "bytes_downloaded",
        "total_downloaded_bytes",
        "total_bytes",
        "bytes",
        "download",
    )
    return directByteKeys.firstNotNullOfOrNull { key -> this[key].asBytesOrNull() }
}

private fun JsonObject.readBucketDateUtc(): String? {
    val dateKeys = listOf("time_group", "date", "day", "timestamp", "created_at", "time")
    return dateKeys.firstNotNullOfOrNull { key -> this[key].asDateUtcOrNull() }
}

private fun JsonElement?.asBytesOrNull(): Long? = when (this) {
    is JsonPrimitive -> asBytesOrNull()
    is JsonObject -> readBucketBytes()
    else -> null
}

private fun JsonPrimitive.asBytesOrNull(): Long? {
    if (content == "null" || content.isBlank()) return null
    return (longOrNull ?: content.toDoubleOrNull()?.toLong())?.takeIf { it >= 0L }
}

private fun JsonElement?.asDateUtcOrNull(): String? = when (this) {
    is JsonPrimitive -> parseUtcDate(content)
    else -> null
}

private fun JsonElement?.asInstantOrNull(): Instant? = when (this) {
    is JsonPrimitive -> parseInstant(content)
    else -> null
}

private fun TorboxCloudItemDto.byteSize(): Long? = (size ?: totalSize)?.takeIf { it >= 0L }

private fun parseInstant(value: String): Instant? {
    val text = value.trim()
    if (text.isEmpty()) return null
    val parsed = runCatching { Instant.parse(text) }.getOrNull()
    if (parsed != null) return parsed

    val numericTime = text.toLongOrNull() ?: return null
    val millis = when {
        numericTime >= MILLIS_TIMESTAMP_THRESHOLD -> numericTime
        numericTime >= SECONDS_TIMESTAMP_THRESHOLD -> numericTime * 1000L
        else -> return null
    }
    return runCatching { Instant.fromEpochMilliseconds(millis) }.getOrNull()
}

private fun parseUtcDate(value: String?): String? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val instant = parseInstant(text)
    if (instant != null) return instant.toLocalDateTime(TimeZone.UTC).date.toString()
    return text.take(10).takeIf { candidate ->
        candidate.length == 10 && candidate[4] == '-' && candidate[7] == '-'
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

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val THIRTY_DAYS_MILLIS = 30L * MILLIS_PER_DAY
private const val SECONDS_TIMESTAMP_THRESHOLD = 1_000_000_000L
private const val MILLIS_TIMESTAMP_THRESHOLD = 100_000_000_000L
