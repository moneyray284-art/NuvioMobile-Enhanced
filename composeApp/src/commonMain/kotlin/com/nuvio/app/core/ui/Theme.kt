package com.nuvio.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.jetbrains_sans_bold
import nuvio.composeapp.generated.resources.jetbrains_sans_regular
import nuvio.composeapp.generated.resources.jetbrains_sans_semibold
import org.jetbrains.compose.resources.Font
import com.nuvio.app.isReducedMotionEnabled
import kotlinx.coroutines.isActive

val LocalAppTheme = staticCompositionLocalOf { AppTheme.WHITE }
val LocalThemePalette = staticCompositionLocalOf { ThemeColors.White }
val LocalAppGlassUiMode = staticCompositionLocalOf { AppGlassUiMode.OFF }

/**
 * The single source of truth for every animated theme gradient in the composition.
 * Components consume this phase rather than owning their own transitions.
 */
@Immutable
data class SharedGradientState(
    val phase: Float = 0f,
    val enabled: Boolean = false,
    val animated: Boolean = false,
    val opacity: Float = 0.85f,
)

val LocalSharedGradientState = staticCompositionLocalOf { SharedGradientState() }

val MaterialTheme.themePalette: ThemeColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePalette.current

val MaterialTheme.appTheme: AppTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTheme.current

private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFF5F7F8)

private fun buildColorScheme(palette: ThemeColorPalette, amoled: Boolean = false) = darkColorScheme(
    primary = palette.secondary,
    onPrimary = palette.onSecondary,
    primaryContainer = palette.focusBackground,
    onPrimaryContainer = contentColorFor(palette.focusBackground),
    secondary = palette.secondaryVariant,
    onSecondary = palette.onSecondaryVariant,
    background = if (amoled) Color.Black else palette.background,
    onBackground = Color(0xFFF5F7F8),
    surface = palette.backgroundElevated,
    onSurface = Color(0xFFF5F7F8),
    surfaceVariant = palette.backgroundCard,
    onSurfaceVariant = Color(0xFF969CA3),
    outline = Color(0xFF252A2A),
    error = Color(0xFFE36A8A),
    onError = Color(0xFFFCE5EC),
)

private fun NuvioThemeTokens.withGlobalGlass(mode: AppGlassUiMode): NuvioThemeTokens {
    if (mode == AppGlassUiMode.OFF) return this
    val c = colors
    val liquid = mode == AppGlassUiMode.LIQUID_GLASS
    val surfaceAlpha = if (liquid) 0.52f else 0.72f
    val border = if (liquid) Color.White.copy(alpha = 0.34f) else c.accent.copy(alpha = 0.58f)
    return copy(
        colors = c.copy(
            surface = c.surface.copy(alpha = surfaceAlpha),
            surfaceElevated = c.surfaceElevated.copy(alpha = surfaceAlpha),
            surfaceCard = c.surfaceCard.copy(alpha = surfaceAlpha),
            surfaceSheet = c.surfaceSheet.copy(alpha = if (liquid) 0.64f else 0.78f),
            surfaceDialog = c.surfaceDialog.copy(alpha = if (liquid) 0.68f else 0.82f),
            surfacePopover = c.surfacePopover.copy(alpha = surfaceAlpha),
            borderSubtle = border.copy(alpha = border.alpha * 0.60f),
            borderDefault = border,
            borderStrong = border.copy(alpha = minOf(1f, border.alpha + 0.18f)),
            borderFocus = if (liquid) Color.White.copy(alpha = 0.72f) else c.accent,
            borderSelected = c.accent.copy(alpha = 0.78f),
        ),
    )
}

private fun androidx.compose.material3.ColorScheme.withGlobalGlass(mode: AppGlassUiMode) =
    if (mode == AppGlassUiMode.OFF) this else copy(
        surface = surface.copy(alpha = if (mode == AppGlassUiMode.LIQUID_GLASS) 0.52f else 0.72f),
        surfaceVariant = surfaceVariant.copy(alpha = if (mode == AppGlassUiMode.LIQUID_GLASS) 0.52f else 0.72f),
        outline = if (mode == AppGlassUiMode.LIQUID_GLASS) Color.White.copy(alpha = 0.34f) else primary.copy(alpha = 0.58f),
    )

private val JetBrainsSans: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.jetbrains_sans_bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.jetbrains_sans_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.jetbrains_sans_regular, FontWeight.Normal, FontStyle.Normal),
    )

private val NuvioTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.pageDisplay,
            lineHeight = NuvioTokens.LineHeight.pageDisplay,
            fontWeight = FontWeight.Bold,
            letterSpacing = NuvioTokens.LetterSpacing.pageDisplay,
        ),
        headlineLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.headline,
            lineHeight = NuvioTokens.LineHeight.headline,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = NuvioTokens.LetterSpacing.headline,
        ),
        titleLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.titleSm,
            lineHeight = NuvioTokens.LineHeight.materialTitleLarge,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyLg,
            lineHeight = NuvioTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyApp,
            lineHeight = NuvioTokens.LineHeight.bodyApp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyMd,
            lineHeight = NuvioTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyMd,
            lineHeight = NuvioTokens.LineHeight.bodySm,
            fontWeight = FontWeight.SemiBold,
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.labelSm,
            lineHeight = NuvioTokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = NuvioTokens.LetterSpacing.label,
        ),
    )

private val NuvioTypeTokens: NuvioTypeScale
    @Composable
    get() = NuvioTypeScale(
        labelXs = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.labelXs,
            lineHeight = NuvioTokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
        ),
        labelSm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.labelSm,
            lineHeight = NuvioTokens.LineHeight.labelSm,
            fontWeight = FontWeight.SemiBold,
        ),
        bodySm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodySm,
            lineHeight = NuvioTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyMd,
            lineHeight = NuvioTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodyLg = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.bodyLg,
            lineHeight = NuvioTokens.LineHeight.bodyLg,
            fontWeight = FontWeight.Normal,
        ),
        titleSm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.titleSm,
            lineHeight = NuvioTokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.titleMd,
            lineHeight = NuvioTokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLg = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.titleLg,
            lineHeight = NuvioTokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.displaySm,
            lineHeight = NuvioTokens.LineHeight.displaySm,
            fontWeight = FontWeight.Bold,
        ),
        displayMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = NuvioTokens.Type.displayMd,
            lineHeight = NuvioTokens.LineHeight.displayMd,
            fontWeight = FontWeight.Bold,
        ),
    )

private val NuvioRippleConfiguration = RippleConfiguration(
    color = Color.Black,
)

@Composable
fun NuvioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.WHITE,
    amoled: Boolean = false,
    customThemeColors: CustomThemeColors = CustomThemeColors.Default,
    animatedGradientsEnabled: Boolean = true,
    animatedThemeOpacity: Float = 0.85f,
    glassUiMode: AppGlassUiMode = AppGlassUiMode.OFF,
    content: @Composable () -> Unit,
) {
    val palette = remember(appTheme, customThemeColors) {
        ThemeColors.getColorPalette(appTheme, customThemeColors)
    }
    val videoPlaybackActive by AnimatedThemePlaybackGate.videoPlaybackActive.collectAsState()
    val isAnimatedTheme = appTheme.isAnimatedGradientTheme
    val safeOpacity = animatedThemeOpacity.coerceIn(0.15f, 1f)
    val reducedMotion = isReducedMotionEnabled()
    val shouldAnimate = isAnimatedTheme && animatedGradientsEnabled && !reducedMotion && !videoPlaybackActive
    val phase = remember { Animatable(0f) }
    LaunchedEffect(shouldAnimate, isAnimatedTheme, animatedGradientsEnabled, reducedMotion) {
        if (!isAnimatedTheme || !animatedGradientsEnabled || reducedMotion) {
            phase.snapTo(0f)
        } else if (shouldAnimate) {
            while (isActive) {
                phase.animateTo(1f, animationSpec = tween(2_600, easing = LinearEasing))
                if (isActive) phase.snapTo(0f)
            }
        }
    }
    val presentationPalette = remember(palette, isAnimatedTheme, safeOpacity) {
        if (isAnimatedTheme) {
            palette.copy(
                secondary = palette.secondary.copy(alpha = safeOpacity),
                secondaryVariant = palette.secondaryVariant.copy(alpha = safeOpacity),
                focusRing = palette.focusRing.copy(alpha = safeOpacity),
                onSecondary = if (safeOpacity < 0.68f) Color.White else palette.onSecondary,
                onSecondaryVariant = if (safeOpacity < 0.68f) Color.White else palette.onSecondaryVariant,
            )
        } else {
            palette
        }
    }
    val colorScheme = buildColorScheme(presentationPalette, amoled = amoled).withGlobalGlass(glassUiMode)
    val tokens = defaultNuvioThemeTokens(presentationPalette, amoled = amoled, colorScheme = colorScheme)
        .withGlobalGlass(glassUiMode)

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f,
        ),
        LocalNuvioThemeTokens provides tokens,
        LocalNuvioTypeScale provides NuvioTypeTokens,
        LocalRippleConfiguration provides NuvioRippleConfiguration,
        LocalAppTheme provides appTheme,
        LocalThemePalette provides presentationPalette,
        LocalAppGlassUiMode provides glassUiMode,
        LocalSharedGradientState provides SharedGradientState(
            phase = phase.value,
            enabled = isAnimatedTheme,
            animated = isAnimatedTheme && animatedGradientsEnabled && !reducedMotion,
            opacity = safeOpacity,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NuvioTypography,
        ) {
            SharedCardStylesProvider {
                SkeletonAnimationProvider(content = content)
            }
        }
    }
}
