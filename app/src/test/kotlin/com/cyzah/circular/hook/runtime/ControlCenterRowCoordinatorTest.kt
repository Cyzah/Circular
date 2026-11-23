package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.ControlCenterRowCoordinatorTest
import com.cyzah.circular.hook.runtime.ControlCenterRowCoordinatorTest.FakeMeasurement
import org.junit.Assert
import org.junit.Test

class ControlCenterRowCoordinatorTest {
    private class FakeMeasurement

        : ControlCenterRowCoordinator.MeasurementSource {
        @JvmField var calls: Int = 0
        @JvmField var batteryWidth: Float = 100.0f
        @JvmField var failMeasurement: Boolean = false
        @JvmField var wifiVisible: Boolean = true

        override fun measure(output: ControlCenterRowInput): Boolean {
            this.calls++
            if (this.failMeasurement) {
                return false
            }
            output.reset()
            output.setBounds(300.0f, 600.0f)
            output.set(
                ControlCenterRowComponent.BATTERY,
                true,
                this.batteryWidth,
                0.0f,
                0.0f,
                0.0f,
                0.0f
            )
            output.set(
                ControlCenterRowComponent.WIFI,
                this.wifiVisible,
                40.0f,
                0.0f,
                0.0f,
                0.0f,
                6.0f
            )
            output.set(
                ControlCenterRowComponent.CELLULAR,
                true,
                60.0f,
                0.0f,
                0.0f,
                0.0f,
                4.0f
            )
            return true
        }
    }

    @Test
    fun sameGenerationReusesStableSnapshotWithoutMeasuring() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)

        Assert.assertTrue(coordinator.updateStable(9L))
        measurement.batteryWidth = 140.0f
        Assert.assertTrue(coordinator.updateStable(9L))

        Assert.assertEquals(1, measurement.calls.toLong())
        Assert.assertEquals(
            100.0f,
            coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ),
            EPSILON
        )

        Assert.assertTrue(coordinator.updateStable(10L))
        Assert.assertEquals(2, measurement.calls.toLong())
        Assert.assertEquals(
            140.0f,
            coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ),
            EPSILON
        )
        Assert.assertEquals(10L, coordinator.stableSnapshot().generation())
    }

    @Test
    fun failedTransitionTargetClearsExistingOwnership() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        Assert.assertTrue(coordinator.transitioning())
        val output = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.sampleTransition(0.5f, output))

        Assert.assertFalse(
            coordinator.beginTransition(
                snapshot(120.0f, 2L),
                3L
            )
        )

        Assert.assertFalse(coordinator.transitioning())
        Assert.assertFalse(coordinator.sampleTransition(0.5f, output))
        Assert.assertEquals(1L, coordinator.currentSnapshot().generation())
    }

    @Test
    fun finishTransitionPreservesMeasuredLayoutAndSettlesTheDrawnResult() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        val output = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.sampleTransition(0.4f, output))

        coordinator.finishTransition()

        Assert.assertFalse(coordinator.transitioning())
        Assert.assertEquals(1L, coordinator.stableSnapshot().generation())
        Assert.assertEquals(
            100.0f,
            coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ),
            EPSILON
        )
        Assert.assertEquals(2L, coordinator.currentSnapshot().generation())
        Assert.assertEquals(
            120.0f, coordinator.currentSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertFalse(coordinator.sampleTransition(0.5f, output))
    }

    @Test
    fun collapsedRowCannotBecomeTheNextExpandedRowWithoutMeasurement() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))

        // In the status bar the three components share the compact icon area.
        // A reopen can reach its settled draw before another container measure.
        val collapsed = ControlCenterRowSnapshot()
        Assert.assertTrue(
            collapsed.copyFromRects(
                booleanArrayOf(true, true, true),
                floatArrayOf(260.0f, 260.0f, 260.0f),
                floatArrayOf(300.0f, 300.0f, 300.0f),
                floatArrayOf(40.0f, 40.0f, 40.0f),
                2L
            )
        )
        val output = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.beginTransition(collapsed, 2L))
        Assert.assertTrue(coordinator.sampleTransition(1.0f, output))
        coordinator.finishTransition()
        Assert.assertEquals(40.0f, coordinator.currentSnapshot().rowWidth(), EPSILON)

        val reopened = ControlCenterRowSnapshot()
        Assert.assertTrue(reopened.copyFrom(coordinator.stableSnapshot()))
        Assert.assertTrue(coordinator.beginTransition(reopened, reopened.generation()))
        Assert.assertTrue(coordinator.sampleTransition(0.0f, output))
        Assert.assertEquals(40.0f, output.rowWidth(), EPSILON)
        Assert.assertTrue(coordinator.sampleTransition(1.0f, output))

        Assert.assertEquals(210.0f, output.rowWidth(), EPSILON)
        Assert.assertEquals(
            100.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertTrue(
            output.right(ControlCenterRowComponent.CELLULAR)
                    <= output.left(ControlCenterRowComponent.WIFI)
        )
        Assert.assertTrue(
            output.right(ControlCenterRowComponent.WIFI)
                    <= output.left(ControlCenterRowComponent.BATTERY)
        )
    }

    @Test
    fun repeatedCollapseAndReopenKeepMeasuredWidthAndContinuousOrigins() {
        val coordinator =
            ControlCenterRowCoordinator(FakeMeasurement())
        Assert.assertTrue(coordinator.updateStable(1L))
        val expanded = ControlCenterRowSnapshot()
        Assert.assertTrue(expanded.copyFrom(coordinator.stableSnapshot()))
        val collapsed = ControlCenterRowSnapshot()
        Assert.assertTrue(
            collapsed.copyFromRects(
                booleanArrayOf(true, true, true),
                floatArrayOf(260.0f, 260.0f, 260.0f),
                floatArrayOf(300.0f, 300.0f, 300.0f),
                floatArrayOf(40.0f, 40.0f, 40.0f), 2L
            )
        )
        val output = ControlCenterRowSnapshot()

        for (cycle in 0..2) {
            Assert.assertTrue(coordinator.beginTransition(collapsed, 2L))
            Assert.assertTrue(coordinator.sampleTransition(0.5f, output))
            Assert.assertEquals(125.0f, output.rowWidth(), EPSILON)
            Assert.assertTrue(coordinator.sampleTransition(1.0f, output))
            coordinator.finishTransition()
            Assert.assertEquals(210, coordinator.measuredHostWidthPx().toLong())

            Assert.assertTrue(coordinator.beginTransition(expanded, 1L))
            Assert.assertTrue(coordinator.sampleTransition(0.0f, output))
            Assert.assertEquals(40.0f, output.rowWidth(), EPSILON)
            Assert.assertTrue(coordinator.sampleTransition(0.5f, output))
            Assert.assertEquals(125.0f, output.rowWidth(), EPSILON)
            coordinator.finishTransition()
        }
    }

    @Test
    fun settledDrawWithoutNewMeasureCanRestoreTheExpandedLayout() {
        val coordinator =
            ControlCenterRowCoordinator(FakeMeasurement())
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        coordinator.finishTransition()

        coordinator.restoreMeasuredLayout()

        Assert.assertEquals(1L, coordinator.currentSnapshot().generation())
        Assert.assertEquals(
            100.0f, coordinator.currentSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
    }

    @Test
    fun newMeasurementDiscardsThePreviousSettledEndpoint() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        coordinator.finishTransition()
        measurement.batteryWidth = 130.0f

        Assert.assertTrue(coordinator.updateStable(3L))

        Assert.assertEquals(3L, coordinator.currentSnapshot().generation())
        Assert.assertEquals(
            130.0f, coordinator.currentSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(240, coordinator.measuredHostWidthPx().toLong())
        coordinator.invalidate()
        Assert.assertEquals(0, coordinator.measuredHostWidthPx().toLong())
        Assert.assertFalse(coordinator.currentSnapshot().valid())
    }

    @Test
    fun reversalAfterSettlingStartsAtTheCurrentLiteralRectangle() {
        val coordinator =
            ControlCenterRowCoordinator(FakeMeasurement())
        Assert.assertTrue(coordinator.updateStable(1L))
        val expanded = ControlCenterRowSnapshot()
        Assert.assertTrue(expanded.copyFrom(coordinator.stableSnapshot()))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        coordinator.finishTransition()
        Assert.assertTrue(coordinator.beginTransition(expanded, 1L))
        val output = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.sampleTransition(0.4f, output))
        Assert.assertEquals(
            112.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )

        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        Assert.assertTrue(coordinator.sampleTransition(0.0f, output))
        Assert.assertEquals(
            112.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertTrue(coordinator.sampleTransition(0.5f, output))
        Assert.assertEquals(
            116.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            100.0f, coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
    }

    @Test
    fun finishingWithoutActiveTransitionKeepsStableSnapshot() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))

        coordinator.finishTransition()

        Assert.assertTrue(coordinator.stableSnapshot().valid())
        Assert.assertEquals(1L, coordinator.stableSnapshot().generation())
        Assert.assertEquals(
            100.0f,
            coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ),
            EPSILON
        )
    }

    @Test
    fun failedStableMeasurementClearsActiveTransitionOwnership() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertEquals(210, coordinator.measuredHostWidthPx().toLong())
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))
        val output = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.sampleTransition(0.5f, output))

        measurement.failMeasurement = true
        Assert.assertFalse(coordinator.updateStable(3L))

        Assert.assertFalse(coordinator.stableSnapshot().valid())
        Assert.assertFalse(coordinator.currentSnapshot().valid())
        Assert.assertEquals(0, coordinator.measuredHostWidthPx().toLong())
        Assert.assertFalse(coordinator.sampleTransition(0.5f, output))
    }

    @Test
    fun hostLossClearsOwnershipAndRecoveryRebuildsFreshStableRows() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L))
        Assert.assertTrue(coordinator.beginTransition(snapshot(120.0f, 2L), 2L))

        coordinator.invalidate()

        Assert.assertFalse(coordinator.transitioning())
        Assert.assertFalse(coordinator.stableSnapshot().valid())
        Assert.assertFalse(coordinator.currentSnapshot().valid())

        measurement.batteryWidth = 130.0f
        Assert.assertTrue(coordinator.updateStable(3L))

        Assert.assertFalse(coordinator.transitioning())
        Assert.assertTrue(coordinator.stableSnapshot().valid())
        Assert.assertEquals(3L, coordinator.stableSnapshot().generation())
        Assert.assertEquals(
            130.0f, coordinator.stableSnapshot().width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
    }

    @Test
    fun diagnosticSignatureIsClaimedOnlyWhenItChanges() {
        val measurement = FakeMeasurement()
        val coordinator =
            ControlCenterRowCoordinator(measurement)

        Assert.assertTrue(coordinator.claimDiagnostic(9L))
        Assert.assertFalse(coordinator.claimDiagnostic(9L))
        Assert.assertTrue(coordinator.claimDiagnostic(10L))
    }

    @Test
    fun disconnectedWifiShrinksHostAndCellularTogether() {
        val measurement = FakeMeasurement()
        val coordinator = ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L, 0L, true))
        measurement.wifiVisible = false
        Assert.assertTrue(coordinator.updateStable(2L, 100L, true))

        Assert.assertEquals(210, coordinator.measuredHostWidthPx().toLong())
        Assert.assertFalse(coordinator.currentSnapshot().visible(ControlCenterRowComponent.WIFI))
        Assert.assertEquals(
            150.0f, coordinator.currentSnapshot().right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        coordinator.restoreMeasuredLayout()
        coordinator.advanceReflow(210L)
        Assert.assertEquals(187, coordinator.measuredHostWidthPx().toLong())
        Assert.assertEquals(
            173.0f, coordinator.currentSnapshot().right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        coordinator.advanceReflow(320L)
        Assert.assertEquals(164, coordinator.measuredHostWidthPx().toLong())
        Assert.assertEquals(
            196.0f, coordinator.currentSnapshot().right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
    }

    @Test
    fun gestureStartsFromTheReflowFrameAlignedToActualHostWidth() {
        val measurement = FakeMeasurement()
        val coordinator = ControlCenterRowCoordinator(measurement)
        Assert.assertTrue(coordinator.updateStable(1L, 0L, true))
        measurement.wifiVisible = false
        Assert.assertTrue(coordinator.updateStable(2L, 100L, true))
        coordinator.advanceReflow(210L)
        val target: ControlCenterRowSnapshot = copySnapshot(coordinator.stableSnapshot())
        val drawn = ControlCenterRowSnapshot()
        Assert.assertTrue(drawn.copyTranslatedFrom(coordinator.currentSnapshot(), -113.0f))

        Assert.assertTrue(coordinator.updateStable(3L, 210L, false))
        Assert.assertEquals(187, coordinator.measuredHostWidthPx().toLong())
        Assert.assertTrue(coordinator.beginTransition(target, target.generation(), 187.0f))
        Assert.assertEquals(187, coordinator.measuredHostWidthPx().toLong())
        val firstFrame = ControlCenterRowSnapshot()
        Assert.assertTrue(coordinator.sampleTransition(0.0f, firstFrame))
        Assert.assertEquals(187.0f, firstFrame.right(ControlCenterRowComponent.BATTERY), EPSILON)
        Assert.assertEquals(
            drawn.left(ControlCenterRowComponent.CELLULAR),
            firstFrame.left(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertEquals(
            drawn.right(ControlCenterRowComponent.CELLULAR),
            firstFrame.right(ControlCenterRowComponent.CELLULAR), EPSILON
        )
        Assert.assertFalse(coordinator.usesMeasuredGeometry())
        Assert.assertTrue(coordinator.sampleTransition(1.0f, firstFrame))
        Assert.assertEquals(187.0f, firstFrame.right(ControlCenterRowComponent.BATTERY), EPSILON)
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun snapshot(
            batteryWidth: Float,
            generation: Long
        ): ControlCenterRowSnapshot {
            val input = ControlCenterRowInput()
            input.reset()
            input.setBounds(300.0f, 600.0f)
            input.set(
                ControlCenterRowComponent.BATTERY,
                true,
                batteryWidth,
                0.0f,
                0.0f,
                0.0f,
                0.0f
            )
            input.set(
                ControlCenterRowComponent.WIFI,
                true,
                40.0f,
                0.0f,
                0.0f,
                0.0f,
                6.0f
            )
            input.set(
                ControlCenterRowComponent.CELLULAR,
                true,
                60.0f,
                0.0f,
                0.0f,
                0.0f,
                4.0f
            )
            val layout = ControlCenterRowLayout()
            Assert.assertTrue(layout.solve(input))
            val snapshot =
                ControlCenterRowSnapshot()
            Assert.assertTrue(snapshot.copyFrom(layout, generation))
            return snapshot
        }

        private fun copySnapshot(source: ControlCenterRowSnapshot?): ControlCenterRowSnapshot {
            val copy = ControlCenterRowSnapshot()
            Assert.assertTrue(copy.copyFrom(source))
            return copy
        }
    }
}
