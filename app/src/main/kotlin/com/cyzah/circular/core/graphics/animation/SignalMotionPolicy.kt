package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.StackedSignals
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Fraction-based stage timing for the dual-row to combined-signal transition.
 * Expansion progress is 1 for the two-row layout and 0 for the combined icon.
 */
object SignalMotionPolicy {
    private const val TRIM_END = 0.30f
    private const val MOVE_END = 1.0f
    private const val SPREAD_START = 0.75f

    /** Preserve the battery's top-cap trajectory; route an intersecting signal to its own side.  */
    @JvmStatic @JvmOverloads
    fun clearBatteryX(
        x: Float, y: Float, halfWidth: Float, halfHeight: Float,
        left: Float, top: Float, right: Float, bottom: Float,
        preferLeft: Boolean, smooth: Boolean = false
    ): Float {
        if (!(right > left) || !(bottom > top) || !java.lang.Float.isFinite(left + top + right + bottom) || x + halfWidth <= left || x - halfWidth >= right || y + halfHeight <= top || y - halfHeight >= bottom) return x
        val routeX = if (preferLeft) left - halfWidth else right + halfWidth
        if (!smooth) {
            return routeX
        }
        val horizontalOverlap = min(
            x + halfWidth - left,
            right - (x - halfWidth)
        )
        val verticalOverlap = min(
            y + halfHeight - top,
            bottom - (y - halfHeight)
        )
        val horizontalDepth = Motion.clamp(
            horizontalOverlap / max(0.001f, halfWidth * 2.0f)
        )
        val verticalDepth = Motion.clamp(
            verticalOverlap / max(0.001f, halfHeight * 2.0f)
        )
        val penetration = min(horizontalDepth, verticalDepth)
        val blend = penetration * penetration * (3.0f - (2.0f * penetration))
        return Motion.mix(x, routeX, blend)
    }

    @JvmStatic fun signalProgress(expansion: Float): Float {
        return Motion.ease(expansion)
    }

    /** Route around painted opening strokes without treating their hollow interior as solid.  */
    @JvmStatic fun clearOpeningBatteryX(
        x: Float, y: Float, halfWidth: Float, halfHeight: Float,
        opening: OpeningBatteryMorph,
        solidLeft: Float, solidTop: Float,
        solidRight: Float, solidBottom: Float,
        preferLeft: Boolean
    ): Float {
        var result = x
        val radius = opening.strokeWidth / 2.0f
        if (opening.outlineAlpha > 0.0f) {
            for (point in 1..<opening.points.size / 2) {
                if (opening.contourStarts[point]) continue
                val index = point * 2
                val ax = opening.points[index - 2]
                val ay = opening.points[index - 1]
                val bx = opening.points[index]
                val by = opening.points[index + 1]
                val low = y - halfHeight - radius
                val high = y + halfHeight + radius
                if (max(ay, by) < low || min(ay, by) > high) continue
                var start = 0.0f
                var end = 1.0f
                if (abs(by - ay) > 0.0001f) {
                    val a = (low - ay) / (by - ay)
                    val b = (high - ay) / (by - ay)
                    start = Motion.clamp(min(a, b))
                    end = Motion.clamp(max(a, b))
                }
                val firstX = Motion.mix(ax, bx, start)
                val lastX = Motion.mix(ax, bx, end)
                val candidate = clearBatteryX(
                    x, y, halfWidth, halfHeight,
                    min(firstX, lastX) - radius, min(ay, by) - radius,
                    max(firstX, lastX) + radius, max(ay, by) + radius,
                    preferLeft, true
                )
                result = if (preferLeft) min(result, candidate) else max(result, candidate)
            }
        }
        if (opening.bodyAlpha > 0.0f) {
            // These bounds have already been projected into the signal host's canvas.
            val candidate = Motion.mix(
                x, clearBatteryX(
                    x, y, halfWidth, halfHeight,
                    solidLeft, solidTop, solidRight, solidBottom, preferLeft, true
                ),
                Motion.clamp(opening.bodyAlpha)
            )
            result = if (preferLeft) min(result, candidate) else max(result, candidate)
        }
        return result
    }

    @JvmStatic fun trimProgress(expansion: Float): Float {
        return smoothPhase(closingProgress(expansion), 0.0f, TRIM_END)
    }

    @JvmStatic fun moveProgress(expansion: Float): Float {
        return smoothPhase(
            closingProgress(expansion),
            TRIM_END,
            MOVE_END
        )
    }

    @JvmStatic fun rowProgress(expansion: Float): Float {
        return smoothPhase(
            closingProgress(expansion),
            SPREAD_START,
            1.0f
        )
    }

    @JvmStatic fun barProgress(expansion: Float): Float {
        return smoothPhase(
            closingProgress(expansion),
            SPREAD_START,
            1.0f
        )
    }

    @JvmStatic fun dotTrim(index: Int, trim: Float): Float {
        if (index <= 0) {
            return 0.0f
        }
        val safeIndex = min(3, index)
        return smoothPhase(
            trim,
            (3.0f - safeIndex) / 3.0f,
            (4.0f - safeIndex) / 3.0f
        )
    }

    @JvmStatic fun dotScale(index: Int, trim: Float, row: Float): Float {
        if (index <= 0) {
            return 1.0f
        }
        val safeIndex = min(3, index)
        val collapsed = 1.0f - Motion.clamp(trim)
        val restored = smoothPhase(
            row,
            (safeIndex - 1.0f) / 3.0f,
            safeIndex / 3.0f
        )
        return max(collapsed, restored)
    }

    @JvmStatic fun elementAlpha(
        sourceAlpha: Float,
        targetAlpha: Float,
        spread: Float,
        mergeFade: Boolean
    ): Float {
        return Motion.mix(sourceAlpha, if (mergeFade) 0.0f else targetAlpha, spread)
    }

    @JvmStatic fun coordinate(
        source: Float,
        sourceAnchor: Float,
        targetAnchor: Float,
        target: Float,
        trim: Float,
        move: Float,
        spread: Float
    ): Float {
        val packed = (source
                + ((sourceAnchor - source) * Motion.clamp(trim)))
        val moved = (packed
                + ((targetAnchor - packed) * Motion.clamp(move)))
        return (moved
                + ((target - moved) * Motion.clamp(spread)))
    }

    @JvmStatic fun updateRowAnchors(
        target: StackedSignals.Target,
        rows: Int
    ) {
        val safeRows = max(
            1,
            min(StackedSignals.Companion.ROWS, rows)
        )
        for (row in 0..<safeRows) {
            var sumX = 0.0f
            var sumY = 0.0f
            val start: Int = row * StackedSignals.Companion.BARS
            for (slot in 0..<StackedSignals.Companion.BARS) {
                val element = start + slot
                sumX += target.x[element]
                sumY += target.y[element]
            }
            target.rowAnchorX[row] = sumX / StackedSignals.Companion.BARS
            target.rowAnchorY[row] = sumY / StackedSignals.Companion.BARS
        }
    }

    private fun closingProgress(expansion: Float): Float {
        return 1.0f - Motion.clamp(expansion)
    }

    private fun smoothPhase(
        value: Float,
        start: Float,
        end: Float
    ): Float {
        val safe = Motion.clamp(value)
        val normalized = if (end <= start)
            0.0f
        else
            Motion.clamp((safe - start) / (end - start))
        return normalized * normalized * (3.0f - (2.0f * normalized))
    }
}
