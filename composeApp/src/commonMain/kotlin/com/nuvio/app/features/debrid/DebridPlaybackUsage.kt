package com.nuvio.app.features.debrid

internal enum class DebridUsageProvider(val providerId: String) {
    TORBOX(DebridProviders.TORBOX_ID),
    PREMIUMIZE(DebridProviders.PREMIUMIZE_ID),
}

internal fun debridUsageProviderFromUrl(url: String?): DebridUsageProvider? {
    val value = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val authority = value.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#')
    if (authority.isBlank()) return null
    val host = authority.substringAfterLast('@').let { rawHost ->
        if (rawHost.startsWith("[")) rawHost.substringAfter('[').substringBefore(']')
        else rawHost.substringBefore(':')
    }.lowercase()
    return when {
        host.contains("torbox") || host.contains("tb-cdn") -> DebridUsageProvider.TORBOX
        host.contains("premiumize.me") || host.contains("energycdn") -> DebridUsageProvider.PREMIUMIZE
        else -> null
    }
}

internal fun debridUsageProviderFromHint(providerId: String?): DebridUsageProvider? = when {
    providerId.equals(DebridProviders.TORBOX_ID, ignoreCase = true) -> DebridUsageProvider.TORBOX
    providerId.equals(DebridProviders.PREMIUMIZE_ID, ignoreCase = true) -> DebridUsageProvider.PREMIUMIZE
    else -> null
}

internal fun resolveDebridUsageProvider(
    url: String?,
    preferredProviderId: String?,
): DebridUsageProvider? =
    debridUsageProviderFromUrl(url) ?: debridUsageProviderFromHint(preferredProviderId)
