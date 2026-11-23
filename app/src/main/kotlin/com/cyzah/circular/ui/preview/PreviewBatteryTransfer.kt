package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.graphics.geometry.RingGeometry

/**
 * Adapts the device battery state machine to the preview's frame clock.
 */
class PreviewBatteryTransfer {
    private var frameTime: Long = 0
    private var initialized = false
    private val transfer = BatteryTransferState(
        BatteryTransferTimeline(object : BatteryTransferTimeline.Clock {
            override fun now(): Long {
                return frameTime
            }
        })
    )

    fun update(expansion: Float, enabled: Boolean, now: Long) {
        this.frameTime = now
        this.initialized = true
        if (!enabled) {
            this.transfer.invalidate()
            return
        }
        val control = Motion.clamp(expansion)
        val morph = PanelMotionFrame.morph(control)
        this.transfer.update(
            control,
            0.0f,
            morph,
            true,
            true,
            morph <= BatteryTransferConfig.CLOSED_MORPH_MAX,
            now
        )
    }

    fun reset() {
        this.transfer.invalidate()
    }

    fun active(): Boolean {
        return this.transfer.ringTransferVisible()
    }

    fun drawState(): BatteryTransferState {
        return this.transfer
    }

    fun needsFrame(): Boolean {
        return this.transfer.needsFrame()
    }

    fun batteryCollapse(expansion: Float): Float {
        return if (this.initialized)
            this.transfer.batteryCollapse(
                expansion,
                PanelMotionFrame.morph(expansion)
            )
        else
            PreviewMorph.batteryCollapse(expansion)
    }

    fun pointVisible(): Boolean {
        return this.transfer.pointVisible()
    }

    fun trackVisible(): Boolean {
        return this.transfer.ringTrackVisible()
    }

    fun traceProgress(): Float {
        return this.transfer.ringTraceProgress()
    }

    fun temporaryTraceAlpha(): Float {
        return this.transfer.temporaryTraceAlpha()
    }

    fun fillProgress(battery: Int): Float {
        return this.transfer.fillProgress(battery)
    }

    companion object {
        @JvmStatic fun copyClosedRing(source: RingGeometry?, target: RingGeometry) {
            target.copyForTransferFrom(source)
        }
    }
}
