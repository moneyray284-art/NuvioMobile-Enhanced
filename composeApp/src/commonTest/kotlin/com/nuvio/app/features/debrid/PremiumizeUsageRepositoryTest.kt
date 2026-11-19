package com.nuvio.app.features.debrid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PremiumizeUsageRepositoryTest {

    @Test
    fun `default ui state is initial empty state`() {
        val state = PremiumizeUsageUiState()
        assertFalse(state.isLoading)
        assertEquals(null, state.customerId)
        assertFalse(state.isActive)
        assertFalse(state.isPremium)
        assertFalse(state.isExpired)
        assertEquals(null, state.premiumUntilEpochMs)
        assertEquals(null, state.daysLeft)
        assertEquals(null, state.limitUsed)
        assertEquals(0, state.pointsUsed)
        assertEquals(1000, state.pointsRemaining)
        assertEquals(0, state.boosterPoints)
        assertEquals(0L, state.todayDownloadedBytes)
        assertEquals(0L, state.monthlyDownloadedBytes)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `points calculation handles bounds correctly`() {
        // limit 0.0 -> 0 used, 1000 left
        val zeroLimit = 0.0.coerceIn(0.0, 1.0)
        assertEquals(0, (zeroLimit * 1000.0).toInt())
        assertEquals(1000, 1000 - (zeroLimit * 1000.0).toInt())

        // limit 0.154 -> 154 used, 846 left
        val midLimit = 0.154.coerceIn(0.0, 1.0)
        val midPointsUsed = kotlin.math.round((midLimit * 1000.0)).toInt()
        assertEquals(154, midPointsUsed)
        assertEquals(846, 1000 - midPointsUsed)

        // limit 1.0 -> 1000 used, 0 left
        val fullLimit = 1.0.coerceIn(0.0, 1.0)
        val fullPointsUsed = kotlin.math.round((fullLimit * 1000.0)).toInt()
        assertEquals(1000, fullPointsUsed)
        assertEquals(0, 1000 - fullPointsUsed)

        // clamped beyond 1.0 -> clamped to 1.0
        val overLimit = 1.5.coerceIn(0.0, 1.0)
        val overPointsUsed = kotlin.math.round((overLimit * 1000.0)).toInt()
        assertEquals(1000, overPointsUsed)
        assertEquals(0, 1000 - overPointsUsed)
    }

    @Test
    fun `fair-use progress percentage and color thresholds`() {
        fun colorThreshold(fraction: Float): String = when {
            fraction >= 0.90f -> "danger"
            fraction >= 0.75f -> "warning"
            else -> "accent"
        }

        assertEquals("accent", colorThreshold(0.0f))
        assertEquals("accent", colorThreshold(0.74f))
        assertEquals("warning", colorThreshold(0.75f))
        assertEquals("warning", colorThreshold(0.89f))
        assertEquals("danger", colorThreshold(0.90f))
        assertEquals("danger", colorThreshold(1.0f))
    }

    @Test
    fun `round renewal timer color thresholds`() {
        fun timerColor(daysLeft: Long?): String = when {
            daysLeft == null || daysLeft <= 0L -> "muted"
            daysLeft <= 7L -> "danger"
            daysLeft <= 30L -> "warning"
            else -> "accent"
        }

        assertEquals("muted", timerColor(null))
        assertEquals("muted", timerColor(0L))
        assertEquals("muted", timerColor(-5L))
        assertEquals("danger", timerColor(1L))
        assertEquals("danger", timerColor(7L))
        assertEquals("warning", timerColor(8L))
        assertEquals("warning", timerColor(30L))
        assertEquals("accent", timerColor(31L))
        assertEquals("accent", timerColor(365L))
    }
}
