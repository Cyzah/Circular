package com.cyzah.circular.domain.settings

import kotlin.math.max
import kotlin.math.min

object SizePolicy {
    @JvmStatic fun minimum(style: DualStyle?): Int {
        return if (style == DualStyle.CAPSULE) 12 else 18
    }

    @JvmStatic fun maximum(style: DualStyle?): Int {
        return 29
    }

    @JvmStatic fun clamp(style: DualStyle?, value: Int): Int {
        return max(minimum(style), min(maximum(style), value))
    }
}
