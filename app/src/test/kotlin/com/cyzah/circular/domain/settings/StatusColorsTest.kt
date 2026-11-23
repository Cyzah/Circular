package com.cyzah.circular.domain.settings

import org.junit.Assert
import org.junit.Test

class StatusColorsTest {
    @Test
    fun transparentControlCenterDigitsRetainTheirDarkRgb() {
        Assert.assertEquals(-0xe5e5e6, StatusColors.controlCenterInnerNumberTint().toLong())
        Assert.assertEquals(
            26, DisplayPolicy.paintAlpha(
                StatusColors.controlCenterInnerNumberTint(), 255, 0.10f
            ).toLong()
        )
    }

    @Test
    fun opaqueBlackBatteryNumberBecomesEightyPercentBlack() {
        Assert.assertEquals(
            -0x34000000,
            StatusColors.batteryNumberTint(-0x1000000).toLong()
        )
    }

    @Test
    fun nonBlackBatteryNumberColorsAreNotChanged() {
        Assert.assertEquals(
            -0x1,
            StatusColors.batteryNumberTint(-0x1).toLong()
        )
        Assert.assertEquals(
            -0xfefeff,
            StatusColors.batteryNumberTint(-0xfefeff).toLong()
        )
    }

    @Test
    fun insideBlackBatteryNumberUsesMoreTransparentGray() {
        Assert.assertEquals(
            -0x57e5e5e6,
            StatusColors.batteryInnerNumberTint(-0x1000000).toLong()
        )
        Assert.assertEquals(
            -0x1,
            StatusColors.batteryInnerNumberTint(-0x1).toLong()
        )
    }
}
