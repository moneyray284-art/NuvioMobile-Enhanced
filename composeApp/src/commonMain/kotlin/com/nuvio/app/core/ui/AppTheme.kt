package com.nuvio.app.core.ui

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_amber
import nuvio.composeapp.generated.resources.theme_arctic_blue
import nuvio.composeapp.generated.resources.theme_crimson
import nuvio.composeapp.generated.resources.theme_custom
import nuvio.composeapp.generated.resources.theme_emerald
import nuvio.composeapp.generated.resources.theme_emerald_aurora
import nuvio.composeapp.generated.resources.theme_gold
import nuvio.composeapp.generated.resources.theme_graphite
import nuvio.composeapp.generated.resources.theme_jade
import nuvio.composeapp.generated.resources.theme_midnight_cyan
import nuvio.composeapp.generated.resources.theme_ocean
import nuvio.composeapp.generated.resources.theme_rose
import nuvio.composeapp.generated.resources.theme_rose_gold
import nuvio.composeapp.generated.resources.theme_royal_amethyst
import nuvio.composeapp.generated.resources.theme_sunset_ember
import nuvio.composeapp.generated.resources.theme_violet
import nuvio.composeapp.generated.resources.theme_white
import org.jetbrains.compose.resources.StringResource

enum class AppTheme {
    MIDNIGHT_CYAN,
    SUNSET_EMBER,
    EMERALD_AURORA,
    ROYAL_AMETHYST,
    GOLD,
    JADE,
    ROSE_GOLD,
    ARCTIC_BLUE,
    GRAPHITE,
    CUSTOM,
    CRIMSON,
    OCEAN,
    VIOLET,
    EMERALD,
    AMBER,
    ROSE,
    WHITE,
}

val AppTheme.isAnimated: Boolean
    get() = when (this) {
        AppTheme.MIDNIGHT_CYAN,
        AppTheme.SUNSET_EMBER,
        AppTheme.EMERALD_AURORA,
        AppTheme.ROYAL_AMETHYST -> true
        else -> false
    }

val AppTheme.labelRes: StringResource
    get() = when (this) {
        AppTheme.MIDNIGHT_CYAN -> Res.string.theme_midnight_cyan
        AppTheme.SUNSET_EMBER -> Res.string.theme_sunset_ember
        AppTheme.EMERALD_AURORA -> Res.string.theme_emerald_aurora
        AppTheme.ROYAL_AMETHYST -> Res.string.theme_royal_amethyst
        AppTheme.GOLD -> Res.string.theme_gold
        AppTheme.JADE -> Res.string.theme_jade
        AppTheme.ROSE_GOLD -> Res.string.theme_rose_gold
        AppTheme.ARCTIC_BLUE -> Res.string.theme_arctic_blue
        AppTheme.GRAPHITE -> Res.string.theme_graphite
        AppTheme.CUSTOM -> Res.string.theme_custom
        AppTheme.CRIMSON -> Res.string.theme_crimson
        AppTheme.OCEAN -> Res.string.theme_ocean
        AppTheme.VIOLET -> Res.string.theme_violet
        AppTheme.EMERALD -> Res.string.theme_emerald
        AppTheme.AMBER -> Res.string.theme_amber
        AppTheme.ROSE -> Res.string.theme_rose
        AppTheme.WHITE -> Res.string.theme_white
    }
