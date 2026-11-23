package com.cyzah.circular.hook.runtime


/**
 * Source-locked external margins shared by every control-center style.
 */
object ControlCenterRowMargins {
    const val BATTERY_LEFT_DP: Float = 0.0f
    const val BATTERY_RIGHT_DP: Float = 0.0f
    const val WIFI_LEFT_DP: Float = 0.0f
    const val WIFI_RIGHT_DP: Float = 6.0f
    const val CELLULAR_LEFT_DP: Float = 0.0f
    const val CELLULAR_RIGHT_DP: Float = 4.0f

    @JvmStatic fun apply(input: ControlCenterRowInput, density: Float) {
        val safeDensity = if (java.lang.Float.isFinite(density) && density > 0.0f)
            density
        else
            0.0f
        input.setMarginLeft(
            ControlCenterRowComponent.BATTERY,
            BATTERY_LEFT_DP * safeDensity
        )
        input.setMarginRight(
            ControlCenterRowComponent.BATTERY,
            BATTERY_RIGHT_DP * safeDensity
        )
        input.setMarginLeft(
            ControlCenterRowComponent.WIFI,
            WIFI_LEFT_DP * safeDensity
        )
        input.setMarginRight(
            ControlCenterRowComponent.WIFI,
            WIFI_RIGHT_DP * safeDensity
        )
        input.setMarginLeft(
            ControlCenterRowComponent.CELLULAR,
            CELLULAR_LEFT_DP * safeDensity
        )
        input.setMarginRight(
            ControlCenterRowComponent.CELLULAR,
            CELLULAR_RIGHT_DP * safeDensity
        )
    }

    @JvmStatic fun physicalLeft(
        marginStartDp: Float,
        marginEndDp: Float,
        rtl: Boolean
    ): Float {
        return if (rtl) marginEndDp else marginStartDp
    }

    @JvmStatic fun physicalRight(
        marginStartDp: Float,
        marginEndDp: Float,
        rtl: Boolean
    ): Float {
        return if (rtl) marginStartDp else marginEndDp
    }
}
