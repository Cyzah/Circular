package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.TechnologyAvoidance
import com.cyzah.circular.hook.runtime.TechnologyRowSeparationTest
import org.junit.Assert
import org.junit.Test

class TechnologyRowSeparationTest {
    @Test
    fun pushesUpperSecondaryRowAboveThePrimaryRow() {
        Assert.assertEquals(
            -10.0f, TechnologyAvoidance.secondaryShift(
                20.0f, 30.0f, 18.0f, 28.0f, 2.0f, true
            ), 0.0f
        )
    }

    @Test
    fun pushesLowerSecondaryRowBelowThePrimaryRow() {
        Assert.assertEquals(
            10.0f, TechnologyAvoidance.secondaryShift(
                10.0f, 20.0f, 12.0f, 22.0f, 2.0f, false
            ), 0.0f
        )
    }

    @Test
    fun leavesAlreadySeparatedRowsUnchanged() {
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.secondaryShift(
                10.0f, 20.0f, 30.0f, 40.0f, 2.0f, false
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.secondaryShift(
                20.0f, 30.0f, 0.0f, 10.0f, 2.0f, true
            ), 0.0f
        )
    }

    @Test
    fun bothRowsKeepTheirOrderDuringPartialAndFullBatteryAvoidance() {
        for (above in booleanArrayOf(true, false)) {
            for (step in 0..1000) {
                // 6.75dp row spacing at density 3, with 16px glyph bounds.
                val right = 52.0f + step * 0.04f
                val topShift: Float = batteryShift(right, -18.125f, -2.125f, above)
                val bottomShift: Float = batteryShift(right, 2.125f, 18.125f, above)
                var top = -18.125f + topShift
                var bottom = 2.125f + bottomShift
                // Card 2 remains the primary when both travel above the battery;
                // card 1 remains the primary in the mirrored below case.
                if (above) {
                    top += TechnologyAvoidance.secondaryShift(
                        bottom, bottom + 16.0f,
                        top, top + 16.0f, 3.0f, true
                    )
                } else {
                    bottom += TechnologyAvoidance.secondaryShift(
                        top, top + 16.0f,
                        bottom, bottom + 16.0f, 3.0f, false
                    )
                }
                Assert.assertTrue("Rows overlap at step " + step, top + 19.0f <= bottom + 0.00001f)
                if (right >= 80.0f) {
                    Assert.assertTrue(if (above) bottom + 16.0f <= -20.25f else top >= 20.25f)
                }
            }
        }
    }

    @Test
    fun fullyExpandedRowsDoNotMoveForAnAlreadyClearBattery() {
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.motionBaselineShift(
                41f, -18.125f, 69f, -2.125f, 80f, -20.25f, 160f, 20.25f,
                3f, 16f, true, Float.NaN, Float.NaN, 1f
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.motionBaselineShift(
                41f, 2.125f, 69f, 18.125f, 80f, -20.25f, 160f, 20.25f,
                3f, 16f, true, Float.NaN, Float.NaN, 1f
            ), 0.0f
        )
    }

    @Test
    fun realIntersectionStillReceivesFullClearanceAtFullExpansion() {
        Assert.assertEquals(
            -38.375f, TechnologyAvoidance.motionBaselineShift(
                60f, 2.125f, 88f, 18.125f, 80f, -20.25f, 160f, 20.25f,
                3f, 16f, true, Float.NaN, Float.NaN, 1f
            ), 0.0001f
        )
    }

    companion object {
        private fun batteryShift(right: Float, top: Float, bottom: Float, above: Boolean): Float {
            return TechnologyAvoidance.baselineShift(
                right - 28.0f, top, right, bottom,
                80f, -20.25f, 160f, 20.25f, 3f, 16f, above
            )
        }
    }
}
