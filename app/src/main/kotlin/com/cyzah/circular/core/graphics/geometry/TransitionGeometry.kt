package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Reusable six-value transition geometry in the destination canvas space.
 */
class TransitionGeometry {
    @JvmField var centreX: Float = 0f
    @JvmField var centreY: Float = 0f
    @JvmField var widthX: Float = 0f
    @JvmField var widthY: Float = 0f
    @JvmField var heightX: Float = 0f
    @JvmField var heightY: Float = 0f

    fun set(
        centreX: Float,
        centreY: Float,
        widthX: Float,
        widthY: Float,
        heightX: Float,
        heightY: Float
    ) {
        this.centreX = centreX
        this.centreY = centreY
        this.widthX = widthX
        this.widthY = widthY
        this.heightX = heightX
        this.heightY = heightY
    }

    fun setCentred(
        centreX: Float,
        centreY: Float,
        width: Float,
        height: Float
    ) {
        set(centreX, centreY, width, 0.0f, 0.0f, height)
    }

    fun copyFrom(source: TransitionGeometry) {
        set(
            source.centreX,
            source.centreY,
            source.widthX,
            source.widthY,
            source.heightX,
            source.heightY
        )
    }

    fun width(): Float {
        return hypot(widthX.toDouble(), widthY.toDouble()).toFloat()
    }

    fun height(): Float {
        return hypot(heightX.toDouble(), heightY.toDouble()).toFloat()
    }

    fun valid(): Boolean {
        return finite(centreX)
                && finite(centreY)
                && finite(widthX)
                && finite(widthY)
                && finite(heightX)
                && finite(heightY)
                && width() > 0.0f && height() > 0.0f
    }

    fun maximumDifference(other: TransitionGeometry?): Float {
        if (other == null) {
            return Float.POSITIVE_INFINITY
        }
        return max(
            abs(centreX - other.centreX),
            max(
                abs(centreY - other.centreY),
                max(
                    abs(widthX - other.widthX),
                    max(
                        abs(widthY - other.widthY),
                        max(
                            abs(heightX - other.heightX),
                            abs(heightY - other.heightY)
                        )
                    )
                )
            )
        )
    }

    companion object {
        @JvmStatic fun interpolateSimilarity(
            source: TransitionGeometry?,
            target: TransitionGeometry?,
            progress: Float,
            output: TransitionGeometry
        ) {
            // Expansion progress is 1 at the source endpoint and 0
            // at the collapsed native endpoint.
            if (source == null || !source.valid()) {
                output.set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                return
            }
            if (target == null || !target.valid()) {
                output.copyFrom(source)
                return
            }
            val value = Motion.clamp(progress)
            val sourceWidth = source.width()
            val sourceHeight = source.height()
            val targetScale = min(
                target.width() / sourceWidth,
                target.height() / sourceHeight
            )
            val scale = targetScale + ((1.0f - targetScale) * value)
            output.set(
                target.centreX
                        + ((source.centreX - target.centreX) * value),
                target.centreY
                        + ((source.centreY - target.centreY) * value),
                source.widthX * scale,
                source.widthY * scale,
                source.heightX * scale,
                source.heightY * scale
            )
        }

        private fun finite(value: Float): Boolean {
            return java.lang.Float.isFinite(value)
        }
    }
}
