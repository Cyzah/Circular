package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.TechnologyAvoidance
import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.abs
import org.junit.Assert
import org.junit.Test

class TechnologyAvoidanceEndpointTest {
    @Test
    fun narrowVerticalEndpointGapDoesNotLeaveAResidualOffset() {
        // The final glyph is already half a pixel above the point.
        Assert.assertEquals(
            0.0f, TechnologyAvoidance.baselineShift(
                10.0f, 2.0f, 20.0f, 9.5f, 18.0f, 10.0f, 22.0f, 14.0f,
                2.0f, 8.0f, true, -2.0f, 0.5f
            ), 0.0f
        )
    }

    @Test
    fun horizontalEndpointGapReleasesAvoidanceContinuously() {
        var previous = Float.NaN
        for (step in 0..1000) {
            val p = step / 1000.0f
            val right = Motion.mix(20.0f, 17.0f, p)
            val shift = TechnologyAvoidance.baselineShift(
                right - 10.0f, 8.0f, right, 16.0f,
                18.0f, 10.0f, 22.0f, 14.0f,
                2.0f, 8.0f, true, 1.0f, -6.0f
            )
            if (java.lang.Float.isFinite(previous)) {
                Assert.assertTrue(abs(shift - previous) < 0.1f)
            }
            if (right >= 18.0f) {
                Assert.assertTrue(16.0f + shift <= 10.0f)
            }
            previous = shift
        }
        Assert.assertEquals(0.0f, previous, 0.0f)
    }
}
