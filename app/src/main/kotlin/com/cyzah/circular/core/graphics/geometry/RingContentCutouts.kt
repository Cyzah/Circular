package com.cyzah.circular.core.graphics.geometry

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Cached visible contours shared by static rendering, animation sampling and projection.  */
class RingContentCutouts {
    private var x = FloatArray(0)
    private var y = FloatArray(0)
    private var distance = FloatArray(0)
    private var sourceDistance = FloatArray(0)
    private var start = BooleanArray(0)
    private var boxes = FloatArray(0)
    private var originalBoxes = FloatArray(0)
    private var overlapAllowances = FloatArray(0)
    private var baseline: Baseline? = null
    private var padding = 0f
    private var sourceRevision = -1
    private var count = 0
    private var active = false

    fun deactivate() {
        active = false
    }

    fun active(): Boolean {
        return active
    }

    fun count(): Int {
        return count
    }

    fun x(i: Int): Float {
        return x[i]
    }

    fun y(i: Int): Float {
        return y[i]
    }

    fun start(i: Int): Boolean {
        return start[i]
    }

    fun distance(i: Int): Float {
        return distance[i]
    }

    fun sourceDistance(i: Int): Float {
        return sourceDistance[i]
    }

    fun length(): Float {
        return if (count == 0) 0f else distance[count - 1]
    }

    fun update(ring: RingGeometry, requested: FloatArray?, pad: Float): Boolean {
        return update(ring, requested, null, pad, null)
    }

    fun update(
        ring: RingGeometry, requested: FloatArray?, originals: FloatArray?,
        pad: Float, reference: RingGeometry?
    ): Boolean {
        var snapshot: Baseline? = null
        if (originals != null && reference != null && reference.valid()) {
            snapshot = if (baseline != null && baseline!!.matches(reference)) baseline else Baseline(reference)
        }
        return updatePrepared(ring, requested, originals, pad, snapshot, null)
    }

    fun copyFrom(ring: RingGeometry, source: RingContentCutouts): Boolean {
        return updatePrepared(
            ring, if (source.active) source.boxes else EMPTY,
            if (source.active) source.originalBoxes else EMPTY, source.padding,
            if (source.active) source.baseline else null, source.overlapAllowances
        )
    }

    private fun updatePrepared(
        ring: RingGeometry, requested: FloatArray?, originals: FloatArray?,
        pad: Float, snapshot: Baseline?, preparedAllowances: FloatArray?
    ): Boolean {
        val values = if (requested == null) EMPTY else requested
        val originalValues = if (snapshot == null || originals == null) EMPTY else originals
        val safePadding = if (java.lang.Float.isFinite(pad)) max(0f, pad) else 0f
        val calibrationChanged =
            baseline != snapshot || java.lang.Float.compare(padding, safePadding) != 0 || !originalBoxes.contentEquals(
                originalValues
            )
        if (sourceRevision == ring.baseRevision() && !calibrationChanged && boxes.contentEquals(values)) return false
        sourceRevision = ring.baseRevision()
        padding = safePadding
        boxes = values.clone()
        originalBoxes = originalValues.clone()
        baseline = snapshot
        if (calibrationChanged) {
            if (preparedAllowances != null && preparedAllowances.size == originalBoxes.size / 4) {
                overlapAllowances = preparedAllowances.clone()
            } else {
                overlapAllowances = FloatArray(originalBoxes.size / 4)
                for (i in overlapAllowances.indices) {
                    val at = i * 4
                    overlapAllowances[i] = if (originalBoxes[at + 2] > originalBoxes[at]
                        && originalBoxes[at + 3] > originalBoxes[at + 1]
                    )
                        baseline!!.maximumPenetration(originalBoxes, at, padding)
                    else
                        -1f
                }
            }
        }
        count = 0
        active = values.size >= 4
        if (!active) return true
        val boundary = ring.rawContourBoundary()
        if (boundary > 0 && boundary < ring.baseLength()) {
            contour(ring, 0f, boundary)
            contour(ring, boundary, ring.baseLength())
        } else {
            contour(ring, 0f, ring.baseLength())
        }
        return true
    }

    fun copyTo(target: RingGeometry) {
        target.receiveContentCutouts(this)
    }

    private fun contour(ring: RingGeometry, from: Float, to: Float) {
        val steps = max(1, min(4096, ceil(((to - from) / .75f).toDouble()).toInt()))
        var previous = from
        // Each side of a pre-existing gap has its own endpoint at the same distance.
        val first = if (from > 0) min(to, from + .0001f) else from
        var visible = visible(ring, first)
        if (visible) append(ring.rawPointX(first), ring.rawPointY(first), true, first)
        for (i in 1..steps) {
            var current = from + (to - from) * i / steps
            if (i == steps && to < ring.baseLength()) current -= .0001f
            val next = visible(ring, current)
            if (next != visible) {
                var a = previous
                var b = current
                for (j in 0..13) {
                    val mid = (a + b) / 2
                    if (visible(ring, mid) == visible) a = mid else b = mid
                }
                val edge = if (visible) a else b
                append(ring.rawPointX(edge), ring.rawPointY(edge), !visible, edge)
            }
            if (next) append(ring.rawPointX(current), ring.rawPointY(current), false, current)
            visible = next
            previous = current
        }
    }

    private fun visible(ring: RingGeometry, at: Float): Boolean {
        val px = ring.rawPointX(at)
        val py = ring.rawPointY(at)
        var i = 0
        while (i + 3 < boxes.size) {
            if (boxes[i + 2] <= boxes[i] || boxes[i + 3] <= boxes[i + 1]) {
                i += 4
                continue
            }
            if (px >= boxes[i] - padding && px <= boxes[i + 2] + padding && py >= boxes[i + 1] - padding && py <= boxes[i + 3] + padding) {
                val box = i / 4
                if (baseline != null && box < overlapAllowances.size && overlapAllowances[box] >= 0 && baseline!!.contains(
                        px,
                        py
                    )
                    && penetration(
                        px, py, boxes[i] - padding, boxes[i + 1] - padding,
                        boxes[i + 2] + padding, boxes[i + 3] + padding
                    ) <= overlapAllowances[box] + .0001f
                ) {
                    i += 4
                    continue
                }
                return false
            }
            i += 4
        }
        return true
    }

    private fun append(px: Float, py: Float, begins: Boolean, sourceAt: Float) {
        if (count == x.size) {
            val capacity = max(64, count * 2)
            x = x.copyOf(capacity)
            y = y.copyOf(capacity)
            distance = distance.copyOf(capacity)
            start = start.copyOf(capacity)
            sourceDistance = sourceDistance.copyOf(capacity)
        }
        x[count] = px
        y[count] = py
        start[count] = begins || count == 0
        sourceDistance[count] = sourceAt
        distance[count] = if (count == 0) 0f else distance[count - 1] + (if (begins)
            0f
        else hypot((px - x[count - 1]).toDouble(), (py - y[count - 1]).toDouble()).toFloat())
        count++
    }

    fun coordinate(at: Float, horizontal: Boolean): Float {
        if (count == 0) return 0f
        val value = max(0f, min(length(), at))
        var low = 0
        var high = count - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (distance[mid] < value) low = mid + 1 else high = mid
        }
        val i = low
        val axis = if (horizontal) x else y
        if (i == 0 || start[i] || distance[i] <= distance[i - 1]) return axis[i]
        val mix = (value - distance[i - 1]) / (distance[i] - distance[i - 1])
        return axis[i - 1] + (axis[i] - axis[i - 1]) * mix
    }

    /** Private immutable copy: callers may freely reuse or mutate their reference geometry.  */
    private class Baseline(source: RingGeometry) {
        private val shape = RingGeometry()
        private val description: FloatArray
        private val pointX: FloatArray
        private val pointY: FloatArray
        private val begins: BooleanArray

        init {
            description = describe(source)
            if (source.kind == RingGeometry.Kind.CAPSULE) {
                shape.setCapsuleOpenings(
                    source.width, source.height,
                    source.topLeftX, source.topRightX, source.bottomLeftX, source.bottomRightX,
                    source.strokeWidth, source.capsuleOriginX, source.capsuleOriginY, source.capsuleScale
                )
            } else {
                shape.setCircularOpenings(
                    source.style, source.left, source.top, source.right, source.bottom,
                    source.topLeftAngle, source.topRightAngle, source.bottomLeftAngle, source.bottomRightAngle,
                    source.strokeWidth
                )
            }
            val count = if (source.hasContentCutouts()) source.contourPointCount() else 0
            pointX = FloatArray(count)
            pointY = FloatArray(count)
            begins = BooleanArray(count)
            for (i in 0..<count) {
                pointX[i] = source.contourPointX(i)
                pointY[i] = source.contourPointY(i)
                begins[i] = source.contourStartsAt(i)
            }
        }

        fun matches(source: RingGeometry): Boolean {
            return description.contentEquals(describe(source))
        }

        fun contains(px: Float, py: Float): Boolean {
            if (pointX.size > 0) {
                for (i in 1..<pointX.size) if (!begins[i]) {
                    val dx = pointX[i] - pointX[i - 1]
                    val dy = pointY[i] - pointY[i - 1]
                    val length = dx * dx + dy * dy
                    var p = if (length > 0) ((px - pointX[i - 1]) * dx + (py - pointY[i - 1]) * dy) / length else 0f
                    p = max(0f, min(1f, p))
                    if (hypot(
                            (px - pointX[i - 1] - dx * p).toDouble(),
                            (py - pointY[i - 1] - dy * p).toDouble()
                        ) <= .005f
                    ) return true
                }
                return false
            }
            if (shape.kind == RingGeometry.Kind.CIRCULAR) {
                val nx = (px - shape.centreX) / (shape.width * .5f)
                val ny = (py - shape.centreY) / (shape.height * .5f)
                if (abs(nx * nx + ny * ny - 1) > .0001f) return false
                val angle = Math.toDegrees(atan2(ny.toDouble(), nx.toDouble())).toFloat()
                return inArc(angle, 90 - shape.bottomRightAngle, shape.rightSideSweep)
                        || inArc(angle, 270 - shape.topLeftAngle, shape.leftSideSweep)
            }
            val x = (px - shape.capsuleOriginX) / shape.capsuleScale
            val y = (py - shape.capsuleOriginY) / shape.capsuleScale
            val radius = shape.radius
            if (abs(y) <= .001f && x >= radius - .001f && x <= shape.width - radius + .001f && (x <= shape.topLeftX + .001f || x >= shape.topRightX - .001f)) return true
            if (abs(y - shape.height) <= .001f && x >= radius - .001f && x <= shape.width - radius + .001f && (x <= shape.bottomLeftX + .001f || x >= shape.bottomRightX - .001f)) return true
            val centre = if (x >= shape.width - radius) shape.width - radius else radius
            if (x > radius + .001f && x < shape.width - radius - .001f) return false
            val nx = (x - centre) / radius
            val ny = (y - radius) / radius
            return abs(nx * nx + ny * ny - 1) <= .0001f
        }

        fun maximumPenetration(box: FloatArray, i: Int, pad: Float): Float {
            val l = box[i] - pad
            val t = box[i + 1] - pad
            val r = box[i + 2] + pad
            val b = box[i + 3] + pad
            var result = 0f
            if (pointX.size > 0) {
                for (p in pointX.indices) {
                    result = max(result, penetration(pointX[p], pointY[p], l, t, r, b))
                    if (p > 0 && !begins[p]) result = max(
                        result,
                        lineMaximum(pointX[p - 1], pointY[p - 1], pointX[p], pointY[p], l, t, r, b)
                    )
                }
                return result
            }
            if (shape.kind == RingGeometry.Kind.CIRCULAR) {
                val rx = shape.width * .5f
                val ry = shape.height * .5f
                result = ellipseMaximum(shape.centreX, shape.centreY, rx, ry, l, t, r, b)
                for (angle in floatArrayOf(
                    90 - shape.bottomRightAngle, 270 + shape.topRightAngle,
                    270 - shape.topLeftAngle, 90 + shape.bottomLeftAngle
                )) {
                    result = max(
                        result, ellipseAt(
                            shape.centreX, shape.centreY, rx, ry,
                            Math.toRadians(angle.toDouble()), l, t, r, b
                        )
                    )
                }
                return result
            }
            val s = shape.capsuleScale
            val ox = shape.capsuleOriginX
            val oy = shape.capsuleOriginY
            val radius = shape.radius * s
            val left = ox + radius
            val right = ox + (shape.width - shape.radius) * s
            val bottom = oy + shape.height * s
            val middle = oy + radius
            result = max(
                ellipseMaximum(left, middle, radius, radius, l, t, r, b),
                ellipseMaximum(right, middle, radius, radius, l, t, r, b)
            )
            result = max(result, lineMaximum(ox + shape.topLeftX * s, oy, left, oy, l, t, r, b))
            result = max(result, lineMaximum(ox + shape.topRightX * s, oy, right, oy, l, t, r, b))
            result = max(result, lineMaximum(ox + shape.bottomLeftX * s, bottom, left, bottom, l, t, r, b))
            return max(result, lineMaximum(ox + shape.bottomRightX * s, bottom, right, bottom, l, t, r, b))
        }

        fun ellipseMaximum(
            cx: Float,
            cy: Float,
            rx: Float,
            ry: Float,
            l: Float,
            t: Float,
            r: Float,
            b: Float
        ): Float {
            var result = 0f
            for (i in 0..3) result = max(
                result,
                ellipseAt(cx, cy, rx, ry, i * Math.PI / 2, l, t, r, b)
            )
            // A minimum of four edge distances can peak at a stationary point
            // or where two distances coincide. These six lines enumerate both.
            result = max(result, ellipseLine(cx, cy, rx, ry, 1f, 0f, (l + r) / 2, l, t, r, b))
            result = max(result, ellipseLine(cx, cy, rx, ry, 0f, 1f, (t + b) / 2, l, t, r, b))
            result = max(result, ellipseLine(cx, cy, rx, ry, 1f, -1f, l - t, l, t, r, b))
            result = max(result, ellipseLine(cx, cy, rx, ry, 1f, 1f, l + b, l, t, r, b))
            result = max(result, ellipseLine(cx, cy, rx, ry, 1f, 1f, r + t, l, t, r, b))
            return max(result, ellipseLine(cx, cy, rx, ry, 1f, -1f, r - b, l, t, r, b))
        }

        fun ellipseLine(
            cx: Float, cy: Float, rx: Float, ry: Float, ax: Float, ay: Float,
            value: Float, l: Float, t: Float, r: Float, b: Float
        ): Float {
            val amplitude = hypot((ax * rx).toDouble(), (ay * ry).toDouble())
            val relative = (value - ax * cx - ay * cy) / amplitude
            if (abs(relative) > 1) return 0f
            val phase = atan2((ay * ry).toDouble(), (ax * rx).toDouble())
            val angle = acos(relative)
            return max(
                ellipseAt(cx, cy, rx, ry, phase + angle, l, t, r, b),
                ellipseAt(cx, cy, rx, ry, phase - angle, l, t, r, b)
            )
        }

        fun ellipseAt(
            cx: Float, cy: Float, rx: Float, ry: Float, angle: Double,
            l: Float, t: Float, r: Float, b: Float
        ): Float {
            val x = cx + rx * cos(angle).toFloat()
            val y = cy + ry * sin(angle).toFloat()
            return if (contains(x, y)) max(0f, penetration(x, y, l, t, r, b)) else 0f
        }

        companion object {
            private fun lineMaximum(
                x1: Float, y1: Float, x2: Float, y2: Float,
                l: Float, t: Float, r: Float, b: Float
            ): Float {
                val at = floatArrayOf(x1 - l, r - x1, y1 - t, b - y1)
                val slope = floatArrayOf(x2 - x1, x1 - x2, y2 - y1, y1 - y2)
                var result = max(0f, max(penetration(x1, y1, l, t, r, b), penetration(x2, y2, l, t, r, b)))
                for (i in 0..3) for (j in i + 1..3) {
                    val denominator = slope[i] - slope[j]
                    if (abs(denominator) < .000001f) continue
                    val p = (at[j] - at[i]) / denominator
                    if (p > 0 && p < 1) result = max(
                        result,
                        penetration(x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, l, t, r, b)
                    )
                }
                return result
            }

            private fun inArc(angle: Float, start: Float, sweep: Float): Boolean {
                if (sweep <= 0) return false
                val distance = (start - angle + 720) % 360
                return distance <= sweep + .0001f || 360 - distance <= .0001f
            }

            private fun describe(source: RingGeometry): FloatArray {
                val count = if (source.hasContentCutouts()) source.contourPointCount() else 0
                val result = FloatArray(22 + count * 3)
                val values = floatArrayOf(
                    source.kind.ordinal.toFloat(), source.left, source.top, source.right, source.bottom,
                    source.topLeftAngle, source.topRightAngle, source.bottomLeftAngle, source.bottomRightAngle,
                    source.width, source.height, source.capsuleOriginX, source.capsuleOriginY, source.capsuleScale,
                    source.topLeftX, source.topRightX, source.bottomLeftX, source.bottomRightX,
                    source.strokeWidth, source.rightSideSweep, source.leftSideSweep, count.toFloat()
                )
                System.arraycopy(values, 0, result, 0, values.size)
                for (i in 0..<count) {
                    result[22 + i * 3] = source.contourPointX(i)
                    result[23 + i * 3] = source.contourPointY(i)
                    result[24 + i * 3] = (if (source.contourStartsAt(i)) 1 else 0).toFloat()
                }
                return result
            }
        }
    }

    companion object {
        private val EMPTY = FloatArray(0)
        private fun penetration(
            px: Float,
            py: Float,
            l: Float,
            t: Float,
            r: Float,
            b: Float
        ): Float {
            return min(min(px - l, r - px), min(py - t, b - py))
        }
    }
}
