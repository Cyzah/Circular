package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.domain.settings.DualStyle

/** Coordinates of the centre of the rendered Wi-Fi bounds in the 120-unit icon.  */
object WifiEndpointGeometry {
    @JvmStatic fun centreY(
        style: DualStyle?, dual: Boolean, swapped: Boolean, showPercent: Boolean,
        regularY: Float, headerY: Float, glyphCentreY: Float, scale: Float
    ): Float {
        if (style == DualStyle.CAPSULE) {
            return 60.0f
        }
        if (dual && style == DualStyle.DUO) {
            return DuoLayout.wifiCentreY() + ((glyphCentreY - 57.0f) * scale)
        }
        if (swapped) {
            return headerY - (if (dual) 2.0f else 0.0f)
        }
        val pivotY = if (dual)
            RingGapPolicy.wifiCentreY(style, showPercent, regularY)
        else
            regularY
        return pivotY + ((glyphCentreY - 57.0f) * scale)
    }

    @JvmStatic fun weight(endpointWeight: Int, expandedWeight: Int, progress: Float): Int {
        return Math.round(Motion.mix(endpointWeight.toFloat(), expandedWeight.toFloat(), Motion.clamp(progress)))
    }

    @JvmStatic fun alpha(endpointAlpha: Float, progress: Float): Float {
        return Motion.mix(Motion.clamp(endpointAlpha), 1.0f, Motion.clamp(progress))
    }
}
