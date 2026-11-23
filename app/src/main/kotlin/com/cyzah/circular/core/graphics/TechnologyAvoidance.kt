package com.cyzah.circular.core.graphics

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.max
import kotlin.math.min

/** Stateless vertical clearance for a technology label passing a battery shape.  */
object TechnologyAvoidance {
    @JvmStatic fun secondaryShift(
        primaryTop: Float, primaryBottom: Float,
        secondaryTop: Float, secondaryBottom: Float,
        gap: Float, above: Boolean
    ): Float {
        if (!java.lang.Float.isFinite(primaryTop) || !java.lang.Float.isFinite(primaryBottom) || !java.lang.Float.isFinite(secondaryTop) || !java.lang.Float.isFinite(
                secondaryBottom
            ) || primaryBottom <= primaryTop || secondaryBottom <= secondaryTop || !java.lang.Float.isFinite(gap) || gap < 0.0f
        ) {
            return 0.0f
        }
        // Keep the primary trajectory intact and only move the fading row
        // farther away from the battery, in its original row direction.
        return if (above) min(0.0f, primaryTop - gap - secondaryBottom) else max(
            0.0f,
            primaryBottom + gap - secondaryTop
        )
    }

    @JvmStatic fun motionBaselineShift(
        labelLeft: Float, labelTop: Float, labelRight: Float,
        labelBottom: Float, batteryLeft: Float, batteryTop: Float,
        batteryRight: Float, batteryBottom: Float,
        gap: Float, approach: Float, above: Boolean,
        endpointHorizontalGap: Float, endpointVerticalGap: Float,
        expansion: Float
    ): Float {
        val remaining = 1.0f - Motion.clamp(expansion)
        // Expanded rows already reserve a horizontal battery gap. Anticipatory
        // clearance must vanish there, without weakening actual intersections.
        if (remaining == 0.0f && (labelRight <= batteryLeft || labelLeft >= batteryRight)) {
            return 0.0f
        }
        return baselineShift(
            labelLeft, labelTop, labelRight, labelBottom,
            batteryLeft, batteryTop, batteryRight, batteryBottom,
            gap * remaining, max(0.0001f, approach * remaining), above,
            endpointHorizontalGap, endpointVerticalGap
        )
    }

    @JvmStatic fun baselineShift(
        labelLeft: Float, labelTop: Float, labelRight: Float,
        labelBottom: Float, batteryLeft: Float, batteryTop: Float,
        batteryRight: Float, batteryBottom: Float,
        gap: Float, approach: Float, above: Boolean,
        endpointHorizontalGap: Float, endpointVerticalGap: Float
    ): Float {
        // Keep the settled glyph at its exact endpoint. A fixed approach zone
        // would otherwise keep pushing a label that already clears the point.
        var gap = gap
        var approach = approach
        if (java.lang.Float.isFinite(endpointVerticalGap) && endpointVerticalGap >= 0.0f) {
            gap = min(gap, endpointVerticalGap)
        } else if (java.lang.Float.isFinite(endpointHorizontalGap) && endpointHorizontalGap > 0.0f) {
            gap = min(gap, endpointHorizontalGap / 2.0f)
            approach = min(approach, endpointHorizontalGap - gap)
        }
        return baselineShift(
            labelLeft, labelTop, labelRight, labelBottom,
            batteryLeft, batteryTop, batteryRight, batteryBottom, gap, approach, above
        )
    }

    @JvmStatic fun baselineShift(
        labelLeft: Float, labelTop: Float, labelRight: Float,
        labelBottom: Float, batteryLeft: Float, batteryTop: Float,
        batteryRight: Float, batteryBottom: Float,
        gap: Float, approach: Float, above: Boolean
    ): Float {
        if (!finiteBounds(labelLeft, labelTop, labelRight, labelBottom) || !finiteBounds(
                batteryLeft,
                batteryTop,
                batteryRight,
                batteryBottom
            ) || !java.lang.Float.isFinite(gap) || gap < 0.0f || !java.lang.Float.isFinite(approach) || approach <= 0.0f
        ) {
            return 0.0f
        }
        val distance = max(
            0.0f, max(
                batteryLeft - gap - labelRight, labelLeft - batteryRight - gap
            )
        )
        val weight = 1.0f - Motion.ease(distance / approach)
        val clearance =
            if (above) min(0.0f, batteryTop - gap - labelBottom) else max(0.0f, batteryBottom + gap - labelTop)
        return clearance * weight
    }

    private fun finiteBounds(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Boolean {
        return java.lang.Float.isFinite(left) && java.lang.Float.isFinite(top)
                && java.lang.Float.isFinite(right) && java.lang.Float.isFinite(bottom)
                && right > left && bottom > top
    }
}
