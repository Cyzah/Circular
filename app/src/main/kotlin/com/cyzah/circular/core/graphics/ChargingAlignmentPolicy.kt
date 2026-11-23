package com.cyzah.circular.core.graphics

/**
 * Preserves the native charging offset while the control-center row is disabled.
 */
class ChargingAlignmentPolicy {
    private var capturedOffset = 0
    private var captured = false

    fun resolve(
        rawOffset: Int,
        charging: Boolean,
        dualRowEnabled: Boolean,
        panelClosed: Boolean
    ): Int {
        if (!charging) {
            clear()
            return rawOffset
        }
        if (dualRowEnabled) {
            clear()
            return 0
        }
        if (panelClosed && rawOffset != 0) {
            this.capturedOffset = rawOffset
            this.captured = true
        }
        return if (this.captured) this.capturedOffset else 0
    }

    private fun clear() {
        this.capturedOffset = 0
        this.captured = false
    }
}
