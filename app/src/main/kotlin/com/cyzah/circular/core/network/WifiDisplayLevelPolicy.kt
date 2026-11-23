package com.cyzah.circular.core.network

/** Supplies the displayed Wi-Fi strength while measurements recover.  */
class WifiDisplayLevelPolicy {
    private var measurementRequired = false
    private var lastValidLevel = -1
    private var lastValidAt: Long = 0

    fun resolve(now: Long, enabled: Boolean, connected: Boolean, measuredLevel: Int): Int {
        if (!enabled || !connected) {
            reset()
            return -1
        }
        this.measurementRequired = enabled && connected && measuredLevel < 0
        if (measuredLevel >= 0) {
            this.lastValidLevel = measuredLevel
            this.lastValidAt = now
            return measuredLevel
        }
        if (this.lastValidLevel >= 0 && now >= this.lastValidAt && now - this.lastValidAt <= LAST_VALID_HOLD_MS) {
            return this.lastValidLevel
        }
        return 0
    }

    fun needsMeasurement(): Boolean {
        return this.measurementRequired
    }

    fun reset() {
        this.measurementRequired = false
        this.lastValidLevel = -1
        this.lastValidAt = 0L
    }

    companion object {
        private const val LAST_VALID_HOLD_MS = 5000L
    }
}
