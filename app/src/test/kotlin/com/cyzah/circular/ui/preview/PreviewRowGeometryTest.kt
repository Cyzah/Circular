package com.cyzah.circular.ui.preview

import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.hook.runtime.ControlCenterRowComponent
import com.cyzah.circular.hook.runtime.ControlCenterRowMeasurementPolicy
import com.cyzah.circular.ui.preview.PreviewRowGeometryTest
import org.junit.Assert
import org.junit.Test

class PreviewRowGeometryTest {
    @Test
    fun widePhysicalRowKeepsDistinctCentresBeyondThreeHundredPixels() {
        val geometry = PreviewRowGeometry()

        Assert.assertTrue(
            geometry.update(
                196.168f, 49.0f, 66.3f, true, true, true, 3.0f
            )
        )

        Assert.assertEquals(341.468f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(
            -170.734f,
            geometry.left(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            170.734f,
            geometry.right(ControlCenterRowComponent.BATTERY), EPSILON
        )
        Assert.assertEquals(
            18.0f,
            geometry.left(ControlCenterRowComponent.BATTERY)
                    - geometry.right(ControlCenterRowComponent.WIFI), EPSILON
        )
        Assert.assertEquals(
            12.0f,
            geometry.left(ControlCenterRowComponent.WIFI)
                    - geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertTrue(
            geometry.centre(ControlCenterRowComponent.BATTERY)
                    > geometry.centre(ControlCenterRowComponent.WIFI)
        )
        Assert.assertTrue(
            geometry.centre(ControlCenterRowComponent.WIFI)
                    > geometry.centre(ControlCenterRowComponent.CELLULAR)
        )
    }

    @Test
    fun hiddenWifiDoesNotKeepItsWidthOrExternalMargins() {
        val geometry = PreviewRowGeometry()

        Assert.assertTrue(
            geometry.update(
                100.0f, 40.0f, 80.0f, true, false, true, 2.0f
            )
        )

        Assert.assertEquals(188.0f, geometry.rowWidth(), EPSILON)
        Assert.assertFalse(geometry.visible(ControlCenterRowComponent.WIFI))
        Assert.assertEquals(0.0f, geometry.width(ControlCenterRowComponent.WIFI), EPSILON)
        Assert.assertEquals(0.0f, geometry.centre(ControlCenterRowComponent.WIFI), EPSILON)
        Assert.assertEquals(
            8.0f,
            geometry.left(ControlCenterRowComponent.BATTERY)
                    - geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            44.0f,
            geometry.centre(ControlCenterRowComponent.BATTERY), EPSILON
        )
        Assert.assertEquals(
            -54.0f,
            geometry.centre(ControlCenterRowComponent.CELLULAR), EPSILON
        )
    }

    @Test
    fun narrowStageScalesTheEntireRowWithoutCollapsingItsGaps() {
        val geometry = PreviewRowGeometry()
        Assert.assertTrue(
            geometry.update(
                100.0f, 40.0f, 80.0f, true, true, true, 1.0f
            )
        )

        val scale: Float = PreviewRowGeometry.Companion.fitScale(
            115.0f, 100.0f, 2.0f, geometry.rowWidth(), 40.0f
        )

        Assert.assertEquals(0.5f, scale, EPSILON)
        Assert.assertEquals(
            -57.5f,
            geometry.left(ControlCenterRowComponent.CELLULAR) * scale, EPSILON
        )
        Assert.assertEquals(
            57.5f,
            geometry.right(ControlCenterRowComponent.BATTERY) * scale, EPSILON
        )
        Assert.assertEquals(
            3.0f,
            (geometry.left(ControlCenterRowComponent.BATTERY)
                    - geometry.right(ControlCenterRowComponent.WIFI)) * scale, EPSILON
        )
        Assert.assertEquals(
            2.0f,
            (geometry.left(ControlCenterRowComponent.WIFI)
                    - geometry.right(ControlCenterRowComponent.CELLULAR)) * scale, EPSILON
        )
    }

    @Test
    fun stageFitRespectsHeightAndPreferredMagnification() {
        Assert.assertEquals(
            0.5f,
            PreviewRowGeometry.Companion.fitScale(400.0f, 20.0f, 2.0f, 100.0f, 40.0f),
            EPSILON
        )
        Assert.assertEquals(
            1.5f,
            PreviewRowGeometry.Companion.fitScale(400.0f, 200.0f, 1.5f, 100.0f, 40.0f),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            PreviewRowGeometry.Companion.fitScale(0.0f, 100.0f, 2.0f, 100.0f, 40.0f),
            EPSILON
        )
        Assert.assertEquals(
            0.0f,
            PreviewRowGeometry.Companion.fitScale(100.0f, Float.NaN, 2.0f, 100.0f, 40.0f),
            EPSILON
        )
    }

    @Test
    fun eachNumberPlacementReservesItsActualBatteryComponentWidth() {
        val geometry = PreviewRowGeometry()
        for (placement in BatteryNumberPlacement.values()) {
            val batteryWidth = ControlCenterRowMeasurementPolicy.batteryWidth(
                81.0f, placement, 90.0f
            )
            Assert.assertTrue(
                geometry.update(
                    batteryWidth, 49.7f, 90.0f, true, true, true, 3.0f
                )
            )

            val expectedBatteryWidth = if (placement == BatteryNumberPlacement.OUTSIDE)
                128.2408f
            else
                85.428f
            val expectedRowWidth = if (placement == BatteryNumberPlacement.OUTSIDE)
                297.9408f
            else
                255.128f
            Assert.assertEquals(
                expectedBatteryWidth,
                geometry.width(ControlCenterRowComponent.BATTERY), EPSILON
            )
            Assert.assertEquals(expectedRowWidth, geometry.rowWidth(), EPSILON)
            Assert.assertEquals(
                18.0f,
                geometry.left(ControlCenterRowComponent.BATTERY)
                        - geometry.right(ControlCenterRowComponent.WIFI), EPSILON
            )
        }
    }

    @Test
    fun invalidOrEmptyRowClearsPreviousCoordinates() {
        val geometry = PreviewRowGeometry()
        Assert.assertTrue(
            geometry.update(
                100.0f, 40.0f, 80.0f, true, true, true, 1.0f
            )
        )

        Assert.assertFalse(
            geometry.update(
                Float.NaN, 40.0f, 80.0f, true, true, true, 1.0f
            )
        )
        Assert.assertFalse(geometry.valid())
        Assert.assertEquals(0.0f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(
            0.0f,
            geometry.centre(ControlCenterRowComponent.BATTERY), EPSILON
        )

        Assert.assertFalse(
            geometry.update(
                100.0f, 40.0f, 80.0f, false, false, false, 1.0f
            )
        )
        Assert.assertFalse(geometry.visible(ControlCenterRowComponent.CELLULAR))
        Assert.assertEquals(
            0.0f,
            geometry.left(ControlCenterRowComponent.CELLULAR), EPSILON
        )
    }

    @Test
    fun disconnectMovesTheRemainingComponentsWithoutChangingTheSceneFit() {
        val geometry = PreviewRowGeometry()
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 0L, false
        )

        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 100L, true
        )
        Assert.assertFalse(geometry.visible(ControlCenterRowComponent.WIFI))
        Assert.assertTrue(geometry.active())
        Assert.assertEquals(230.0f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(184.0f, geometry.targetRowWidth(), EPSILON)
        Assert.assertEquals(
            -35.0f,
            geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            115.0f,
            geometry.right(ControlCenterRowComponent.BATTERY), EPSILON
        )

        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 210L, true
        )
        Assert.assertEquals(207.0f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(184.0f, geometry.targetRowWidth(), EPSILON)
        Assert.assertEquals(
            -12.0f,
            geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            115.0f,
            geometry.right(ControlCenterRowComponent.BATTERY), EPSILON
        )

        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 320L, true
        )
        Assert.assertFalse(geometry.active())
        Assert.assertEquals(184.0f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(
            11.0f,
            geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
    }

    @Test
    fun reconnectReservesRoomBeforeMakingWifiVisible() {
        val geometry = PreviewRowGeometry()
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 0L, false
        )
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 100L, true
        )
        Assert.assertFalse(geometry.visible(ControlCenterRowComponent.WIFI))
        Assert.assertEquals(230.0f, geometry.targetRowWidth(), EPSILON)
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 210L, true
        )
        Assert.assertFalse(geometry.visible(ControlCenterRowComponent.WIFI))
        Assert.assertEquals(207.0f, geometry.rowWidth(), EPSILON)
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 320L, true
        )
        Assert.assertTrue(geometry.visible(ControlCenterRowComponent.WIFI))
        Assert.assertFalse(geometry.active())
        Assert.assertEquals(
            4.0f, geometry.left(ControlCenterRowComponent.WIFI)
                    - geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            6.0f, geometry.left(ControlCenterRowComponent.BATTERY)
                    - geometry.right(ControlCenterRowComponent.WIFI), EPSILON
        )
    }

    @Test
    fun leavingTheSettledExpandedStateSnapsToTheCurrentTarget() {
        val geometry = PreviewRowGeometry()
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 0L, false
        )
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 100L, true
        )
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 150L, false
        )
        Assert.assertFalse(geometry.active())
        Assert.assertEquals(184.0f, geometry.rowWidth(), EPSILON)
        Assert.assertEquals(
            11.0f,
            geometry.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
    }

    @Test
    fun connectionChangesKeepStageWidthAndBatteryAnchorFixed() {
        val geometry = PreviewRowGeometry()
        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, true, true, 1.0f, 0L, false
        )
        Assert.assertEquals(230.0f, geometry.stageRowWidth(), EPSILON)
        Assert.assertEquals(
            115.0f,
            geometry.right(ControlCenterRowComponent.BATTERY), EPSILON
        )

        geometry.update(
            100.0f, 40.0f, 80.0f,
            true, false, true, 1.0f, 100L, false
        )
        Assert.assertEquals(184.0f, geometry.targetRowWidth(), EPSILON)
        Assert.assertEquals(230.0f, geometry.stageRowWidth(), EPSILON)
        Assert.assertEquals(
            115.0f,
            geometry.right(ControlCenterRowComponent.BATTERY), EPSILON
        )
        Assert.assertEquals(
            0.5f,
            PreviewRowGeometry.Companion.fitScale(
                115.0f, 100.0f,
                2.0f, geometry.stageRowWidth(), 40.0f
            ), EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
