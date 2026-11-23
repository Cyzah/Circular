package com.cyzah.circular.hook.runtime

import android.os.Handler
import android.os.SystemClock
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline
import com.cyzah.circular.core.graphics.animation.DuoWifiTransitionState
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame

/**
 * Schedules only the post-collapse battery-ring state. Panel morph, native
 * alpha and DUO Wi-Fi are direct functions of the live SystemUI expansion.
 */
class PanelMotionController(private val module: ModuleMain, private val main: Handler) {
    private val transfer = BatteryTransferState(
        BatteryTransferTimeline(object : BatteryTransferTimeline.Clock {
            override fun now(): Long {
                return SystemClock.uptimeMillis()
            }
        })
    )
    private var lastTransferPhase: BatteryTransferState.Phase? = null
    private var lastTransferHost = Int.MIN_VALUE
    private val duoWifi = DuoWifiTransitionState()
    private val duoTick: Runnable = object : Runnable {
        override fun run() {
            advanceDuoWifi()
        }
    }
    private val transferTick: Runnable = object : Runnable {
        override fun run() {
            advanceBatteryTransfer()
        }
    }

    init {
        this.transfer.update(
            0.0f,
            0.0f,
            0.0f,
            false,
            true,
            true,
            SystemClock.uptimeMillis()
        )
    }

    fun advanceMorph() {
        val now = SystemClock.uptimeMillis()
        updateBatteryTransfer(now)
        scheduleBatteryTransfer()
        invalidateBatteries()
    }

    fun snapMorph(ignoredTarget: Float) {
        advanceMorph()
    }

    fun synchronizeDuo() {
        val target = this.module.duoDual
                && this.module.state.dualSim()
                && this.module.state.showWifi()
        if (!this.duoWifi.begin(target, SystemClock.uptimeMillis())) {
            return
        }
        this.module.log(
            4,
            ModuleMain.Companion.TAG,
            ("duoWifiMotion from=" + this.duoWifi.progress()
                    + " target=" + target
                    + " durationMs=" + DUO_DURATION_MS)
        )
        this.main.removeCallbacks(this.duoTick)
        this.main.post(this.duoTick)
    }

    fun advanceDuoWifi() {
        val now = SystemClock.uptimeMillis()
        this.duoWifi.advance(now, DUO_DURATION_MS)
        invalidateBatteries()
        if (!this.duoWifi.complete(now, DUO_DURATION_MS)) {
            this.main.postDelayed(this.duoTick, FRAME_DELAY_MS)
        }
    }

    fun morphValue(): Float {
        return PanelMotionFrame.morph(this.module.control)
    }

    fun currentDuoWifiProgress(wifiConnected: Boolean): Float {
        return if (this.duoWifi.progress() >= 0.0f)
            this.duoWifi.progress()
        else
            (if (wifiConnected) 1.0f else 0.0f)
    }

    fun duoWifiProgress(): Float {
        return this.duoWifi.progress()
    }

    fun batteryTransferState(): BatteryTransferState {
        return this.transfer
    }

    fun refreshBatteryTransfer() {
        updateBatteryTransfer(SystemClock.uptimeMillis())
        scheduleBatteryTransfer()
        invalidateBatteries()
    }

    fun invalidateBatteryTransfer() {
        this.main.removeCallbacks(this.transferTick)
        this.transfer.invalidate()
        invalidateBatteries()
    }

    fun dispose() {
        this.main.removeCallbacks(this.duoTick)
        this.main.removeCallbacks(this.transferTick)
    }

    private fun advanceBatteryTransfer() {
        updateBatteryTransfer(SystemClock.uptimeMillis())
        if (this.transfer.needsFrame()) {
            this.main.postDelayed(this.transferTick, FRAME_DELAY_MS)
        }
        invalidateBatteries()
    }

    private fun updateBatteryTransfer(now: Long) {
        val host0Available = this.module.transferHostAttached(0)
        val anyHostAvailable = host0Available
                || this.module.transferHostAttached(5)
                || this.module.transferHostAttached(6)
        val ready = BatteryTransferPolicy.runtimeReady(
            this.module.state.battery >= 0,
            this.module.transferScreenInteractive(),
            this.module.transferKeyguardVisible(),
            this.module.transferPrivacyBlocked(),
            anyHostAvailable
                    && this.module.mode == 2 && this.module.apkOk
                    && this.module.sessionSettings().compatible()
        )
        val animationReady = ready
                && this.module.stackedOn()
                && this.module.state.readyForMerge()
                && this.module.state.stackedReady()
        val morph = morphValue()
        this.transfer.update(
            this.module.control,
            this.module.shade,
            morph,
            ready,
            animationReady,
            morph <= BatteryTransferConfig.CLOSED_MORPH_MAX
                    && host0Available,
            this.module.panelGestureReleased(),
            now
        )
        val controlHost = this.module.preferredBatteryPaintHost()
        val diagnosticHost = if (this.transfer.batteryShapeVisible()
            && controlHost != null
        )
            controlHost.from
        else
            0
        if (this.transfer.phase() != this.lastTransferPhase
            || diagnosticHost != this.lastTransferHost
        ) {
            this.lastTransferPhase = this.transfer.phase()
            this.lastTransferHost = diagnosticHost
            this.module.recordBatteryTransferDiagnostic()
        }
    }

    private fun scheduleBatteryTransfer() {
        this.main.removeCallbacks(this.transferTick)
        if (this.transfer.needsFrame()) {
            this.main.post(this.transferTick)
        }
    }

    private fun invalidateBatteries() {
        for (session in this.module.batteries.values) {
            this.module.drawingClips.update(session)
            session.battery.invalidate()
        }
    }

    companion object {
        private const val DUO_DURATION_MS = 260L
        private const val FRAME_DELAY_MS = 16L
    }
}
