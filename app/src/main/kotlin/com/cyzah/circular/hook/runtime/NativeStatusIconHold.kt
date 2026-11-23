package com.cyzah.circular.hook.runtime

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Keeps native status icons hidden across rapid control-center/notification-center
 * ownership changes. The hold is time-bounded and released on the existing main
 * handler; it never starts a worker thread or performs per-frame logging.
 */
class NativeStatusIconHold(module: ModuleMain) {
    private val module: ModuleMain?
    private val main: Handler
    private val releaseTask: Runnable = object : Runnable {
        override fun run() {
            this@NativeStatusIconHold.release()
        }
    }
    private val hideTask: Runnable = object : Runnable {
        override fun run() {
            hidePosted = false
            hideNativeIconsNow()
        }
    }

    @Volatile
    private var holdUntil: Long = 0
    private var hidePosted = false

    init {
        this.module = module
        this.main = module.main
    }

    fun activate() {
        val now = SystemClock.uptimeMillis()
        val wasActive = this.holdUntil > now
        if (!wasActive) {
            this.module!!.log(4, ModuleMain.Companion.TAG, "nativeStatusIconHold=active")
        }
        this.holdUntil = now + HOLD_MS
        this.main.removeCallbacks(this.releaseTask)
        this.main.postDelayed(this.releaseTask, HOLD_MS)
        if (!wasActive) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                hideNativeIconsNow()
            } else if (!this.hidePosted) {
                this.hidePosted = true
                this.main.post(this.hideTask)
            }
        }
    }

    fun active(): Boolean {
        return this.holdUntil > SystemClock.uptimeMillis()
    }

    fun ownsSignals(session: IconSession?): Boolean {
        return session != null && SessionPaintPolicy.hidesNativeSignalState(
            session.from,
            session.merge,
            session.stackedHold,
            this.module!!.stackedOn() && this.module.mode == 2,
            session.ambient,
            active(),
            session.draw && !session.closed
        )
    }

    fun networkChanged() {
        if (this.module!!.mode == 2 && this.module.stackedOn()
            && this.module.networks?.consumeWifiDisconnect() == true
        ) {
            activate()
            refreshSessions(false)
        }
    }

    fun reset() {
        this.main.removeCallbacks(this.releaseTask)
        this.main.removeCallbacks(this.hideTask)
        this.holdUntil = 0L
        this.hidePosted = false
        refreshSessions(true)
    }

    private fun release() {
        if (SessionPaintPolicy.shouldHoldNativeIcons(
                max(this.module!!.control, this.module.shade),
                this.module.stackedOn(),
                this.module.mode == 2
            )
        ) {
            activate()
            return
        }
        this.holdUntil = 0L
        this.module.log(4, ModuleMain.Companion.TAG, "nativeStatusIconHold=released")
        refreshSessions(false)
    }

    private fun refreshSessions(resetRows: Boolean) {
        for (session in this.module!!.batteries.values) {
            if (session.from == 0 || session.from == 1 || session.from == 5 || session.from == 6) {
                if (resetRows) {
                    session.stackedHold = false
                    session.controlCenterRow.invalidate()
                }
                session.changed()
                session.requestLayout()
                session.invalidateSignals()
            }
        }
    }

    private fun hideNativeIconsNow() {
        if (!SessionPaintPolicy.forceHidesNativeIcons(active())
            || this.module!!.visibleState == null
        ) {
            return
        }
        for (view in this.module.signals.keys) {
            if (!ownsSignals(this.module.signals.get(view))) {
                continue
            }
            try {
                this.module.visibleState!!.invoke(
                    view,
                    0,
                    false
                )
            } catch (ignored: Throwable) {
            }
        }
    }

    companion object {
        private const val HOLD_MS = 800L
    }
}
