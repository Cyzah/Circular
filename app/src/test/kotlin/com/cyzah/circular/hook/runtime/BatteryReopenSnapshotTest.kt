package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class BatteryReopenSnapshotTest {
    private class Fixture : BatteryTransferTimeline.Clock {
        @JvmField var now: Long = 1000
        @JvmField val state: BatteryTransferState = BatteryTransferState(BatteryTransferTimeline(this))
        override fun now(): Long {
            return now
        }

        fun frame(progress: Float) {
            state.update(progress, 0f, progress, true, true, true, now)
        }

        fun path() {
            frame(0f)
            frame(1f)
            frame(0f)
        }
    }

    @Test
    fun reopeningRetainsTheDisplayedHalfTraceAndPointBeforeFading() {
        val f = Fixture()
        f.path()
        f.now += 325
        f.frame(0f)
        f.frame(0.2f)
        val old = f.state.reopeningSnapshot()
        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, f.state.phase())
        Assert.assertEquals(1f, f.state.reopenAlpha(), 0f)
        Assert.assertEquals(0f, f.state.openingAlpha(), 0f)
        Assert.assertTrue(f.state.needsFrame())
        Assert.assertFalse(old.trackVisible)
        Assert.assertTrue(old.pointVisible)
        Assert.assertEquals(0.5f, old.pointProgress, 0.0001f)
        Assert.assertEquals(0.5f, old.traceProgress, 0.0001f)
        Assert.assertEquals(0f, old.fillFraction, 0f)
        f.now += 60
        f.frame(0.4f)
        Assert.assertEquals(0.5f, f.state.reopenAlpha(), 0.0001f)
        Assert.assertEquals(1f, f.state.reopenAlpha() + f.state.openingAlpha(), 0.0001f)
        Assert.assertEquals(0.5f, old.traceProgress, 0.0001f)
        f.now += 60
        f.frame(0.4f)
        Assert.assertEquals(0f, f.state.reopenAlpha(), 0f)
        Assert.assertEquals(1f, f.state.openingAlpha(), 0f)
        Assert.assertFalse(f.state.needsFrame())
    }

    @Test
    fun reopeningDuringRestoreFreezesThePartialFillInsteadOfCompletingIt() {
        val f = Fixture()
        f.path()
        f.now += 650
        f.frame(0f)
        f.now += 160
        f.frame(0f)
        f.frame(0.1f)
        val old = f.state.reopeningSnapshot()
        Assert.assertTrue(old.trackVisible)
        Assert.assertFalse(old.pointVisible)
        Assert.assertEquals(0.5f, old.fillFraction, 0.0001f)
        Assert.assertEquals(0f, old.traceAlpha, 0f)
        f.now += 60
        f.frame(0.3f)
        Assert.assertEquals(0.5f, old.fillFraction, 0.0001f)
    }

    @Test
    fun settledOpenAndCancelledOpeningReleaseTheSnapshot() {
        val f = Fixture()
        f.path()
        f.frame(0.2f)
        f.frame(1f)
        Assert.assertEquals(0f, f.state.reopenAlpha(), 0f)
        Assert.assertFalse(f.state.needsFrame())
        f.frame(0f)
        f.frame(0.2f)
        Assert.assertEquals(1f, f.state.reopenAlpha(), 0f)
        f.frame(0f)
        Assert.assertEquals(0f, f.state.reopenAlpha(), 0f)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, f.state.phase())
    }

    @Test
    fun invalidationDoesNotLeaveAReopeningFrameScheduled() {
        val f = Fixture()
        f.path()
        f.frame(0.2f)
        f.state.invalidate()
        Assert.assertEquals(0f, f.state.reopenAlpha(), 0f)
        Assert.assertEquals(1f, f.state.openingAlpha(), 0f)
        Assert.assertFalse(f.state.needsFrame())
    }
}
