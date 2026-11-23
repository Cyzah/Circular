package com.cyzah.circular.core.graphics.geometry

import kotlin.math.max
import kotlin.math.min

object CompactGeometry {
    @JvmStatic fun height(i: Int, i2: Int, i3: Int, i4: Int): Int {
        if (i <= i3 || i4 <= i3) {
            return i3
        }
        return max(i3, min(max(i2, i), i4))
    }

    @JvmStatic fun width(i: Int, i2: Int, i3: Int): Int {
        return if (i3 > 0) min(i, i3) else i2
    }
}
