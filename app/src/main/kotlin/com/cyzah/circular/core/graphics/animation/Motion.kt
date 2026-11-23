package com.cyzah.circular.core.graphics.animation

import kotlin.math.max
import kotlin.math.min

object Motion {
    @JvmStatic fun clamp(f: Float): Float {
        if (java.lang.Float.isFinite(f)) {
            return max(0.0f, min(1.0f, f))
        }
        return 0.0f
    }

    @JvmStatic fun ease(f: Float): Float {
        val fClamp = clamp(f)
        return fClamp * fClamp * (3.0f - (fClamp * 2.0f))
    }

    @JvmStatic fun mix(f: Float, f2: Float, f3: Float): Float {
        return f + ((f2 - f) * clamp(f3))
    }

    @JvmStatic fun mixBounded(from: Float, to: Float, progress: Float, maximum: Float): Float {
        val safeMaximum = if (java.lang.Float.isFinite(maximum)) max(0.0f, maximum) else 0.0f
        if (!java.lang.Float.isFinite(from) || from < 0.0f) {
            return if (java.lang.Float.isFinite(to)) max(0.0f, min(safeMaximum, to)) else 0.0f
        }
        if (!java.lang.Float.isFinite(to) || to < 0.0f) {
            return max(0.0f, min(safeMaximum, from))
        }
        return max(0.0f, min(safeMaximum, mix(from, to, progress)))
    }

    @JvmStatic fun width(i: Int, i2: Int, f: Float): Int {
        return Math.round(mix(i.toFloat(), i2.toFloat(), f))
    }

    @JvmStatic fun batteryWidthOffset(i: Int, i2: Int, z: Boolean): Int {
        return if (z) i2 - i else i - i2
    }

    @JvmStatic fun signalLevel(i: Int, num: Int?): Int {
        val iMin: Int
        val i2: Int
        if (num == null) {
            i2 = -1
            iMin = min(4, i)
        } else {
            iMin = min(4, num)
            i2 = 0
        }
        return max(i2, iMin)
    }

    @JvmStatic fun nativeLevel(str: String?): Int {
        if (str == null) {
            return -1
        }
        if (str == "stat_sys_signal_null" || str.startsWith("stat_sys_signal_null_")) {
            return -2
        }
        val prefixLength = "stat_sys_signal_".length
        if (!str.startsWith("stat_sys_signal_") || str.length <= prefixLength) return -1
        val level = str[prefixLength]
        if (level !in '0'..'4') return -1
        if (str.length > prefixLength + 1 && str[prefixLength + 1] != '_') {
            return -1
        }
        return level - '0'
    }
}
