package com.cyzah.circular.hook.runtime


/** Limits layout retries when native animation moves the reserved row again.  */
class WidthPreDrawGuard {
    private var deferrals = 0

    @JvmOverloads
    fun shouldDefer(actual: Int, required: Int, progress: Float, closedEndpoint: Boolean = false): Boolean {
        if (!ControlCenterRowHostPolicy.needsWidthBeforeDraw(actual, required, closedEndpoint)
            || !java.lang.Float.isFinite(progress)
        ) {
            reset()
            return false
        }
        if (this.deferrals >= MAX_DEFERRALS) {
            return false
        }
        this.deferrals++
        return true
    }

    fun reset() {
        this.deferrals = 0
    }

    companion object {
        private const val MAX_DEFERRALS = 2
    }
}
