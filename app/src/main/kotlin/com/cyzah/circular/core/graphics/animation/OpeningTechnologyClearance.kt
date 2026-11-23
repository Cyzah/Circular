package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.TechnologyAvoidance
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import kotlin.math.max
import kotlin.math.min

/** Gesture-driven vertical clearance while the ring becomes a solid battery.  */
object OpeningTechnologyClearance {
    const val EXIT_START: Float = 0.06f
    const val EXIT_END: Float = 0.16f

    @JvmStatic fun gapProgress(expansion: Float): Float {
        return Motion.ease(Motion.clamp(expansion) / 0.08f)
    }

    @JvmStatic fun shapeProgress(expansion: Float): Float {
        return Motion.ease((Motion.clamp(expansion) - EXIT_END) / (0.65f - EXIT_END))
    }

    @JvmStatic fun textProgress(expansion: Float): Float {
        return StackedOverlayLayout.technologyProgress(
            Motion.clamp((Motion.clamp(expansion) - EXIT_START) / (1.0f - EXIT_START))
        )
    }

    @JvmStatic fun fontProgress(expansion: Float): Float {
        return StackedOverlayLayout.technologyProgress(
            Motion.clamp((Motion.clamp(expansion) - EXIT_END) / (1.0f - EXIT_END))
        )
    }

    @JvmStatic fun horizontalJoin(expansion: Float): Float {
        return Motion.ease((Motion.clamp(expansion) - EXIT_END) / (0.35f - EXIT_END))
    }

    @JvmStatic fun shift(
        labelLeft: Float, labelTop: Float, labelRight: Float, labelBottom: Float,
        contourLeft: Float, contourTop: Float, contourRight: Float, contourBottom: Float,
        fullLeft: Float, fullTop: Float, fullRight: Float, fullBottom: Float,
        expansion: Float, gap: Float, approach: Float, above: Boolean
    ): Float {
        if (!java.lang.Float.isFinite(expansion) || !java.lang.Float.isFinite(gap) || gap < 0.0f) {
            return 0.0f
        }
        val progress = Motion.clamp(expansion)
        if (progress == 0.0f) {
            return 0.0f
        }
        // The label starts inside the open ring. Begin leaving through its gap
        // before the shrinking side reaches it, without ejecting the closed
        // frame to the outside of the entire contour in a single step.
        val clearanceGap = max(1.0f, gap)
        // Short glyphs can need a much taller detour. Releasing that detour
        // over only the glyph height makes the return visibly abrupt.
        val clearanceApproach = max(
            approach,
            max(contourBottom - contourTop, fullBottom - fullTop) / 2.0f
        )
        val contourShift = (TechnologyAvoidance.motionBaselineShift(
            labelLeft, labelTop, labelRight, labelBottom,
            contourLeft, contourTop, contourRight, contourBottom,
            clearanceGap, clearanceApproach, above, Float.NaN, Float.NaN, progress
        )
                * Motion.ease((progress - EXIT_START) / (EXIT_END - EXIT_START)))
        // Reserve the final body before its hollow interior begins filling.
        // Taking the stronger displacement keeps this handoff on the same side.
        val solidShift = (TechnologyAvoidance.motionBaselineShift(
            labelLeft, labelTop, labelRight, labelBottom,
            fullLeft, fullTop, fullRight, fullBottom,
            clearanceGap, clearanceApproach, above, Float.NaN, Float.NaN, progress
        )
                * Motion.ease((progress - 0.4f) / 0.25f))
        return if (above) min(contourShift, solidShift) else max(contourShift, solidShift)
    }
}
