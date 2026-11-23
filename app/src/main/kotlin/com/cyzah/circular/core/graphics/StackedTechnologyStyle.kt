package com.cyzah.circular.core.graphics

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.domain.settings.DualStyle

/** Endpoint and opacity policy for the two cached technology text painters.  */
object StackedTechnologyStyle {
    @JvmStatic fun targetSize(style: DualStyle?, statusBarSize: Float, hostSize: Float): Float {
        return if (usesStatusBarEndpoint(style)) statusBarSize else hostSize
    }

    @JvmStatic fun usesStatusBarEndpoint(style: DualStyle?): Boolean {
        return style == DualStyle.GROUPED || style == DualStyle.LAYERED || style == DualStyle.DUO
    }

    @JvmStatic fun endpointAlpha(opacity: Float, expansion: Float): Float {
        return opacity * (1.0f - Motion.clamp(expansion))
    }

    @JvmStatic fun hostAlpha(opacity: Float, expansion: Float): Float {
        return opacity * Motion.clamp(expansion)
    }
}
