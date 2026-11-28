package com.nuvio.app.features.player

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmbientModeTest {
    @Test
    fun presetsUseExpectedIntensityAndBalancedIsTheDefault() {
        assertEquals(0f, AmbientMode.OFF.intensity)
        assertEquals(0.28f, AmbientMode.SUBTLE.intensity)
        assertEquals(0.55f, AmbientMode.BALANCED.intensity)
        assertEquals(0.80f, AmbientMode.VIVID.intensity)
        assertEquals(1f, AmbientMode.MAX.intensity)
        assertEquals(AmbientMode.BALANCED, PlayerSettingsUiState().ambientMode)
        assertEquals(AmbientMode.BALANCED, AmbientMode.fromStorageValue(null))
    }

    @Test
    fun ambientOnlyRendersForFitWithMeaningfulAspectMismatch() {
        assertTrue(
            shouldRenderAmbient(
                mode = AmbientMode.BALANCED,
                resizeMode = PlayerResizeMode.Fit,
                videoAspectRatio = 2.35f,
                screenAspectRatio = 1.78f,
            ),
        )
        assertFalse(
            shouldRenderAmbient(
                mode = AmbientMode.BALANCED,
                resizeMode = PlayerResizeMode.Fit,
                videoAspectRatio = 1.82f,
                screenAspectRatio = 1.78f,
            ),
        )
        assertFalse(
            shouldRenderAmbient(
                mode = AmbientMode.BALANCED,
                resizeMode = PlayerResizeMode.Fill,
                videoAspectRatio = 2.35f,
                screenAspectRatio = 1.78f,
            ),
        )
        assertFalse(
            shouldRenderAmbient(
                mode = AmbientMode.BALANCED,
                resizeMode = PlayerResizeMode.Zoom,
                videoAspectRatio = 2.35f,
                screenAspectRatio = 1.78f,
            ),
        )
        assertFalse(
            shouldRenderAmbient(
                mode = AmbientMode.OFF,
                resizeMode = PlayerResizeMode.Fit,
                videoAspectRatio = 2.35f,
                screenAspectRatio = 1.78f,
            ),
        )
        assertFalse(
            shouldRenderAmbient(
                mode = AmbientMode.BALANCED,
                resizeMode = PlayerResizeMode.Fit,
                videoAspectRatio = Float.NaN,
                screenAspectRatio = 1.78f,
            ),
        )
    }

    @Test
    fun edgeColorsAverageVisiblePixelsAndRejectMalformedBuffers() {
        val colors = ambientEdgeColorsFromPixels(
            width = 2,
            height = 2,
            pixels = intArrayOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0xFFFFFFFF.toInt(),
            ),
        )

        assertEquals(Color(0.5f, 0.5f, 0f), colors?.top)
        assertEquals(Color(0.5f, 0.5f, 1f), colors?.bottom)
        assertEquals(Color(0.5f, 0f, 0.5f), colors?.start)
        assertEquals(Color(0.5f, 1f, 0.5f), colors?.end)
        assertNull(ambientEdgeColorsFromPixels(2, 2, intArrayOf(0)))
    }
}
