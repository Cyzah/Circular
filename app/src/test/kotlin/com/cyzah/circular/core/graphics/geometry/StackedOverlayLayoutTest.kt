package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import org.junit.Assert
import org.junit.Test

class StackedOverlayLayoutTest {
    @Test
    fun fitsLabelsWhenAvailableWidthCoversTextAndPads() {
        Assert.assertTrue(StackedOverlayLayout.fitsLabels(105.0f, 60.0f, 2.0f))
    }

    @Test
    fun hidesLabelsWhenAvailableWidthIsTooNarrow() {
        Assert.assertFalse(StackedOverlayLayout.fitsLabels(104.0f, 60.0f, 2.0f))
    }

    @Test
    fun singleRowLabelFitUsesTheOverlappingSignalAndTextWidth() {
        val required = StackedSignals.Companion.singleRowFillWidthPx(18.0f, 2.0f)

        Assert.assertTrue(StackedOverlayLayout.fitsLabels(required, 18.0f, 2.0f, singleRow = true))
        Assert.assertFalse(StackedOverlayLayout.fitsLabels(required - 0.01f, 18.0f, 2.0f, singleRow = true))
    }

    @Test
    fun labelFitUsesTheRowWidthAfterNativeReservation() {
        Assert.assertEquals(
            143.0f,
            StackedOverlayLayout.labelFitWidth(100.0f, 243.0f),
            0.001f
        )
    }

    @Test
    fun barsUseInsetWhenLabelsAreVisible() {
        Assert.assertEquals(
            95.666667f, StackedOverlayLayout.barLeft(
                false, 100.0f, 300.0f, 2.0f, true, true
            ), 0.001f
        )
    }

    @Test
    fun barsAreCentredWhenLabelsAreHidden() {
        Assert.assertEquals(
            179.85f, StackedOverlayLayout.barLeft(
                false, 100.0f, 300.0f, 2.0f, false, true
            ), 0.001f
        )
    }

    @Test
    fun rtlBarsStartFromRightEdge() {
        Assert.assertEquals(
            264.033333f, StackedOverlayLayout.barLeft(
                true, 100.0f, 300.0f, 2.0f, true, true
            ), 0.001f
        )
    }

    @Test
    fun previewKeepsBarInsetWhenLabelsAreHidden() {
        Assert.assertEquals(
            95.666667f, StackedOverlayLayout.barLeft(
                false, 100.0f, 300.0f, 2.0f, false, false
            ), 0.001f
        )
    }

    @Test
    fun technologyStartsAFixedGapAfterTheSignalBars() {
        Assert.assertEquals(
            144.633333f,
            StackedOverlayLayout.labelLeft(
                false,
                95.666667f,
                60.0f,
                2.0f
            ),
            0.001f
        )
        Assert.assertEquals(
            195.366667f,
            StackedOverlayLayout.labelLeft(
                true,
                264.033333f,
                60.0f,
                2.0f
            ),
            0.001f
        )
    }

    @Test
    fun horizontalConvergenceWaitsWhileTheBatteryIsStillWide() {
        for (placement in BatteryNumberPlacement.values()) {
            val horizontal = SignalMotionPolicy.signalProgress(
                StackedOverlayLayout.signalXProgress(0.5f, placement)
            )
            Assert.assertEquals(0.0f, SignalMotionPolicy.moveProgress(horizontal), 0.0001f)
            Assert.assertTrue(1.0f - StackedOverlayLayout.technologyProgress(horizontal) < 0.01f)
        }
    }

    @Test
    fun delayedHorizontalMotionStillReachesAndRetracesBothEndpoints() {
        for (placement in BatteryNumberPlacement.values()) {
            Assert.assertEquals(0.0f, StackedOverlayLayout.signalXProgress(0.0f, placement), 0.0f)
            Assert.assertEquals(1.0f, StackedOverlayLayout.signalXProgress(1.0f, placement), 0.0f)
            var previous = 0.0f
            for (frame in 1..1000) {
                val progress = frame / 1000.0f
                val value = StackedOverlayLayout.signalXProgress(progress, placement)
                Assert.assertTrue(value >= previous)
                Assert.assertTrue(value - previous <= 0.0041f)
                Assert.assertTrue(value >= progress)
                previous = value
            }
        }
    }

    @Test
    fun outsideNumberDoesNotChangeSignalBarPadding() {
        Assert.assertEquals(
            StackedSignals.fillWidthPx(60.0f, 2.0f),
            StackedSignals.Companion.fillWidthPx(
                60.0f,
                2.0f,
                BatteryNumberPlacement.OUTSIDE
            ),
            0.001f
        )
    }

    @Test
    fun outsideModeDoesNotUseTheHiddenLabelFallback() {
        Assert.assertFalse(
            StackedOverlayLayout.allowsLabelFitFallback(
                BatteryNumberPlacement.OUTSIDE
            )
        )
        Assert.assertTrue(
            StackedOverlayLayout.allowsLabelFitFallback(
                BatteryNumberPlacement.INSIDE
            )
        )
        Assert.assertTrue(
            StackedOverlayLayout.allowsLabelFitFallback(
                BatteryNumberPlacement.HIDDEN
            )
        )
    }
}
