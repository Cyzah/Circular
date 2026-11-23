package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.BatteryReadoutHandoff
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.ReadoutMotion
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement

/** Two real readouts share one moving centre and height during panel collapse.  */
class ClosingBatteryReadout {
    private val handoff = BatteryReadoutHandoff()
    private val sourceMotion = ReadoutMotion()
    private val targetMotion = ReadoutMotion()
    private val sourceDigits = RectF()
    private val targetDigits = RectF()
    private val targetReadout = RectF()
    private var ringX = 0f
    private var ringY = 0f
    private var ringUnit = 0f
    private var targetContentUnit = 0f
    private var batteryX = 0f
    private var batteryY = 0f
    private var batteryScale = 0f
    private var placement: BatteryNumberPlacement? = null
    private var contentBattery = 0
    private var endpoint: IconPainter? = null
    private var host: IconPainter? = null
    private var renderer: BatteryShapeRenderer? = null
    private var valid = false

    fun prepare(
        endpoint: IconPainter?, host: IconPainter?, renderer: BatteryShapeRenderer?,
        state: IconState?, contentBattery: Int, placement: BatteryNumberPlacement?,
        ringX: Float, ringY: Float, ringUnit: Float,
        fullBattery: RectF?, expansion: Float
    ): Boolean {
        reset()
        if (endpoint == null || host == null || renderer == null || state == null || placement == null || fullBattery == null || fullBattery.isEmpty()
            || !java.lang.Float.isFinite(ringX) || !java.lang.Float.isFinite(ringY) || !java.lang.Float.isFinite(ringUnit) || ringUnit <= 0 || !java.lang.Float.isFinite(
                expansion
            )
        ) return false
        this.endpoint = endpoint
        this.host = host
        this.renderer = renderer
        this.placement = placement
        this.contentBattery = contentBattery
        this.ringX = ringX
        this.ringY = ringY
        this.ringUnit = ringUnit
        this.targetContentUnit = endpoint.contentPixelUnit()
        val scaleX = fullBattery.width() / BatteryTransferConfig.SVG_WIDTH
        this.batteryScale = fullBattery.height() / BatteryTransferConfig.SVG_HEIGHT
        val anchor = if (placement == BatteryNumberPlacement.OUTSIDE)
            BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG
        else
            BatteryTransferConfig.NUMBER_INSIDE_X_SVG
        this.batteryX = fullBattery.left + anchor * (scaleX - batteryScale)
        this.batteryY = fullBattery.top
        renderer.numberBoundsSvg(host, contentBattery, placement, sourceDigits)
        project(sourceDigits, batteryX, batteryY, batteryScale)
        endpoint.batteryDigitsBounds(state, targetDigits)
        endpoint.batteryReadoutBounds(state, targetReadout)
        if (targetDigits.isEmpty()) targetDigits.set(targetReadout)
        project(targetDigits, ringX, ringY, ringUnit)
        val source = !sourceDigits.isEmpty()
        val target = !targetDigits.isEmpty()
        handoff.update(expansion, source, target)
        if (!source && !target) return false
        if (!source) sourceDigits.set(targetDigits)
        if (!target) targetDigits.set(sourceDigits)
        // No CC number means there is no imaginary start position to travel from.
        var move = if (source && target) handoff.move else 1.0f
        if (!target) move = 0.0f
        valid = sourceMotion.update(
            sourceDigits.left, sourceDigits.top,
            sourceDigits.right, sourceDigits.bottom, targetDigits.left, targetDigits.top,
            targetDigits.right, targetDigits.bottom, move
        )
                && targetMotion.update(
            targetDigits.left, targetDigits.top,
            targetDigits.right, targetDigits.bottom, sourceDigits.left, sourceDigits.top,
            sourceDigits.right, sourceDigits.bottom, 1.0f - move
        )
        return valid
    }

    fun reset() {
        valid = false
    }

    fun valid(): Boolean {
        return valid
    }

    fun cutout(): Boolean {
        return valid && placement == BatteryNumberPlacement.INSIDE
    }

    fun drawSource(canvas: Canvas, tint: Int) {
        if (!valid || handoff.sourceAlpha <= 0) return
        val save = canvas.save()
        try {
            transform(canvas, sourceMotion)
            canvas.translate(batteryX, batteryY)
            canvas.scale(batteryScale, batteryScale)
            renderer!!.drawReadoutGlyph(
                canvas, host!!, contentBattery, placement,
                tint, handoff.sourceAlpha, cutout()
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    fun drawTarget(canvas: Canvas, state: IconState?, tint: Int, coloured: Boolean) {
        if (!valid || handoff.targetAlpha <= 0) return
        val save = canvas.save()
        try {
            transform(canvas, targetMotion)
            endpoint!!.drawPartMaskedAtReferenceUnit(
                canvas, ringX, ringY, ringUnit * 120.0f, state!!, tint, coloured,
                (1 or IconPainter.Companion.MASK_HIDE_CENTRE or IconPainter.Companion.MASK_HIDE_SIGNALS
                        or IconPainter.Companion.MASK_HIDE_WIFI or IconPainter.Companion.MASK_HIDE_TECHNOLOGY
                        or IconPainter.Companion.MASK_HIDE_BATTERY_RING), handoff.targetAlpha, targetContentUnit
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    companion object {
        private fun transform(canvas: Canvas, motion: ReadoutMotion) {
            canvas.translate(motion.centreX, motion.centreY)
            canvas.scale(motion.scale, motion.scale)
            canvas.translate(-motion.sourceCentreX, -motion.sourceCentreY)
        }

        private fun project(bounds: RectF, x: Float, y: Float, unit: Float) {
            if (!bounds.isEmpty()) bounds.set(
                x + bounds.left * unit, y + bounds.top * unit,
                x + bounds.right * unit, y + bounds.bottom * unit
            )
        }
    }
}
