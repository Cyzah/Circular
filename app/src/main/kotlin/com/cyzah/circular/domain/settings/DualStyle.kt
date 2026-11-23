package com.cyzah.circular.domain.settings

enum class DualStyle {
    LAYERED,
    GROUPED,
    CAPSULE,
    DUO;

    fun allowsSwapContents(): Boolean {
        return this != DualStyle.DUO && this != DualStyle.CAPSULE
    }

    fun allowsShowPercent(): Boolean {
        return this != DualStyle.CAPSULE
    }

    companion object {
        @JvmStatic fun from(grouped: Boolean, capsule: Boolean, duo: Boolean): DualStyle {
            if (duo) {
                return DualStyle.DUO
            }
            if (capsule) {
                return DualStyle.CAPSULE
            }
            if (grouped) {
                return DualStyle.GROUPED
            }
            return DualStyle.LAYERED
        }
    }
}
