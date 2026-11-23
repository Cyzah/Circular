package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class NativeNeighbourMotionTest {
    @Test
    fun anchoredClosingReconcilesShrinkingWidthBeforeTheNativeHostMovesAgain() {
        // Native host has moved left 6px: old width 100 would drag the neighbour
        // left, while width 94 keeps its established screen boundary at 920.
        Assert.assertTrue(ControlCenterRowHostPolicy.needsWidthBeforeDraw(100, 94, false, true))
        Assert.assertEquals(920.0f, 1014.0f - 94.0f, 0.0f)
        Assert.assertTrue(!ControlCenterRowHostPolicy.needsWidthBeforeDraw(94, 94, false, true))
        Assert.assertTrue(!ControlCenterRowHostPolicy.needsWidthBeforeDraw(100, 94, false, false))
    }

    @Test
    fun unusedClearanceDoesNotPullTheSpeedIconLeftBeforeClosingRight() {
        var previous = 920.0f
        for (step in 1000 downTo 0) {
            val p = step / 1000.0f
            val sourceLeft = 1000.0f - 60.0f * (1.0f - p)
            val remaining = ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                1000000, 0, p, BatteryNumberPlacement.OUTSIDE
            ) / 1000000.0f
            val current = NativeNeighbourMotion.boundary(
                920.0f, 940.0f, remaining,
                min(sourceLeft, 940.0f)
            )
            if (step == 1000) {
                Assert.assertEquals(920.0f, current, 0.0f)
            }
            Assert.assertTrue(current >= previous)
            Assert.assertTrue(current <= sourceLeft)
            previous = current
        }
        Assert.assertEquals(940.0f, previous, 0.0f)
    }

    @Test
    fun realContentCollisionStillLimitsTheNeighbourBoundary() {
        Assert.assertEquals(900.0f, NativeNeighbourMotion.boundary(920f, 940f, 0.5f, 900f), 0.0f)
        Assert.assertEquals(930.0f, NativeNeighbourMotion.boundary(920f, 940f, 0.5f, 950f), 0.0f)
        Assert.assertEquals(940.0f, NativeNeighbourMotion.boundary(920f, 940f, 0f, 980f), 0.0f)
    }

    @Test
    fun movingNativeHostKeepsItsNeighbourMonotonicAfterWidthRounding() {
        var previous = 920.0f
        for (step in 1000 downTo 0) {
            val p = step / 1000.0f
            // getLocationOnScreen supplies integer coordinates in the adapter.
            val hostRight = Math.round(1020.0f - 60.0f * (1.0f - p)).toFloat()
            val sourceBoundary = hostRight - 20.0f
            val width = NativeNeighbourMotion.reservedWidth(
                hostRight, 920f,
                sourceBoundary, 940f,
                NativeNeighbourMotion.remaining(p, BatteryNumberPlacement.OUTSIDE)
            )
            val neighbourBoundary = hostRight - width
            Assert.assertTrue("Reversed at expansion " + p, neighbourBoundary >= previous)
            Assert.assertTrue(neighbourBoundary <= sourceBoundary)
            Assert.assertTrue(neighbourBoundary <= 940.0f)
            previous = neighbourBoundary
        }
        Assert.assertEquals(940.0f, previous, 0.0f)
    }

    @Test
    fun reservationStillTracksContentThatHasNotReleasedItsSpace() {
        // A desired boundary of 930 must stop at the content's current 870 edge.
        Assert.assertEquals(
            130, NativeNeighbourMotion.reservedWidth(
                1000f, 920f, 800f, 940f, 0.5f
            ).toLong()
        )
        // A left-moving row must not use its rightmost endpoint as the safe edge.
        Assert.assertEquals(
            60, NativeNeighbourMotion.reservedWidth(
                1000f, 980f, 1000f, 940f, 0.5f
            ).toLong()
        )
        // Stopping and reversing the same gesture has no accumulated correction.
        NativeNeighbourMotion.reservedWidth(1030f, 920f, 800f, 940f, 0.2f)
        Assert.assertEquals(
            130, NativeNeighbourMotion.reservedWidth(
                1000f, 920f, 800f, 940f, 0.5f
            ).toLong()
        )
    }
}
