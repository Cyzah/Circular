package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Exact open-ring geometry in the 120-unit icon coordinate system.
 * 
 * 
 * The path direction is lower-right, up the right side, across the top gap,
 * down the left side, and then to lower-left.
 */
class RingGeometry {
    enum class Kind {
        CIRCULAR,
        CAPSULE,
        INVALID
    }

    @JvmField var kind: Kind = Kind.INVALID
    @JvmField var style: DualStyle? = DualStyle.LAYERED
    @JvmField var revision: Int = 0
    @JvmField var length: Float = 0f
    @JvmField var startX: Float = 0f
    @JvmField var startY: Float = 0f
    @JvmField var endX: Float = 0f
    @JvmField var endY: Float = 0f
    @JvmField var strokeWidth: Float = 0f

    @JvmField var left: Float = 0f
    @JvmField var top: Float = 0f
    @JvmField var right: Float = 0f
    @JvmField var bottom: Float = 0f
    @JvmField var centreX: Float = 0f
    @JvmField var centreY: Float = 0f
    @JvmField var radius: Float = 0f
    @JvmField var width: Float = 0f
    @JvmField var height: Float = 0f
    @JvmField var topHalfAngle: Float = 0f
    @JvmField var bottomHalfAngle: Float = 0f
    @JvmField var topLeftAngle: Float = 0f
    @JvmField var topRightAngle: Float = 0f
    @JvmField var bottomLeftAngle: Float = 0f
    @JvmField var bottomRightAngle: Float = 0f
    @JvmField var halfTop: Float = 0f
    @JvmField var halfBottom: Float = 0f
    @JvmField var topLeftX: Float = 0f
    @JvmField var topRightX: Float = 0f
    @JvmField var bottomLeftX: Float = 0f
    @JvmField var bottomRightX: Float = 0f
    @JvmField var capsuleOriginX: Float = 0f
    @JvmField var capsuleOriginY: Float = 0f
    @JvmField var capsuleScale: Float = 1.0f
    @JvmField var sideSweep: Float = 0f
    @JvmField var rightSideSweep: Float = 0f
    @JvmField var leftSideSweep: Float = 0f
    @JvmField var arcLength: Float = 0f
    private val cutouts = RingContentCutouts()
    private var sourceLength = 0f
    private var sourceRevision = 0

    fun baseRevision(): Int {
        return sourceRevision
    }

    fun baseLength(): Float {
        return sourceLength
    }

    fun hasContentCutouts(): Boolean {
        return cutouts.active()
    }

    fun contourPointCount(): Int {
        return cutouts.count()
    }

    fun contourPointX(i: Int): Float {
        return cutouts.x(i)
    }

    fun contourPointY(i: Int): Float {
        return cutouts.y(i)
    }

    fun contourStartsAt(i: Int): Boolean {
        return cutouts.start(i)
    }

    fun contourPointDistance(i: Int): Float {
        return cutouts.distance(i)
    }

    fun contourPointSourceDistance(i: Int): Float {
        return cutouts.sourceDistance(i)
    }

    fun baseContourLength(): Float {
        return sourceLength
    }

    fun firstContourLength(): Float {
        return if (kind == Kind.CIRCULAR) arcLength else if (kind == Kind.CAPSULE)
            (capsuleBottomRightLength() + arcLength + capsuleTopRightLength()) * capsuleScale
        else
            0f
    }

    fun copyContentCutoutsFrom(source: RingGeometry) {
        source.cutouts.copyTo(this)
    }

    fun setContentCutouts(boxes: FloatArray?, padding: Float) {
        if (!cutouts.update(this, boxes, padding)) return
        updateCutoutEndpoints()
    }

    fun setContentCutouts(
        movedBoxes: FloatArray?, originalBoxes: FloatArray?,
        padding: Float, referenceGeometry: RingGeometry?
    ) {
        if (!cutouts.update(this, movedBoxes, originalBoxes, padding, referenceGeometry)) return
        updateCutoutEndpoints()
    }

    fun receiveContentCutouts(source: RingContentCutouts) {
        if (!cutouts.copyFrom(this, source)) return
        updateCutoutEndpoints()
    }

    private fun updateCutoutEndpoints() {
        this.length = if (cutouts.active()) cutouts.length() else sourceLength
        if (cutouts.active() && cutouts.count() > 0) {
            startX = cutouts.x(0)
            startY = cutouts.y(0)
            endX = cutouts.x(cutouts.count() - 1)
            endY = cutouts.y(cutouts.count() - 1)
        } else if (!cutouts.active()) {
            startX = rawPointX(0f)
            startY = rawPointY(0f)
            endX = rawPointX(sourceLength)
            endY = rawPointY(sourceLength)
        }
        revision++
    }

    private fun changedBase() {
        sourceLength = length
        sourceRevision++
        cutouts.deactivate()
    }

    fun rawContourBoundary(): Float {
        if (continuousContour()) return 0f
        return firstContourLength()
    }

    fun setCircular(
        style: DualStyle?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        topHalfAngle: Float,
        bottomHalfAngle: Float,
        strokeWidth: Float
    ) {
        val upper: Float = angle(topHalfAngle)
        val lower: Float = angle(bottomHalfAngle)
        setCircularOpenings(style, left, top, right, bottom, upper, upper, lower, lower, strokeWidth)
    }

    /** Four signed angular extents, measured towards each named side from its axis.  */
    fun setCircularOpenings(
        style: DualStyle?, left: Float, top: Float, right: Float, bottom: Float,
        topLeftAngle: Float, topRightAngle: Float,
        bottomLeftAngle: Float, bottomRightAngle: Float, strokeWidth: Float
    ) {
        var tl: Float = signedAngle(topLeftAngle)
        var tr: Float = signedAngle(topRightAngle)
        var bl: Float = signedAngle(bottomLeftAngle)
        var br: Float = signedAngle(bottomRightAngle)
        if (tl + tr < 0) {
            tr = (tr - tl) / 2
            tl = -tr
        }
        if (bl + br < 0) {
            br = (br - bl) / 2
            bl = -br
        }
        if (sameCircular(style, left, top, right, bottom, tl, tr, bl, br, strokeWidth)) return
        val valid = java.lang.Float.isFinite(left)
                && java.lang.Float.isFinite(top)
                && java.lang.Float.isFinite(right)
                && java.lang.Float.isFinite(bottom)
                && right > left && bottom > top
        if (!valid) {
            invalidate(style)
            return
        }
        this.kind = Kind.CIRCULAR
        this.style = style
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
        this.centreX = (left + right) / 2.0f
        this.centreY = (top + bottom) / 2.0f
        this.radius = min(right - left, bottom - top) / 2.0f
        this.width = right - left
        this.height = bottom - top
        this.topLeftAngle = tl
        this.topRightAngle = tr
        this.bottomLeftAngle = bl
        this.bottomRightAngle = br
        this.topHalfAngle = (tl + tr) / 2
        this.bottomHalfAngle = (bl + br) / 2
        this.strokeWidth = positive(strokeWidth)
        this.rightSideSweep = clamp(180.0f - tr - br, 0f, 360f)
        this.leftSideSweep = clamp(180.0f - tl - bl, 0f, 360f)
        this.sideSweep = (rightSideSweep + leftSideSweep) / 2
        this.arcLength = radius * degreesToRadians(rightSideSweep)
        this.length = arcLength + radius * degreesToRadians(leftSideSweep)
        if (this.length <= 0.0f) {
            invalidate(style)
            return
        }
        changedBase()
        this.startX = rawPointX(0f)
        this.startY = rawPointY(0f)
        this.endX = rawPointX(sourceLength)
        this.endY = rawPointY(sourceLength)
        this.revision++
    }

    fun setCapsule(
        width: Float,
        height: Float,
        halfTop: Float,
        halfBottom: Float,
        strokeWidth: Float
    ) {
        setCapsule(
            width,
            height,
            halfTop,
            halfBottom,
            strokeWidth,
            0.0f,
            0.0f,
            1.0f
        )
    }

    fun setCapsule(
        width: Float,
        height: Float,
        halfTop: Float,
        halfBottom: Float,
        strokeWidth: Float,
        originX: Float,
        originY: Float,
        scale: Float
    ) {
        val maximumHalf = max(0.0f, (width - height) / 2.0f)
        val upper: Float = clamp(halfTop, 0f, maximumHalf)
        val lower: Float = clamp(halfBottom, 0f, maximumHalf)
        val centre = width / 2.0f
        setCapsuleOpenings(
            width, height, centre - upper, centre + upper,
            centre - lower, centre + lower, strokeWidth, originX, originY, scale
        )
    }

    /** Independent local X coordinates of the straight top and bottom gap edges.  */
    fun setCapsuleOpenings(
        width: Float, height: Float,
        topLeftX: Float, topRightX: Float, bottomLeftX: Float, bottomRightX: Float,
        strokeWidth: Float, originX: Float, originY: Float, scale: Float
    ) {
        if (!java.lang.Float.isFinite(width) || !java.lang.Float.isFinite(height) || width <= 0.0f || height <= 0.0f) {
            invalidate(DualStyle.CAPSULE)
            return
        }
        val minimumX = min(height / 2, width / 2)
        val maximumX = max(width - height / 2, width / 2)
        var tl: Float = clamp(topLeftX, minimumX, maximumX)
        var tr: Float = clamp(topRightX, minimumX, maximumX)
        var bl: Float = clamp(bottomLeftX, minimumX, maximumX)
        var br: Float = clamp(bottomRightX, minimumX, maximumX)
        if (tl > tr) {
            tr = (tl + tr) / 2
            tl = tr
        }
        if (bl > br) {
            br = (bl + br) / 2
            bl = br
        }
        if (sameCapsule(width, height, tl, tr, bl, br, strokeWidth, originX, originY, scale)) return
        this.kind = Kind.CAPSULE
        this.style = DualStyle.CAPSULE
        this.left = 0.0f
        this.top = 0.0f
        this.right = width
        this.bottom = height
        this.width = width
        this.height = height
        this.capsuleOriginX = if (java.lang.Float.isFinite(originX)) originX else 0.0f
        this.capsuleOriginY = if (java.lang.Float.isFinite(originY)) originY else 0.0f
        this.capsuleScale = if (java.lang.Float.isFinite(scale) && scale > 0.0f)
            scale
        else
            1.0f
        this.radius = height / 2.0f
        this.centreX = width / 2.0f
        this.centreY = height / 2.0f
        this.topLeftX = tl
        this.topRightX = tr
        this.bottomLeftX = bl
        this.bottomRightX = br
        this.halfTop = (tr - tl) / 2
        this.halfBottom = (br - bl) / 2
        this.topHalfAngle = 0.0f
        this.bottomHalfAngle = 0.0f
        this.bottomRightAngle = 0f
        this.bottomLeftAngle = this.bottomRightAngle
        this.topRightAngle = this.bottomLeftAngle
        this.topLeftAngle = this.topRightAngle
        this.sideSweep = 180.0f
        this.leftSideSweep = 180.0f
        this.rightSideSweep = this.leftSideSweep
        this.arcLength = radius * Math.PI.toFloat()
        this.strokeWidth = positive(strokeWidth)
        this.startX = capsuleX(this.bottomRightX)
        this.startY = capsuleY(height)
        this.endX = capsuleX(this.bottomLeftX)
        this.endY = capsuleY(height)
        val bottomRight = capsuleBottomRightLength()
        val rightTop = capsuleTopRightLength()
        val topLeft = capsuleTopLeftLength()
        val bottomLeft = capsuleBottomLeftLength()
        this.length = ((bottomRight + bottomLeft
                + rightTop + topLeft
                + (arcLength * 2.0f))) * this.capsuleScale
        if (this.length <= 0.0f) {
            invalidate(DualStyle.CAPSULE)
            return
        }
        changedBase()
        this.revision++
    }

    fun pointX(distance: Float): Float {
        return if (cutouts.active()) cutouts.coordinate(distance, true) else rawPointX(distance)
    }

    fun rawPointX(distance: Float): Float {
        val safe: Float = clamp(distance, 0.0f, sourceLength)
        if (kind == Kind.CIRCULAR) {
            val angle = circularPointRadians(safe)
            return centreX + (width * .5f * cos(angle.toDouble()).toFloat())
        }
        if (kind == Kind.CAPSULE) {
            return capsuleX(
                capsulePointXUnscaled(
                    safe / this.capsuleScale
                )
            )
        }
        return 0.0f
    }

    fun pointY(distance: Float): Float {
        return if (cutouts.active()) cutouts.coordinate(distance, false) else rawPointY(distance)
    }

    fun rawPointY(distance: Float): Float {
        val safe: Float = clamp(distance, 0.0f, sourceLength)
        if (kind == Kind.CIRCULAR) {
            val angle = circularPointRadians(safe)
            return centreY + (height * .5f * sin(angle.toDouble()).toFloat())
        }
        if (kind == Kind.CAPSULE) {
            return capsuleY(
                capsulePointYUnscaled(
                    safe / this.capsuleScale
                )
            )
        }
        return 0.0f
    }

    fun tangentX(distance: Float): Float {
        val safe: Float = clamp(distance, 0.0f, length)
        if (kind == Kind.CIRCULAR) {
            val angle = circularPointRadians(safe)
            return sin(angle.toDouble()).toFloat()
        }
        if (kind == Kind.CAPSULE) {
            return capsuleTangentX(safe / this.capsuleScale)
        }
        return 0.0f
    }

    fun tangentY(distance: Float): Float {
        val safe: Float = clamp(distance, 0.0f, length)
        if (kind == Kind.CIRCULAR) {
            val angle = circularPointRadians(safe)
            return -cos(angle.toDouble()).toFloat()
        }
        if (kind == Kind.CAPSULE) {
            return capsuleTangentY(safe / this.capsuleScale)
        }
        return 0.0f
    }

    fun valid(): Boolean {
        return kind != Kind.INVALID && length > 0.0f && java.lang.Float.isFinite(length)
                && java.lang.Float.isFinite(startX)
                && java.lang.Float.isFinite(startY)
                && java.lang.Float.isFinite(endX)
                && java.lang.Float.isFinite(endY)
    }

    fun continuousContour(): Boolean {
        return (kind == Kind.CIRCULAR && topLeftAngle + topRightAngle <= 0.0001f)
                || (kind == Kind.CAPSULE && topRightX - topLeftX <= 0.0001f)
    }

    fun copyClosedTopFrom(source: RingGeometry?) {
        if (source == null || !source.valid()) {
            invalidate(if (source == null) style else source.style)
            return
        }
        if (source.kind == Kind.CAPSULE) {
            setCapsuleOpenings(
                source.width,
                source.height,
                source.centreX,
                source.centreX,
                source.bottomLeftX,
                source.bottomRightX,
                source.strokeWidth,
                source.capsuleOriginX,
                source.capsuleOriginY,
                source.capsuleScale
            )
            return
        }
        setCircularOpenings(
            source.style,
            source.left,
            source.top,
            source.right,
            source.bottom,
            0.0f,
            0.0f,
            source.bottomLeftAngle,
            source.bottomRightAngle,
            source.strokeWidth
        )
    }

    fun copyFrom(source: RingGeometry?) {
        if (source == null || !source.valid()) {
            invalidate(if (source == null) style else source.style)
            return
        }
        if (source.kind == Kind.CAPSULE) {
            setCapsuleOpenings(
                source.width,
                source.height,
                source.topLeftX,
                source.topRightX,
                source.bottomLeftX,
                source.bottomRightX,
                source.strokeWidth,
                source.capsuleOriginX,
                source.capsuleOriginY,
                source.capsuleScale
            )
            source.cutouts.copyTo(this)
            return
        }
        setCircularOpenings(
            source.style,
            source.left,
            source.top,
            source.right,
            source.bottom,
            source.topLeftAngle,
            source.topRightAngle,
            source.bottomLeftAngle,
            source.bottomRightAngle,
            source.strokeWidth
        )
        source.cutouts.copyTo(this)
    }

    fun copyForTransferFrom(source: RingGeometry?) {
        // Transfer keeps the readout visible, so its measured gaps must survive too.
        copyFrom(source)
    }

    private fun circularPointRadians(distance: Float): Float {
        if (arcLength > 0 && distance <= arcLength) {
            val angle: Float = 90.0f - bottomRightAngle - degrees(distance / radius)
            return degreesToRadians(angle)
        }
        val remaining = distance - arcLength
        val angle: Float = 270.0f - topLeftAngle - degrees(remaining / radius)
        return degreesToRadians(angle)
    }

    private fun capsulePointXUnscaled(distance: Float): Float {
        var distance = distance
        val bottomRight = capsuleBottomRightLength()
        if (distance <= bottomRight) {
            val localStartX = bottomRightX
            return localStartX + (((right - radius) - localStartX)
                    * ratio(distance, bottomRight))
        }
        distance -= bottomRight
        if (distance <= arcLength) {
            val angle: Float = 90.0f - degrees(distance / radius)
            return right - radius + (radius * cos(degreesToRadians(angle).toDouble()).toFloat())
        }
        distance -= arcLength
        val rightTop = capsuleTopRightLength()
        if (distance <= rightTop) {
            return right - radius + ((topRightX - (right - radius))
                    * ratio(distance, rightTop))
        }
        distance -= rightTop
        val topLeft = capsuleTopLeftLength()
        if (distance <= topLeft) {
            return topLeftX + ((radius - topLeftX)
                    * ratio(distance, topLeft))
        }
        distance -= topLeft
        if (distance <= arcLength) {
            val angle: Float = 270.0f - degrees(distance / radius)
            return radius + (radius * cos(degreesToRadians(angle).toDouble()).toFloat())
        }
        distance -= arcLength
        val bottomLeft = capsuleBottomLeftLength()
        return radius + ((bottomLeftX - radius)
                * ratio(distance, bottomLeft))
    }

    private fun capsulePointYUnscaled(distance: Float): Float {
        var distance = distance
        val bottomRight = capsuleBottomRightLength()
        if (distance <= bottomRight) {
            return height
        }
        distance -= bottomRight
        if (distance <= arcLength) {
            val angle: Float = 90.0f - degrees(distance / radius)
            return radius + (radius * sin(degreesToRadians(angle).toDouble()).toFloat())
        }
        distance -= arcLength
        val rightTop = capsuleTopRightLength()
        if (distance <= rightTop) {
            return 0.0f
        }
        distance -= rightTop
        val topLeft = capsuleTopLeftLength()
        if (distance <= topLeft) {
            return 0.0f
        }
        distance -= topLeft
        if (distance <= arcLength) {
            val angle: Float = 270.0f - degrees(distance / radius)
            return radius + (radius * sin(degreesToRadians(angle).toDouble()).toFloat())
        }
        return height
    }

    private fun capsuleTangentX(distance: Float): Float {
        var distance = distance
        val bottomRight = capsuleBottomRightLength()
        if (distance <= bottomRight) {
            return 1.0f
        }
        distance -= bottomRight
        if (distance <= arcLength) {
            val angle: Float = 90.0f - degrees(distance / radius)
            return sin(degreesToRadians(angle).toDouble()).toFloat()
        }
        distance -= arcLength
        val rightTop = capsuleTopRightLength()
        if (distance <= rightTop) {
            return -1.0f
        }
        distance -= rightTop
        val topLeft = capsuleTopLeftLength()
        if (distance <= topLeft) {
            return -1.0f
        }
        distance -= topLeft
        if (distance <= arcLength) {
            val angle: Float = 270.0f - degrees(distance / radius)
            return sin(degreesToRadians(angle).toDouble()).toFloat()
        }
        return 1.0f
    }

    private fun capsuleTangentY(distance: Float): Float {
        var distance = distance
        val bottomRight = capsuleBottomRightLength()
        if (distance <= bottomRight) {
            return 0.0f
        }
        distance -= bottomRight
        if (distance <= arcLength) {
            val angle: Float = 90.0f - degrees(distance / radius)
            return -cos(degreesToRadians(angle).toDouble()).toFloat()
        }
        distance -= arcLength
        val rightTop = capsuleTopRightLength()
        if (distance <= rightTop) {
            return 0.0f
        }
        distance -= rightTop
        val topLeft = capsuleTopLeftLength()
        if (distance <= topLeft) {
            return 0.0f
        }
        distance -= topLeft
        if (distance <= arcLength) {
            val angle: Float = 270.0f - degrees(distance / radius)
            return -cos(degreesToRadians(angle).toDouble()).toFloat()
        }
        return 0.0f
    }

    private fun capsuleBottomRightLength(): Float {
        return max(
            0.0f,
            (right - radius) - bottomRightX
        )
    }

    private fun capsuleTopRightLength(): Float {
        return max(
            0.0f,
            (right - radius) - topRightX
        )
    }

    private fun capsuleTopLeftLength(): Float {
        return max(
            0.0f,
            topLeftX - radius
        )
    }

    private fun capsuleBottomLeftLength(): Float {
        return max(
            0.0f,
            bottomLeftX - radius
        )
    }

    private fun capsuleX(localX: Float): Float {
        return this.capsuleOriginX + (localX * this.capsuleScale)
    }

    private fun capsuleY(localY: Float): Float {
        return this.capsuleOriginY + (localY * this.capsuleScale)
    }

    private fun invalidate(style: DualStyle?) {
        this.kind = Kind.INVALID
        this.style = style
        this.length = 0.0f
        this.startX = 0.0f
        this.startY = 0.0f
        this.endX = 0.0f
        this.endY = 0.0f
        this.revision++
    }

    private fun sameCircular(
        style: DualStyle?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        topLeftAngle: Float,
        topRightAngle: Float,
        bottomLeftAngle: Float,
        bottomRightAngle: Float,
        strokeWidth: Float
    ): Boolean {
        return this.kind == Kind.CIRCULAR && this.style == style && java.lang.Float.compare(
            this.left,
            left
        ) == 0 && java.lang.Float.compare(this.top, top) == 0 && java.lang.Float.compare(
            this.right,
            right
        ) == 0 && java.lang.Float.compare(this.bottom, bottom) == 0 && java.lang.Float.compare(
            this.topLeftAngle,
            topLeftAngle
        ) == 0 && java.lang.Float.compare(this.topRightAngle, topRightAngle) == 0 && java.lang.Float.compare(
            this.bottomLeftAngle,
            bottomLeftAngle
        ) == 0 && java.lang.Float.compare(this.bottomRightAngle, bottomRightAngle) == 0 && java.lang.Float.compare(
            this.strokeWidth,
            positive(strokeWidth)
        ) == 0
    }

    private fun sameCapsule(
        width: Float,
        height: Float,
        topLeftX: Float,
        topRightX: Float,
        bottomLeftX: Float,
        bottomRightX: Float,
        strokeWidth: Float,
        originX: Float,
        originY: Float,
        scale: Float
    ): Boolean {
        if (this.kind != Kind.CAPSULE || java.lang.Float.compare(this.width, width) != 0 || java.lang.Float.compare(
                this.height,
                height
            ) != 0 || java.lang.Float.compare(this.strokeWidth, positive(strokeWidth)) != 0 || java.lang.Float.compare(
                this.capsuleOriginX,
                if (java.lang.Float.isFinite(originX)) originX else 0.0f
            ) != 0 || java.lang.Float.compare(
                this.capsuleOriginY,
                if (java.lang.Float.isFinite(originY)) originY else 0.0f
            ) != 0 || java.lang.Float.compare(
                this.capsuleScale,
                if (java.lang.Float.isFinite(scale) && scale > 0.0f) scale else 1.0f
            ) != 0
        ) {
            return false
        }
        return java.lang.Float.compare(this.topLeftX, topLeftX) == 0 && java.lang.Float.compare(
            this.topRightX,
            topRightX
        ) == 0 && java.lang.Float.compare(this.bottomLeftX, bottomLeftX) == 0 && java.lang.Float.compare(
            this.bottomRightX,
            bottomRightX
        ) == 0
    }

    companion object {
        private fun ratio(value: Float, total: Float): Float {
            return if (total > 0.0f) clamp(value / total, 0.0f, 1.0f) else 0.0f
        }

        private fun angle(value: Float): Float {
            return clamp(value, 0.0f, 89.0f)
        }

        private fun signedAngle(value: Float): Float {
            return if (java.lang.Float.isFinite(value)) clamp(value, -179.0f, 179.0f) else 0f
        }

        private fun positive(value: Float): Float {
            return if (java.lang.Float.isFinite(value) && value > 0.0f) value else 0.0f
        }

        private fun degrees(radians: Float): Float {
            return Math.toDegrees(radians.toDouble()).toFloat()
        }

        private fun degreesToRadians(degrees: Float): Float {
            return Math.toRadians(degrees.toDouble()).toFloat()
        }

        private fun clamp(value: Float, minimum: Float, maximum: Float): Float {
            if (!java.lang.Float.isFinite(value)) {
                return minimum
            }
            return max(minimum, min(maximum, value))
        }
    }
}
