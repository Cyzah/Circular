package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningTechnologyClearance
import com.cyzah.circular.core.graphics.animation.ReadoutMotion
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement

/** One measured readout transform shared by the live opening and settings preview.  */
class OpeningBatteryReadout {
    private val motion = ReadoutMotion()
    private val sourceDigits = RectF()
    private val sourceReadout = RectF()
    private val targetDigits = RectF()
    private var sourceX = 0f
    private var sourceY = 0f
    private var sourceUnit = 0f
    private var sourceContentUnit = 0f
    private var readoutOpacity = 1.0f

    fun prepare(
        endpoint: IconPainter?, host: IconPainter?, renderer: BatteryShapeRenderer?,
        state: IconState?, contentBattery: Int, placement: BatteryNumberPlacement?,
        sourceX: Float, sourceY: Float, sourceUnit: Float,
        targetLeft: Float, targetTop: Float, targetWidth: Float, targetHeight: Float,
        expansion: Float
    ): Boolean {
        this.motion.valid = false
        this.sourceReadout.setEmpty()
        if (endpoint == null || host == null || renderer == null || state == null || placement == null || !positive(
                sourceUnit
            ) || !positive(targetWidth) || !positive(targetHeight) || !java.lang.Float.isFinite(sourceX) || !java.lang.Float.isFinite(
                sourceY
            ) || !java.lang.Float.isFinite(targetLeft) || !java.lang.Float.isFinite(targetTop) || !java.lang.Float.isFinite(expansion)
        ) {
            return false
        }
        this.sourceX = sourceX
        this.sourceY = sourceY
        this.sourceUnit = sourceUnit
        this.sourceContentUnit = endpoint.contentPixelUnit()
        // The moving painted digits reach the CC glyphs before the caller's
        // solid-fill crossfade replaces them with outside text or an inside cutout.
        // A hidden destination instead fades the old readout away on its route.
        this.readoutOpacity = Motion.mix(
            1.0f, if (placement != BatteryNumberPlacement.HIDDEN) 1.0f else 0.0f,
            OpeningTechnologyClearance.shapeProgress(expansion)
        )
        endpoint.batteryReadoutBounds(state, this.sourceReadout)
        if (this.sourceReadout.isEmpty()) {
            return false
        }
        endpoint.batteryDigitsBounds(state, this.sourceDigits)
        val hasSourceDigits = !this.sourceDigits.isEmpty()
        if (!hasSourceDigits) {
            // A charge symbol can be visible even with the old percentage hidden.
            this.sourceDigits.set(this.sourceReadout)
        }
        projectSource(this.sourceReadout)
        projectSource(this.sourceDigits)

        this.targetDigits.setEmpty()
        if (hasSourceDigits && placement != BatteryNumberPlacement.HIDDEN) {
            renderer.numberBoundsSvg(host, contentBattery, placement, this.targetDigits)
        }
        if (this.targetDigits.isEmpty()) {
            // No target number exists: carry the readout with the body at its
            // natural size until the caller's existing opacity fades it away.
            val centreX =
                (targetLeft + targetWidth * BatteryTransferConfig.BODY_WIDTH / BatteryTransferConfig.SVG_WIDTH / 2.0f)
            val centreY = targetTop + targetHeight / 2.0f
            val halfWidth = this.sourceDigits.width() / 2.0f
            val halfHeight = this.sourceDigits.height() / 2.0f
            this.targetDigits.set(
                centreX - halfWidth, centreY - halfHeight,
                centreX + halfWidth, centreY + halfHeight
            )
        } else {
            val scaleX = targetWidth / BatteryTransferConfig.SVG_WIDTH
            val scaleY = targetHeight / BatteryTransferConfig.SVG_HEIGHT
            val anchorX = if (placement == BatteryNumberPlacement.INSIDE)
                BatteryTransferConfig.NUMBER_INSIDE_X_SVG
            else
                BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG
            // Match BatteryShapeRenderer's scaleY/scaleX correction about its
            // number anchor: glyphs stay isotropic even when the shell stretches.
            this.targetDigits.set(
                targetLeft + anchorX * scaleX + (this.targetDigits.left - anchorX) * scaleY,
                targetTop + this.targetDigits.top * scaleY,
                targetLeft + anchorX * scaleX + (this.targetDigits.right - anchorX) * scaleY,
                targetTop + this.targetDigits.bottom * scaleY
            )
        }
        return this.motion.update(
            this.sourceDigits.left, this.sourceDigits.top,
            this.sourceDigits.right, this.sourceDigits.bottom,
            this.targetDigits.left, this.targetDigits.top,
            this.targetDigits.right, this.targetDigits.bottom,
            OpeningTechnologyClearance.shapeProgress(expansion)
        )
    }

    fun draw(
        canvas: Canvas, endpoint: IconPainter, state: IconState?,
        color: Int, coloured: Boolean, alpha: Float
    ) {
        if (!this.motion.valid || alpha <= 0.0f) {
            return
        }
        val save = canvas.save()
        try {
            canvas.translate(this.motion.centreX, this.motion.centreY)
            canvas.scale(this.motion.scale, this.motion.scale)
            canvas.translate(-this.motion.sourceCentreX, -this.motion.sourceCentreY)
            endpoint.drawPartMaskedAtReferenceUnit(
                canvas,
                this.sourceX,
                this.sourceY,
                this.sourceUnit * 120.0f,
                state!!,
                color,
                coloured,
                (1 or IconPainter.Companion.MASK_HIDE_CENTRE or IconPainter.Companion.MASK_HIDE_SIGNALS
                        or IconPainter.Companion.MASK_HIDE_WIFI or IconPainter.Companion.MASK_HIDE_TECHNOLOGY
                        or IconPainter.Companion.MASK_HIDE_BATTERY_RING),
                alpha * this.readoutOpacity,
                this.sourceContentUnit
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun projectSource(bounds: RectF) {
        bounds.set(
            this.sourceX + bounds.left * this.sourceUnit,
            this.sourceY + bounds.top * this.sourceUnit,
            this.sourceX + bounds.right * this.sourceUnit,
            this.sourceY + bounds.bottom * this.sourceUnit
        )
    }

    companion object {
        private fun positive(value: Float): Boolean {
            return java.lang.Float.isFinite(value) && value > 0.0f
        }
    }
}
