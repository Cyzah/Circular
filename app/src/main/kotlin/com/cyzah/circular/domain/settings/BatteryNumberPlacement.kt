package com.cyzah.circular.domain.settings

enum class BatteryNumberPlacement(@JvmField val value: Int) {
    HIDDEN(0),
    INSIDE(1),
    OUTSIDE(2);

    companion object {
        @JvmField val DEFAULT: BatteryNumberPlacement = OUTSIDE

        @JvmStatic fun from(value: Int): BatteryNumberPlacement =
            entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}
