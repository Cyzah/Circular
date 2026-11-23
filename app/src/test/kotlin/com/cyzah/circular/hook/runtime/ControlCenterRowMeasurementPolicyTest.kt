package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.hook.runtime.ControlCenterRowMeasurementPolicyTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowMeasurementPolicyTest {
    @Test
    fun nativeVisibilityAggregationTreatsAnyVisibleChildAsVisible() {
        Assert.assertEquals(
            ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE,
            ControlCenterRowMeasurementPolicy.aggregateNativeVisibility(
                true, true
            )
        )
        Assert.assertEquals(
            ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE,
            ControlCenterRowMeasurementPolicy.aggregateNativeVisibility(
                false, true
            )
        )
        Assert.assertEquals(
            ControlCenterRowMeasurementPolicy.NativeVisibility.HIDDEN,
            ControlCenterRowMeasurementPolicy.aggregateNativeVisibility(
                false, false
            )
        )
    }

    @Test
    fun marginTableProducesSixAndFourDpGaps() {
        val input = ControlCenterRowInput()
        input.reset()
        input.setBounds(300.0f, 500.0f)
        ControlCenterRowMargins.apply(input, 3.25f)

        Assert.assertEquals(
            19.5f,
            input.marginLeft(ControlCenterRowComponent.BATTERY)
                    + input.marginRight(ControlCenterRowComponent.WIFI),
            EPSILON
        )
        Assert.assertEquals(
            13.0f,
            input.marginLeft(ControlCenterRowComponent.WIFI)
                    + input.marginRight(ControlCenterRowComponent.CELLULAR),
            EPSILON
        )
    }

    @Test
    fun logicalMarginsConvertBeforeSolving() {
        Assert.assertEquals(
            5.0f,
            ControlCenterRowMargins.physicalLeft(
                5.0f, 7.0f, false
            ),
            EPSILON
        )
        Assert.assertEquals(
            7.0f,
            ControlCenterRowMargins.physicalRight(
                5.0f, 7.0f, false
            ),
            EPSILON
        )
        Assert.assertEquals(
            7.0f,
            ControlCenterRowMargins.physicalLeft(
                5.0f, 7.0f, true
            ),
            EPSILON
        )
        Assert.assertEquals(
            5.0f,
            ControlCenterRowMargins.physicalRight(
                5.0f, 7.0f, true
            ),
            EPSILON
        )
    }

    @Test
    fun allStylesShareTheSameExternalMarginTable() {
        for (style in DualStyle.entries.toTypedArray().indices) {
            val input = ControlCenterRowInput()
            input.reset()
            input.setBounds(300.0f, 500.0f)
            ControlCenterRowMargins.apply(input, 3.25f)
            Assert.assertEquals(
                19.5f,
                input.marginLeft(ControlCenterRowComponent.BATTERY)
                        + input.marginRight(ControlCenterRowComponent.WIFI),
                EPSILON
            )
            Assert.assertEquals(
                13.0f,
                input.marginLeft(ControlCenterRowComponent.WIFI)
                        + input.marginRight(ControlCenterRowComponent.CELLULAR),
                EPSILON
            )
        }
    }

    @Test
    fun occupiedWidthIncludesBothInnerPaddings() {
        Assert.assertEquals(
            31.5f,
            ControlCenterRowMeasurementPolicy.occupiedWidth(
                20.0f,
                4.5f,
                7.0f
            ),
            EPSILON
        )
    }

    @Test
    fun invalidOrNegativeWidthInputsReturnZero() {
        Assert.assertEquals(
            0.0f,
            ControlCenterRowMeasurementPolicy.occupiedWidth(
                -1.0f,
                4.5f,
                7.0f
            ),
            0.0f
        )
        Assert.assertEquals(
            0.0f,
            ControlCenterRowMeasurementPolicy.cellularWidth(
                30.0f,
                Float.NaN,
                22.0f,
                28.0f
            ),
            0.0f
        )
    }

    @Test
    fun cellularWidthUsesTheWiderTechnologyLabel() {
        Assert.assertEquals(
            64.0f,
            ControlCenterRowMeasurementPolicy.cellularWidth(
                30.0f,
                6.0f,
                22.0f,
                28.0f
            ),
            EPSILON
        )
    }

    @Test
    fun nativeVisibilityUsesTheThreeWantedStates() {
        val input = ControlCenterRowInput()
        input.reset()

        Assert.assertTrue(
            ControlCenterRowMeasurementPolicy.applyNativeVisibility(
                input,
                ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE,
                ControlCenterRowMeasurementPolicy.NativeVisibility.HIDDEN
            )
        )
        Assert.assertTrue(input.visible(ControlCenterRowComponent.WIFI))
        Assert.assertFalse(input.visible(ControlCenterRowComponent.CELLULAR))

        input.reset()
        Assert.assertFalse(
            ControlCenterRowMeasurementPolicy.applyNativeVisibility(
                input,
                ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE,
                ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE
            )
        )
        Assert.assertFalse(input.visible(ControlCenterRowComponent.WIFI))
        Assert.assertFalse(input.visible(ControlCenterRowComponent.CELLULAR))
    }

    @Test
    fun batteryWidthUsesStableCompleteGeometryAndNumberSlot() {
        Assert.assertEquals(
            92.81067f,
            ControlCenterRowMeasurementPolicy.batteryWidth(
                88.0f,
                BatteryNumberPlacement.INSIDE,
                36.5f
            ),
            EPSILON
        )
        Assert.assertEquals(
            114.30132f,
            ControlCenterRowMeasurementPolicy.batteryWidth(
                88.0f,
                BatteryNumberPlacement.OUTSIDE,
                36.5f
            ),
            EPSILON
        )
    }

    @Test
    fun outsideNumberWidthUsesMeasuredGlyphWidthAndSourceGap() {
        Assert.assertEquals(
            21.49066f,
            ControlCenterRowMeasurementPolicy.outsideNumberWidthPx(
                88.0f,
                36.5f
            ),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            ControlCenterRowMeasurementPolicy.outsideNumberWidthPx(
                88.0f,
                Float.NaN
            ),
            0.0f
        )
    }

    @Test
    fun textCacheInvalidatesForFontScaleLocaleAndTypeface() {
        val cache =
            ControlCenterTextMeasurementCache()
        val typeface = Any()
        cache.store(
            "5G+",
            26.0f,
            1.15f,
            "zh-CN",
            120,
            typeface,
            8L,
            41.5f
        )

        Assert.assertTrue(
            cache.matches(
                "5G+", 26.0f, 1.15f, "zh-CN", 120,
                typeface, 8L
            )
        )
        Assert.assertFalse(
            cache.matches(
                "5G+", 26.0f, 1.30f, "zh-CN", 120,
                typeface, 8L
            )
        )
        Assert.assertFalse(
            cache.matches(
                "5G+", 26.0f, 1.15f, "en-US", 120,
                typeface, 8L
            )
        )
        Assert.assertFalse(
            cache.matches(
                "5G+", 26.0f, 1.15f, "zh-CN", 120,
                Any(), 8L
            )
        )
        Assert.assertFalse(
            cache.matches(
                "5G+", 26.0f, 1.15f, "zh-CN", 120,
                typeface, 9L
            )
        )
    }

    @Test
    fun textCacheUpdatesWhenTechnologyChanges() {
        val cache =
            ControlCenterTextMeasurementCache()
        val typeface = Any()
        cache.store(
            "5G+",
            26.0f,
            1.15f,
            "zh-CN",
            120,
            typeface,
            8L,
            41.5f
        )
        cache.store(
            "5G",
            26.0f,
            1.15f,
            "zh-CN",
            120,
            typeface,
            8L,
            37.0f
        )

        Assert.assertTrue(
            cache.matches(
                "5G", 26.0f, 1.15f, "zh-CN", 120,
                typeface, 8L
            )
        )
        Assert.assertFalse(
            cache.matches(
                "5G+", 26.0f, 1.15f, "zh-CN", 120,
                typeface, 8L
            )
        )
        Assert.assertEquals(37.0f, cache.width(), EPSILON)
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
