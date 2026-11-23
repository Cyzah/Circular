package com.cyzah.circular.hook.runtime

import kotlin.math.max

/** Coalesces visibility changes without losing the last change in a burst.  */
class RelayoutScheduler(private val host: Host) : Runnable {
    interface Host {
        fun now(): Long
        fun post(callback: Runnable?, delayMillis: Long)
        fun perform()
    }

    private var hasSignature = false
    private var signature: Long = 0
    private var pending = false
    private var hasRun = false
    private var lastRun: Long = 0

    fun request(nextSignature: Long) {
        if (this.hasSignature && this.signature == nextSignature) {
            return
        }
        this.hasSignature = true
        this.signature = nextSignature
        if (this.pending) {
            return
        }
        val now = this.host.now()
        val delay = if (this.hasRun) max(0L, 120L - (now - this.lastRun)) else
            0L
        this.pending = true
        this.host.post(this, delay)
    }

    override fun run() {
        if (!this.pending) {
            return
        }
        this.pending = false
        this.hasRun = true
        this.lastRun = this.host.now()
        this.host.perform()
    }
}
