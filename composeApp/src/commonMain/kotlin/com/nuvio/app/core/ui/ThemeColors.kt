package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

object ThemeColors {

    val Gold = ThemeColorPalette(
        secondary = Color(0xFFE8A91C),
        secondaryVariant = Color(0xFF9A6200),
        accentGradient = listOf(
            Color(0xFF8A5700),
            Color(0xFFE8A91C),
            Color(0xFFFFF1A8),
            Color(0xFFFFD45C),
            Color(0xFF9A6200),
        ),
        nativeAccentHex = "#FFD45C",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFD45C),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0E0B),
        backgroundElevated = Color(0xFF1D1A14),
        backgroundCard = Color(0xFF262116),
    )

    val Jade = ThemeColorPalette(
        secondary = Color(0xFF22D37C),
        secondaryVariant = Color(0xFF0BBF9A),
        accentGradient = listOf(Color(0xFF7BF08D), Color(0xFF22D37C), Color(0xFF0BBF9A)),
        nativeAccentHex = "#7BF08D",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFF7BF08D),
        focusBackground = Color(0xFF153A2C),
        background = Color(0xFF0B0F0D),
        backgroundElevated = Color(0xFF141D18),
        backgroundCard = Color(0xFF16251D),
    )

    val RoseGold = ThemeColorPalette(
        secondary = Color(0xFFEC70A9),
        secondaryVariant = Color(0xFFB75AFF),
        accentGradient = listOf(Color(0xFFB75AFF), Color(0xFFEC70A9), Color(0xFFFFB37A)),
        nativeAccentHex = "#FFB37A",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFFB37A),
        focusBackground = Color(0xFF442037),
        background = Color(0xFF100C0F),
        backgroundElevated = Color(0xFF1F161D),
        backgroundCard = Color(0xFF281A24),
    )

    val ArcticBlue = ThemeColorPalette(
        secondary = Color(0xFF3185F5),
        secondaryVariant = Color(0xFF4D55E8),
        accentGradient = listOf(Color(0xFF4DE3FF), Color(0xFF3185F5), Color(0xFF4D55E8)),
        nativeAccentHex = "#4DE3FF",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF4DE3FF),
        focusBackground = Color(0xFF172844),
        background = Color(0xFF0B0E14),
        backgroundElevated = Color(0xFF141A24),
        backgroundCard = Color(0xFF161E2A),
    )

    val Graphite = ThemeColorPalette(
        secondary = Color(0xFFAAB2BE),
        secondaryVariant = Color(0xFF687381),
        accentGradient = listOf(Color(0xFFF3F5F7), Color(0xFFAAB2BE), Color(0xFF687381)),
        nativeAccentHex = "#F3F5F7",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFFFFFFFF),
        focusRing = Color(0xFFF3F5F7),
        focusBackground = Color(0xFF30343A),
        background = Color(0xFF0C0D0F),
        backgroundElevated = Color(0xFF17191D),
        backgroundCard = Color(0xFF20242A),
    )

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFE53935),
        secondaryVariant = Color(0xFFC62828),
        nativeAccentHex = "#E53935",
        focusRing = Color(0xFFFF5252),
        focusBackground = Color(0xFF3D1A1A),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1A),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF1E88E5),
        secondaryVariant = Color(0xFF1565C0),
        nativeAccentHex = "#1E88E5",
        focusRing = Color(0xFF42A5F5),
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFF8E24AA),
        secondaryVariant = Color(0xFF6A1B9A),
        nativeAccentHex = "#8E24AA",
        focusRing = Color(0xFFAB47BC),
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF43A047),
        secondaryVariant = Color(0xFF2E7D32),
        nativeAccentHex = "#43A047",
        focusRing = Color(0xFF66BB6A),
        focusBackground = Color(0xFF1A3D1E),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF1A241A),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFB8C00),
        secondaryVariant = Color(0xFFEF6C00),
        nativeAccentHex = "#FB8C00",
        focusRing = Color(0xFFFFA726),
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFD81B60),
        secondaryVariant = Color(0xFFC2185B),
        nativeAccentHex = "#D81B60",
        focusRing = Color(0xFFEC407A),
        focusBackground = Color(0xFF3D1A2D),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF241A1F),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )

    val Sunset = ThemeColorPalette(
        secondary = Color(0xFFEC4899),
        secondaryVariant = Color(0xFFF97316),
        accentGradient = listOf(
            Color(0xFFF43F5E),
            Color(0xFFF97316),
            Color(0xFFEC4899),
            Color(0xFFF43F5E),
        ),
        nativeAccentHex = "#EC4899",
        focusRing = Color(0xFFEC4899),
        focusBackground = Color(0xFF3D1A2A),
        background = Color(0xFF100B0E),
        backgroundElevated = Color(0xFF1E141B),
        backgroundCard = Color(0xFF261822),
    )

    val Amethyst = ThemeColorPalette(
        secondary = Color(0xFFA855F7),
        secondaryVariant = Color(0xFFEC4899),
        accentGradient = listOf(
            Color(0xFF7C3AED),
            Color(0xFFEC4899),
            Color(0xFF38BDF8),
            Color(0xFF7C3AED),
        ),
        nativeAccentHex = "#A855F7",
        focusRing = Color(0xFFA855F7),
        focusBackground = Color(0xFF25183D),
        background = Color(0xFF0B0B14),
        backgroundElevated = Color(0xFF151424),
        backgroundCard = Color(0xFF1C1B2F),
    )

    val AuroraVeil = ThemeColorPalette(
        secondary = Color(0xFF2DD4BF),
        secondaryVariant = Color(0xFF6366F1),
        accentGradient = listOf(
            Color(0xFF0EA5E9),
            Color(0xFF6366F1),
            Color(0xFF2DD4BF),
            Color(0xFF0EA5E9),
        ),
        nativeAccentHex = "#2DD4BF",
        focusRing = Color(0xFF2DD4BF),
        focusBackground = Color(0xFF102B31),
        background = Color(0xFF090C12),
        backgroundElevated = Color(0xFF141923),
        backgroundCard = Color(0xFF1B222D),
    )

    val VelvetEclipse = ThemeColorPalette(
        secondary = Color(0xFFF43F5E),
        secondaryVariant = Color(0xFF9333EA),
        accentGradient = listOf(
            Color(0xFFBE123C),
            Color(0xFF9333EA),
            Color(0xFFF43F5E),
            Color(0xFFBE123C),
        ),
        nativeAccentHex = "#F43F5E",
        focusRing = Color(0xFFF43F5E),
        focusBackground = Color(0xFF321522),
        background = Color(0xFF090C12),
        backgroundElevated = Color(0xFF141923),
        backgroundCard = Color(0xFF241A23),
    )

    val MidnightTide = ThemeColorPalette(
        secondary = Color(0xFF6B8CAE),
        secondaryVariant = Color(0xFF3B2F63),
        accentGradient = listOf(
            Color(0xFF1E3A5F),
            Color(0xFF3B2F63),
            Color(0xFF0F4C5C),
            Color(0xFF1E3A5F),
        ),
        nativeAccentHex = "#6B8CAE",
        focusRing = Color(0xFF6B8CAE),
        focusBackground = Color(0xFF1A2534),
        background = Color(0xFF090C12),
        backgroundElevated = Color(0xFF141923),
        backgroundCard = Color(0xFF1B222D),
    )

    val TitaniumMist = ThemeColorPalette(
        secondary = Color(0xFF94A3B8),
        secondaryVariant = Color(0xFF64748B),
        accentGradient = listOf(
            Color(0xFF4B5563),
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFF475569),
        ),
        nativeAccentHex = "#94A3B8",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF94A3B8),
        focusBackground = Color(0xFF252D38),
        background = Color(0xFF10151C),
        backgroundElevated = Color(0xFF191F28),
        backgroundCard = Color(0xFF222A35),
    )

    val ObsidianBronze = ThemeColorPalette(
        secondary = Color(0xFFB89B7A),
        secondaryVariant = Color(0xFF8C6D4F),
        accentGradient = listOf(
            Color(0xFF29221E),
            Color(0xFF5C4433),
            Color(0xFF8C6D4F),
            Color(0xFF3B2D24),
        ),
        nativeAccentHex = "#B89B7A",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFB89B7A),
        focusBackground = Color(0xFF332920),
        background = Color(0xFF120F0D),
        backgroundElevated = Color(0xFF1E1915),
        backgroundCard = Color(0xFF2A221C),
    )

    val NordicPine = ThemeColorPalette(
        secondary = Color(0xFF5F8D84),
        secondaryVariant = Color(0xFF365E57),
        accentGradient = listOf(
            Color(0xFF162B28),
            Color(0xFF244741),
            Color(0xFF365E57),
            Color(0xFF1B3531),
        ),
        nativeAccentHex = "#5F8D84",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF5F8D84),
        focusBackground = Color(0xFF1C332F),
        background = Color(0xFF0B1110),
        backgroundElevated = Color(0xFF141D1B),
        backgroundCard = Color(0xFF1B2724),
    )

    val DuskSlate = ThemeColorPalette(
        secondary = Color(0xFF868CB0),
        secondaryVariant = Color(0xFF53587A),
        accentGradient = listOf(
            Color(0xFF23273A),
            Color(0xFF3B3E5B),
            Color(0xFF53587A),
            Color(0xFF2B2F45),
        ),
        nativeAccentHex = "#868CB0",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF868CB0),
        focusBackground = Color(0xFF292D43),
        background = Color(0xFF0D0F17),
        backgroundElevated = Color(0xFF171A26),
        backgroundCard = Color(0xFF202436),
    )

    val SmokedMerlot = ThemeColorPalette(
        secondary = Color(0xFFA66376),
        secondaryVariant = Color(0xFF6E3446),
        accentGradient = listOf(
            Color(0xFF2D161E),
            Color(0xFF522534),
            Color(0xFF6E3446),
            Color(0xFF381B25),
        ),
        nativeAccentHex = "#A66376",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFA66376),
        focusBackground = Color(0xFF351D27),
        background = Color(0xFF120B0E),
        backgroundElevated = Color(0xFF20151A),
        backgroundCard = Color(0xFF2B1B22),
    )

    fun getColorPalette(
        theme: AppTheme,
        customColors: CustomThemeColors = CustomThemeColors.Default,
    ): ThemeColorPalette = when (theme) {
        AppTheme.CUSTOM -> customColors.toColorPalette()
        AppTheme.GOLD -> Gold
        AppTheme.JADE -> Jade
        AppTheme.ROSE_GOLD -> RoseGold
        AppTheme.ARCTIC_BLUE -> ArcticBlue
        AppTheme.GRAPHITE -> Graphite
        AppTheme.CRIMSON -> Crimson
        AppTheme.OCEAN -> Ocean
        AppTheme.VIOLET -> Violet
        AppTheme.EMERALD -> Emerald
        AppTheme.AMBER -> Amber
        AppTheme.ROSE -> Rose
        AppTheme.WHITE -> White
        AppTheme.SUNSET -> Sunset
        AppTheme.AMETHYST -> Amethyst
        AppTheme.AURORA_VEIL -> AuroraVeil
        AppTheme.VELVET_ECLIPSE -> VelvetEclipse
        AppTheme.MIDNIGHT_TIDE -> MidnightTide
        AppTheme.TITANIUM_MIST -> TitaniumMist
        AppTheme.OBSIDIAN_BRONZE -> ObsidianBronze
        AppTheme.NORDIC_PINE -> NordicPine
        AppTheme.DUSK_SLATE -> DuskSlate
        AppTheme.SMOKED_MERLOT -> SmokedMerlot
    }
}

/** Blend gradient theme accents onto the fixed dark surface at the chosen opacity. */
fun ThemeColorPalette.withGradientOpacity(opacity: Float): ThemeColorPalette {
    val base = Color(0xFF141923)
    fun blend(color: Color) = androidx.compose.ui.graphics.lerp(base, color, opacity.coerceIn(0.15f, 1f))
    return copy(
        secondary = blend(secondary),
        secondaryVariant = blend(secondaryVariant),
        accentGradient = accentGradient.map(::blend),
        onSecondary = if (opacity < 0.68f) Color.White else onSecondary,
        onSecondaryVariant = if (opacity < 0.68f) Color.White else onSecondaryVariant,
        focusRing = blend(focusRing),
    )
}
