package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.PathMeasure
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.geometry.ContourLengths
import com.cyzah.circular.core.graphics.geometry.RingTrackStyle
import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.max

/** Paints the same sampled contour used by opening text clearance.  */
class OpeningBatteryRenderer {
    private val outline = Path()
    private val fill = Path()
    private val measure = PathMeasure()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contours = ContourLengths()

    fun draw(
        canvas: Canvas, frame: OpeningBatteryMorph, battery: Int, plainTint: Int,
        batteryTint: Int, opacity: Float
    ) {
        if (opacity <= 0.0f) return
        val save = if (opacity < 1.0f) canvas.saveLayerAlpha(null, Math.round(255 * opacity)) else -1
        try {
            draw(canvas, frame, battery, plainTint, batteryTint)
        } finally {
            if (save >= 0) canvas.restoreToCount(save)
        }
    }

    fun draw(canvas: Canvas, frame: OpeningBatteryMorph, battery: Int, plainTint: Int, batteryTint: Int) {
        if (!frame.valid || frame.outlineAlpha <= 0.0f) {
            return
        }
        val save = canvas.save()
        try {
            if (frame.contourAtTarget) {
                canvas.clipRect(
                    frame.bodyLeft, frame.bodyTop,
                    frame.bodyLeft + frame.bodyWidth, frame.bodyTop + frame.bodyHeight
                )
            }
            drawContour(canvas, frame, battery, plainTint, batteryTint)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun drawContour(
        canvas: Canvas, frame: OpeningBatteryMorph, battery: Int,
        plainTint: Int, batteryTint: Int
    ) {
        outline.reset()
        for (point in 0..<frame.points.size / 2) {
            val index = point * 2
            if (frame.contourStarts[point]) outline.moveTo(frame.points[index], frame.points[index + 1])
            else outline.lineTo(frame.points[index], frame.points[index + 1])
        }
        if (frame.contourAtTarget) outline.close()
        paint.setStyle(Paint.Style.STROKE)
        paint.setStrokeCap(Paint.Cap.ROUND)
        paint.setStrokeJoin(Join.ROUND)
        paint.setStrokeWidth(frame.strokeWidth)
        val trackTint = RingTrackStyle.tint(frame.capsule, battery, plainTint, batteryTint)
        val trackAlpha = RingTrackStyle.alpha(frame.capsule)
        paint.setColor(trackTint)
        paint.setAlpha(DisplayPolicy.paintAlpha(trackTint, trackAlpha, frame.outlineAlpha))
        canvas.drawPath(outline, paint)

        // Keep the actual charge ratio while the outer band changes shape.
        measure.setPath(outline, false)
        contours.reset()
        do {
            contours.add(measure.getLength())
        } while (measure.nextContour())
        val start = contours.total() * (1.0f - Motion.clamp(battery / 100.0f))
        measure.setPath(outline, false)
        fill.reset()
        for (contour in 0..<contours.count()) {
            val local = max(0f, start - contours.start(contour))
            if (local < contours.length(contour)) measure.getSegment(local, contours.length(contour), fill, true)
            if (!measure.nextContour()) break
        }
        paint.setColor(batteryTint)
        paint.setAlpha(DisplayPolicy.paintAlpha(batteryTint, 255, frame.outlineAlpha))
        canvas.drawPath(fill, paint)
    }
}
