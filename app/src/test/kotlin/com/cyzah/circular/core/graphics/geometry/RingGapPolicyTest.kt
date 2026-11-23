package com.cyzah.circular.core.graphics.geometry

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import org.junit.Assert
import org.junit.Test

class RingGapPolicyTest {
    @Test
    fun singleCardRoundCapsClearEverySignalDotAcrossAllWeights() {
        for (percent in 80..180) {
            val weight = percent * 0.012f
            val halfAngle = RingGapPolicy.singleHalfAngle(weight)
            val radians = Math.toRadians(halfAngle.toDouble())
            val capOffsetX = 50.0f * sin(radians).toFloat()
            val capY = 58.0f + (50.0f * cos(radians).toFloat())
            val combinedRadii = (3.75f + 4.9f) * weight
            for (side in intArrayOf(-1, 1)) {
                val capX = 60.0f + (side * capOffsetX)
                for (dot in 0..3) {
                    val dx = capX - IconGeometry.singleDotX(dot, weight)
                    val dy = capY - IconGeometry.singleDotY(dot, weight)
                    val clearance = hypot(dx.toDouble(), dy.toDouble()).toFloat() - combinedRadii
                    Assert.assertTrue(
                        ("weight=" + percent + ", side=" + side
                                + ", dot=" + dot + ", clearance=" + clearance),
                        clearance >= 0.999f
                    )
                }
            }
        }
    }

    @Test
    fun defaultSingleCardGapRetainsItsExistingAppearance() {
        Assert.assertEquals(57.5f, RingGapPolicy.singleHalfAngle(1.2f), 0.0001f)
    }
}
