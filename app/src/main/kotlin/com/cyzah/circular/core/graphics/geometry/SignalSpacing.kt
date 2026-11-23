package com.cyzah.circular.core.graphics.geometry

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object SignalSpacing {
    private const val BASE_RADIUS = 4.9f
    private const val RING_CENTRE_X = 60.0f
    private const val RING_CENTRE_Y = 58.0f
    private const val OUTER_RING_RADIUS = 50.0f
    private const val INNER_RING_RADIUS = 38.0f
    private const val MIN_LAYER_WEIGHT = 0.96f
    private const val MAX_LAYER_SPACING_WEIGHT = 1.2f
    private const val RAW_LAYER_DROP = 10.0f
    private const val LAYER_UP_SHIFT = 3.5f
    private const val LAYER_GAP_REDUCTION = 4.0f

    @JvmStatic fun radius(weightScale: Float): Float {
        return BASE_RADIUS * max(0.01f, weightScale)
    }

    @JvmStatic fun scaleFromPercent(percent: Int): Float {
        val safePercent = max(80, min(180, percent))
        return safePercent * 0.012f
    }

    @JvmStatic fun dotRadiusFromPercent(percent: Int): Float {
        return radius(scaleFromPercent(percent))
    }

    @JvmStatic fun rowCentres(count: Int, centreX: Float, weightScale: Float, edgeGap: Float, availableWidth: Float): FloatArray {
        val safeCount = max(0, count)
        val centres = FloatArray(safeCount)
        for (i in 0..<safeCount) {
            centres[i] = rowCentre(
                i,
                safeCount,
                centreX,
                weightScale,
                edgeGap,
                availableWidth
            )
        }
        return centres
    }

    @JvmStatic fun rowCentre(
        index: Int,
        count: Int,
        centreX: Float,
        weightScale: Float,
        edgeGap: Float,
        availableWidth: Float
    ): Float {
        val safeCount = max(0, count)
        if (safeCount == 0) {
            return centreX
        }
        val safeIndex = max(0, min(safeCount - 1, index))
        val radius = radius(weightScale)
        var gap = edgeGap
        if (safeCount > 1) {
            val maxGap = max(
                0.0f,
                (availableWidth - (2.0f * radius * safeCount))
                        / (safeCount - 1)
            )
            gap = min(edgeGap, maxGap)
        }
        val step = (2.0f * radius) + gap
        val first = centreX - (((safeCount - 1) * step) / 2.0f)
        return first + (safeIndex * step)
    }

    @JvmStatic fun pairCentres(centreY: Float, weightScale: Float, edgeGap: Float): FloatArray {
        return floatArrayOf(
            pairCentre(0, centreY, weightScale, edgeGap),
            pairCentre(1, centreY, weightScale, edgeGap)
        )
    }

    @JvmStatic fun pairCentre(
        index: Int,
        centreY: Float,
        weightScale: Float,
        edgeGap: Float
    ): Float {
        val half = radius(weightScale) + (edgeGap / 2.0f)
        return if (index <= 0) centreY - half else centreY + half
    }

    @JvmStatic fun outerRow(weightScale: Float): FloatArray {
        val points = FloatArray(8)
        for (i in 0..3) {
            points[i * 2] = outerX(i, weightScale)
            points[i * 2 + 1] = outerY(i, weightScale)
        }
        return points
    }

    @JvmStatic fun innerRow(weightScale: Float): FloatArray {
        val points = FloatArray(6)
        for (i in 0..2) {
            points[i * 2] = innerX(i, weightScale)
            points[i * 2 + 1] = innerY(i, weightScale)
        }
        return points
    }

    @JvmStatic fun groupedRow(weightScale: Float): FloatArray {
        val points = FloatArray(8)
        for (i in 0..3) {
            points[i * 2] = groupedX(i, weightScale)
            points[i * 2 + 1] = circleY(points[i * 2], OUTER_RING_RADIUS)
        }
        return points
    }

    @JvmStatic fun groupedX(index: Int, weightScale: Float): Float {
        return ringX(index, weightScale)
    }

    @JvmStatic fun outerX(index: Int, weightScale: Float): Float {
        return rowCentre(
            index,
            4,
            RING_CENTRE_X,
            layerSpacingWeight(weightScale),
            5.7f,
            84.0f
        )
    }

    @JvmStatic fun outerY(index: Int, weightScale: Float): Float {
        val baseY = circleY(outerX(index, weightScale), OUTER_RING_RADIUS)
        return baseY + (outerCentreY(weightScale) - RING_CENTRE_Y)
    }

    @JvmStatic fun ringY(index: Int, weightScale: Float): Float {
        return circleY(ringX(index, weightScale), OUTER_RING_RADIUS)
    }

    @JvmStatic fun outerCentreY(weightScale: Float): Float {
        val innerAverage = averageInnerY(weightScale)
        val outerOffsetAverage = averageBaseOuterY(weightScale) - RING_CENTRE_Y
        return innerAverage + layerGap() - outerOffsetAverage
    }

    @JvmStatic fun innerX(index: Int, weightScale: Float): Float {
        return rowCentre(
            index,
            3,
            RING_CENTRE_X,
            layerSpacingWeight(weightScale),
            5.7f,
            84.0f
        )
    }

    @JvmStatic fun innerY(index: Int, weightScale: Float): Float {
        return circleY(innerX(index, weightScale), INNER_RING_RADIUS) - LAYER_UP_SHIFT
    }

    @JvmStatic fun ringX(index: Int, weightScale: Float): Float {
        return rowCentre(
            index,
            4,
            RING_CENTRE_X,
            weightScale,
            5.7f,
            84.0f
        )
    }

    private fun layerSpacingWeight(weightScale: Float): Float {
        return min(MAX_LAYER_SPACING_WEIGHT, max(0.01f, weightScale))
    }

    private fun layerGap(): Float {
        val innerAverage = rawInnerAverageY(MIN_LAYER_WEIGHT)
        val outerAverage = averageBaseOuterY(MIN_LAYER_WEIGHT) + RAW_LAYER_DROP
        return outerAverage - innerAverage - LAYER_GAP_REDUCTION
    }

    private fun averageInnerY(weightScale: Float): Float {
        var sum = 0.0f
        for (i in 0..2) {
            sum += innerY(i, weightScale)
        }
        return sum / 3.0f
    }

    private fun rawInnerAverageY(weightScale: Float): Float {
        var sum = 0.0f
        for (i in 0..2) {
            sum += circleY(innerX(i, weightScale), INNER_RING_RADIUS)
        }
        return sum / 3.0f
    }

    private fun averageBaseOuterY(weightScale: Float): Float {
        var sum = 0.0f
        for (i in 0..3) {
            sum += circleY(outerX(i, weightScale), OUTER_RING_RADIUS)
        }
        return sum / 4.0f
    }

    private fun circleY(x: Float, radius: Float): Float {
        val dx = min(radius, abs(x - RING_CENTRE_X))
        val dy = sqrt(max(0.0f, (radius * radius) - (dx * dx)).toDouble()).toFloat()
        return RING_CENTRE_Y + dy
    }
}
