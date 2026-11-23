package com.cyzah.circular.core.graphics.animation

/** The last displayed closing ring, retained briefly when opening interrupts it.  */
class ReopeningRingSnapshot {
    @JvmField var trackVisible: Boolean = false
    @JvmField var pointVisible: Boolean = false
    @JvmField var traceProgress: Float = 0f
    @JvmField var traceAlpha: Float = 0f
    @JvmField var fillFraction: Float = 0f
    @JvmField var pointProgress: Float = 0f
    private var alpha = 0f
    private var started: Long = 0

    fun capture(
        track: Boolean, trace: Float, traceOpacity: Float, fill: Float,
        point: Boolean, pointAt: Float, now: Long
    ) {
        trackVisible = track
        traceProgress = Motion.clamp(trace)
        traceAlpha = Motion.clamp(traceOpacity)
        fillFraction = Motion.clamp(fill)
        pointVisible = point
        pointProgress = Motion.clamp(pointAt)
        started = now
        alpha = 1.0f
    }

    fun sample(now: Long) {
        if (alpha > 0.0f) alpha = 1.0f - Motion.ease((now - started) / DURATION_MS.toFloat())
    }

    fun alpha(): Float {
        return alpha
    }

    fun clear() {
        alpha = 0.0f
        pointVisible = false
        trackVisible = pointVisible
        pointProgress = 0.0f
        fillFraction = pointProgress
        traceAlpha = fillFraction
        traceProgress = traceAlpha
    }

    companion object {
        const val DURATION_MS: Long = 120L
    }
}
