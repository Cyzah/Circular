package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.ControlCenterRowReflowTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowReflowTest {
    @Test
    fun disconnectKeepsFirstFrameThenMovesWholeRowWithSmoothTiming() {
        val reflow = ControlCenterRowReflow()
        reflow.update(connected(1L), 0L, false)

        reflow.update(disconnected(2L), 1000L, true)

        Assert.assertTrue(reflow.active())
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        assertFrame(reflow.snapshot(), -230.0f, 230.0f)
        reflow.sample(1055L)
        assertFrame(reflow.snapshot(), -222.8125f, 222.8125f)
        reflow.sample(1110L)
        assertFrame(reflow.snapshot(), -207.0f, 207.0f)
        reflow.sample(1220L)
        assertFrame(reflow.snapshot(), -184.0f, 184.0f)
        Assert.assertFalse(reflow.active())
    }

    @Test
    fun reconnectReservesSpaceBeforeShowingWifi() {
        val reflow = ControlCenterRowReflow()
        reflow.update(disconnected(1L), 0L, false)

        reflow.update(connected(2L), 1000L, true)

        assertFrame(reflow.snapshot(), -184.0f, 184.0f)
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        reflow.sample(1110L)
        assertFrame(reflow.snapshot(), -207.0f, 207.0f)
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        reflow.sample(1219L)
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        reflow.sample(1220L)
        Assert.assertTrue(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        assertFrame(reflow.snapshot(), -230.0f, 230.0f)
        Assert.assertEquals(
            4.0f,
            reflow.snapshot().left(ControlCenterRowComponent.WIFI)
                    - reflow.snapshot().right(ControlCenterRowComponent.CELLULAR),
            EPSILON
        )
        Assert.assertFalse(reflow.active())
    }

    @Test
    fun retargetSamplesCurrentPositionWithoutJumping() {
        val reflow = ControlCenterRowReflow()
        reflow.update(connected(1L), 0L, false)
        reflow.update(disconnected(2L), 1000L, true)

        // No explicit sample: retarget must first catch up to this clock time.
        reflow.update(connected(3L), 1110L, true)

        assertFrame(reflow.snapshot(), -207.0f, 207.0f)
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        reflow.sample(1220L)
        assertFrame(reflow.snapshot(), -218.5f, 218.5f)
        reflow.update(disconnected(4L), 1220L, true)
        assertFrame(reflow.snapshot(), -218.5f, 218.5f)
        Assert.assertFalse(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        reflow.sample(1440L)
        assertFrame(reflow.snapshot(), -184.0f, 184.0f)
        Assert.assertFalse(reflow.active())
    }

    @Test
    fun hostRightEdgeAndGenerationChangesDoNotRestartAnimation() {
        val reflow = ControlCenterRowReflow()
        val translated = ControlCenterRowSnapshot()
        Assert.assertTrue(translated.copyTranslatedFrom(connected(1L), 700.0f))
        reflow.update(translated, 0L, false)
        assertFrame(reflow.snapshot(), -230.0f, 230.0f)
        reflow.update(disconnected(2L), 1000L, true)

        Assert.assertTrue(translated.copyTranslatedFrom(disconnected(8L), 420.0f))
        reflow.update(translated, 1110L, true)

        assertFrame(reflow.snapshot(), -207.0f, 207.0f)
        reflow.sample(1220L)
        assertFrame(reflow.snapshot(), -184.0f, 184.0f)
        Assert.assertEquals(8L, reflow.snapshot().generation())
        Assert.assertFalse(reflow.active())
    }

    @Test
    fun existingComponentWidthChangesAnimateBothEdgesAndRowWidth() {
        val reflow = ControlCenterRowReflow()
        reflow.update(disconnected(1L), 0L, false)
        val widerBattery: ControlCenterRowSnapshot = rects(
            booleanArrayOf(true, false, true),
            floatArrayOf(-120.0f, 0.0f, -204.0f),
            floatArrayOf(0.0f, 0.0f, -124.0f), 2L
        )

        reflow.update(widerBattery, 1000L, true)
        reflow.sample(1110L)

        Assert.assertEquals(
            -110.0f,
            reflow.snapshot().left(ControlCenterRowComponent.BATTERY), EPSILON
        )
        Assert.assertEquals(
            110.0f,
            reflow.snapshot().width(ControlCenterRowComponent.BATTERY), EPSILON
        )
        Assert.assertEquals(
            -194.0f,
            reflow.snapshot().left(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(194.0f, reflow.snapshot().rowWidth(), EPSILON)
    }

    @Test
    fun disablingAnimationImmediatelyUsesTheLatestTarget() {
        val reflow = ControlCenterRowReflow()
        reflow.update(connected(1L), 0L, false)
        reflow.update(disconnected(2L), 1000L, true)

        reflow.update(disconnected(3L), 1055L, false)

        assertFrame(reflow.snapshot(), -184.0f, 184.0f)
        Assert.assertFalse(reflow.active())
    }

    @Test
    fun invalidTargetsAndClearDiscardOldAnimationCoordinates() {
        val reflow = ControlCenterRowReflow()
        reflow.update(connected(1L), 0L, false)
        reflow.update(disconnected(2L), 1000L, true)

        reflow.update(ControlCenterRowSnapshot(), 1110L, true)

        Assert.assertFalse(reflow.snapshot().valid())
        Assert.assertEquals(0.0f, reflow.snapshot().rowWidth(), EPSILON)
        Assert.assertFalse(reflow.active())
        reflow.sample(2000L)
        Assert.assertFalse(reflow.snapshot().valid())
        reflow.update(connected(3L), 2010L, true)
        Assert.assertTrue(reflow.snapshot().visible(ControlCenterRowComponent.WIFI))
        Assert.assertFalse(reflow.active())
        reflow.clear()
        Assert.assertFalse(reflow.snapshot().valid())
        reflow.update(null, 2020L, true)
        Assert.assertFalse(reflow.active())
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun assertFrame(
            snapshot: ControlCenterRowSnapshot,
            cellularLeft: Float, rowWidth: Float
        ) {
            Assert.assertTrue(snapshot.valid())
            Assert.assertEquals(0.0f, snapshot.rowRight(), EPSILON)
            Assert.assertEquals(rowWidth, snapshot.rowWidth(), EPSILON)
            Assert.assertEquals(-rowWidth, snapshot.rowLeft(), EPSILON)
            Assert.assertEquals(
                cellularLeft,
                snapshot.left(ControlCenterRowComponent.CELLULAR), EPSILON
            )
            Assert.assertEquals(
                -100.0f,
                snapshot.left(ControlCenterRowComponent.BATTERY), EPSILON
            )
            Assert.assertEquals(
                0.0f,
                snapshot.right(ControlCenterRowComponent.BATTERY), EPSILON
            )
        }

        private fun connected(generation: Long): ControlCenterRowSnapshot {
            return rects(
                booleanArrayOf(true, true, true),
                floatArrayOf(-100.0f, -146.0f, -230.0f),
                floatArrayOf(0.0f, -106.0f, -150.0f), generation
            )
        }

        private fun disconnected(generation: Long): ControlCenterRowSnapshot {
            return rects(
                booleanArrayOf(true, false, true),
                floatArrayOf(-100.0f, 0.0f, -184.0f),
                floatArrayOf(0.0f, 0.0f, -104.0f), generation
            )
        }

        private fun rects(
            visible: BooleanArray?,
            left: FloatArray, right: FloatArray,
            generation: Long
        ): ControlCenterRowSnapshot {
            val snapshot = ControlCenterRowSnapshot()
            val widths = FloatArray(left.size)
            for (index in widths.indices) {
                widths[index] = right[index] - left[index]
            }
            Assert.assertTrue(snapshot.copyFromRects(visible, left, right, widths, generation))
            return snapshot
        }
    }
}
