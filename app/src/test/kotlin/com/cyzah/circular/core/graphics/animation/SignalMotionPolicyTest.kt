package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicyTest
import org.junit.Assert
import org.junit.Test

class SignalMotionPolicyTest {
    @Test
    fun openingAvoidanceLeavesHollowInteriorsAndContourGapsOpen() {
        val opening: OpeningBatteryMorph = openSides()
        Assert.assertEquals(20.0f, openingX(20f, 10f, opening, true), .0001f)
        Assert.assertEquals(20.0f, openingX(20f, 20f, opening, true), .0001f)
        Assert.assertEquals(20.0f, openingX(20f, 0f, opening, false), .0001f)
    }

    @Test
    fun openingAvoidanceStillRoutesDotsTouchingThePaintedStroke() {
        val opening: OpeningBatteryMorph = openSides()
        Assert.assertEquals(7.0f, openingX(10f, 10f, opening, true), .0001f)
        Assert.assertEquals(33.0f, openingX(30f, 10f, opening, false), .0001f)
        Assert.assertEquals(7.0f, openingX(10f, 0f, opening, true), .0001f)
    }

    @Test
    fun openingAvoidanceKeepsTheProjectedSolidBatteryClearWhenFilled() {
        val opening: OpeningBatteryMorph = openSides()
        opening.outlineAlpha = 0f
        opening.bodyAlpha = 1f
        Assert.assertEquals(39.0f, openingX(50f, 10f, opening, true), .0001f)
        Assert.assertEquals(61.0f, openingX(50f, 10f, opening, false), .0001f)
    }

    @Test
    fun pinnedBatteryRoutesOnlyIntersectingSignalBoundsToTheirOwnSide() {
        Assert.assertEquals(16f, SignalMotionPolicy.clearBatteryX(20f, 10f, 2f, 2f, 18f, 0f, 30f, 20f, true), .0001f)
        Assert.assertEquals(32f, SignalMotionPolicy.clearBatteryX(20f, 10f, 2f, 2f, 18f, 0f, 30f, 20f, false), .0001f)
        Assert.assertEquals(20f, SignalMotionPolicy.clearBatteryX(20f, 30f, 2f, 2f, 18f, 0f, 30f, 20f, true), .0001f)
        Assert.assertEquals(10f, SignalMotionPolicy.clearBatteryX(10f, 10f, 2f, 2f, 18f, 0f, 30f, 20f, true), .0001f)
    }

    @Test
    fun batteryAvoidanceDoesNotTeleportSignalsAcrossObstacleEdges() {
        val epsilon = 0.001f
        Assert.assertEquals(
            0.0f,
            SignalMotionPolicy.clearBatteryX(
                9.0f + epsilon, 5.0f,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, false, true
            )
                    - SignalMotionPolicy.clearBatteryX(
                9.0f - epsilon, 5.0f,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, false, true
            ),
            0.01f
        )
        Assert.assertEquals(
            0.0f,
            SignalMotionPolicy.clearBatteryX(
                31.0f + epsilon, 5.0f,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, true, true
            )
                    - SignalMotionPolicy.clearBatteryX(
                31.0f - epsilon, 5.0f,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, true, true
            ),
            0.01f
        )
        Assert.assertEquals(
            0.0f,
            SignalMotionPolicy.clearBatteryX(
                20.0f, -1.0f - epsilon,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, false, true
            )
                    - SignalMotionPolicy.clearBatteryX(
                20.0f, -1.0f + epsilon,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, false, true
            ),
            0.01f
        )
        Assert.assertEquals(
            0.0f,
            SignalMotionPolicy.clearBatteryX(
                20.0f, 11.0f + epsilon,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, true, true
            )
                    - SignalMotionPolicy.clearBatteryX(
                20.0f, 11.0f - epsilon,
                1.0f, 1.0f, 10.0f, 0.0f, 30.0f, 10.0f, true, true
            ),
            0.01f
        )
    }

    @Test
    fun signalTimelineSlowsBothEndsWithoutChangingDirection() {
        Assert.assertTrue(SignalMotionPolicy.signalProgress(0.10f) < 0.10f)
        Assert.assertEquals(0.5f, SignalMotionPolicy.signalProgress(0.5f), EPSILON)
        Assert.assertTrue(SignalMotionPolicy.signalProgress(0.90f) > 0.90f)
        Assert.assertEquals(
            0.0f,
            SignalMotionPolicy.signalProgress(0.0f),
            EPSILON
        )
        Assert.assertEquals(
            1.0f,
            SignalMotionPolicy.signalProgress(1.0f),
            EPSILON
        )
    }

    @Test
    fun stagesMeetAtTheirBoundariesWithoutGaps() {
        Assert.assertEquals(0.0f, SignalMotionPolicy.trimProgress(1.0f), EPSILON)
        Assert.assertEquals(1.0f, SignalMotionPolicy.trimProgress(0.70f), EPSILON)
        Assert.assertEquals(0.0f, SignalMotionPolicy.moveProgress(0.70f), EPSILON)
        Assert.assertEquals(1.0f, SignalMotionPolicy.moveProgress(0.0f), EPSILON)
        Assert.assertEquals(0.0f, SignalMotionPolicy.rowProgress(0.25f), EPSILON)
        Assert.assertEquals(1.0f, SignalMotionPolicy.rowProgress(0.0f), EPSILON)
        Assert.assertEquals(0.0f, SignalMotionPolicy.barProgress(0.25f), EPSILON)
        Assert.assertEquals(1.0f, SignalMotionPolicy.barProgress(0.0f), EPSILON)
    }

    @Test
    fun moveStageHoldsSignalPointsNearTheSourceLonger() {
        Assert.assertTrue(SignalMotionPolicy.moveProgress(0.60f) < 0.22f)
        Assert.assertTrue(SignalMotionPolicy.moveProgress(0.50f) < 0.80f)
    }

    @Test
    fun moveStageIsAboutOneFifthAtHalfCollapsedPanel() {
        val halfPanel = SignalMotionPolicy.signalProgress(0.50f)

        Assert.assertEquals(
            0.20f,
            SignalMotionPolicy.moveProgress(halfPanel),
            0.03f
        )
    }

    @Test
    fun moveStageRemainsActiveAtQuarterExpandedPanel() {
        val quarterPanel = SignalMotionPolicy.signalProgress(0.25f)

        Assert.assertTrue(
            SignalMotionPolicy.moveProgress(quarterPanel)
                    < 0.90f
        )
    }

    @Test
    fun coordinateVisitsSourceAnchorTargetAnchorAndTargetInOrder() {
        Assert.assertEquals(
            70.0f,
            SignalMotionPolicy.coordinate(
                70.0f, 10.0f, 100.0f, 130.0f,
                0.0f, 0.0f, 0.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            10.0f,
            SignalMotionPolicy.coordinate(
                70.0f, 10.0f, 100.0f, 130.0f,
                1.0f, 0.0f, 0.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            100.0f,
            SignalMotionPolicy.coordinate(
                70.0f, 10.0f, 100.0f, 130.0f,
                1.0f, 1.0f, 0.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            130.0f,
            SignalMotionPolicy.coordinate(
                70.0f, 10.0f, 100.0f, 130.0f,
                1.0f, 1.0f, 1.0f
            ),
            EPSILON
        )
    }

    @Test
    fun dotsCollapseInReverseRevealOrder() {
        Assert.assertEquals(0.0f, SignalMotionPolicy.dotTrim(0, 1.0f), 0.0f)
        Assert.assertEquals(0.648f, SignalMotionPolicy.dotTrim(3, 0.2f), EPSILON)
        Assert.assertEquals(0.0f, SignalMotionPolicy.dotTrim(2, 0.2f), 0.0f)
        Assert.assertEquals(1.0f, SignalMotionPolicy.dotScale(0, 1.0f, 0.0f), 0.0f)
        Assert.assertEquals(0.0f, SignalMotionPolicy.dotScale(3, 1.0f, 0.0f), 0.0f)
        Assert.assertEquals(1.0f, SignalMotionPolicy.dotScale(3, 1.0f, 1.0f), 0.0f)
    }

    @Test
    fun expandedRowsRetainAllFourElementsBeforeMerging() {
        for (slot in 0..<StackedSignals.Companion.BARS) {
            Assert.assertEquals(
                0.25f,
                SignalMotionPolicy.elementAlpha(0.25f, 1.0f, 0.0f, slot == 3),
                EPSILON
            )
        }
    }

    @Test
    fun collapsedLayeredRowDoesNotPaintItsDuplicatedFourthDot() {
        for (slot in 0..<StackedSignals.Companion.BARS) {
            Assert.assertEquals(
                if (slot == 3) 0.0f else 0.188f,
                SignalMotionPolicy.elementAlpha(1.0f, 0.188f, 1.0f, slot == 3),
                EPSILON
            )
        }
    }

    @Test
    fun mergingElementsFadeWhileRetainedElementsReachTheirTargetOpacity() {
        Assert.assertEquals(
            0.2f,
            SignalMotionPolicy.elementAlpha(0.4f, 1.0f, 0.5f, true), EPSILON
        )
        Assert.assertEquals(
            0.7f,
            SignalMotionPolicy.elementAlpha(0.4f, 1.0f, 0.5f, false), EPSILON
        )
        Assert.assertEquals(
            1.0f,
            SignalMotionPolicy.elementAlpha(0.4f, 1.0f, 1.0f, false), EPSILON
        )
    }

    @Test
    fun closingStagesNeverMoveBackwardWhenExpansionDecreases() {
        var previousTrim = -1.0f
        var previousMove = -1.0f
        var previousRow = -1.0f
        var previousBar = -1.0f
        for (step in 100 downTo 0) {
            val expansion = step / 100.0f
            val trim = SignalMotionPolicy.trimProgress(expansion)
            val move = SignalMotionPolicy.moveProgress(expansion)
            val row = SignalMotionPolicy.rowProgress(expansion)
            val bar = SignalMotionPolicy.barProgress(expansion)
            Assert.assertEquals(1.0f, if (trim >= previousTrim) 1.0f else 0.0f, 0.0f)
            Assert.assertEquals(1.0f, if (move >= previousMove) 1.0f else 0.0f, 0.0f)
            Assert.assertEquals(1.0f, if (row >= previousRow) 1.0f else 0.0f, 0.0f)
            Assert.assertEquals(1.0f, if (bar >= previousBar) 1.0f else 0.0f, 0.0f)
            previousTrim = trim
            previousMove = move
            previousRow = row
            previousBar = bar
        }
    }

    @Test
    fun rowAnchorsUseTheAverageTargetForEachSignalRow() {
        val target = StackedSignals.Target()
        for (index in 0..<StackedSignals.Companion.ELEMENTS) {
            val row: Int = index / StackedSignals.Companion.BARS
            val slot: Int = index % StackedSignals.Companion.BARS
            target.x[index] = 100.0f + (row * 40.0f) + slot
            target.y[index] = 20.0f + (row * 10.0f) - slot
        }

        SignalMotionPolicy.updateRowAnchors(target, 2)

        Assert.assertEquals(101.5f, target.rowAnchorX[0], EPSILON)
        Assert.assertEquals(18.5f, target.rowAnchorY[0], EPSILON)
        Assert.assertEquals(141.5f, target.rowAnchorX[1], EPSILON)
        Assert.assertEquals(28.5f, target.rowAnchorY[1], EPSILON)
    }

    companion object {
        private fun openSides(): OpeningBatteryMorph {
            val opening = OpeningBatteryMorph()
            opening.points = floatArrayOf(10f, 0f, 10f, 20f, 30f, 20f, 30f, 0f)
            opening.contourStarts = booleanArrayOf(true, false, true, false)
            opening.strokeWidth = 4f
            opening.outlineAlpha = 1f
            opening.valid = true
            return opening
        }

        private fun openingX(x: Float, y: Float, opening: OpeningBatteryMorph, preferLeft: Boolean): Float {
            return SignalMotionPolicy.clearOpeningBatteryX(
                x, y, 1f, 1f, opening,
                40f, 0f, 60f, 20f, preferLeft
            )
        }

        private const val EPSILON = 0.0001f
    }
}
