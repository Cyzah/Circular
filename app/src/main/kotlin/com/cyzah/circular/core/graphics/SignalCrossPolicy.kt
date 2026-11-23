package com.cyzah.circular.core.graphics

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object SignalCrossPolicy {
    const val ALPHA: Int = 128
    const val ARM: Float = 4.0f
    const val STROKE_WIDTH: Float = 4.0f

    /** 返回每个点是否保留：点圆与叉线笔画相交时，该点被叉遮住并移除。  */
    @JvmStatic fun visibleDots(
        points: FloatArray, centreX: Float, centreY: Float,
        dotRadius: Float, weight: Float
    ): BooleanArray {
        val visible = BooleanArray(points.size / 2)
        for (i in visible.indices) {
            val x = points[i * 2]
            val y = points[(i * 2) + 1]
            visible[i] = !coversDot(x, y, centreX, centreY, dotRadius, weight)
        }
        return visible
    }

    @JvmStatic fun coversDot(
        x: Float, y: Float, centreX: Float, centreY: Float,
        dotRadius: Float, weight: Float
    ): Boolean {
        val halfStroke = (STROKE_WIDTH * max(0.01f, weight)) / 2.0f
        val contactRadius = dotRadius + halfStroke
        val down = segmentDistance(
            x, y,
            centreX - ARM, centreY - ARM,
            centreX + ARM, centreY + ARM
        )
        val up = segmentDistance(
            x, y,
            centreX + ARM, centreY - ARM,
            centreX - ARM, centreY + ARM
        )
        return min(down, up) <= contactRadius
    }

    private fun segmentDistance(
        px: Float, py: Float,
        x1: Float, y1: Float, x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSquared = (dx * dx) + (dy * dy)
        if (lengthSquared <= 0.0f) {
            return distance(px, py, x1, y1)
        }
        var t = (((px - x1) * dx) + ((py - y1) * dy)) / lengthSquared
        t = max(0.0f, min(1.0f, t))
        return distance(px, py, x1 + (t * dx), y1 + (t * dy))
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(((dx * dx) + (dy * dy)).toDouble()).toFloat()
    }
}
