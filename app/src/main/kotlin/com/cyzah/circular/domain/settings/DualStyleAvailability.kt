package com.cyzah.circular.domain.settings

object DualStyleAvailability {
    @JvmStatic fun allows(style: DualStyle?, dualSim: Boolean): Boolean {
        return dualSim || style == DualStyle.LAYERED || style == DualStyle.CAPSULE
    }

    @JvmStatic fun fallback(selected: DualStyle?, dualSim: Boolean): DualStyle? {
        return if (allows(selected, dualSim)) selected else DualStyle.LAYERED
    }

    @JvmStatic fun enabledOptions(dualSim: Boolean): BooleanArray {
        val styles: Array<DualStyle?> = DualStyle.entries.toTypedArray()
        val enabled = BooleanArray(styles.size)
        for (index in styles.indices) {
            enabled[index] = allows(styles[index], dualSim)
        }
        return enabled
    }
}
