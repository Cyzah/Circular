package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StyleProfile

/**
 * Source-locked appearance used only by control-center hosts 5 and 6.
 */
object ControlCenterAppearance {
    const val BATTERY_SIZE: Int = 27
    const val CAPSULE_WIFI_SCALE: Int = 110

    /**
     * Legacy fallback spacing for hosts without a solved row. The expanded
     * control-center path uses `ControlCenterRowMargins` instead.
     */
    const val CAPSULE_WIFI_LEFT_GAP_DP: Float = 4.0f
    const val WIFI_FINAL_CANVAS_SCALE: Float = 1.0f

    private val COMMON = StyleProfile(
        27,
        100,
        95,
        110,
        95,
        115,
        true,
        90,
        145,
        false
    )

    @JvmStatic fun profile(style: DualStyle?): StyleProfile {
        return COMMON
    }

    @JvmStatic @JvmOverloads
    fun profile(
        style: DualStyle?,
        size: Int,
        swapContents: Boolean = COMMON.swapContents
    ): StyleProfile {
        return StyleProfile(
            size,
            COMMON.weight,
            COMMON.wifiScale,
            COMMON.wifiWeight,
            COMMON.technologyScale,
            COMMON.technologyWeight,
            COMMON.showPercent,
            COMMON.percentScale,
            COMMON.percentWeight,
            swapContents
        )
    }

    @JvmStatic @JvmOverloads
    fun capsuleBodyProfile(
        size: Int,
        weight: Int,
        swapContents: Boolean = false
    ): StyleProfile {
        return StyleProfile(
            size,
            weight,
            CAPSULE_WIFI_SCALE,
            COMMON.wifiWeight,
            COMMON.technologyScale,
            COMMON.technologyWeight,
            COMMON.showPercent,
            COMMON.percentScale,
            COMMON.percentWeight,
            swapContents
        )
    }
}
