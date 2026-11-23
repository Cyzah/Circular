package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.ContourLengths
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Android path cache for an exact [RingGeometry].
 */
class RingPathAdapter {
    private val path = Path()
    private val segment = Path()
    private val measure = PathMeasure()
    private val oval = RectF()
    private val position = FloatArray(2)
    private val tangent = FloatArray(2)
    private val contours = ContourLengths()
    private var revision = Int.MIN_VALUE
    private var geometry: RingGeometry? = null

    fun update(geometry: RingGeometry?) {
        val sameGeometry = this.geometry == geometry
        this.geometry = geometry
        if (geometry == null || !geometry.valid()) {
            this.revision = Int.MIN_VALUE
            this.contours.reset()
            return
        }
        if (sameGeometry && this.revision == geometry.revision) {
            return
        }
        this.revision = geometry.revision
        this.path.reset()
        if (geometry.hasContentCutouts()) {
            for (i in 0..<geometry.contourPointCount()) {
                if (geometry.contourStartsAt(i)) this.path.moveTo(geometry.contourPointX(i), geometry.contourPointY(i))
                else this.path.lineTo(geometry.contourPointX(i), geometry.contourPointY(i))
            }
        } else if (geometry.kind == RingGeometry.Kind.CAPSULE) {
            buildCapsule(geometry)
        } else if (geometry.kind == RingGeometry.Kind.CIRCULAR) {
            buildCircular(geometry)
        }
        this.measure.setPath(this.path, false)
        this.contours.reset()
        do {
            this.contours.add(this.measure.getLength())
        } while (this.measure.nextContour())
    }

    fun valid(): Boolean {
        return this.geometry != null && this.geometry!!.valid()
                && this.contours.total() > 0.0f
    }

    fun length(): Float {
        return if (valid()) this.contours.total() else 0.0f
    }

    fun getPositionX(progress: Float): Float {
        sample(progress)
        return this.position[0]
    }

    fun getPositionY(progress: Float): Float {
        sample(progress)
        return this.position[1]
    }

    fun getTangentX(progress: Float): Float {
        sample(progress)
        return this.tangent[0]
    }

    fun getTangentY(progress: Float): Float {
        sample(progress)
        return this.tangent[1]
    }

    fun drawPrefix(
        canvas: Canvas,
        progress: Float,
        paint: Paint?
    ) {
        drawRange(canvas, 0.0f, Motion.clamp(progress), paint)
    }

    fun drawSuffix(
        canvas: Canvas,
        progress: Float,
        paint: Paint?
    ) {
        drawRange(
            canvas,
            1.0f - Motion.clamp(progress),
            1.0f,
            paint
        )
    }

    fun drawTrack(canvas: Canvas, paint: Paint?) {
        if (!valid()) {
            return
        }
        canvas.drawPath(this.path, paint!!)
    }

    fun drawRange(
        canvas: Canvas,
        start: Float,
        end: Float,
        paint: Paint?
    ) {
        if (!valid() || end <= start) {
            return
        }
        val globalStart = this.contours.total() * Motion.clamp(start)
        val globalEnd = this.contours.total() * Motion.clamp(end)
        this.measure.setPath(this.path, false)
        for (contour in 0..<this.contours.count()) {
            val contourStart = this.contours.start(contour)
            val contourEnd = (contourStart
                    + this.contours.length(contour))
            val localStart = max(globalStart, contourStart) - contourStart
            val localEnd = min(globalEnd, contourEnd) - contourStart
            if (localEnd > localStart) {
                this.segment.reset()
                val extracted = this.measure.getSegment(
                    localStart,
                    localEnd,
                    this.segment,
                    true
                )
                if (extracted) {
                    canvas.drawPath(this.segment, paint!!)
                }
            }
            if (!this.measure.nextContour()) {
                break
            }
        }
    }

    private fun sample(progress: Float) {
        this.position[0] = 0.0f
        this.position[1] = 0.0f
        this.tangent[0] = 0.0f
        this.tangent[1] = 0.0f
        if (!valid()) {
            return
        }
        val contour = this.contours.contourAt(progress)
        if (contour < 0 || !moveToContour(contour)) {
            return
        }
        this.measure.getPosTan(
            this.contours.localDistance(contour, progress),
            this.position,
            this.tangent
        )
    }

    private fun moveToContour(contour: Int): Boolean {
        this.measure.setPath(this.path, false)
        for (index in 0..<contour) {
            if (!this.measure.nextContour()) {
                return false
            }
        }
        return true
    }

    private fun buildCircular(geometry: RingGeometry) {
        this.oval.set(
            geometry.left,
            geometry.top,
            geometry.right,
            geometry.bottom
        )
        val start = 90.0f - geometry.bottomRightAngle
        this.path.moveTo(geometry.startX, geometry.startY)
        if (geometry.rightSideSweep > 0) {
            this.path.arcTo(this.oval, start, -geometry.rightSideSweep, false)
        }
        val leftStart = 270.0f - geometry.topLeftAngle
        val leftStartRadians = Math.toRadians(leftStart.toDouble()).toFloat()
        if (!geometry.continuousContour()) {
            this.path.moveTo(
                geometry.centreX
                        + (geometry.width * .5f
                        * cos(leftStartRadians.toDouble()).toFloat()),
                geometry.centreY
                        + (geometry.height * .5f
                        * sin(leftStartRadians.toDouble()).toFloat())
            )
        }
        if (geometry.leftSideSweep > 0) {
            this.path.arcTo(this.oval, leftStart, -geometry.leftSideSweep, false)
        }
    }

    private fun buildCapsule(geometry: RingGeometry) {
        val radius = geometry.radius
        this.path.moveTo(geometry.startX, geometry.startY)
        this.path.lineTo(
            capsuleX(geometry, geometry.right - radius),
            capsuleY(geometry, geometry.height)
        )
        this.oval.set(
            capsuleX(geometry, geometry.right - (radius * 2.0f)),
            capsuleY(geometry, 0.0f),
            capsuleX(geometry, geometry.right),
            capsuleY(geometry, geometry.height)
        )
        this.path.arcTo(this.oval, 90.0f, -180.0f, false)
        this.path.lineTo(
            capsuleX(geometry, geometry.topRightX),
            capsuleY(geometry, 0.0f)
        )
        if (!geometry.continuousContour()) {
            this.path.moveTo(
                capsuleX(geometry, geometry.topLeftX),
                capsuleY(geometry, 0.0f)
            )
        }
        this.path.lineTo(
            capsuleX(geometry, radius),
            capsuleY(geometry, 0.0f)
        )
        this.oval.set(
            capsuleX(geometry, 0.0f),
            capsuleY(geometry, 0.0f),
            capsuleX(geometry, radius * 2.0f),
            capsuleY(geometry, geometry.height)
        )
        this.path.arcTo(this.oval, 270.0f, -180.0f, false)
        this.path.lineTo(geometry.endX, geometry.endY)
    }

    companion object {
        private fun capsuleX(geometry: RingGeometry, localX: Float): Float {
            return geometry.capsuleOriginX + (localX * geometry.capsuleScale)
        }

        private fun capsuleY(geometry: RingGeometry, localY: Float): Float {
            return geometry.capsuleOriginY + (localY * geometry.capsuleScale)
        }
    }
}
