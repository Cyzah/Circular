package com.cyzah.circular.core.graphics.animation

import kotlin.math.max
import kotlin.math.min

/**
 * Pure, interruptible DUO Wi-Fi transition time state.
 */
class DuoWifiTransitionState {
    private var progress = -1.0f
    private var target = false
    private var startProgress = 0f
    private var startAt: Long = 0

    fun begin(nextTarget: Boolean, now: Long): Boolean {
        if (this.progress < 0.0f) {
            this.progress = if (nextTarget) 1.0f else 0.0f
            this.target = nextTarget
            return false
        }
        if (nextTarget == this.target) {
            return false
        }
        this.target = nextTarget
        this.startProgress = this.progress
        this.startAt = now
        return true
    }

    fun advance(now: Long, durationMs: Long): Float {
        val end = if (this.target) 1.0f else 0.0f
        val raw = min(
            1.0f,
            max(
                0.0f, (now - this.startAt)
                        / max(1L, durationMs).toFloat()
            )
        )
        this.progress = Motion.mix(
            this.startProgress,
            end,
            Motion.ease(raw)
        )
        if (raw >= 1.0f) {
            this.progress = end
        }
        return this.progress
    }

    fun complete(now: Long, durationMs: Long): Boolean {
        return now - this.startAt >= max(1L, durationMs)
    }

    fun progress(): Float {
        return this.progress
    }
}
