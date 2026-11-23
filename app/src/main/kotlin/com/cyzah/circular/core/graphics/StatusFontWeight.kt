package com.cyzah.circular.core.graphics

import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.max

/** Maps the existing sliders to the supplied Rounded family's real weights.  */
object StatusFontWeight {
    @JvmStatic fun batteryStrokeRatio(percent: Int): Float {
        return max(0.0f, (BatteryHeader.weight(percent) - 100).toFloat()) * 0.00075f
    }

    @JvmStatic fun battery(percent: Int): Int {
        return map(BatteryHeader.weight(percent), 50, 700)
    }

    @JvmStatic fun technology(percent: Int): Int {
        return map(DisplayPolicy.weight(percent), 80, 600)
    }

    private fun map(percent: Int, minimum: Int, normal: Int): Int {
        return if (percent <= 100)
            Math.round(400.0f + (percent - minimum) * (normal - 400.0f) / (100 - minimum))
        else
            Math.round(normal + (percent - 100) * (900.0f - normal) / 80.0f)
    }
}
