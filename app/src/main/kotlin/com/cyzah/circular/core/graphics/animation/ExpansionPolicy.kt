package com.cyzah.circular.core.graphics.animation

import kotlin.math.max

object ExpansionPolicy {
    @JvmStatic fun progress(z: Boolean, z2: Boolean, z3: Boolean, f: Float): Float {
        if (z || (z2 && !z3)) {
            return 0.0f
        }
        return Motion.ease(f)
    }

    @JvmStatic fun panelEnabled(i: Int, z: Boolean): Boolean {
        if (i != 0) {
            if (!z) {
                return false
            }
            if (i != 1 && i != 2) {
                return false
            }
        }
        return true
    }

    @JvmStatic fun width(i: Int, i2: Int, i3: Int, f: Float): Int {
        return Motion.width(i, max(i, i2 + i3), f)
    }

    @JvmStatic fun nativeTranslation(f: Float, f2: Float, f3: Float, f4: Float, f5: Float): Float {
        val fClamp = Motion.clamp(f3)
        return (Motion.mix(f, f2, fClamp) - (f4 * fClamp)) - (f5 * (1.0f - fClamp))
    }

    @JvmStatic fun fakeType(str: String?): Boolean {
        return "FAKE_SYSTEM_ICON" == str || "KEYGUARD_FAKE_SYSTEM_ICON" == str
    }
}
