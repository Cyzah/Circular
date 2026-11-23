package com.cyzah.circular.core.graphics

import com.cyzah.circular.core.graphics.TechnologyAvoidanceTest
import kotlin.math.abs
import org.junit.Assert
import org.junit.Test

class TechnologyAvoidanceTest {
    @Test
    fun overlappingLabelClearsTheWholeBatteryOnEitherSide() {
        Assert.assertEquals(-8.0f, shift(10.0f, 8.0f, 20.0f, 16.0f, true), 0.0f)
        Assert.assertEquals(24.0f, shift(10.0f, 8.0f, 20.0f, 16.0f, false), 0.0f)
    }

    @Test
    fun distantLabelsAndLabelsAlreadyOnTheChosenSideStayPut() {
        Assert.assertEquals(0.0f, shift(-20.0f, 8.0f, -10.0f, 16.0f, true), 0.0f)
        Assert.assertEquals(0.0f, shift(60.0f, 8.0f, 70.0f, 16.0f, false), 0.0f)
        Assert.assertEquals(0.0f, shift(10.0f, -4.0f, 20.0f, 8.0f, true), 0.0f)
        Assert.assertEquals(0.0f, shift(10.0f, 32.0f, 20.0f, 40.0f, false), 0.0f)
    }

    @Test
    fun approachBeginsBeforeHorizontalContact() {
        Assert.assertEquals(-4.0f, shift(2.0f, 8.0f, 12.0f, 16.0f, true), 0.0001f)
        Assert.assertEquals(12.0f, shift(2.0f, 8.0f, 12.0f, 16.0f, false), 0.0001f)
        Assert.assertEquals(-8.0f, shift(6.0f, 8.0f, 16.0f, 16.0f, true), 0.0001f)
    }

    @Test
    fun chosenSideDoesNotSwitchWhileTheOriginalBaselineCrossesTheBattery() {
        // A vertical-intersection early return would jump at the top/bottom boundary.
        Assert.assertEquals(-32.0f, shift(10.0f, 32.0f, 20.0f, 40.0f, true), 0.0f)
        Assert.assertEquals(36.0f, shift(10.0f, -4.0f, 20.0f, 8.0f, false), 0.0f)
    }

    @Test
    fun longAndTallGlyphBoundsStayClearOfCompleteLineAndPointShapes() {
        val batteries = arrayOf<FloatArray>(
            floatArrayOf(50.0f, 20.0f, 130.0f, 60.0f),
            floatArrayOf(60.0f, 10.0f, 66.0f, 70.0f),
            floatArrayOf(55.0f, 30.0f, 61.0f, 36.0f)
        )
        for (battery in batteries) {
            for (width in floatArrayOf(5.0f, 18.0f, 42.0f)) {
                for (height in floatArrayOf(8.0f, 16.0f, 36.0f)) {
                    for (above in booleanArrayOf(true, false)) {
                        for (step in 0..600) {
                            val left = step / 4.0f
                            val top = 32.0f
                            val shift = TechnologyAvoidance.baselineShift(
                                left, top, left + width, top + height,
                                battery[0], battery[1], battery[2], battery[3],
                                2.0f, height, above
                            )
                            Assert.assertTrue(java.lang.Float.isFinite(shift))
                            Assert.assertTrue(if (above) shift <= 0.0f else shift >= 0.0f)
                            if (left + width >= battery[0] - 2.0f
                                && left <= battery[2] + 2.0f
                            ) {
                                Assert.assertTrue(
                                    if (above)
                                        top + height + shift <= battery[1] - 1.999f
                                    else
                                        top + shift >= battery[3] + 1.999f
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun crossingBothHorizontalEdgesIsContinuousAndReversible() {
        val forward = FloatArray(6001)
        for (above in booleanArrayOf(true, false)) {
            var previous = 0.0f
            for (step in forward.indices) {
                val left = -10.0f + step * 0.01f
                val current: Float = shift(left, 8.0f, left + 10.0f, 16.0f, above)
                Assert.assertTrue(abs(current - previous) < 0.05f)
                forward[step] = current
                previous = current
            }
            Assert.assertEquals(0.0f, previous, 0.0f)
            for (step in forward.indices.reversed()) {
                val left = -10.0f + step * 0.01f
                Assert.assertEquals(
                    forward[step],
                    shift(left, 8.0f, left + 10.0f, 16.0f, above), 0.0f
                )
            }
        }
    }

    @Test
    fun emptyOrInvalidBoundsDoNotMoveTheLabel() {
        Assert.assertEquals(0.0f, shift(20.0f, 8.0f, 20.0f, 16.0f, true), 0.0f)
        Assert.assertEquals(0.0f, shift(10.0f, 8.0f, 20.0f, 8.0f, true), 0.0f)
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.baselineShift(
                10.0f, 8.0f, 20.0f, 16.0f, 18.0f, 10.0f, 18.0f, 30.0f,
                2.0f, 8.0f, true
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.baselineShift(
                10.0f, 8.0f, 20.0f, 16.0f, 18.0f, 10.0f, 40.0f, 30.0f,
                2.0f, 0.0f, true
            ), 0.0f
        )
        val coordinates = floatArrayOf(
            10.0f, 8.0f, 20.0f, 16.0f,
            18.0f, 10.0f, 40.0f, 30.0f, 2.0f, 8.0f
        )
        for (coordinate in coordinates.indices) {
            val original = coordinates[coordinate]
            for (invalid in floatArrayOf(
                Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )) {
                coordinates[coordinate] = invalid
                Assert.assertEquals(
                    0.0f, TechnologyAvoidance.baselineShift(
                        coordinates[0], coordinates[1], coordinates[2], coordinates[3],
                        coordinates[4], coordinates[5], coordinates[6], coordinates[7],
                        coordinates[8], coordinates[9], true
                    ), 0.0f
                )
            }
            coordinates[coordinate] = original
        }
    }

    companion object {
        private fun shift(
            left: Float, top: Float, right: Float, bottom: Float,
            above: Boolean
        ): Float {
            return TechnologyAvoidance.baselineShift(
                left, top, right, bottom,
                18.0f, 10.0f, 40.0f, 30.0f, 2.0f, 8.0f, above
            )
        }
    }
}
