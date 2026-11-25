package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.hook.runtime.StackedSignalsGeometryTest
import com.cyzah.circular.ui.preview.PreviewRowGeometry
import org.junit.Assert
import org.junit.Test

class StackedSignalsGeometryTest {
    @Test
    fun expandedRowsOccupyThirteenDpAcrossDisplayDensities() {
        Assert.assertEquals(
            6.5f,
            StackedSignals.Companion.sourceVisualBottomOffset(2, 1.0f), EPSILON
        )
        Assert.assertEquals(
            19.5f,
            StackedSignals.Companion.sourceVisualBottomOffset(2, 3.0f), EPSILON
        )
        Assert.assertEquals(
            5.75f,
            StackedSignals.Companion.sourceVisualBottomOffset(1, 1.0f), EPSILON
        )
    }

    @Test
    fun enlargedBarsKeepTheirProportionsAndPhysicalSpacing() {
        Assert.assertEquals(
            15.816667f,
            StackedSignals.sourceVisualRightOffset(1.0f)
                    - StackedSignals.sourceVisualLeftOffset(1.0f), EPSILON
        )
        Assert.assertEquals(3.0f, StackedSignals.Companion.TEXT_GAP_DP, EPSILON)
        Assert.assertEquals(3.5f, StackedSignals.Companion.LEFT_PAD_DP, EPSILON)
        Assert.assertEquals(7.0f, StackedSignals.Companion.labelSize(), EPSILON)
    }

    @Test
    fun cellularReservationIncludesEnlargedBarsAndUnscaledPadding() {
        Assert.assertEquals(
            104.633333f,
            StackedSignals.fillWidthPx(60.0f, 2.0f), EPSILON
        )
        Assert.assertEquals(
            47.3f,
            StackedSignals.fillWidthPx(0.0f, 2.0f), EPSILON
        )
    }

    @Test
    fun singleRowReservationMatchesTheStackedLabelAndSignalGeometry() {
        val labelWidth = 20.0f
        val density = 2.0f
        val width = StackedSignals.Companion.singleRowFillWidthPx(labelWidth, density)
        val left = 20.0f
        val right = left + width
        val barLeft = StackedSignals.Companion.singleRowBarLeft(
            left, right, density, false, labelWidth
        )
        val shortestBarCentre = StackedSignals.Companion.singleRowShortestBarCentreX(
            barLeft, density, false
        )

        Assert.assertEquals(30.0f, shortestBarCentre, EPSILON)
        Assert.assertEquals(38.816666f, width, EPSILON)
        Assert.assertEquals(47.3f, StackedSignals.Companion.singleRowFillWidthPx(0.0f, density), EPSILON)
    }

    @Test
    fun previewReservesTheEnlargedCellularWidth() {
        val geometry = PreviewRowGeometry()

        Assert.assertTrue(
            geometry.update(
                196.168f, 49.0f, StackedSignals.fillWidthPx(0.0f, 3.0f),
                true, true, true, 3.0f
            )
        )

        Assert.assertEquals(
            70.95f,
            geometry.width(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(346.118f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(
            12.0f,
            geometry.left(ControlCenterRowComponent.WIFI)
                    - geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
