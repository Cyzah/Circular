package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Join
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.RingTrackStyle
import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.max

/** Shared ring transfer layers; hosts supply geometry and their canvas transform.  */
class BatteryTransferRingRenderer {
    private val path = RingPathAdapter()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas, geometry: RingGeometry, state: BatteryTransferState,
        battery: Int, plainTint: Int, batteryTint: Int,
        originX: Float, originY: Float, unit: Float
    ) {
        drawLayers(
            canvas, geometry, battery, plainTint, batteryTint, originX, originY, unit,
            state.ringTrackVisible(), state.ringTraceProgress(),
            RingTrackStyle.temporaryAlpha(state.ringTrackVisible(), state.temporaryTraceAlpha()),
            state.fillProgress(max(0, battery)), state.pointVisible(),
            state.pointPathProgress(), 1.0f
        )
    }

    fun drawReopen(
        canvas: Canvas, geometry: RingGeometry, state: BatteryTransferState,
        battery: Int, plainTint: Int, batteryTint: Int,
        originX: Float, originY: Float, unit: Float
    ) {
        val previous = state.reopeningSnapshot()
        val alpha = state.reopenAlpha()
        if (alpha <= 0) return
        val layer = if (alpha < 1) canvas.saveLayerAlpha(null, Math.round(255 * alpha)) else -1
        try {
            drawLayers(
                canvas, geometry, battery, plainTint, batteryTint, originX, originY, unit,
                previous.trackVisible, previous.traceProgress, previous.traceAlpha,
                Motion.clamp(battery / 100.0f) * previous.fillFraction, previous.pointVisible,
                previous.pointProgress, 1.0f
            )
        } finally {
            if (layer >= 0) canvas.restoreToCount(layer)
        }
    }

    private fun drawLayers(
        canvas: Canvas, geometry: RingGeometry,
        battery: Int, plainTint: Int, batteryTint: Int,
        originX: Float, originY: Float, unit: Float,
        trackVisible: Boolean, traceProgress: Float, temporaryAlpha: Float,
        fillProgress: Float, pointVisible: Boolean, pointProgress: Float,
        opacity: Float
    ) {
        if (unit <= 0.0f || opacity <= 0.0f) {
            return
        }
        this.path.update(geometry)
        if (!this.path.valid()) {
            return
        }
        this.paint.setStyle(Paint.Style.STROKE)
        this.paint.setStrokeCap(Paint.Cap.ROUND)
        this.paint.setStrokeJoin(Join.ROUND)
        this.paint.setStrokeWidth(geometry.strokeWidth)
        val capsule = geometry.kind == RingGeometry.Kind.CAPSULE
        val trackTint = RingTrackStyle.tint(capsule, battery, plainTint, batteryTint)
        val trackAlpha = RingTrackStyle.alpha(capsule)
        val save = canvas.save()
        try {
            canvas.translate(originX, originY)
            canvas.scale(unit, unit)
            if (trackVisible) {
                setColor(trackTint, trackAlpha, opacity)
                this.path.drawTrack(canvas, this.paint)
            }
            if (temporaryAlpha > 0.0f) {
                setColor(trackTint, trackAlpha, temporaryAlpha * opacity)
                this.path.drawPrefix(canvas, traceProgress, this.paint)
            }
            if (fillProgress > 0.0f) {
                setColor(BatteryTransferPolicy.realFillTint(batteryTint), 255, opacity)
                this.path.drawSuffix(canvas, fillProgress, this.paint)
            }
            if (pointVisible) {
                this.paint.setStyle(Paint.Style.FILL)
                setColor(batteryTint, 255, opacity)
                canvas.drawCircle(
                    this.path.getPositionX(pointProgress),
                    this.path.getPositionY(pointProgress), geometry.strokeWidth / 2.0f,
                    this.paint
                )
            }
        } finally {
            canvas.restoreToCount(save)
            this.paint.setStyle(Paint.Style.FILL)
        }
    }

    private fun setColor(tint: Int, alpha: Int, opacity: Float) {
        this.paint.setColor(tint)
        this.paint.setAlpha(DisplayPolicy.paintAlpha(tint, alpha, opacity))
    }
}
