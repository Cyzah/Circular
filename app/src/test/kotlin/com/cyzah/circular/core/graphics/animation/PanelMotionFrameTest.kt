package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.PanelMotionFrameTest
import org.junit.Assert
import org.junit.Test

class PanelMotionFrameTest {
    @Test
    fun morphIsAFunctionOfTheCurrentNativeExpansion() {
        val expected = 0.42f

        Assert.assertEquals(expected, PanelMotionFrame.morph(0.42f), EPSILON)
        Assert.assertEquals(expected, PanelMotionFrame.morph(0.42f), EPSILON)
        Assert.assertEquals(
            0.18f,
            PanelMotionFrame.morph(0.18f),
            EPSILON
        )
        Assert.assertEquals(0.0f, PanelMotionFrame.morph(-1.0f), EPSILON)
        Assert.assertEquals(1.0f, PanelMotionFrame.morph(2.0f), EPSILON)
    }

    @Test
    fun reverseNeedsNoInheritedTimelineState() {
        Assert.assertEquals(
            PanelMotionFrame.morph(0.73f),
            PanelMotionFrame.morph(0.73f),
            0.0f
        )
        Assert.assertEquals(
            PanelMotionFrame.morph(0.31f),
            PanelMotionFrame.morph(0.31f),
            0.0f
        )
    }

    @Test
    fun nativeAlphaIsAlsoAFunctionOfTheLiveExpansion() {
        Assert.assertEquals(
            0.42f,
            PanelMotionFrame.nativeAlpha(0.42f),
            EPSILON
        )
        Assert.assertEquals(0.0f, PanelMotionFrame.nativeAlpha(-1.0f), EPSILON)
        Assert.assertEquals(1.0f, PanelMotionFrame.nativeAlpha(2.0f), EPSILON)
    }

    @Test
    fun duoWifiUsesTheCurrentConnectivityTarget() {
        Assert.assertEquals(
            1.0f,
            PanelMotionFrame.duoWifiProgress(true),
            0.0f
        )
        Assert.assertEquals(
            0.0f,
            PanelMotionFrame.duoWifiProgress(false),
            0.0f
        )
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
