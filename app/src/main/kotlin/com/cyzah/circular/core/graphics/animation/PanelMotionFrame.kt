package com.cyzah.circular.core.graphics.animation

/**
 * Native-expansion-derived animation channels shared by every dual-SIM style.
 */
object PanelMotionFrame {
    @JvmStatic fun morph(control: Float): Float {
        return Motion.clamp(control)
    }

    @JvmStatic fun nativeAlpha(control: Float): Float {
        return Motion.clamp(control)
    }

    @JvmStatic fun duoWifiProgress(wifiConnected: Boolean): Float {
        return if (wifiConnected) 1.0f else 0.0f
    }
}
