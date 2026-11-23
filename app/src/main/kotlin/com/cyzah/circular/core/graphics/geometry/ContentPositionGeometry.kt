package com.cyzah.circular.core.graphics.geometry

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Physical-pixel positioning and conservative endcap-aware ring openings.  */
object ContentPositionGeometry {
    const val TOP: Int = 0
    const val CENTER: Int = 1
    const val BOTTOM: Int = 2

    @JvmStatic fun units(pixels: Int, pixelsPerUnit: Float): Float {
        return if (java.lang.Float.isFinite(pixelsPerUnit) && pixelsPerUnit > 0)
            pixels / pixelsPerUnit
        else
            0f
    }

    @JvmStatic fun halfPixelUnits(counts: Int, pixelsPerUnit: Float): Float {
        return if (java.lang.Float.isFinite(pixelsPerUnit) && pixelsPerUnit > 0)
            counts * .5f / pixelsPerUnit
        else
            0f
    }

    @JvmStatic fun mix(from: Float, to: Float, progress: Float): Float {
        val p = if (java.lang.Float.isFinite(progress)) max(0f, min(1f, progress)) else 0f
        return from + (to - from) * p
    }

    /** Maximum angle from the opening's axis at which the padded box meets the ellipse.  */
    @JvmStatic fun halfGap(
        top: Boolean, cx: Float, cy: Float, rx: Float, ry: Float,
        left: Float, upper: Float, right: Float, bottom: Float,
        clearance: Float
    ): Float {
        if (!(rx > 0) || !(ry > 0) || !(right > left) || !(bottom > upper) || !java.lang.Float.isFinite(left + upper + right + bottom + clearance)) return 0f
        val pad = max(0f, clearance)
        val l = left - pad
        val t = upper - pad
        val r = right + pad
        val b = bottom + pad
        var result = -1f
        for (i in 0..3) {
            result = max(result, candidate(i * 90f, top, cx, cy, rx, ry, l, t, r, b))
        }
        for (i in 0..1) {
            val x = ((if (i == 0) l else r) - cx) / rx
            if (abs(x) <= 1) {
                val angle = Math.toDegrees(acos(x.toDouble())).toFloat()
                result = max(result, candidate(angle, top, cx, cy, rx, ry, l, t, r, b))
                result = max(result, candidate(360 - angle, top, cx, cy, rx, ry, l, t, r, b))
            }
            val y = ((if (i == 0) t else b) - cy) / ry
            if (abs(y) <= 1) {
                val angle = Math.toDegrees(asin(y.toDouble())).toFloat()
                result = max(result, candidate(angle, top, cx, cy, rx, ry, l, t, r, b))
                result = max(result, candidate(180 - angle, top, cx, cy, rx, ry, l, t, r, b))
            }
        }
        return if (result < 0) 0f else min(179f, result + .25f)
    }

    /**
     * Signed extent from an opening's axis towards one edge of its padded box.
     * A negative extent means that edge has crossed the axis; it must not enlarge
     * the opposite edge of the opening.
     */
    @JvmStatic fun openingEdge(
        top: Boolean, leftSide: Boolean,
        cx: Float, cy: Float, rx: Float, ry: Float,
        left: Float, upper: Float, right: Float, bottom: Float,
        clearance: Float
    ): Float {
        if (!(rx > 0) || !(ry > 0) || !(right > left) || !(bottom > upper) || !java.lang.Float.isFinite(cx + cy + rx + ry + left + upper + right + bottom + clearance)) return 0f
        val pad = max(0f, clearance)
        val l = left - pad
        val t = upper - pad
        val r = right + pad
        val b = bottom + pad
        var result = Float.NEGATIVE_INFINITY
        for (i in 0..3) {
            result = max(
                result, edgeCandidate(
                    i * 90f, top, leftSide,
                    cx, cy, rx, ry, l, t, r, b
                )
            )
        }
        for (i in 0..1) {
            val x = ((if (i == 0) l else r) - cx) / rx
            if (abs(x) <= 1) {
                val angle = Math.toDegrees(acos(x.toDouble())).toFloat()
                result = max(
                    result, edgeCandidate(
                        angle, top, leftSide,
                        cx, cy, rx, ry, l, t, r, b
                    )
                )
                result = max(
                    result, edgeCandidate(
                        360 - angle, top, leftSide,
                        cx, cy, rx, ry, l, t, r, b
                    )
                )
            }
            val y = ((if (i == 0) t else b) - cy) / ry
            if (abs(y) <= 1) {
                val angle = Math.toDegrees(asin(y.toDouble())).toFloat()
                result = max(
                    result, edgeCandidate(
                        angle, top, leftSide,
                        cx, cy, rx, ry, l, t, r, b
                    )
                )
                result = max(
                    result, edgeCandidate(
                        180 - angle, top, leftSide,
                        cx, cy, rx, ry, l, t, r, b
                    )
                )
            }
        }
        return if (java.lang.Float.isFinite(result)) min(179f, result + .25f) else 0f
    }

    private fun edgeCandidate(
        angle: Float, top: Boolean, leftSide: Boolean,
        cx: Float, cy: Float, rx: Float, ry: Float,
        l: Float, t: Float, r: Float, b: Float
    ): Float {
        val radians = Math.toRadians(angle.toDouble())
        val x = cx + rx * cos(radians).toFloat()
        val y = cy + ry * sin(radians).toFloat()
        if (x < l - .001f || x > r + .001f || y < t - .001f || y > b + .001f) {
            return Float.NEGATIVE_INFINITY
        }
        val delta = (angle - (if (top) 270 else 90) + 540) % 360 - 180
        if (abs(delta) > 90.0001f) return Float.NEGATIVE_INFINITY
        return if (top == leftSide) -delta else delta
    }

    private fun candidate(
        angle: Float, top: Boolean, cx: Float, cy: Float,
        rx: Float, ry: Float, l: Float, t: Float, r: Float, b: Float
    ): Float {
        val radians = Math.toRadians(angle.toDouble())
        val x = cx + rx * cos(radians).toFloat()
        val y = cy + ry * sin(radians).toFloat()
        if (x < l - .001f || x > r + .001f || y < t - .001f || y > b + .001f) return -1f
        val difference = (angle - (if (top) 270 else 90) + 720) % 360
        return min(difference, 360 - difference)
    }
}
