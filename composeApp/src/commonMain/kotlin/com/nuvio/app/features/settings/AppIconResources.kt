package com.nuvio.app.features.settings

import com.nuvio.app.core.ui.AppTheme
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal val AppIconOption.labelResource: StringResource
    get() = when (this) {
        AppIconOption.ORIGINAL -> Res.string.settings_appearance_app_icon_original
        AppIconOption.ARCTIC_BLUE -> Res.string.settings_appearance_app_icon_arctic_blue
        AppIconOption.EMERALD -> Res.string.settings_appearance_app_icon_emerald
        AppIconOption.ROSE_GOLD -> Res.string.settings_appearance_app_icon_rose_gold
        AppIconOption.COPPER -> Res.string.settings_appearance_app_icon_copper
        AppIconOption.GRAPHITE -> Res.string.settings_appearance_app_icon_graphite
        AppIconOption.SUNSET -> Res.string.settings_appearance_app_icon_sunset
        AppIconOption.CYBER_MINT -> Res.string.settings_appearance_app_icon_cyber_mint
        AppIconOption.AMETHYST -> Res.string.settings_appearance_app_icon_amethyst
        AppIconOption.SOLAR_FLARE -> Res.string.settings_appearance_app_icon_solar_flare
    }

internal val AppIconOption.previewResource: DrawableResource
    get() = when (this) {
        AppIconOption.ORIGINAL -> Res.drawable.app_icon_original
        AppIconOption.ARCTIC_BLUE -> Res.drawable.app_icon_arctic_blue
        AppIconOption.EMERALD -> Res.drawable.app_icon_emerald
        AppIconOption.ROSE_GOLD -> Res.drawable.app_icon_rose_gold
        AppIconOption.COPPER -> Res.drawable.app_icon_copper
        AppIconOption.GRAPHITE -> Res.drawable.app_icon_graphite
        AppIconOption.SUNSET -> Res.drawable.app_icon_sunset
        AppIconOption.CYBER_MINT -> Res.drawable.app_icon_cyber_mint
        AppIconOption.AMETHYST -> Res.drawable.app_icon_amethyst
        AppIconOption.SOLAR_FLARE -> Res.drawable.app_icon_solar_flare
    }

internal val AppIconOption.wordmarkResource: DrawableResource
    get() = when (this) {
        AppIconOption.ORIGINAL -> Res.drawable.app_logo_wordmark_original
        AppIconOption.ARCTIC_BLUE -> Res.drawable.app_logo_wordmark_arctic_blue
        AppIconOption.EMERALD -> Res.drawable.app_logo_wordmark_emerald
        AppIconOption.ROSE_GOLD -> Res.drawable.app_logo_wordmark_rose_gold
        AppIconOption.COPPER -> Res.drawable.app_logo_wordmark_copper
        AppIconOption.GRAPHITE -> Res.drawable.app_logo_wordmark_graphite
        AppIconOption.SUNSET -> Res.drawable.app_logo_wordmark_sunset
        AppIconOption.CYBER_MINT -> Res.drawable.app_logo_wordmark_cyber_mint
        AppIconOption.AMETHYST -> Res.drawable.app_logo_wordmark_amethyst
        AppIconOption.SOLAR_FLARE -> Res.drawable.app_logo_wordmark_solar_flare
    }

internal fun AppTheme.wordmarkResource(fallback: AppIconOption): DrawableResource =
    when (this) {
        AppTheme.GOLD -> Res.drawable.app_logo_wordmark_gold
        AppTheme.JADE -> AppIconOption.EMERALD.wordmarkResource
        AppTheme.ROSE_GOLD -> AppIconOption.ROSE_GOLD.wordmarkResource
        AppTheme.ARCTIC_BLUE -> AppIconOption.ARCTIC_BLUE.wordmarkResource
        AppTheme.GRAPHITE -> AppIconOption.GRAPHITE.wordmarkResource
        AppTheme.SUNSET -> AppIconOption.SUNSET.wordmarkResource
        AppTheme.CYBER_MINT -> AppIconOption.CYBER_MINT.wordmarkResource
        AppTheme.AMETHYST -> AppIconOption.AMETHYST.wordmarkResource
        AppTheme.SOLAR_FLARE -> AppIconOption.SOLAR_FLARE.wordmarkResource
        else -> fallback.wordmarkResource
    }
