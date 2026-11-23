package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.max
import kotlin.math.min

/**
 * Allocation-free collapse geometry shared by both control-center hosts.
 */
class BatteryShapeGeometry {
    enum class Stage {
        COMPLETE,
        SHRINKING,
        LINE,
        POINT
    }

    @JvmField var stage: Stage = Stage.COMPLETE
    @JvmField var collapse: Float = 0f
    @JvmField var width: Float = 0f
    @JvmField var height: Float = 0f
    @JvmField var centreX: Float = 0f
    @JvmField var centreY: Float = 0f
    @JvmField var cornerRadius: Float = 0f
    @JvmField var bodyWidth: Float = 0f
    @JvmField var batteryContentAlpha: Float = 1.0f

    init {
        update(
            0.0f,
            BatteryTransferConfig.FULL_CENTRE_X_UNITS,
            BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
            BatteryTransferConfig.FULL_WIDTH_UNITS,
            BatteryTransferConfig.FULL_HEIGHT_UNITS,
            BatteryTransferConfig.FULL_CENTRE_X_UNITS,
            BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
            BatteryTransferConfig.POINT_DIAMETER_UNITS,
            BatteryTransferConfig.POINT_DIAMETER_UNITS
        )
    }

    fun update(
        collapse: Float,
        fullX: Float,
        fullY: Float,
        fullWidth: Float,
        fullHeight: Float,
        endX: Float,
        endY: Float,
        lineWidth: Float,
        pointDiameter: Float
    ) {
        this.collapse = Motion.clamp(collapse)
        val safeEndX: Float = finite(endX, fullX)
        val safeEndY: Float = finite(endY, fullY)
        val safeLineWidth: Float = positive(lineWidth, pointDiameter)
        val safePoint: Float = positive(pointDiameter, 1.0f)
        val horizontalRaw = Motion.clamp(
            this.collapse / BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
        )
        val verticalRaw = Motion.clamp(
            (this.collapse - BatteryTransferConfig.HORIZONTAL_COLLAPSE_END)
                    / (1.0f - BatteryTransferConfig.HORIZONTAL_COLLAPSE_END)
        )
        val horizontal = BatteryTransferPolicy.horizontalShrinkProgress(horizontalRaw)
        val vertical = BatteryTransferPolicy.verticalShrinkProgress(
            verticalRaw
        )
        val safeFullWidth: Float = positive(fullWidth, safeLineWidth)
        val safeFullHeight: Float = positive(fullHeight, safePoint)

        this.width = Motion.mix(safeFullWidth, safeLineWidth, horizontal)
        this.height = Motion.mix(safeFullHeight, safePoint, vertical)
        this.width = Motion.mix(this.width, safePoint, vertical)
        this.centreX = Motion.mix(
            finite(fullX, safeEndX),
            safeEndX,
            Motion.ease(this.collapse)
        )
        // Compose bottom-up compression with a gesture-driven translation of
        // the top cap. Its directed distance to the real ring start never
        // changes sign: it can travel, but never overshoot and correct back.
        val fullTopCap: Float = finite(fullY, safeEndY) - (safeFullHeight - safePoint) / 2.0f
        var verticalStartCap = Motion.mix(
            fullTopCap, safeEndY,
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
        )
        // Leave enough upward shrink for the lower cap even when the whole
        // shape is travelling down towards an endpoint below the source row.
        verticalStartCap = max(
            verticalStartCap,
            safeEndY - max(0f, safeFullHeight - safePoint) / 2.0f
        )
        verticalStartCap = max(
            min(fullTopCap, safeEndY),
            min(max(fullTopCap, safeEndY), verticalStartCap)
        )
        val capY = if (verticalRaw > 0)
            Motion.mix(verticalStartCap, safeEndY, vertical)
        else
            Motion.mix(fullTopCap, verticalStartCap, horizontal)
        this.centreY = capY + (this.height - safePoint) / 2.0f

        if (this.collapse >= 1.0f) {
            this.centreX = safeEndX
            this.centreY = safeEndY
            this.width = safePoint
            this.height = safePoint
            this.stage = Stage.POINT
        } else if (this.collapse <= 0.0f) {
            this.stage = Stage.COMPLETE
        } else if (this.collapse >= BatteryTransferConfig.HORIZONTAL_COLLAPSE_END) {
            this.stage = Stage.LINE
        } else {
            this.stage = Stage.SHRINKING
        }

        this.bodyWidth = Motion.mix(
            safeFullWidth * BatteryTransferConfig.BODY_WIDTH
                    / BatteryTransferConfig.SVG_WIDTH, safeLineWidth, horizontal
        )
        this.bodyWidth = Motion.mix(this.bodyWidth, safePoint, vertical)
        // Rebuild the corners in physical units. Scaling the whole SVG would
        // keep a tall vertical radius after its horizontal radius had collapsed.
        val radius = BatteryTransferConfig.BODY_RADIUS * min(
            this.bodyWidth / BatteryTransferConfig.BODY_WIDTH,
            this.height / BatteryTransferConfig.SVG_HEIGHT
        )
        // Finish rounding during horizontal collapse. The vertical stage then
        // brings two semicircles of the final point together at a fixed radius.
        this.cornerRadius = Motion.mix(
            radius,
            min(this.bodyWidth, this.height) / 2.0f, horizontal
        )
        this.batteryContentAlpha = 1.0f - Motion.ease(
            this.collapse / BatteryTransferConfig.BATTERY_CONTENT_FADE_END
        )
    }

    companion object {
        private fun positive(value: Float, fallback: Float): Float {
            return if (java.lang.Float.isFinite(value) && value > 0.0f)
                value
            else max(0.0f, finite(fallback, 1.0f))
        }

        private fun finite(value: Float, fallback: Float): Float {
            return if (java.lang.Float.isFinite(value)) value else finite(fallback, 0.0f)
        }
    }
}
