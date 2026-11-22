package com.nuvio.app.core.ui

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_amber
import nuvio.composeapp.generated.resources.theme_crimson
import nuvio.composeapp.generated.resources.theme_emerald
import nuvio.composeapp.generated.resources.theme_ocean
import nuvio.composeapp.generated.resources.theme_rose
import nuvio.composeapp.generated.resources.theme_violet
import nuvio.composeapp.generated.resources.theme_white
import nuvio.composeapp.generated.resources.theme_gold
import nuvio.composeapp.generated.resources.theme_jade
import nuvio.composeapp.generated.resources.theme_rose_gold
import nuvio.composeapp.generated.resources.theme_arctic_blue
import nuvio.composeapp.generated.resources.theme_graphite
import nuvio.composeapp.generated.resources.theme_custom
import nuvio.composeapp.generated.resources.theme_sunset
import nuvio.composeapp.generated.resources.theme_cyber_mint
import nuvio.composeapp.generated.resources.theme_amethyst
import nuvio.composeapp.generated.resources.theme_solar_flare
import nuvio.composeapp.generated.resources.theme_aurora_veil
import nuvio.composeapp.generated.resources.theme_velvet_eclipse
import nuvio.composeapp.generated.resources.theme_midnight_tide
import org.jetbrains.compose.resources.StringResource

enum class AppTheme {
    GOLD,
    JADE,
    ROSE_GOLD,
    ARCTIC_BLUE,
    GRAPHITE,
    CUSTOM,
    WHITE,
    CRIMSON,
    OCEAN,
    VIOLET,
    EMERALD,
    AMBER,
    ROSE,
    SUNSET,
    CYBER_MINT,
    AMETHYST,
    SOLAR_FLARE,
    AURORA_VEIL,
    VELVET_ECLIPSE,
    MIDNIGHT_TIDE,
}

val AppTheme.isGradientTheme: Boolean
    get() = when (this) {
        AppTheme.SUNSET,
        AppTheme.CYBER_MINT,
        AppTheme.AMETHYST,
        AppTheme.SOLAR_FLARE,
        AppTheme.AURORA_VEIL,
        AppTheme.VELVET_ECLIPSE,
        AppTheme.MIDNIGHT_TIDE -> true
        else -> false
    }

val AppTheme.labelRes: StringResource
    get() = when (this) {
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
        AppTheme.SUNSET -> Res.string.theme_sunset
        AppTheme.CYBER_MINT -> Res.string.theme_cyber_mint
        AppTheme.AMETHYST -> Res.string.theme_amethyst
        AppTheme.SOLAR_FLARE -> Res.string.theme_solar_flare
        AppTheme.AURORA_VEIL -> Res.string.theme_aurora_veil
        AppTheme.VELVET_ECLIPSE -> Res.string.theme_velvet_eclipse
        AppTheme.MIDNIGHT_TIDE -> Res.string.theme_midnight_tide
    }
