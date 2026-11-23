package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Chooses a screen-space neighbour boundary without inheriting host translation.  */
object NativeNeighbourMotion {
    @JvmStatic fun boundary(expanded: Float, closed: Float, remaining: Float, safe: Float): Float {
        return min(Motion.mix(closed, expanded, remaining), safe)
    }

    @JvmStatic fun remaining(progress: Float, placement: BatteryNumberPlacement?): Float {
        val horizontal = StackedOverlayLayout.signalXProgress(progress, placement)
        val signal = 1.0f - SignalMotionPolicy.moveProgress(
            SignalMotionPolicy.signalProgress(horizontal)
        )
        return max(signal, StackedOverlayLayout.technologyXProgress(progress))
    }

    @JvmStatic fun reservedWidth(
        hostRight: Float, expandedBoundary: Float,
        sourceBoundary: Float, closedBoundary: Float, remaining: Float
    ): Int {
        // The latest-moving component defines the safe edge only when moving
        // right. For leftward content, retain the leftmost endpoint instead.
        val safe = if (sourceBoundary <= closedBoundary)
            Motion.mix(closedBoundary, sourceBoundary, remaining)
        else
            closedBoundary
        val boundary = boundary(expandedBoundary, closedBoundary, remaining, safe)
        return max(1, ceil((hostRight - boundary).toDouble()).toInt())
    }
}
