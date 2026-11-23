package com.cyzah.circular.domain.settings

import kotlin.math.max
import kotlin.math.min

object ColorSelection {
    @JvmStatic fun fraction(f: Float, f2: Float, f3: Float): Float {
        if (java.lang.Float.isFinite(f) && java.lang.Float.isFinite(f2) && java.lang.Float.isFinite(f3) && f3 > 0.0f) {
            return max(0.0f, min(1.0f, (f - f2) / f3))
        }
        return 0.0f
    }
}
