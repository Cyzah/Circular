package com.cyzah.circular.core.graphics.animation

class MotionHandoff {
    private var dirty = false
    private var progress = 0f

    fun progress(f: Float) {
        val fClamp = Motion.clamp(f)
        if (this.progress != fClamp) {
            this.progress = fClamp
            this.dirty = true
        }
    }

    fun geometryChanged() {
        this.dirty = true
    }

    fun committed() {
        this.dirty = false
    }

    fun owns(z: Boolean, z2: Boolean, z3: Boolean): Boolean {
        if (!z || z2 || z3) {
            return false
        }
        return this.progress > 0.0f || this.dirty
    }
}
