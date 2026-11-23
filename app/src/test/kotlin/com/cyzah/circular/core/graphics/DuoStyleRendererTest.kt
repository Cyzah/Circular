package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class DuoStyleRendererTest {
    @Test
    fun batteryMovesBelowOnlyWhenWifiAndDataAreUnavailable() {
        Assert.assertTrue(DuoStyleRenderer.Companion.batteryBelowWanted(false, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.batteryBelowWanted(true, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.batteryBelowWanted(false, true))
        Assert.assertFalse(DuoStyleRenderer.Companion.batteryBelowWanted(true, true))
    }

    @Test
    fun dataIsAvailableOnlyForKnownPresentConnectedNonAirplaneSim() {
        Assert.assertTrue(DuoStyleRenderer.Companion.dataAvailable(true, true, true, true, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.dataAvailable(false, true, true, true, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.dataAvailable(true, false, true, true, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.dataAvailable(true, true, false, true, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.dataAvailable(true, true, true, false, false))
        Assert.assertFalse(DuoStyleRenderer.Companion.dataAvailable(true, true, true, true, true))
    }

    @Test
    fun batteryBelowProgressMatchesFallbackAndWifiAnimation() {
        Assert.assertEquals(
            1.0f,
            DuoStyleRenderer.Companion.batteryBelowProgress(true, false, 0.0f),
            0.001f
        )
        Assert.assertEquals(
            0.0f,
            DuoStyleRenderer.Companion.batteryBelowProgress(false, true, 0.4f),
            0.001f
        )
        Assert.assertEquals(
            0.6f,
            DuoStyleRenderer.Companion.batteryBelowProgress(false, false, 0.4f),
            0.001f
        )
    }
}
