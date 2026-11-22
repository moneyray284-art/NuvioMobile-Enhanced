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
import nuvio.composeapp.generated.resources.theme_amethyst
import nuvio.composeapp.generated.resources.theme_aurora_veil
import nuvio.composeapp.generated.resources.theme_velvet_eclipse
import nuvio.composeapp.generated.resources.theme_midnight_tide
import nuvio.composeapp.generated.resources.theme_titanium_mist
import nuvio.composeapp.generated.resources.theme_obsidian_bronze
import nuvio.composeapp.generated.resources.theme_nordic_pine
import nuvio.composeapp.generated.resources.theme_dusk_slate
import nuvio.composeapp.generated.resources.theme_smoked_merlot
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
    AMETHYST,
    AURORA_VEIL,
    VELVET_ECLIPSE,
    MIDNIGHT_TIDE,
    TITANIUM_MIST,
    OBSIDIAN_BRONZE,
    NORDIC_PINE,
    DUSK_SLATE,
    SMOKED_MERLOT,
}

val AppTheme.isGradientTheme: Boolean
    get() = when (this) {
        AppTheme.SUNSET,
        AppTheme.AMETHYST,
        AppTheme.AURORA_VEIL,
        AppTheme.VELVET_ECLIPSE,
        AppTheme.MIDNIGHT_TIDE,
        AppTheme.TITANIUM_MIST,
        AppTheme.OBSIDIAN_BRONZE,
        AppTheme.NORDIC_PINE,
        AppTheme.DUSK_SLATE,
        AppTheme.SMOKED_MERLOT -> true
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
        AppTheme.AMETHYST -> Res.string.theme_amethyst
        AppTheme.AURORA_VEIL -> Res.string.theme_aurora_veil
        AppTheme.VELVET_ECLIPSE -> Res.string.theme_velvet_eclipse
        AppTheme.MIDNIGHT_TIDE -> Res.string.theme_midnight_tide
        AppTheme.TITANIUM_MIST -> Res.string.theme_titanium_mist
        AppTheme.OBSIDIAN_BRONZE -> Res.string.theme_obsidian_bronze
        AppTheme.NORDIC_PINE -> Res.string.theme_nordic_pine
        AppTheme.DUSK_SLATE -> Res.string.theme_dusk_slate
        AppTheme.SMOKED_MERLOT -> Res.string.theme_smoked_merlot
    }
