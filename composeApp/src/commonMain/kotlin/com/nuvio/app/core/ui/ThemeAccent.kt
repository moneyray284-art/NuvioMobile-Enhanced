package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Uses NuvioTheme's lone gradient phase, avoiding per-control animations. */
@Composable
fun ThemeColorPalette.animatedAccentBrush(alpha: Float = 1f): Brush {
    val shared = LocalSharedGradientState.current
    if (!shared.enabled || accentGradient.size < 2) return accentBrush(alpha)
    val colors = accentGradient.map { color ->
        color.copy(alpha = color.alpha * alpha * shared.opacity)
    }
    // Mirror has a 2x period; travelling that complete distance makes phase 0 == phase 1.
    val offset = if (shared.animated) shared.phase * 2_000f else 0f
    return Brush.linearGradient(
        colors = colors,
        start = Offset(offset, 0f),
        end = Offset(offset + 1_000f, 0f),
        tileMode = TileMode.Mirror,
    )
}

fun ThemeColorPalette.accentBrush(): Brush =
    if (accentGradient.size >= 2) Brush.linearGradient(accentGradient)
    else SolidColor(accentGradient.firstOrNull() ?: secondary)

fun ThemeColorPalette.accentBrush(alpha: Float): Brush =
    if (accentGradient.size >= 2) {
        Brush.linearGradient(accentGradient.map { color -> color.copy(alpha = alpha) })
    } else {
        SolidColor((accentGradient.firstOrNull() ?: secondary).copy(alpha = alpha))
    }

fun ThemeColorPalette.nativeAccentGradientHex(): List<String> =
    if (accentGradient.size >= 2) {
        accentGradient.map { color -> formatHexColor(color.toArgb() and 0xFFFFFF) }
    } else {
        listOf(nativeAccentHex)
    }

fun Modifier.gradientMask(brush: Brush): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcIn)
            }
        }

/** Adds the selected global glass treatment without altering a component's layout. */
@Composable
fun Modifier.globalGlassSurface(shape: Shape): Modifier {
    val mode = LocalAppGlassUiMode.current
    val palette = LocalThemePalette.current
    if (mode == AppGlassUiMode.OFF) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        val accent = palette.secondary
        val highlight = Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (mode == AppGlassUiMode.LIQUID_GLASS) 0.22f else 0.06f),
            0.35f to Color.Transparent,
        )
        onDrawWithContent {
            drawContent()
            if (mode == AppGlassUiMode.LIQUID_GLASS) {
                clipPath(path) { drawRect(highlight) }
                drawOutline(outline, Color.White.copy(alpha = 0.34f), style = Stroke(width = 1.dp.toPx()))
            } else {
                drawOutline(outline, accent.copy(alpha = 0.08f), style = Stroke(width = 14.dp.toPx()))
                drawOutline(outline, accent.copy(alpha = 0.12f), style = Stroke(width = 7.dp.toPx()))
                drawOutline(outline, accent.copy(alpha = 0.60f), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

/** Draws the selected animated palette over Nuvio's dark base once, behind app content. */
@Composable
fun Modifier.animatedThemeBackdrop(): Modifier {
    val shared = LocalSharedGradientState.current
    val palette = LocalThemePalette.current
    if (!shared.enabled) return this
    return drawWithCache {
        val width = size.width.coerceAtLeast(1f)
        val offset = shared.phase * width * 2f
        val brush = Brush.linearGradient(
            colors = palette.accentGradient.map { it.copy(alpha = it.alpha * shared.opacity) },
            start = Offset(offset, 0f),
            end = Offset(offset + width, size.height.coerceAtLeast(1f)),
            tileMode = TileMode.Mirror,
        )
        onDrawBehind {
            drawRect(Color(0xFF141923))
            drawRect(brush)
        }
    }
}
