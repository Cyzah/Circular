package com.cyzah.circular.core.graphics.animation

class KeyguardHandoff {
    private var prepared = false
    private var returning = false

    fun prepare(z: Boolean, z2: Boolean, z3: Boolean, z4: Boolean, f: Float): Boolean {
        val z5: Boolean
        if (!z2 || !z3) {
            clear()
            return z
        }
        if (z && z4) {
            this.prepared = true
        }
        if (!z && this.prepared) {
            z5 = true
        } else {
            z5 = false
        }
        this.returning = z5
        if (z || this.prepared) {
            return true
        }
        return false
    }

    fun retained(): Boolean {
        return this.returning
    }

    fun clear() {
        this.prepared = false
        this.returning = false
    }

    companion object {
        @JvmStatic fun suppressReflow(i: Int, z: Boolean, z2: Boolean, z3: Boolean): Boolean {
            return i == 1 && z && !z2 && !z3
        }
    }
}
