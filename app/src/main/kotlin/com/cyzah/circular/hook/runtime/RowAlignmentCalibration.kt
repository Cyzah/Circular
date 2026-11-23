package com.cyzah.circular.hook.runtime

import kotlin.math.abs

/** Stable row offset capture; compensation itself must never become a new sample.  */
class RowAlignmentCalibration {
    private var previousPanelLeft = Float.NaN
    private var previousStatusLeft = Float.NaN
    private var hostShift = 0f
    var isCalibrated: Boolean = false
        private set

    fun sample(panelLeft: Float, statusLeft: Float): Boolean {
        if (this.isCalibrated) {
            return false
        }
        val stable = abs(this.previousPanelLeft - panelLeft) < 1.5f
                && abs(this.previousStatusLeft - statusLeft) < 1.5f
        this.previousPanelLeft = panelLeft
        this.previousStatusLeft = statusLeft
        val shift = panelLeft - statusLeft
        if (stable && abs(shift) < 150.0f) {
            this.hostShift = shift
            this.isCalibrated = true
            return true
        }
        return false
    }

    fun hostShift(): Float {
        return this.hostShift
    }

    fun speedShift(): Float {
        return -this.hostShift
    }

    fun reset() {
        this.previousPanelLeft = Float.NaN
        this.previousStatusLeft = Float.NaN
        this.hostShift = 0.0f
        this.isCalibrated = false
    }
}
