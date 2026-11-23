package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.RelayoutSchedulerTest.FakeHost
import java.util.ArrayList
import org.junit.Assert
import org.junit.Test

class RelayoutSchedulerTest {
    @Test
    fun lastNativeVisibilityChangeWithinTheWindowIsStillLaidOut() {
        val host = FakeHost()
        val scheduler = RelayoutScheduler(host)
        host.visible = true
        scheduler.request(1L)
        host.advance(0L)
        Assert.assertEquals(1, host.layouts.size.toLong())
        Assert.assertEquals(true, host.layouts.get(0))

        host.advance(40L)
        host.visible = false
        scheduler.request(2L)
        host.advance(119L)
        Assert.assertEquals(1, host.layouts.size.toLong())
        host.advance(120L)
        Assert.assertEquals(2, host.layouts.size.toLong())
        Assert.assertEquals(false, host.layouts.get(1))
    }

    @Test
    fun repeatedVisibilityDuringOwnLayoutDoesNotStartAnEndlessLoop() {
        val host = FakeHost()
        val scheduler = RelayoutScheduler(host)
        scheduler.request(1L)
        host.advance(0L)
        host.advance(16L)
        scheduler.request(1L)
        host.advance(240L)
        Assert.assertEquals(1, host.layouts.size.toLong())
    }

    @Test
    fun burstsCoalesceAndUseTheLatestStateAtTheOriginalDeadline() {
        val host = FakeHost()
        val scheduler = RelayoutScheduler(host)
        scheduler.request(1L)
        host.advance(0L)
        host.advance(40L)
        scheduler.request(2L)
        host.advance(100L)
        host.visible = true
        scheduler.request(3L)
        host.advance(120L)
        Assert.assertEquals(2, host.layouts.size.toLong())
        Assert.assertEquals(true, host.layouts.get(1))
        host.advance(300L)
        Assert.assertEquals(2, host.layouts.size.toLong())
        scheduler.request(4L)
        host.advance(300L)
        Assert.assertEquals(3, host.layouts.size.toLong())
    }

    private class FakeHost : RelayoutScheduler.Host {
        @JvmField var time: Long = 0
        @JvmField var due: Long = 0
        @JvmField var queued: Runnable? = null
        @JvmField var visible: Boolean = false
        @JvmField val layouts: MutableList<Boolean?> = ArrayList<Boolean?>()

        override fun now(): Long {
            return this.time
        }

        override fun post(callback: Runnable?, delayMillis: Long) {
            if (this.queued != null) {
                throw AssertionError("More than one layout scheduled")
            }
            this.due = this.time + delayMillis
            this.queued = callback
        }

        override fun perform() {
            this.layouts.add(this.visible)
        }

        fun advance(nextTime: Long) {
            while (this.queued != null && this.due <= nextTime) {
                this.time = this.due
                val callback = this.queued
                this.queued = null
                callback!!.run()
            }
            this.time = nextTime
        }
    }
}
