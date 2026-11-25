package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.geometry.SignalSpacing
import org.junit.Assert
import org.junit.Test

class SignalSpacingGeometryTest {
    @Test
    fun singleRingDotsAreEvenlySpacedAlongTheArc() {
        val weight = 1.2f
        val points = (0..3).map { index ->
            floatArrayOf(
                SignalSpacing.singleRingX(index, weight),
                SignalSpacing.singleRingY(index, weight)
            )
        }
        val distances = (0..2).map { index ->
            val dx = points[index + 1][0] - points[index][0]
            val dy = points[index + 1][1] - points[index][1]
            kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }
        val referenceDx = SignalSpacing.ringX(1, weight) - SignalSpacing.ringX(0, weight)
        val referenceDy = SignalSpacing.ringY(1, weight) - SignalSpacing.ringY(0, weight)
        val correctOuterSpacing = kotlin.math.hypot(referenceDx.toDouble(), referenceDy.toDouble()).toFloat()
        Assert.assertEquals(correctOuterSpacing, distances[0], 0.0001f)
        Assert.assertEquals(distances[0], distances[1], 0.0001f)
        Assert.assertEquals(distances[1], distances[2], 0.0001f)
    }

    @Test
    fun scalarRowsMatchExistingBatchGeometry() {
        val expected = SignalSpacing.rowCentres(
            4,
            60.0f,
            1.2f,
            5.7f,
            84.0f
        )
        for (index in expected.indices) {
            Assert.assertEquals(
                expected[index],
                SignalSpacing.rowCentre(
                    index,
                    4,
                    60.0f,
                    1.2f,
                    5.7f,
                    84.0f
                ),
                0.0f
            )
        }
    }

    @Test
    fun scalarPairsMatchExistingBatchGeometry() {
        val expected = SignalSpacing.pairCentres(
            58.0f,
            1.2f,
            4.4f
        )
        for (index in expected.indices) {
            Assert.assertEquals(
                expected[index],
                SignalSpacing.pairCentre(
                    index,
                    58.0f,
                    1.2f,
                    4.4f
                ),
                0.0f
            )
        }
    }

    @Test
    fun scalarCoordinatesClampInvalidIndices() {
        Assert.assertEquals(
            SignalSpacing.ringX(0, 1.2f),
            SignalSpacing.ringX(-4, 1.2f),
            0.0f
        )
        Assert.assertEquals(
            SignalSpacing.ringX(3, 1.2f),
            SignalSpacing.ringX(99, 1.2f),
            0.0f
        )
    }
}
