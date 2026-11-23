package com.cyzah.circular.domain.settings

import org.junit.Assert
import org.junit.Test

class BatteryNumberPlacementTest {
    @Test
    fun defaultsToOutsideRight() {
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            BatteryNumberPlacement.DEFAULT
        )
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            BatteryNumberPlacement.from(Int.MIN_VALUE - 1)
        )
    }

    @Test
    fun mapsAllThreeChoices() {
        Assert.assertEquals(
            BatteryNumberPlacement.HIDDEN,
            BatteryNumberPlacement.from(0)
        )
        Assert.assertEquals(
            BatteryNumberPlacement.INSIDE,
            BatteryNumberPlacement.from(1)
        )
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            BatteryNumberPlacement.from(2)
        )
    }

    @Test
    fun clampsInvalidValuesToDefault() {
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            BatteryNumberPlacement.from(-1)
        )
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            BatteryNumberPlacement.from(99)
        )
    }
}
