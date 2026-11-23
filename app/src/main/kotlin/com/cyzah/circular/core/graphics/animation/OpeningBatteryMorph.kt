package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.geometry.RingGeometry
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Pure gesture-driven samples for the ring-to-battery opening contour.  */
class OpeningBatteryMorph {
    private val regularPoints = FloatArray(SIDE_POINTS * 4)
    private val targetPoints = FloatArray(SIDE_POINTS * 4)
    private val regularStarts = BooleanArray(SIDE_POINTS * 2)
    private var positionedPoints = FloatArray(0)
    private var positionedStarts = BooleanArray(0)
    @JvmField var points: FloatArray = regularPoints
    @JvmField var contourStarts: BooleanArray = regularStarts
    @JvmField val gapAnchor: FloatArray = FloatArray(2)

    private val openingRing = RingGeometry()
    @JvmField var strokeWidth: Float = 0f
    @JvmField var left: Float = 0f
    @JvmField var top: Float = 0f
    @JvmField var right: Float = 0f
    @JvmField var bottom: Float = 0f
    @JvmField var solidAlpha: Float = 0f
    @JvmField var outlineAlpha: Float = 0f
    @JvmField var contentAlpha: Float = 0f
    @JvmField var bodyAlpha: Float = 0f
    @JvmField var interiorInset: Float = 0f
    @JvmField var interiorWidth: Float = 0f
    @JvmField var interiorHeight: Float = 0f
    @JvmField var interiorRadiusX: Float = 0f
    @JvmField var interiorRadiusY: Float = 0f
    @JvmField var targetClearanceProgress: Float = 0f
    @JvmField var capsule: Boolean = false
    @JvmField var contourAtTarget: Boolean = false
    @JvmField var bodyLeft: Float = 0f
    @JvmField var bodyTop: Float = 0f
    @JvmField var bodyWidth: Float = 0f
    @JvmField var bodyHeight: Float = 0f
    @JvmField var totalLeft: Float = 0f
    @JvmField var totalTop: Float = 0f
    @JvmField var totalWidth: Float = 0f
    @JvmField var totalHeight: Float = 0f
    @JvmField var gapCentreX: Float = 0f
    @JvmField var valid: Boolean = false

    init {
        regularStarts[0] = true
        regularStarts[SIDE_POINTS] = true
    }

    fun update(
        ring: RingGeometry?, ringX: Float, ringY: Float, ringUnitX: Float, ringUnitY: Float,
        targetLeft: Float, targetTop: Float, targetWidth: Float, targetHeight: Float,
        expansion: Float
    ) {
        reset()
        if (ring == null || !ring.valid() || !java.lang.Float.isFinite(ringX) || !java.lang.Float.isFinite(ringY) || !positive(ringUnitX) || !positive(
                ringUnitY
            ) || !java.lang.Float.isFinite(targetLeft) || !java.lang.Float.isFinite(targetTop) || !positive(targetWidth) || !positive(
                targetHeight
            ) || !java.lang.Float.isFinite(expansion)
        ) {
            return
        }
        val progress = Motion.clamp(expansion)
        this.capsule = ring.kind == RingGeometry.Kind.CAPSULE
        val gapProgress = OpeningTechnologyClearance.gapProgress(progress)
        if (this.capsule) {
            val edge = ring.height / 2.0f
            this.openingRing.setCapsuleOpenings(
                ring.width, ring.height,
                Motion.mix(ring.topLeftX, edge, gapProgress),
                Motion.mix(ring.topRightX, ring.width - edge, gapProgress),
                Motion.mix(ring.bottomLeftX, edge, gapProgress),
                Motion.mix(ring.bottomRightX, ring.width - edge, gapProgress),
                ring.strokeWidth, ring.capsuleOriginX, ring.capsuleOriginY, ring.capsuleScale
            )
        } else {
            // Open the existing gaps before the labels leave their interior.
            // Preserve two complete side arcs instead of cutting holes at glyphs.
            this.openingRing.setCircularOpenings(
                ring.style, ring.left, ring.top, ring.right, ring.bottom,
                Motion.mix(ring.topLeftAngle, max(75.0f, ring.topLeftAngle), gapProgress),
                Motion.mix(ring.topRightAngle, max(75.0f, ring.topRightAngle), gapProgress),
                Motion.mix(ring.bottomLeftAngle, max(75.0f, ring.bottomLeftAngle), gapProgress),
                Motion.mix(ring.bottomRightAngle, max(75.0f, ring.bottomRightAngle), gapProgress),
                ring.strokeWidth
            )
        }
        val morph = OpeningTechnologyClearance.shapeProgress(progress)
        if (ring.hasContentCutouts()) {
            this.openingRing.copyContentCutoutsFrom(ring)
            if (!this.openingRing.valid()) this.openingRing.copyForTransferFrom(ring)
        }
        this.contourAtTarget = morph >= 1.0f
        this.totalLeft = targetLeft
        this.totalTop = targetTop
        this.totalWidth = targetWidth
        this.totalHeight = targetHeight
        this.bodyLeft = targetLeft
        this.bodyTop = targetTop
        this.bodyWidth = (targetWidth * BatteryTransferConfig.BODY_WIDTH
                / BatteryTransferConfig.SVG_WIDTH)
        this.bodyHeight = targetHeight
        this.strokeWidth = ring.strokeWidth * min(ringUnitX, ringUnitY)
        this.solidAlpha = Motion.ease((progress - 0.65f) / 0.2f)
        // Transfer the outer band first, then fill its hollow interior inward.
        // Never use a thinner stroke as an intermediate representation.
        this.bodyAlpha = Motion.ease(this.solidAlpha / 0.25f)
        this.outlineAlpha = 1.0f - this.bodyAlpha
        this.contentAlpha = this.solidAlpha
        this.targetClearanceProgress = Motion.ease((progress - 0.4f) / 0.25f)
        val maximumInset = min(this.bodyWidth, this.bodyHeight) / 2.0f
        this.interiorInset = Motion.mix(
            min(this.strokeWidth, maximumInset),
            maximumInset, this.solidAlpha
        )
        this.interiorWidth = max(0.0f, this.bodyWidth - 2.0f * this.interiorInset)
        this.interiorHeight = max(0.0f, this.bodyHeight - 2.0f * this.interiorInset)
        // Scale curvature with the remaining centred hole. Subtracting inset
        // from the outer radius would flatten its corners before it disappears.
        val innerScale = min(
            this.interiorWidth / this.bodyWidth,
            this.interiorHeight / this.bodyHeight
        )
        this.interiorRadiusX = min(
            this.bodyWidth / 2.0f,
            targetWidth * BatteryTransferConfig.BODY_RADIUS / BatteryTransferConfig.SVG_WIDTH
        ) * innerScale
        this.interiorRadiusY = min(
            this.bodyHeight / 2.0f,
            targetHeight * BatteryTransferConfig.BODY_RADIUS / BatteryTransferConfig.SVG_HEIGHT
        ) * innerScale

        var topRightX: Float = topX(this.openingRing, false)
        var topLeftX: Float = topX(this.openingRing, true)
        var topRightY: Float = topY(this.openingRing, false)
        var topLeftY: Float = topY(this.openingRing, true)
        if (this.openingRing.hasContentCutouts()) {
            var lastRight = -1
            var firstLeft = -1
            for (i in 0..<this.openingRing.contourPointCount()) {
                if (this.openingRing.contourPointSourceDistance(i) <= this.openingRing.firstContourLength()) {
                    lastRight = i
                } else if (firstLeft < 0) {
                    firstLeft = i
                }
            }
            if (lastRight >= 0 || firstLeft >= 0) {
                val rightAnchor = if (lastRight >= 0) lastRight else firstLeft
                val leftAnchor = if (firstLeft >= 0) firstLeft else lastRight
                topRightX = this.openingRing.contourPointX(rightAnchor)
                topRightY = this.openingRing.contourPointY(rightAnchor)
                topLeftX = this.openingRing.contourPointX(leftAnchor)
                topLeftY = this.openingRing.contourPointY(leftAnchor)
            }
        }
        val centreX = Motion.mix(
            ringX + (topRightX + topLeftX) * .5f * ringUnitX,
            this.bodyLeft + this.bodyWidth / 2.0f, morph
        )
        this.gapCentreX = centreX
        gapAnchor[0] = centreX
        gapAnchor[1] = Motion.mix(
            ringY + (topRightY + topLeftY) * .5f * ringUnitY,
            this.bodyTop + this.strokeWidth / 2.0f, morph
        )
        // Degenerate target sizes can contain less than one full stroke. Keep
        // the configured stroke and clip it at the settled body boundary.
        val inset = min(this.strokeWidth / 2.0f, maximumInset)
        val bodyRight = this.bodyLeft + this.bodyWidth - inset
        val bodyTop = this.bodyTop + inset
        val bodyBottom = this.bodyTop + this.bodyHeight - inset
        val targetCentreX = this.bodyLeft + this.bodyWidth / 2.0f
        val radiusX = max(
            0.0f, min(
                this.bodyWidth / 2.0f - inset,
                targetWidth * BatteryTransferConfig.BODY_RADIUS
                        / BatteryTransferConfig.SVG_WIDTH - inset
            )
        )
        val radiusY = max(
            0.0f, min(
                this.bodyHeight / 2.0f - inset,
                targetHeight * BatteryTransferConfig.BODY_RADIUS
                        / BatteryTransferConfig.SVG_HEIGHT - inset
            )
        )
        val straight = bodyRight - radiusX - targetCentreX
        val vertical = max(0.0f, bodyBottom - bodyTop - radiusY * 2.0f)
        // The target body uses almost circular SVG corners; this symmetric
        // ellipse-length estimate also behaves well under anisotropic projection.
        val arc = Math.PI.toFloat() * (radiusX + radiusY) / 4.0f
        val halfLength = straight * 2.0f + vertical + arc * 2.0f
        for (index in 0..<SIDE_POINTS) {
            val fraction = index / (SIDE_POINTS - 1).toFloat()
            var distance = fraction * halfLength
            val x: Float
            val y: Float
            if (distance <= straight) {
                x = targetCentreX + distance
                y = bodyBottom
            } else if ((straight.let { distance -= it; distance }) <= arc) {
                val angle: Float = Math.PI.toFloat() / 2.0f * (1.0f - ratio(distance, arc))
                x = bodyRight - radiusX + radiusX * cos(angle.toDouble()).toFloat()
                y = bodyBottom - radiusY + radiusY * sin(angle.toDouble()).toFloat()
            } else if ((arc.let { distance -= it; distance }) <= vertical) {
                x = bodyRight
                y = bodyBottom - radiusY - distance
            } else if ((vertical.let { distance -= it; distance }) <= arc) {
                val angle: Float = -Math.PI.toFloat() / 2.0f * ratio(distance, arc)
                x = bodyRight - radiusX + radiusX * cos(angle.toDouble()).toFloat()
                y = bodyTop + radiusY + radiusY * sin(angle.toDouble()).toFloat()
            } else {
                distance -= arc
                x = bodyRight - radiusX - distance
                y = bodyTop
            }
            val ringDistance = this.openingRing.firstContourLength() * fraction
            targetPoints[index * 2] = x
            targetPoints[index * 2 + 1] = y
            this.points[index * 2] = Motion.mix(
                ringX + this.openingRing.pointX(ringDistance) * ringUnitX,
                x, morph
            )
            this.points[index * 2 + 1] = Motion.mix(
                ringY + this.openingRing.pointY(ringDistance) * ringUnitY,
                y, morph
            )
        }
        for (index in 0..<SIDE_POINTS) {
            val source: Int = (SIDE_POINTS - index - 1) * 2
            val target: Int = (SIDE_POINTS + index) * 2
            targetPoints[target] = this.bodyLeft * 2 + this.bodyWidth - targetPoints[source]
            targetPoints[target + 1] = targetPoints[source + 1]
            val fraction = index / (SIDE_POINTS - 1).toFloat()
            val distance = (this.openingRing.firstContourLength()
                    + (this.openingRing.baseContourLength() - this.openingRing.firstContourLength()) * fraction)
            val sx = if (index == 0) topLeftX else this.openingRing.pointX(distance)
            val sy = if (index == 0) topLeftY else this.openingRing.pointY(distance)
            this.points[target] = Motion.mix(ringX + sx * ringUnitX, targetPoints[target], morph)
            this.points[target + 1] = Motion.mix(ringY + sy * ringUnitY, targetPoints[target + 1], morph)
        }
        if (this.contourAtTarget) {
            System.arraycopy(targetPoints, 0, regularPoints, 0, targetPoints.size)
            regularStarts[SIDE_POINTS] = false
        } else if (ring.hasContentCutouts()) {
            positionActualContours(this.openingRing, ringX, ringY, ringUnitX, ringUnitY, morph)
        }
        updateBounds()
        this.valid = java.lang.Float.isFinite(this.left) && java.lang.Float.isFinite(this.top)
                && java.lang.Float.isFinite(this.right) && java.lang.Float.isFinite(this.bottom)
                && this.right > this.left && this.bottom > this.top
    }

    private fun positionActualContours(
        ring: RingGeometry,
        x: Float,
        y: Float,
        ux: Float,
        uy: Float,
        morph: Float
    ) {
        val count = ring.contourPointCount()
        var visibleRight = ring.length
        for (i in 0..<count) if (ring.contourPointSourceDistance(i) > ring.firstContourLength()) {
            visibleRight = ring.contourPointDistance(i)
            break
        }
        // A fully hidden side has no contour to grow. Redistribute the remaining
        // real visible perimeter over the whole target rather than losing half of it.
        val whole = visibleRight <= 0 || ring.length - visibleRight <= 0
        var outputCount = count
        var sample = 1
        if (morph > 0) for (i in 1..<count) {
            if (ring.contourStartsAt(i)) continue
            val previous = ring.contourPointDistance(i - 1)
            val current = ring.contourPointDistance(i)
            while (sample < 2 * (SIDE_POINTS - 1)
                && sampleDistance(sample, visibleRight, ring.length, whole) <= previous
            ) sample++
            while (sample < 2 * (SIDE_POINTS - 1)
                && sampleDistance(sample, visibleRight, ring.length, whole) < current
            ) {
                outputCount++
                sample++
            }
        }
        if (positionedPoints.size != outputCount * 2) {
            positionedPoints = FloatArray(outputCount * 2)
            positionedStarts = BooleanArray(outputCount)
        }
        sample = 1
        var output = 0
        for (i in 0..<count) {
            val current = ring.contourPointDistance(i)
            if (morph > 0 && i > 0 && !ring.contourStartsAt(i)) {
                val previous = ring.contourPointDistance(i - 1)
                while (sample < 2 * (SIDE_POINTS - 1)
                    && sampleDistance(sample, visibleRight, ring.length, whole) <= previous
                ) sample++
                while (sample < 2 * (SIDE_POINTS - 1)
                    && sampleDistance(sample, visibleRight, ring.length, whole) < current
                ) {
                    val at: Float = sampleDistance(sample, visibleRight, ring.length, whole)
                    val fraction = (at - previous) / (current - previous)
                    val sx = Motion.mix(ring.contourPointX(i - 1), ring.contourPointX(i), fraction)
                    val sy = Motion.mix(ring.contourPointY(i - 1), ring.contourPointY(i), fraction)
                    positionPoint(output++, x + sx * ux, y + sy * uy, sample.toFloat(), morph, false)
                    sample++
                }
            }
            val targetAt: Float = if (whole)
                current / ring.length * 2 * (SIDE_POINTS - 1)
            else
                if (current <= visibleRight)
                    current / visibleRight * (SIDE_POINTS - 1)
                else
                    (1 + (current - visibleRight) / (ring.length - visibleRight)) * (SIDE_POINTS - 1)
            positionPoint(
                output++, x + ring.contourPointX(i) * ux,
                y + ring.contourPointY(i) * uy, targetAt, morph, ring.contourStartsAt(i)
            )
        }
        points = positionedPoints
        contourStarts = positionedStarts
    }

    private fun positionPoint(
        index: Int, sourceX: Float, sourceY: Float,
        targetAt: Float, morph: Float, start: Boolean
    ) {
        val at = max(0f, min((2 * (SIDE_POINTS - 1)).toFloat(), targetAt))
        val lower = at.toInt()
        val upper = min(2 * (SIDE_POINTS - 1), lower + 1)
        // The target template repeats its top point at the side boundary.
        val a = (if (lower < SIDE_POINTS) lower else lower + 1) * 2
        val b = (if (upper < SIDE_POINTS) upper else upper + 1) * 2
        val tx = Motion.mix(targetPoints[a], targetPoints[b], at - lower)
        val ty = Motion.mix(targetPoints[a + 1], targetPoints[b + 1], at - lower)
        positionedPoints[index * 2] = Motion.mix(sourceX, tx, morph)
        positionedPoints[index * 2 + 1] = Motion.mix(sourceY, ty, morph)
        positionedStarts[index] = start
    }

    private fun updateBounds() {
        this.right = this.points[0]
        this.left = this.right
        this.bottom = this.points[1]
        this.top = this.bottom
        var index = 2
        while (index < this.points.size) {
            this.left = min(this.left, this.points[index])
            this.right = max(this.right, this.points[index])
            this.top = min(this.top, this.points[index + 1])
            this.bottom = max(this.bottom, this.points[index + 1])
            index += 2
        }
        val radius = this.strokeWidth / 2.0f
        this.left -= radius
        this.right += radius
        this.top -= radius
        this.bottom += radius
        if (this.solidAlpha > 0.0f) {
            this.left = min(this.left, this.totalLeft)
            this.top = min(this.top, this.totalTop)
            this.right = max(this.right, this.totalLeft + this.totalWidth)
            this.bottom = max(this.bottom, this.totalTop + this.totalHeight)
        }
    }

    private fun reset() {
        this.points = regularPoints
        this.contourStarts = regularStarts
        this.regularStarts[SIDE_POINTS] = true
        this.valid = false
        this.contentAlpha = 0.0f
        this.outlineAlpha = this.contentAlpha
        this.solidAlpha = this.outlineAlpha
        this.targetClearanceProgress = 0.0f
        this.capsule = false
        this.contourAtTarget = false
        this.interiorHeight = 0.0f
        this.interiorWidth = this.interiorHeight
        this.interiorInset = this.interiorWidth
        this.bodyAlpha = this.interiorInset
        this.interiorRadiusY = 0.0f
        this.interiorRadiusX = this.interiorRadiusY
        this.bottom = 0.0f
        this.right = this.bottom
        this.top = this.right
        this.left = this.top
        this.strokeWidth = 0.0f
        this.bodyHeight = 0.0f
        this.bodyWidth = this.bodyHeight
        this.bodyTop = this.bodyWidth
        this.bodyLeft = this.bodyTop
        this.totalHeight = 0.0f
        this.totalWidth = this.totalHeight
        this.totalTop = this.totalWidth
        this.totalLeft = this.totalTop
        this.gapCentreX = 0.0f
    }

    companion object {
        const val SIDE_POINTS: Int = 65
        private fun sampleDistance(
            sample: Int,
            right: Float,
            total: Float,
            whole: Boolean
        ): Float {
            val sideIntervals: Int = SIDE_POINTS - 1
            if (whole) return total * sample / (2 * sideIntervals)
            return if (sample <= sideIntervals)
                right * sample / sideIntervals
            else
                right + (total - right) * (sample - sideIntervals) / sideIntervals
        }

        private fun topX(ring: RingGeometry, left: Boolean): Float {
            if (ring.kind == RingGeometry.Kind.CAPSULE) return (ring.capsuleOriginX
                    + (if (left) ring.topLeftX else ring.topRightX) * ring.capsuleScale)
            val angle = Math.toRadians((if (left) 270 - ring.topLeftAngle else 270 + ring.topRightAngle).toDouble())
            return ring.centreX + ring.width / 2 * cos(angle).toFloat()
        }

        private fun topY(ring: RingGeometry, left: Boolean): Float {
            if (ring.kind == RingGeometry.Kind.CAPSULE) return ring.capsuleOriginY
            val angle = Math.toRadians((if (left) 270 - ring.topLeftAngle else 270 + ring.topRightAngle).toDouble())
            return ring.centreY + ring.height / 2 * sin(angle).toFloat()
        }

        private fun positive(value: Float): Boolean {
            return java.lang.Float.isFinite(value) && value > 0.0f
        }

        private fun ratio(value: Float, total: Float): Float {
            return if (total > 0.0f) Motion.clamp(value / total) else 0.0f
        }
    }
}
