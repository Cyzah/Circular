package com.cyzah.circular.core.graphics.geometry

import org.junit.Assert
import org.junit.Test

class CapsuleGeometryTest {
    @Test
    fun contentWidthExcludesTheRemovedChargingBolt() {
        Assert.assertEquals(
            60.0f,
            CapsuleGeometry.contentWidth(
                10.0f,
                20.0f,
                30.0f
            ),
            0.001f
        )
        Assert.assertEquals(
            30.0f,
            CapsuleGeometry.contentWidth(
                10.0f,
                0.0f,
                30.0f
            ),
            0.001f
        )
    }

    @Test
    fun minimumWidthGrowsWithHeavySignalDots() {
        Assert.assertEquals(202.8f, CapsuleGeometry.minimumBodyWidth(1.0f), 0.001f)
        Assert.assertEquals(219.232f, CapsuleGeometry.minimumBodyWidth(2.0f), 0.001f)
    }

    @Test
    fun technologyAndBatteryDigitsKeepTheirFullMeasuredWidth() {
        val contentWidth = CapsuleGeometry.contentWidth(10.0f, 70.0f, 120.0f)

        Assert.assertEquals(262.4f, CapsuleGeometry.bodyWidth(contentWidth, 1.0f), 0.001f)
    }

    @Test
    fun reservationIncludesUncompressedContentAndBodyPadding() {
        val bodyWidth = CapsuleGeometry.bodyWidth(200.0f, 1.0f)

        Assert.assertEquals(288.936f, CapsuleGeometry.reserveWidth(bodyWidth, 1.0f), 0.001f)
    }

    @Test
    fun scaleNeverUpscalesAndHonoursAspectLimit() {
        Assert.assertEquals(1.0f, CapsuleGeometry.scale(240.0f, 2.7f), 0.001f)
        Assert.assertEquals(0.81f, CapsuleGeometry.scale(400.0f, 2.7f), 0.001f)
    }

    @Test
    fun reserveWidthIncludesStrokeShiftAndSideGap() {
        Assert.assertEquals(
            326.536f,
            CapsuleGeometry.reserveWidth(300.0f, 1.0f),
            0.001f
        )
    }

    @Test
    fun wifiScaleIsZeroWhenGlyphBoundsAreUnavailable() {
        Assert.assertEquals(
            2.0f,
            CapsuleGeometry.wifiScale(20.0f, 10.0f, 1.0f),
            0.001f
        )
        Assert.assertEquals(
            0.0f,
            CapsuleGeometry.wifiScale(20.0f, 0.0f, 1.0f),
            0.0f
        )
        Assert.assertEquals(
            0.0f,
            CapsuleGeometry.wifiScale(20.0f, Float.NaN, 1.0f),
            0.0f
        )
    }
}
