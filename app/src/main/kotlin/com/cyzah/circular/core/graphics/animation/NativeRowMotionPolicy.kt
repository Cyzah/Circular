package com.cyzah.circular.core.graphics.animation

import kotlin.math.max

object NativeRowMotionPolicy {
    @JvmStatic fun speedBoundary(closed: Float, expanded: Float, remaining: Float): Float {
        return Motion.mix(closed, expanded, remaining)
    }

    @JvmStatic fun sourceCorrection(expandedSource: Float, currentSource: Float): Float {
        return if (java.lang.Float.isFinite(expandedSource) && java.lang.Float.isFinite(currentSource)) max(
            0.0f,
            expandedSource - currentSource
        ) else
            0.0f
    }

    @JvmStatic fun iconCorrection(currentBoundary: Float, desiredBoundary: Float): Float {
        return if (java.lang.Float.isFinite(currentBoundary) && java.lang.Float.isFinite(desiredBoundary))
            desiredBoundary - currentBoundary
        else
            0.0f
    }

    class Endpoints {
        @JvmField var closed: Float = Float.NaN
        @JvmField var expanded: Float = Float.NaN
        fun closed(sample: Float, progress: Float) {
            if (java.lang.Float.isFinite(sample) && (!java.lang.Float.isFinite(closed)
                        || progress <= BatteryTransferConfig.HANDOFF_MAX)
            ) closed = sample
        }

        fun expanded(sample: Float, progress: Float, closing: Boolean) {
            if (java.lang.Float.isFinite(sample) && (!java.lang.Float.isFinite(expanded)
                        || (progress >= 0.95f && !closing))
            ) expanded = sample
        }

        fun invalidate() {
            expanded = Float.NaN
            closed = expanded
        }
    }

    class OpeningSlots {
        private var start = 0f
        private var fraction = 0f

        fun sample(progress: Float, closing: Boolean) {
            val p = Motion.clamp(progress)
            if (closing || p <= BatteryTransferConfig.HANDOFF_MAX) {
                start = p
                fraction = 0.0f
            } else {
                fraction = if (p >= 1.0f) 1.0f else Motion.clamp((p - start) / (1.0f - start))
            }
        }

        fun space(): Float {
            return Motion.ease(fraction / 0.7f)
        }

        fun alpha(): Float {
            return Motion.ease((fraction - 0.7f) / 0.3f)
        }

        fun reset() {
            fraction = 0.0f
            start = fraction
        }
    }

    class Translation {
        private var lastWritten = 0f
        private var correction = 0f
        private var owned = false

        fun apply(current: Float, value: Float): Float {
            if (!java.lang.Float.isFinite(current) || !java.lang.Float.isFinite(value)) return release(current)
            val base = current - applied(current)
            this.correction = value
            this.lastWritten = base + value
            this.owned = true
            return this.lastWritten
        }

        fun applied(current: Float): Float {
            return if (this.owned && current == this.lastWritten) this.correction else 0.0f
        }

        fun release(current: Float): Float {
            val base = current - applied(current)
            this.owned = false
            this.correction = 0.0f
            return base
        }
    }
}
