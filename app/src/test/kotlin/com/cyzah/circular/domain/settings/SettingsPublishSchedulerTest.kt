package com.cyzah.circular.domain.settings

import java.util.ArrayList
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class SettingsPublishSchedulerTest {
    @Test
    fun firstChangeOfANewGestureIsImmediateAfterFlushOrDiscretePublication() {
        val clock = Clock()
        clock.change(1)
        clock.publisher.flush()
        clock.advance(1)
        clock.change(2)
        Assert.assertEquals(mutableListOf<Int?>(1, 2), clock.published)
        clock.value = 3
        clock.publisher.publishNow()
        clock.advance(2)
        clock.change(4)
        Assert.assertEquals(mutableListOf<Int?>(1, 2, 3, 4), clock.published)
    }

    @Test
    fun firstChangeIsImmediateAndFiftyMillisecondBoundaryPublishesLatest() {
        val clock = Clock()
        clock.change(1)
        Assert.assertEquals(mutableListOf<Int?>(1), clock.published)
        clock.advance(49)
        clock.change(2)
        Assert.assertEquals(mutableListOf<Int?>(1), clock.published)
        clock.advance(50)
        Assert.assertEquals(mutableListOf<Int?>(1, 2), clock.published)
        clock.change(3)
        clock.publisher.flush()
        clock.publisher.flush()
        Assert.assertEquals(mutableListOf<Int?>(1, 2, 3), clock.published)
        Assert.assertNull(clock.work)
    }

    @Test
    fun oneSecondOfRealChangesHasAtMostTwentyOneTimedPublicationsAndFinalValue() {
        val clock = Clock()
        for (time in 0..1000) {
            clock.advance(time.toLong())
            clock.change(time + 1)
        }
        Assert.assertTrue(clock.published.size <= 21)
        clock.publisher.flush()
        Assert.assertEquals(1001, clock.published.get(clock.published.size - 1))
        val count = clock.published.size
        clock.publisher.flush()
        clock.advance(1100)
        Assert.assertEquals(count.toLong(), clock.published.size.toLong())
    }

    @Test
    fun immediateToggleStyleOrDiagnosticConsumesThePendingLatestRow() {
        val clock = Clock()
        clock.change(1)
        clock.advance(49)
        clock.change(2)
        clock.value = 3
        clock.publisher.publishNow()
        Assert.assertEquals(mutableListOf<Int?>(1, 3), clock.published)
        Assert.assertNull(clock.work)
        clock.advance(50)
        Assert.assertEquals(2, clock.published.size.toLong())
    }

    @Test
    fun releasePauseAndCloseCannotLeaveOrReplayPendingCallbacks() {
        val clock = Clock()
        clock.change(1)
        clock.advance(999)
        clock.change(2)
        clock.advance(1000)
        clock.change(3)
        clock.publisher.flush()
        Assert.assertEquals(mutableListOf<Int?>(1, 2, 3), clock.published)
        clock.change(4)
        clock.advance(1001)
        clock.change(5)
        clock.publisher.close()
        clock.publisher.flush()
        clock.publisher.publishNow()
        clock.publisher.changedContinuously()
        clock.advance(2000)
        Assert.assertEquals(mutableListOf<Int?>(1, 2, 3, 4), clock.published)
    }

    @Test
    fun closingWithoutPendingChangeHasNoSideEffects() {
        val clock = Clock()
        clock.publisher.flush()
        clock.publisher.close()
        clock.publisher.close()
        Assert.assertTrue(clock.published.isEmpty())
    }

    private class Clock : SettingsPublishScheduler.Scheduler, Runnable {
        @JvmField var now: Long = 0
        @JvmField var due: Long = 0
        @JvmField var work: Runnable? = null
        @JvmField var value: Int = 0
        @JvmField val published: MutableList<Int?> = ArrayList<Int?>()
        @JvmField val publisher: SettingsPublishScheduler = SettingsPublishScheduler(this, this, 50)
        override fun nowMillis(): Long {
            return now
        }

        override fun post(task: Runnable, delay: Long) {
            Assert.assertNull("only one pending publication", work)
            work = task
            due = now + delay
        }

        override fun remove(task: Runnable) {
            if (work === task) work = null
        }

        override fun run() {
            published.add(value)
        }

        fun change(next: Int) {
            value = next
            publisher.changedContinuously()
        }

        fun advance(time: Long) {
            while (work != null && due <= time) {
                now = due
                val pending = work
                work = null
                pending!!.run()
            }
            now = time
        }
    }
}
