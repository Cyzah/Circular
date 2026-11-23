package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.ui.preview.PreviewBatteryTransferTest
import org.junit.Assert
import org.junit.Test

class PreviewBatteryTransferTest {
    @Test
    fun pathRunsFromLowerRightWithoutShowingFullTrack() {
        val transfer = PreviewBatteryTransfer()

        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, true, 425L)

        Assert.assertTrue(transfer.active())
        Assert.assertTrue(transfer.pointVisible())
        Assert.assertFalse(transfer.trackVisible())
        Assert.assertEquals(0.5f, transfer.traceProgress(), 0.0001f)
        Assert.assertEquals(1.0f, transfer.temporaryTraceAlpha(), 0.0001f)
    }

    @Test
    fun restoreShowsTrackAndUsesLatestBatteryPercentage() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, true, 750L)
        transfer.update(0.0f, true, 910L)

        Assert.assertTrue(transfer.trackVisible())
        Assert.assertFalse(transfer.pointVisible())
        Assert.assertEquals(1.0f, transfer.traceProgress(), 0.0001f)
        Assert.assertEquals(0.37f, transfer.fillProgress(74), 0.0001f)
        Assert.assertEquals(0.5f, transfer.temporaryTraceAlpha(), 0.0001f)
    }

    @Test
    fun initialAndRepeatedClosedFramesDoNotInventATransfer() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(0.0f, true, 0L)
        transfer.update(0.0f, true, 1000L)
        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.needsFrame())
    }

    @Test
    fun openingPreviewFollowsExpansionWithoutRunningARingTimeline() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(0.0f, true, 0L)
        transfer.update(0.4f, true, 100L)

        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.pointVisible())
        Assert.assertFalse(transfer.trackVisible())
        Assert.assertFalse(transfer.needsFrame())
        val collapse = transfer.batteryCollapse(0.4f)
        transfer.update(0.4f, true, 5100L)
        Assert.assertEquals(collapse, transfer.batteryCollapse(0.4f), 0.0001f)

        transfer.update(1.0f, true, 5100L)
        Assert.assertEquals(0.0f, transfer.batteryCollapse(1.0f), 0.0001f)
        Assert.assertFalse(transfer.needsFrame())
    }

    @Test
    fun repeatedClosedFramesKeepTheDeviceTimeCurve() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, true, 230L)
        Assert.assertEquals(0.104f, transfer.traceProgress(), 0.0001f)
        transfer.update(0.0f, true, 360L)
        Assert.assertEquals(0.352f, transfer.traceProgress(), 0.0001f)
    }

    @Test
    fun reopeningImmediatelyCancelsTheCurrentPath() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, true, 425L)
        transfer.update(0.4f, true, 425L)
        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.pointVisible())
        Assert.assertTrue(transfer.needsFrame())
        Assert.assertEquals(1.0f, transfer.drawState().reopenAlpha(), 0.0001f)
        val collapse = transfer.batteryCollapse(0.4f)
        transfer.update(0.4f, true, 545L)
        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.needsFrame())
        Assert.assertEquals(0.0f, transfer.drawState().reopenAlpha(), 0.0001f)
        Assert.assertEquals(collapse, transfer.batteryCollapse(0.4f), 0.0001f)
        Assert.assertEquals(0.0f, transfer.traceProgress(), 0.0001f)
    }

    @Test
    fun cancellingAnIncompleteReopenReturnsAlongTheOpeningPath() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, true, 750L)
        transfer.update(0.0f, true, 910L)
        Assert.assertEquals(0.37f, transfer.fillProgress(74), 0.0001f)
        transfer.update(0.4f, true, 910L)
        Assert.assertFalse(transfer.active())
        Assert.assertTrue(transfer.needsFrame())
        Assert.assertEquals(1.0f, transfer.drawState().reopenAlpha(), 0.0001f)
        transfer.update(0.0f, true, 990L)
        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.pointVisible())
        Assert.assertFalse(transfer.needsFrame())
        Assert.assertEquals(0.0f, transfer.drawState().reopenAlpha(), 0.0001f)
        Assert.assertEquals(0.0f, transfer.traceProgress(), 0.0001f)
        transfer.update(0.0f, true, 1110L)
        Assert.assertEquals(0.74f, transfer.fillProgress(74), 0.0001f)
        Assert.assertFalse(transfer.needsFrame())
        Assert.assertEquals(0.0f, transfer.traceProgress(), 0.0001f)
    }

    @Test
    fun disablingTheSceneCancelsTransferAndDoesNotReplayAtZero() {
        val transfer = PreviewBatteryTransfer()
        transfer.update(1.0f, true, 0L)
        transfer.update(0.0f, true, 100L)
        transfer.update(0.0f, false, 230L)
        Assert.assertFalse(transfer.active())
        Assert.assertFalse(transfer.needsFrame())
        transfer.update(0.0f, true, 300L)
        Assert.assertFalse(transfer.active())
    }

    @Test
    fun circularTransferPreservesNumberGapsForEveryStyle() {
        for (style in arrayOf<DualStyle>(DualStyle.LAYERED, DualStyle.GROUPED, DualStyle.DUO)) {
            for (topGap in floatArrayOf(0.0f, 28.0f)) {
                val source = RingGeometry()
                source.setCircular(
                    style, 10.0f, 10.0f, 110.0f, 110.0f,
                    topGap, 42.0f, 7.5f
                )
                val preview = RingGeometry()
                PreviewBatteryTransfer.Companion.copyClosedRing(source, preview)

                Assert.assertEquals(topGap, preview.topHalfAngle, 0.0001f)
                Assert.assertEquals(topGap == 0.0f, preview.continuousContour())
                Assert.assertEquals(42.0f, preview.bottomHalfAngle, 0.0001f)
                assertSameTransferPathAsDevice(source, preview)
            }
        }
    }

    @Test
    fun capsuleTransferPreservesBothGapsAndItsTransform() {
        for (topGap in floatArrayOf(0.0f, 18.0f)) {
            val source = RingGeometry()
            source.setCapsule(
                240.0f, 104.0f, topGap, 34.0f, 7.072f,
                100.0f, 20.0f, 0.5f
            )
            val preview = RingGeometry()
            PreviewBatteryTransfer.Companion.copyClosedRing(source, preview)

            Assert.assertEquals(topGap, preview.halfTop, 0.0001f)
            Assert.assertEquals(topGap == 0.0f, preview.continuousContour())
            Assert.assertEquals(34.0f, preview.halfBottom, 0.0001f)
            Assert.assertEquals(177.0f, preview.startX, 0.0001f)
            Assert.assertEquals(72.0f, preview.startY, 0.0001f)
            Assert.assertEquals(143.0f, preview.endX, 0.0001f)
            Assert.assertEquals(72.0f, preview.endY, 0.0001f)
            assertSameTransferPathAsDevice(source, preview)
        }
    }

    companion object {
        private fun assertSameTransferPathAsDevice(
            source: RingGeometry?, preview: RingGeometry
        ) {
            val device = RingGeometry()
            device.copyForTransferFrom(source)
            Assert.assertTrue(preview.valid())
            Assert.assertEquals(device.length, preview.length, 0.0001f)
            for (progress in floatArrayOf(0.0f, 0.2f, 0.5f, 0.8f, 1.0f)) {
                val distance = device.length * progress
                Assert.assertEquals(device.pointX(distance), preview.pointX(distance), 0.0001f)
                Assert.assertEquals(device.pointY(distance), preview.pointY(distance), 0.0001f)
            }
        }
    }
}
