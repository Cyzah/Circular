package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.geometry.SignalSpacing
import org.junit.Assert
import org.junit.Test

class SignalSpacingGeometryTest {
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
            SignalSpacing.rowCentre(
                -4,
                4,
                60.0f,
                1.2f,
                5.7f,
                84.0f
            ),
            0.0f
        )
        Assert.assertEquals(
            SignalSpacing.ringX(3, 1.2f),
            SignalSpacing.rowCentre(
                99,
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
