package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class CapsuleStyleRendererTest {
    @Test
    fun emptySignalLeavesAllThreeDotsOnTrack() {
        Assert.assertEquals(48, CapsuleStyleRenderer.Companion.dotFill(0, 0).toLong())
        Assert.assertEquals(48, CapsuleStyleRenderer.Companion.dotFill(0, 2).toLong())
    }

    @Test
    fun fractionalBarsUsePartialDotAlpha() {
        Assert.assertEquals(203, CapsuleStyleRenderer.Companion.dotFill(1, 0).toLong())
        Assert.assertEquals(152, CapsuleStyleRenderer.Companion.dotFill(2, 1).toLong())
        Assert.assertEquals(100, CapsuleStyleRenderer.Companion.dotFill(3, 2).toLong())
    }

    @Test
    fun fullSignalLightsAllThreeDots() {
        Assert.assertEquals(255, CapsuleStyleRenderer.Companion.dotFill(4, 0).toLong())
        Assert.assertEquals(255, CapsuleStyleRenderer.Companion.dotFill(4, 1).toLong())
        Assert.assertEquals(255, CapsuleStyleRenderer.Companion.dotFill(4, 2).toLong())
    }

    @Test
    fun invalidGeometryFallsBackToFiniteSafeValues() {
        Assert.assertEquals(0.0f, CapsuleStyleRenderer.Companion.safeLength(Float.NaN), 0.0f)
        Assert.assertEquals(0.0f, CapsuleStyleRenderer.Companion.safeLength(-4.0f), 0.0f)
        Assert.assertEquals(4.0f, CapsuleStyleRenderer.Companion.safeLength(4.0f), 0.0f)
    }
}
