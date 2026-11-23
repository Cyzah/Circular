package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayoutTest
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import org.junit.Assert
import org.junit.Test

class BatteryShapeLayoutTest {
    @Test
    fun transferPointKeepsItsPixelDiameterWhenBatteryFrameUsesAnotherScale() {
        val units = BatteryShapeLayout.transferPointDiameterUnits(7.5f, 0.6f, 0.75f)
        Assert.assertEquals(6.0f, units, 0.0001f)
        Assert.assertEquals(4.5f, units * 0.75f, 0.0001f)
    }

    @Test
    fun outsideNumberKeepsTheFullSvgBatteryScale() {
        Assert.assertEquals(
            112.0f,
            BatteryShapeLayout.widthUnits(BatteryNumberPlacement.OUTSIDE),
            EPSILON
        )
        Assert.assertEquals(
            -10.085f,
            BatteryShapeLayout.centreXUnits(
                BatteryNumberPlacement.OUTSIDE
            ),
            EPSILON
        )
        Assert.assertEquals(
            102.3744f,
            BatteryShapeLayout.bodyWidthUnits(
                BatteryNumberPlacement.OUTSIDE
            ),
            EPSILON
        )
        Assert.assertEquals(
            53.167f,
            BatteryShapeLayout.bodyHeightUnits(
                BatteryNumberPlacement.OUTSIDE
            ),
            EPSILON
        )
    }

    @Test
    fun chargedClipUsesTheFullSvgViewportSoCornersStayRounded() {
        Assert.assertEquals(0.0f, BatteryShapeLayout.chargedClipLeftSvg(), EPSILON)
        Assert.assertEquals(0.0f, BatteryShapeLayout.chargedClipTopSvg(), EPSILON)
        Assert.assertEquals(
            BatteryTransferConfig.SVG_HEIGHT,
            BatteryShapeLayout.chargedClipBottomSvg(),
            EPSILON
        )
        Assert.assertTrue(BatteryShapeLayout.clipsChargedArea(75))
        Assert.assertFalse(BatteryShapeLayout.clipsChargedArea(0))
        Assert.assertFalse(BatteryShapeLayout.clipsChargedArea(100))
    }

    @Test
    fun allStylesUseOneCanonicalBatterySizeAndOutsideReserve() {
        Assert.assertEquals(99.44f, BatteryShapeLayout.minimumPx(88.0f), EPSILON)
        Assert.assertEquals(
            105.525f,
            BatteryShapeLayout.rowReservePx(
                88.0f,
                BatteryNumberPlacement.OUTSIDE,
                3.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            30.906f,
            BatteryShapeLayout.rowReservePx(
                88.0f,
                BatteryNumberPlacement.INSIDE,
                3.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            30.906f,
            BatteryShapeLayout.rowReservePx(
                88.0f,
                BatteryNumberPlacement.HIDDEN,
                3.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            53.0f,
            BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
            EPSILON
        )
    }

    @Test
    fun outsideBatteryMovesOnlyEnoughToClearWifi() {
        Assert.assertEquals(
            42.0f,
            BatteryShapeLayout.avoidWifiOverlapShiftPx(
                100.0f,
                130.0f,
                12.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            BatteryShapeLayout.avoidWifiOverlapShiftPx(
                150.0f,
                130.0f,
                12.0f
            ),
            EPSILON
        )
    }

    @Test
    fun fullBatteryCentreMovesLeftOnlyWhenItsRightEdgeWouldClip() {
        Assert.assertEquals(
            100.0f,
            BatteryShapeLayout.clampBatteryCentreXToRightEdge(
                100.0f,
                100.0f,
                0.0f,
                80.0f,
                1.0f,
                240.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            80.0f,
            BatteryShapeLayout.clampBatteryCentreXToRightEdge(
                100.0f,
                100.0f,
                0.0f,
                80.0f,
                1.0f,
                220.0f
            ),
            EPSILON
        )
    }

    @Test
    fun controlCenterWifiBatteryGapIsSixDp() {
        Assert.assertEquals(
            18.0f,
            BatteryShapeLayout.wifiBatteryMinimumGapPx(3.0f),
            EPSILON
        )
    }

    @Test
    fun wifiAvoidanceUsesTheRenderedBodyEdgeForEveryPlacement() {
        try {
            val method =
                BatteryShapeLayout::class.java.getDeclaredMethod(
                    "fullBatteryLeftPx",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                    BatteryNumberPlacement::class.java,
                    Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                -15.155f,
                (method.invoke(
                    null,
                    0.0f,
                    88.0f,
                    88.0f,
                    99.44f,
                    BatteryNumberPlacement.INSIDE,
                    3.0f
                ) as Float),
                EPSILON
            )
            Assert.assertEquals(
                -89.774f,
                (method.invoke(
                    null,
                    0.0f,
                    88.0f,
                    88.0f,
                    99.44f,
                    BatteryNumberPlacement.OUTSIDE,
                    3.0f
                ) as Float),
                EPSILON
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing rendered battery-edge geometry",
                error
            )
        }
    }

    @Test
    fun outsideNumberOverflowUsesMeasuredGlyphWidth() {
        Assert.assertEquals(
            14.110f,
            BatteryShapeLayout.outsideNumberOverflowUnits(
                75.0f,
                50.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            BatteryShapeLayout.outsideNumberOverflowUnits(
                40.0f,
                50.0f
            ),
            EPSILON
        )
    }

    @Test
    fun outsideBatteryAdaptsToCurrentNumberWidth() {
        try {
            val method =
                BatteryShapeLayout::class.java.getDeclaredMethod(
                    "outsideNumberBatteryShiftUnits",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                22.576f,
                (method.invoke(
                    null,
                    100.0f,
                    60.0f
                ) as Float),
                EPSILON
            )
            Assert.assertEquals(
                0.0f,
                (method.invoke(
                    null,
                    100.0f,
                    100.0f
                ) as Float),
                EPSILON
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing outside-number adaptive battery shift",
                error
            )
        }
    }

    @Test
    fun outsideNumberRightEdgeMovesLeftBySevenDp() {
        try {
            val method =
                BatteryShapeLayout::class.java.getDeclaredMethod(
                    "outsideNumberRightShiftPx",
                    Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                21.0f,
                (method.invoke(null, 3.0f) as Float),
                EPSILON
            )
            Assert.assertEquals(
                0.0f,
                (method.invoke(null, Float.NaN) as Float),
                EPSILON
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing outside-number right-edge shift",
                error
            )
        }
    }

    @Test
    fun outsideNumberOverflowAlsoExpandsTheWholeRowReserve() {
        try {
            val method =
                BatteryShapeLayout::class.java.getDeclaredMethod(
                    "outsideNumberOverflowReservePx",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                11.693f,
                (method.invoke(
                    null,
                    88.0f,
                    14.110058f
                ) as Float),
                EPSILON
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing whole-row outside-number reserve",
                error
            )
        }
    }

    @Test
    fun outsideBatteryAndNumberShiftLeftByFourDp() {
        Assert.assertEquals(
            12.0f,
            BatteryShapeLayout.outsideGroupLeftShiftPx(3.0f),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            BatteryShapeLayout.outsideGroupLeftShiftPx(Float.NaN),
            EPSILON
        )
    }

    @Test
    fun controlCenterBatteryShiftsLeftByTwoDp() {
        try {
            val method =
                BatteryShapeLayout::class.java.getDeclaredMethod(
                    "controlCenterBatteryLeftShiftPx",
                    Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                6.0f,
                (method.invoke(null, 3.0f) as Float),
                EPSILON
            )
            Assert.assertEquals(
                0.0f,
                (method.invoke(null, Float.NaN) as Float),
                EPSILON
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing control-center battery left shift",
                error
            )
        }
    }

    @Test
    fun capsuleAndDuoOriginsConvergeOnTheCanonicalBatteryFrame() {
        val sharedFrameMinimum = BatteryShapeLayout.frameMinimumPx(27.0f)
        Assert.assertEquals(27.0f, sharedFrameMinimum, EPSILON)
        Assert.assertEquals(30.51f, BatteryShapeLayout.minimumPx(27.0f), EPSILON)
        val capsuleOrigin = BatteryShapeLayout.canonicalOriginX(
            298.0f,
            22.0f,
            sharedFrameMinimum,
            false
        )
        val duoOrigin = BatteryShapeLayout.canonicalOriginX(
            293.0f,
            27.0f,
            sharedFrameMinimum,
            false
        )

        Assert.assertEquals(293.0f, capsuleOrigin, EPSILON)
        Assert.assertEquals(293.0f, duoOrigin, EPSILON)
        Assert.assertEquals(capsuleOrigin, duoOrigin, EPSILON)
        Assert.assertEquals(
            146.5f,
            BatteryShapeLayout.canonicalOriginY(
                (320.0f - 27.0f) / 2.0f,
                27.0f,
                sharedFrameMinimum
            ),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            BatteryShapeLayout.canonicalOriginX(
                0.0f,
                22.0f,
                sharedFrameMinimum,
                true
            ),
            EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
