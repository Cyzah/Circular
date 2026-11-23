package com.cyzah.circular.core.graphics.animation

/**
 * Injectable monotonic timeline. It never schedules work; the existing main
 * Handler asks for the current time and redraws only while a phase is running.
 */
class BatteryTransferTimeline(private val clock: Clock) {
    fun interface Clock {
        fun now(): Long
    }

    fun now(): Long {
        return this.clock.now()
    }

    fun progress(start: Long, durationMs: Long): Float {
        if (durationMs <= 0L) {
            return 1.0f
        }
        val raw = (now() - start) / durationMs.toFloat()
        return Motion.ease(raw)
    }

    fun complete(start: Long, durationMs: Long): Boolean {
        return durationMs <= 0L || now() - start >= durationMs
    }

    fun running(start: Long, durationMs: Long): Boolean {
        return durationMs > 0L && !complete(start, durationMs)
    }
}
