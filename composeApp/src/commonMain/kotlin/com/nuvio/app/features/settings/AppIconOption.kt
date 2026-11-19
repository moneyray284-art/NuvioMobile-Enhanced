package com.nuvio.app.features.settings

import com.nuvio.app.isIos

internal enum class AppIconOption(
    val key: String,
    val platformName: String?,
    val iosOnly: Boolean = false,
) {
    ORIGINAL(
        key = "original",
        platformName = null,
    ),
    ARCTIC_BLUE(
        key = "arctic_blue",
        platformName = "AppIconArcticBlue",
    ),
    EMERALD(
        key = "emerald",
        platformName = "AppIconEmerald",
    ),
    ROSE_GOLD(
        key = "rose_gold",
        platformName = "AppIconRoseGold",
    ),
    COPPER(
        key = "copper",
        platformName = "AppIconCopper",
    ),
    GRAPHITE(
        key = "graphite",
        platformName = "AppIconGraphite",
    ),
    SUNSET(
        key = "sunset",
        platformName = "AppIconSunset",
        iosOnly = true,
    ),
    CYBER_MINT(
        key = "cyber_mint",
        platformName = "AppIconCyberMint",
        iosOnly = true,
    ),
    AMETHYST(
        key = "amethyst",
        platformName = "AppIconAmethyst",
        iosOnly = true,
    ),
    SOLAR_FLARE(
        key = "solar_flare",
        platformName = "AppIconSolarFlare",
        iosOnly = true,
    ),
    ;

    val isAvailableOnCurrentPlatform: Boolean
        get() = !iosOnly || isIos

    companion object {
        fun fromPlatformName(name: String?): AppIconOption =
            entries.firstOrNull { it.platformName == name && it.isAvailableOnCurrentPlatform } ?: ORIGINAL
    }
}
