package com.cyzah.circular.core.network

import com.cyzah.circular.hook.runtime.BurnInDrift
import kotlin.math.min

class WifiRecovery {
    private var attempts = 0
    private var pending = false

    fun schedule(): Long {
        if (this.pending) {
            return -1L
        }
        this.pending = true
        val jMin = min(BurnInDrift.Companion.INTERVAL_MS, (2000 shl min(5, this.attempts)).toLong())
        this.attempts = min(6, this.attempts + 1)
        return jMin
    }

    fun fired() {
        this.pending = false
    }

    fun reset() {
        this.pending = false
        this.attempts = 0
    }

    fun pending(): Boolean {
        return this.pending
    }

    fun attempts(): Int {
        return this.attempts
    }

    companion object {
        @JvmStatic fun validRssi(i: Int): Boolean {
            return i > -127 && i < 0
        }

        @JvmStatic fun needsMeasurement(z: Boolean, z2: Boolean, z3: Boolean, i: Int): Boolean {
            if (z) {
                return z2 && z3 && i < 0
            }
            return true
        }

        @JvmStatic fun preferFresh(z: Boolean, z2: Boolean, i: Int, z3: Boolean, i2: Int): Boolean {
            if (z3 && validRssi(i2)) {
                return if (!z && z2 && validRssi(i)) false else true
            }
            return false
        }
    }
}
