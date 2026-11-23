package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.ControlCenterAppearance
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StyleProfile

/** Compatibility facade; the shared projection lives in the domain layer.  */
object ControlCenterAppearance {
    @JvmField val BATTERY_SIZE: Int = ControlCenterAppearance.BATTERY_SIZE
    @JvmField val CAPSULE_WIFI_SCALE: Int = ControlCenterAppearance.CAPSULE_WIFI_SCALE
    @JvmField val CAPSULE_WIFI_LEFT_GAP_DP: Float = ControlCenterAppearance.CAPSULE_WIFI_LEFT_GAP_DP
    @JvmField val WIFI_FINAL_CANVAS_SCALE: Float = ControlCenterAppearance.WIFI_FINAL_CANVAS_SCALE
    @JvmStatic fun profile(style: DualStyle?): StyleProfile? {
        return ControlCenterAppearance.profile(style)
    }

    @JvmStatic fun profile(style: DualStyle?, size: Int): StyleProfile {
        return profile(style, size)
    }

    @JvmStatic fun profile(style: DualStyle?, size: Int, swap: Boolean): StyleProfile {
        return ControlCenterAppearance.profile(style, size, swap)
    }

    @JvmStatic fun capsuleBodyProfile(size: Int, weight: Int): StyleProfile {
        return capsuleBodyProfile(size, weight)
    }

    @JvmStatic fun capsuleBodyProfile(size: Int, weight: Int, swap: Boolean): StyleProfile {
        return ControlCenterAppearance.capsuleBodyProfile(size, weight, swap)
    }
}
