package com.cyzah.circular.core.graphics

import com.cyzah.circular.domain.settings.DualStyle

object ChargeBoltPolicy {
    @JvmStatic fun showBolt(style: DualStyle?): Boolean {
        return style == DualStyle.CAPSULE
    }
}
