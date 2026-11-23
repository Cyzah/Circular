package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import org.junit.Assert
import org.junit.Test

class BatteryTransferGeometryContractTest {
    @Test
    fun batteryGeometryMatchesProvidedSvg() {
        Assert.assertEquals(198.44f, BatteryTransferConfig.SVG_WIDTH, 0.001f)
        Assert.assertEquals(94.2f, BatteryTransferConfig.SVG_HEIGHT, 0.001f)
        Assert.assertEquals(0.0f, BatteryTransferConfig.BODY_X, 0.001f)
        Assert.assertEquals(0.0f, BatteryTransferConfig.BODY_Y, 0.001f)
        Assert.assertEquals(181.3855f, BatteryTransferConfig.BODY_WIDTH, 0.001f)
        Assert.assertEquals(94.2f, BatteryTransferConfig.BODY_HEIGHT, 0.001f)
        Assert.assertEquals(38.0f, BatteryTransferConfig.BODY_RADIUS, 0.001f)
        Assert.assertEquals(187.1006f, BatteryTransferConfig.TERMINAL_START_X, 0.001f)
        Assert.assertEquals(198.44f, BatteryTransferConfig.TERMINAL_END_X, 0.001f)
        Assert.assertEquals(47.1f, BatteryTransferConfig.TERMINAL_CENTRE_Y, 0.001f)
        Assert.assertEquals(14.1867f, BatteryTransferConfig.TERMINAL_HALF_HEIGHT, 0.001f)
    }

    @Test
    fun cubicControlsMatchTheProvidedBatteryPath() {
        Assert.assertEquals(44.0990f, BatteryTransferConfig.BODY_CORNER_X, 0.001f)
        Assert.assertEquals(44.1492f, BatteryTransferConfig.BODY_CORNER_Y, 0.001f)
        Assert.assertEquals(137.2865f, BatteryTransferConfig.BODY_RIGHT_CURVE_X, 0.001f)
        Assert.assertEquals(169.5925f, BatteryTransferConfig.BODY_RIGHT_CONTROL_X, 0.001f)
        Assert.assertEquals(11.7930f, BatteryTransferConfig.BODY_LEFT_CONTROL_X, 0.001f)
        Assert.assertEquals(11.8034f, BatteryTransferConfig.BODY_TOP_CONTROL_Y, 0.001f)
        Assert.assertEquals(82.3966f, BatteryTransferConfig.BODY_BOTTOM_CONTROL_Y, 0.001f)
        Assert.assertEquals(61.2867f, BatteryTransferConfig.TERMINAL_BOTTOM_Y, 0.001f)
        Assert.assertEquals(32.9133f, BatteryTransferConfig.TERMINAL_TOP_Y, 0.001f)
    }

    @Test
    fun numberLayoutScalesWithTheNewSvg() {
        Assert.assertEquals(271.2333f, BatteryTransferConfig.OUTSIDE_SVG_WIDTH, 0.001f)
        Assert.assertEquals(204.0f, BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG, 0.001f)
        Assert.assertEquals(86.8179f, BatteryTransferConfig.NUMBER_FONT_SIZE_SVG, 0.001f)
        Assert.assertEquals(99.2200f, BatteryTransferConfig.OUTSIDE_NUMBER_FONT_SIZE_SVG, 0.001f)
        Assert.assertEquals(90.6804f, BatteryTransferConfig.NUMBER_INSIDE_X_SVG, 0.001f)
    }

    @Test
    fun controlCenterReadoutsGainOneSizeInsideAndOutside() {
        Assert.assertEquals(
            49.0f * BatteryTransferConfig.SVG_HEIGHT / BatteryTransferConfig.FULL_HEIGHT_UNITS,
            BatteryTransferConfig.numberTextSizeSvg(
                BatteryNumberPlacement.INSIDE
            ),
            0.001f
        )
        Assert.assertEquals(
            49.0f,
            BatteryTransferConfig.numberTextSizeSvg(
                BatteryNumberPlacement.INSIDE
            )
                    * BatteryTransferConfig.FULL_HEIGHT_UNITS
                    / BatteryTransferConfig.SVG_HEIGHT,
            0.001f
        )
        Assert.assertEquals(
            49.0f,
            BatteryTransferConfig.CONTROL_CENTER_NUMBER_UNITS,
            0.001f
        )
        Assert.assertEquals(
            56.0f,
            BatteryTransferConfig.numberTextSizeSvg(
                BatteryNumberPlacement.OUTSIDE
            )
                    * BatteryTransferConfig.FULL_HEIGHT_UNITS
                    / BatteryTransferConfig.SVG_HEIGHT,
            0.001f
        )
    }

    @Test
    fun batteryAndTerminalShareOneContinuousPercentage() {
        Assert.assertEquals(
            0.0f,
            BatteryTransferConfig.chargedBodyRightSvg(0),
            0.001f
        )
        Assert.assertEquals(
            144.5437f,
            BatteryTransferConfig.chargedBodyRightSvg(75),
            0.001f
        )
        Assert.assertEquals(
            181.3855f,
            BatteryTransferConfig.chargedBodyRightSvg(95),
            0.001f
        )
        Assert.assertEquals(
            181.3855f,
            BatteryTransferConfig.chargedBodyRightSvg(100),
            0.001f
        )
        Assert.assertEquals(
            181.3855f,
            BatteryTransferConfig.chargedBodyRightSvg(140),
            0.001f
        )
        try {
            val method =
                BatteryTransferConfig::class.java.getDeclaredMethod(
                    "chargedTerminalRightSvg",
                    Int::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                187.1006f,
                (method.invoke(null, 75) as Float),
                0.001f
            )
            Assert.assertEquals(
                194.5855f,
                (method.invoke(null, 98) as Float),
                0.001f
            )
            Assert.assertEquals(
                198.44f,
                (method.invoke(null, 100) as Float),
                0.001f
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing continuous battery-and-terminal charge geometry",
                error
            )
        }
        Assert.assertEquals(
            192.7249f,
            BatteryTransferConfig.chargedBodyRightSvg(100)
                    + (BatteryTransferConfig.TERMINAL_END_X
                    - BatteryTransferConfig.TERMINAL_START_X),
            0.001f
        )
    }

    @Test
    fun controlCenterBatteryDoesNotDrawChargeSymbol() {
        Assert.assertFalse(BatteryTransferConfig.chargeSymbolEnabled())
    }

    @Test
    fun outsideNumberWeightIsCappedWithoutAffectingLightNumbers() {
        Assert.assertEquals(
            600,
            BatteryTransferConfig.outsideNumberFontWeight(120).toLong()
        )
        Assert.assertEquals(
            390,
            BatteryTransferConfig.outsideNumberFontWeight(60).toLong()
        )
    }
}
