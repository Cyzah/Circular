package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class StatusFontWeightTest {
    @Test
    fun batteryDefaultMatchesTheOld130PercentBoldAndLightEndStaysReadable() {
        Assert.assertEquals(700, StatusFontWeight.battery(100).toLong())
        Assert.assertEquals(600, StatusFontWeight.technology(100).toLong())
        Assert.assertEquals(400, StatusFontWeight.battery(50).toLong())
        Assert.assertEquals(400, StatusFontWeight.technology(80).toLong())
        Assert.assertEquals(900, StatusFontWeight.battery(180).toLong())
        Assert.assertEquals(900, StatusFontWeight.technology(180).toLong())
    }

    @Test
    fun weightIsMonotonicAcrossTheWholeSliderRangeAndClampedOutsideIt() {
        for (percent in 51..180) {
            Assert.assertTrue(StatusFontWeight.battery(percent) > StatusFontWeight.battery(percent - 1))
        }
        for (percent in 81..180) {
            Assert.assertTrue(StatusFontWeight.technology(percent) > StatusFontWeight.technology(percent - 1))
        }
        Assert.assertEquals(400, StatusFontWeight.battery(-10).toLong())
        Assert.assertEquals(400, StatusFontWeight.technology(-10).toLong())
        Assert.assertEquals(900, StatusFontWeight.battery(300).toLong())
        Assert.assertEquals(900, StatusFontWeight.technology(300).toLong())
    }

    @Test
    fun batteryHeavyEndAddsStrokeBeyondTheBlackFaceWithoutChangingDefault() {
        Assert.assertEquals(0.0f, StatusFontWeight.batteryStrokeRatio(50), 0.00001f)
        Assert.assertEquals(0.0f, StatusFontWeight.batteryStrokeRatio(100), 0.00001f)
        Assert.assertEquals(0.03f, StatusFontWeight.batteryStrokeRatio(140), 0.00001f)
        Assert.assertEquals(0.06f, StatusFontWeight.batteryStrokeRatio(180), 0.00001f)
        Assert.assertEquals(0.06f, StatusFontWeight.batteryStrokeRatio(300), 0.00001f)
        for (percent in 101..180) {
            Assert.assertTrue(
                StatusFontWeight.batteryStrokeRatio(percent)
                        > StatusFontWeight.batteryStrokeRatio(percent - 1)
            )
        }
    }
}
