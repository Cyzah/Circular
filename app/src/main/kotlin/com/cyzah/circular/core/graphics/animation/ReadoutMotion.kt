package com.cyzah.circular.core.graphics.animation

import kotlin.math.max
import kotlin.math.min

/** Maps the old readout to the new readout without changing glyph proportions.  */
class ReadoutMotion {
    @JvmField var centreX: Float = 0f
    @JvmField var centreY: Float = 0f
    @JvmField var scale: Float = 0f
    @JvmField var sourceCentreX: Float = 0f
    @JvmField var sourceCentreY: Float = 0f
    @JvmField var valid: Boolean = false

    fun update(
        sourceLeft: Float, sourceTop: Float, sourceRight: Float, sourceBottom: Float,
        targetLeft: Float, targetTop: Float, targetRight: Float, targetBottom: Float,
        progress: Float
    ): Boolean {
        this.valid = false
        if (!finiteBounds(sourceLeft, sourceTop, sourceRight, sourceBottom) || !finiteBounds(
                targetLeft,
                targetTop,
                targetRight,
                targetBottom
            ) || !java.lang.Float.isFinite(progress)
        ) {
            return false
        }
        this.sourceCentreX = sourceLeft + (sourceRight - sourceLeft) / 2.0f
        this.sourceCentreY = sourceTop + (sourceBottom - sourceTop) / 2.0f
        val targetCentreX = targetLeft + (targetRight - targetLeft) / 2.0f
        val targetCentreY = targetTop + (targetBottom - targetTop) / 2.0f
        val fraction = max(0.0f, min(1.0f, progress))
        this.centreX = this.sourceCentreX + (targetCentreX - this.sourceCentreX) * fraction
        this.centreY = this.sourceCentreY + (targetCentreY - this.sourceCentreY) * fraction
        val heightRatio = (targetBottom - targetTop) / (sourceBottom - sourceTop)
        this.scale = 1.0f + (heightRatio - 1.0f) * fraction
        this.valid = java.lang.Float.isFinite(this.centreX) && java.lang.Float.isFinite(this.centreY)
                && java.lang.Float.isFinite(this.scale) && this.scale > 0.0f
        return this.valid
    }

    fun mapX(x: Float): Float {
        return this.centreX + (x - this.sourceCentreX) * this.scale
    }

    fun mapY(y: Float): Float {
        return this.centreY + (y - this.sourceCentreY) * this.scale
    }

    companion object {
        private fun finiteBounds(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Boolean {
            return java.lang.Float.isFinite(left) && java.lang.Float.isFinite(top)
                    && java.lang.Float.isFinite(right) && java.lang.Float.isFinite(bottom)
                    && right > left && bottom > top
        }
    }
}
