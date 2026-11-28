package com.nuvio.app.features.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.size.Size
import com.nuvio.app.core.ui.background.softwareBitmap
import com.nuvio.app.features.details.components.loadedBackdropImageBitmap
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_ambient_mode_balanced
import nuvio.composeapp.generated.resources.player_ambient_mode_max
import nuvio.composeapp.generated.resources.player_ambient_mode_off
import nuvio.composeapp.generated.resources.player_ambient_mode_subtle
import nuvio.composeapp.generated.resources.player_ambient_mode_title
import nuvio.composeapp.generated.resources.player_ambient_mode_vivid
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AmbientMode(val intensity: Float) {
    OFF(0f),
    SUBTLE(0.28f),
    BALANCED(0.55f),
    VIVID(0.80f),
    MAX(1f),
    ;

    companion object {
        fun fromStorageValue(value: String?): AmbientMode =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: BALANCED
    }
}

internal data class AmbientEdgeColors(
    val top: Color,
    val bottom: Color,
    val start: Color,
    val end: Color,
) {
    companion object {
        val Fallback = AmbientEdgeColors(
            top = Color(0xFF276A9E),
            bottom = Color(0xFF172E59),
            start = Color(0xFF393A75),
            end = Color(0xFF287A91),
        )
    }
}

internal fun shouldRenderAmbient(
    mode: AmbientMode,
    resizeMode: PlayerResizeMode,
    videoAspectRatio: Float,
    screenAspectRatio: Float,
): Boolean =
    mode != AmbientMode.OFF &&
        resizeMode == PlayerResizeMode.Fit &&
        videoAspectRatio.isFinite() &&
        screenAspectRatio.isFinite() &&
        videoAspectRatio > 0f &&
        screenAspectRatio > 0f &&
        abs(videoAspectRatio - screenAspectRatio) > 0.04f

internal fun ambientEdgeColorsFromPixels(
    width: Int,
    height: Int,
    pixels: IntArray,
): AmbientEdgeColors? {
    if (width < 2 || height < 2 || pixels.size < width * height) return null

    fun averageColor(indices: IntArray): Color? {
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0
        for (index in indices) {
            val pixel = pixels[index]
            if ((pixel ushr 24) < 64) continue
            red += (pixel ushr 16) and 0xFF
            green += (pixel ushr 8) and 0xFF
            blue += pixel and 0xFF
            count++
        }
        if (count == 0) return null
        return Color(
            red = red.toFloat() / count / 255f,
            green = green.toFloat() / count / 255f,
            blue = blue.toFloat() / count / 255f,
        )
    }

    val top = IntArray(width) { it }
    val bottom = IntArray(width) { (height - 1) * width + it }
    val start = IntArray(height) { it * width }
    val end = IntArray(height) { it * width + width - 1 }
    val fallback = AmbientEdgeColors.Fallback
    return AmbientEdgeColors(
        top = averageColor(top) ?: fallback.top,
        bottom = averageColor(bottom) ?: fallback.bottom,
        start = averageColor(start) ?: fallback.start,
        end = averageColor(end) ?: fallback.end,
    )
}

@Composable
internal fun AmbientModePopup(
    expanded: Boolean,
    selectedMode: AmbientMode,
    onSelect: (AmbientMode) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        AmbientModeSelector(
            selectedMode = selectedMode,
            onSelect = onSelect,
            modifier = Modifier.widthIn(min = 220.dp, max = 280.dp).padding(8.dp),
        )
    }
}

@Composable
internal fun AmbientModeSelector(
    selectedMode: AmbientMode,
    onSelect: (AmbientMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = mapOf(
        AmbientMode.OFF to Res.string.player_ambient_mode_off,
        AmbientMode.SUBTLE to Res.string.player_ambient_mode_subtle,
        AmbientMode.BALANCED to Res.string.player_ambient_mode_balanced,
        AmbientMode.VIVID to Res.string.player_ambient_mode_vivid,
        AmbientMode.MAX to Res.string.player_ambient_mode_max,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.player_ambient_mode_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
        AmbientMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else Color.Transparent,
                    )
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(labels.getValue(mode)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(mode.intensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AmbientLetterboxOverlay(
    mode: AmbientMode,
    resizeMode: PlayerResizeMode,
    videoAspectRatio: Float,
    screenAspectRatio: Float,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (!shouldRenderAmbient(mode, resizeMode, videoAspectRatio, screenAspectRatio)) {
        return
    }
    val edgeColors = rememberAmbientEdgeColors(artworkUrl)
    val top by animateColorAsState(edgeColors.top, tween(850), label = "ambientTop")
    val bottom by animateColorAsState(edgeColors.bottom, tween(850), label = "ambientBottom")
    val start by animateColorAsState(edgeColors.start, tween(850), label = "ambientStart")
    val end by animateColorAsState(edgeColors.end, tween(850), label = "ambientEnd")
    val density = LocalDensity.current
    val glowDepthPx = with(density) { 180.dp.toPx() }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val screenRatio = size.width / size.height.coerceAtLeast(1f)
            if (!shouldRenderAmbient(mode, resizeMode, videoAspectRatio, screenRatio)) return@Canvas
            val alpha = mode.intensity.coerceIn(0f, 1f) * 0.72f
            if (videoAspectRatio > screenRatio) {
                val imageHeight = size.width / videoAspectRatio
                val barHeight = ((size.height - imageHeight) / 2f).coerceAtLeast(0f)
                if (barHeight <= 0f) return@Canvas
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(top.copy(alpha = alpha), Color.Transparent),
                        startY = 0f,
                        endY = minOf(barHeight, glowDepthPx),
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, bottom.copy(alpha = alpha)),
                        startY = size.height - minOf(barHeight, glowDepthPx),
                        endY = size.height,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                )
            } else {
                val imageWidth = size.height * videoAspectRatio
                val barWidth = ((size.width - imageWidth) / 2f).coerceAtLeast(0f)
                if (barWidth <= 0f) return@Canvas
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(start.copy(alpha = alpha), Color.Transparent),
                        startX = 0f,
                        endX = minOf(barWidth, glowDepthPx),
                    ),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, end.copy(alpha = alpha)),
                        startX = size.width - minOf(barWidth, glowDepthPx),
                        endX = size.width,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width - barWidth, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                )
            }
        }
    }
}

@Composable
private fun rememberAmbientEdgeColors(artworkUrl: String?): AmbientEdgeColors {
    val url = artworkUrl?.takeIf(String::isNotBlank)
    if (url == null) return AmbientEdgeColors.Fallback

    val context = LocalPlatformContext.current
    val request = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size(16, 9))
            .scale(Scale.FIT)
            .softwareBitmap()
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val painterState by painter.state.collectAsState()
    val result = (painterState as? AsyncImagePainter.State.Success)?.result
    return remember(url, result) {
        runCatching {
            val bitmap: ImageBitmap = loadedBackdropImageBitmap(result ?: return@runCatching null)
                ?: return@runCatching null
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.readPixels(pixels)
            ambientEdgeColorsFromPixels(bitmap.width, bitmap.height, pixels)
        }.getOrNull() ?: AmbientEdgeColors.Fallback
    }
}
