package com.cyzah.circular.core.network

/**
 * Bridges short Wi-Fi transition failures without exposing native status icons.
 */
class NetworkReadinessHold {
    private var latched = false
    private var lastReadyAt: Long = 0

    fun observe(now: Long, present: Boolean, healthy: Boolean) {
        if (!present) {
            reset()
            return
        }
        if (healthy) {
            this.latched = true
            this.lastReadyAt = now
        }
    }

    fun ready(now: Long, healthy: Boolean): Boolean {
        if (healthy) {
            return true
        }
        if (!this.latched || now < this.lastReadyAt) {
            return false
        }
        return now - this.lastReadyAt <= GRACE_MS
    }

    fun reset() {
        this.latched = false
        this.lastReadyAt = 0L
    }

    companion object {
        private const val GRACE_MS = 5000L
    }
}
