package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.animation.DuoWifiTransitionState
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.DualStyle

/**
 * Mutable presentation state shared by every settings preview view.
 */
class PreviewRuntimeState(private var dark: Boolean, expansion: Float, duoWifiProgress: Float) {
    private var expansion: Float
    private var duoWifiProgress: Float
    private var duoWifi: DuoWifiTransitionState? = null
    private var duoWifiAnimating = false
    private val batteryTransfer = PreviewBatteryTransfer()

    init {
        this.expansion = clamp(expansion)
        this.duoWifiProgress = clamp(duoWifiProgress)
    }

    fun dark(): Boolean {
        return dark
    }

    /** Neutral status-bar colors follow this preview's own background state.  */
    fun iconTint(): Int {
        return if (dark) -0x19000001 else -0x41000000
    }

    fun captionTint(): Int {
        return if (dark) -0x1 else -0x1000000
    }

    fun toggleDark(): Boolean {
        dark = !dark
        return dark
    }

    fun expansion(): Float {
        return expansion
    }

    fun setExpansion(expansion: Float) {
        this.expansion = clamp(expansion)
    }

    fun batteryCollapse(): Float {
        return this.batteryTransfer.batteryCollapse(expansion)
    }

    fun duoWifiProgress(): Float {
        return duoWifiProgress
    }

    fun setDuoWifiProgress(duoWifiProgress: Float) {
        this.duoWifiProgress = clamp(duoWifiProgress)
        this.duoWifi = null
        this.duoWifiAnimating = false
    }

    fun batteryTransfer(): PreviewBatteryTransfer {
        return batteryTransfer
    }

    fun updateMotion(
        batteryEnabled: Boolean,
        style: DualStyle?,
        scene: IconState,
        now: Long
    ) {
        this.batteryTransfer.update(this.expansion, batteryEnabled, now)
        val wifiTarget = style == DualStyle.DUO && scene.dualSim()
                && scene.showWifi()
        if (this.duoWifi == null) {
            this.duoWifi = DuoWifiTransitionState()
            this.duoWifi!!.begin(wifiTarget, now)
            this.duoWifiProgress = this.duoWifi!!.progress()
            return
        }
        if (this.duoWifi!!.begin(wifiTarget, now)) {
            this.duoWifiAnimating = true
        }
        if (this.duoWifiAnimating) {
            this.duoWifiProgress = this.duoWifi!!.advance(now, 260L)
            this.duoWifiAnimating = !this.duoWifi!!.complete(now, 260L)
        }
    }

    fun needsFrame(): Boolean {
        return this.batteryTransfer.needsFrame() || this.duoWifiAnimating
    }

    fun resetBatteryTransfer() {
        batteryTransfer.reset()
    }

    companion object {
        private fun clamp(value: Float): Float {
            return Motion.clamp(value)
        }
    }
}
