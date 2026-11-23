package com.cyzah.circular.core.graphics.geometry

import org.junit.Assert
import org.junit.Test

class RingTrackStyleTest {
    @Test
    fun capsuleTrackKeepsItsBatteryColourAndOpacityThroughTheHandoff() {
        Assert.assertEquals(-0xe63256, RingTrackStyle.tint(true, 42, -0xf0f10, -0xe63256).toLong())
        Assert.assertEquals(96, RingTrackStyle.alpha(true).toLong())
        Assert.assertEquals(-0x1fbfc0, RingTrackStyle.tint(true, 0, -0xf0f10, -0x1fbfc0).toLong())
    }

    @Test
    fun emptyCircularBatteryUsesTheWarningTintBeforeAndAfterRestoring() {
        Assert.assertEquals(-0x1fbfc0, RingTrackStyle.tint(false, 0, -0xf0f10, -0x1fbfc0).toLong())
        Assert.assertEquals(48, RingTrackStyle.alpha(false).toLong())
    }

    @Test
    fun nonEmptyCircularBatteryRetainsItsNeutralTrack() {
        Assert.assertEquals(-0xf0f10, RingTrackStyle.tint(false, 42, -0xf0f10, -0xe63256).toLong())
        Assert.assertEquals(48, RingTrackStyle.alpha(false).toLong())
    }

    @Test
    fun completedTrackReplacesTheTemporaryTraceWithoutDoublePainting() {
        Assert.assertEquals(1f, RingTrackStyle.temporaryAlpha(false, 1f), 0f)
        Assert.assertEquals(0f, RingTrackStyle.temporaryAlpha(true, 1f), 0f)
        Assert.assertEquals(0f, RingTrackStyle.temporaryAlpha(true, 0.4f), 0f)
        Assert.assertEquals(0.4f, RingTrackStyle.temporaryAlpha(false, 0.4f), 0f)
    }
}
