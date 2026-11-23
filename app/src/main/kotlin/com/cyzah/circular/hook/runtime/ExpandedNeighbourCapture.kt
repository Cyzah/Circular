package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion

/** Decides when a committed native row can replace the expanded neighbour endpoint.  */
class ExpandedNeighbourCapture {
    private var initialized = false
    private var closing = false
    private var lastControl = 0f
    private var lastMorph = 0f

    fun sample(control: Float, morph: Float, layoutReady: Boolean): Boolean {
        if (!java.lang.Float.isFinite(control) || !java.lang.Float.isFinite(morph)) return false
        val currentControl = Motion.clamp(control)
        val currentMorph = Motion.clamp(morph)
        val observed = this.initialized
        if (observed) {
            if (currentControl < lastControl || currentMorph < lastMorph) {
                this.closing = true
            } else if (currentControl > lastControl || currentMorph > lastMorph) {
                this.closing = false
            }
        }
        this.lastControl = currentControl
        this.lastMorph = currentMorph
        this.initialized = true
        return observed && !closing && layoutReady
                && BatteryTransferPolicy.openSettled(currentControl, currentMorph)
    }

    fun closing(): Boolean {
        return this.closing
    }

    fun reset() {
        this.initialized = false
        this.closing = false
        this.lastMorph = 0.0f
        this.lastControl = this.lastMorph
    }
}
