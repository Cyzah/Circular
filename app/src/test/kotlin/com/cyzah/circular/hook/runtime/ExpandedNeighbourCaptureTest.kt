package com.cyzah.circular.hook.runtime

import org.junit.Assert
import org.junit.Test

class ExpandedNeighbourCaptureTest {
    @Test
    fun nativeOpenPlateauBelowOneCanCaptureAfterDirectionIsObserved() {
        val capture = ExpandedNeighbourCapture()
        Assert.assertFalse(capture.sample(0.979f, 0.979f, true))
        Assert.assertTrue(capture.sample(0.979f, 0.979f, true))
    }

    @Test
    fun openingCapturesOnlyAfterBothChannelsReachTheOpenRegion() {
        val capture = ExpandedNeighbourCapture()
        Assert.assertFalse(capture.sample(0.8f, 0.8f, true))
        Assert.assertFalse(capture.sample(0.94f, 0.96f, true))
        Assert.assertTrue(capture.sample(0.97f, 0.979f, true))
    }

    @Test
    fun closingPlateauCannotReplaceTheExpandedEndpoint() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.979f, 0.979f, true)
        Assert.assertTrue(capture.sample(0.979f, 0.979f, true))
        Assert.assertFalse(capture.sample(0.97f, 0.97f, true))
        Assert.assertFalse(capture.sample(0.97f, 0.97f, true))
        Assert.assertFalse(capture.sample(0.96f, 0.96f, true))
        Assert.assertFalse(capture.sample(0.96f, 0.96f, true))
    }

    @Test
    fun pendingLayoutStillRecordsTheStartOfClosing() {
        val capture = ExpandedNeighbourCapture()
        Assert.assertFalse(capture.sample(0.979f, 0.979f, false))
        Assert.assertFalse(capture.sample(0.97f, 0.97f, false))
        Assert.assertFalse(capture.sample(0.97f, 0.97f, true))
    }

    @Test
    fun settledLayoutCanCaptureWithoutWaitingForAnotherProgressChange() {
        val capture = ExpandedNeighbourCapture()
        Assert.assertFalse(capture.sample(0.979f, 0.979f, false))
        Assert.assertTrue(capture.sample(0.979f, 0.979f, true))
    }

    @Test
    fun nativeControlDecreaseWinsOverAHeldOrIncreasingMorph() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.99f, 0.96f, true)
        Assert.assertFalse(capture.sample(0.98f, 0.97f, true))
        Assert.assertFalse(capture.sample(0.98f, 0.97f, true))
    }

    @Test
    fun morphDecreaseWinsOverAHeldOrIncreasingControl() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.96f, 0.99f, true)
        Assert.assertFalse(capture.sample(0.97f, 0.98f, true))
        Assert.assertFalse(capture.sample(0.97f, 0.98f, true))
    }

    @Test
    fun reversingBackTowardOpenAllowsANewCommittedEndpoint() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.979f, 0.979f, true)
        Assert.assertFalse(capture.sample(0.96f, 0.96f, true))
        Assert.assertTrue(capture.sample(0.97f, 0.97f, true))
        Assert.assertTrue(capture.sample(0.979f, 0.979f, true))
    }

    @Test
    fun resetRequiresFreshDirectionHistoryAfterHostReattachment() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.979f, 0.979f, true)
        capture.sample(0.96f, 0.96f, true)
        capture.reset()
        Assert.assertFalse(capture.sample(0.979f, 0.979f, true))
        Assert.assertTrue(capture.sample(0.979f, 0.979f, true))
    }

    @Test
    fun invalidSamplesCannotForgetAnObservedClosingDirection() {
        val capture = ExpandedNeighbourCapture()
        capture.sample(0.979f, 0.979f, true)
        capture.sample(0.96f, 0.96f, true)
        Assert.assertFalse(capture.sample(Float.NaN, 0.979f, true))
        Assert.assertFalse(capture.sample(0.979f, Float.POSITIVE_INFINITY, true))
        Assert.assertFalse(capture.sample(0.96f, 0.96f, true))
    }
}
