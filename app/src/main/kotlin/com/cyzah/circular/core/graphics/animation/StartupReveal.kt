package com.cyzah.circular.core.graphics.animation

/** A new native host becomes visible without a full-brightness first-frame flash.  */
class StartupReveal {
    private var started = -1L

    fun alpha(now: Long, tintReady: Boolean): Float {
        if (!tintReady) return 0.0f
        if (started < 0L) started = now
        return Motion.ease((now - started) / 160.0f)
    }
}
