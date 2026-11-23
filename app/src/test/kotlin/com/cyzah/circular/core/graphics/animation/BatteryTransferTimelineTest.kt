package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.BatteryTransferTimelineTest.MutableClock
import org.junit.Assert
import org.junit.Test

class BatteryTransferTimelineTest {
    @Test
    fun progressIsClampedAndDroppedFramesJumpCorrectly() {
        val clock = MutableClock(1000L)
        val timeline = BatteryTransferTimeline(clock)

        Assert.assertEquals(0.0f, timeline.progress(1000L, 200L), 0.0f)
        Assert.assertEquals(0.5f, timeline.progress(900L, 200L), 0.001f)
        Assert.assertEquals(1.0f, timeline.progress(100L, 200L), 0.0f)
        Assert.assertTrue(timeline.complete(100L, 200L))
    }

    @Test
    fun invalidDurationDoesNotCreateRunningTimeline() {
        val clock = MutableClock(500L)
        val timeline = BatteryTransferTimeline(clock)

        Assert.assertEquals(1.0f, timeline.progress(500L, 0L), 0.0f)
        Assert.assertTrue(timeline.complete(500L, -1L))
        Assert.assertFalse(timeline.running(500L, 0L))
    }

    private class MutableClock
        (private val now: Long) : BatteryTransferTimeline.Clock {
        override fun now(): Long {
            return now
        }
    }
}
