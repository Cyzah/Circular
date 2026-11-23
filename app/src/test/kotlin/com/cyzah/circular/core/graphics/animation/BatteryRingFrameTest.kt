package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.BatteryRingFrameTest
import org.junit.Assert
import org.junit.Test

class BatteryRingFrameTest {
    @Test
    fun transferRingDoesNotFollowTheNarrowNativeBatteryToTheRight() {
        val width = 230.0f
        val minimum = 72.0f
        val nativeWidth = 54.0f
        val progress = 0.75f
        val movingOrigin = Motion.mix(
            width - minimum / 2.0f,
            width - nativeWidth / 2.0f, progress
        ) - minimum / 2.0f
        Assert.assertEquals(164.75f, movingOrigin, EPSILON)

        val origin = BatteryRingFrame.originX(
            true, width, minimum, false, 0.0f, movingOrigin
        )

        Assert.assertEquals(158.0f, origin, EPSILON)
        Assert.assertEquals(226.25f, origin + 113.75f * minimum / 120.0f, EPSILON)
        Assert.assertTrue(origin + 113.75f * minimum / 120.0f <= width)
    }

    @Test
    fun ringProjectionAndDrawingKeepTheSameDriftInEitherDirection() {
        Assert.assertEquals(
            160.0f,
            BatteryRingFrame.originX(true, 230.0f, 72.0f, false, 2.0f, 164.75f),
            EPSILON
        )
        Assert.assertEquals(
            2.0f,
            BatteryRingFrame.originX(true, 230.0f, 72.0f, true, 2.0f, -6.75f),
            EPSILON
        )
        Assert.assertEquals(
            11.0f,
            BatteryRingFrame.originY(true, 100.0f, 72.0f, -3.0f, 12.0f),
            EPSILON
        )
    }

    @Test
    fun wideCapsuleTransferUsesTheRightAnchoredClosedFrame() {
        Assert.assertEquals(
            178.0f,
            BatteryRingFrame.originX(true, 250.0f, 72.0f, false, 0.0f, 89.0f),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            BatteryRingFrame.originX(true, 250.0f, 72.0f, true, 0.0f, 89.0f),
            EPSILON
        )
    }

    @Test
    fun ordinaryPartMotionAndSimpleIconCentringRemainUnchanged() {
        Assert.assertEquals(
            164.75f,
            BatteryRingFrame.originX(false, 230.0f, 72.0f, false, 2.0f, 164.75f),
            EPSILON
        )
        Assert.assertEquals(
            89.0f,
            BatteryRingFrame.originX(false, 250.0f, 72.0f, false, 0.0f, 89.0f),
            EPSILON
        )
        Assert.assertEquals(
            12.0f,
            BatteryRingFrame.originY(false, 100.0f, 72.0f, -3.0f, 12.0f),
            EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
