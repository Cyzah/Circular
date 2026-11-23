package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object RingGapPolicy {
    private const val MIN_LAYER_WEIGHT = 0.96f
    private const val LAYER_SIDE_REDUCTION = 3.0f
    private const val SINGLE_BASE_HALF_ANGLE = 57.5f
    private const val SINGLE_CAP_DOT_CLEARANCE = 1.0f

    @JvmStatic fun lowerGapWidth(style: DualStyle?, weightScale: Float, centerContentWidth: Float): Float {
        val count = if (style == DualStyle.GROUPED) 4 else if (style == DualStyle.LAYERED) 3 else 0
        if (count == 0) {
            return max(0.0f, centerContentWidth)
        }
        val radius = SignalSpacing.radius(weightScale)
        val firstX = if (style == DualStyle.LAYERED)
            SignalSpacing.innerX(0, weightScale)
        else
            SignalSpacing.ringX(0, weightScale)
        val lastX = if (style == DualStyle.LAYERED)
            SignalSpacing.innerX(count - 1, weightScale)
        else
            SignalSpacing.ringX(count - 1, weightScale)
        val signalWidth = (lastX - firstX) + (2.0f * radius)
        return max(centerContentWidth, signalWidth)
    }

    @JvmStatic fun topGapWidth(
        showBatteryNumber: Boolean, swapContents: Boolean,
        batteryNumberWidth: Float, networkHeaderWidth: Float
    ): Float {
        if (swapContents) {
            return max(0.0f, networkHeaderWidth)
        }
        return if (showBatteryNumber) max(0.0f, batteryNumberWidth) else 0.0f
    }

    @JvmStatic fun topHalfAngle(
        showBatteryNumber: Boolean, swapContents: Boolean,
        batteryNumberWidth: Float, networkHeaderWidth: Float
    ): Float {
        val width = topGapWidth(showBatteryNumber, swapContents, batteryNumberWidth, networkHeaderWidth)
        return BatteryHeader.halfGap(width, 1.2f)
    }

    @JvmStatic fun lowerHalfAngle(style: DualStyle?, weightScale: Float): Float {
        if (style != DualStyle.GROUPED && style != DualStyle.LAYERED) {
            return 55.0f
        }
        val currentExtra = BatteryHeader.halfGap(
            lowerGapWidth(style, weightScale, 0.0f),
            weightScale
        )
        val referenceWeight = if (style == DualStyle.LAYERED) MIN_LAYER_WEIGHT else 1.2f
        val defaultExtra = BatteryHeader.halfGap(
            lowerGapWidth(style, referenceWeight, 0.0f),
            referenceWeight
        )
        val baseHalf = if (style == DualStyle.LAYERED) layerBaseHalfAngle() else 55.0f
        return baseHalf + ((currentExtra - defaultExtra) * 0.5f)
    }

    /** Keeps round battery-ring caps clear of the outer single-card dots.  */
    @JvmStatic fun singleHalfAngle(weightScale: Float): Float {
        val weight = max(0.01f, weightScale)
        val dotHalfAngle = Math.toDegrees(
            atan2(
                (60.0f - SignalSpacing.ringX(0, weight)).toDouble(),
                (SignalSpacing.ringY(0, weight) - 58.0f).toDouble()
            )
        ).toFloat()
        val minimumDistance = (SignalSpacing.radius(weight)
                + (3.75f * weight) + SINGLE_CAP_DOT_CLEARANCE)
        // Dot centres and cap centres share the 50-unit ring. Their chord
        // must accommodate both radii plus the visible clearance.
        val separationAngle = Math.toDegrees(
            2.0 * asin(
                min(1.0f, minimumDistance / 100.0f).toDouble()
            )
        ).toFloat()
        return max(SINGLE_BASE_HALF_ANGLE, dotHalfAngle + separationAngle)
    }

    @JvmStatic fun layerSideClearance(weightScale: Float, halfAngle: Float): Float {
        val pointEdge = SignalSpacing.outerX(0, weightScale) - SignalSpacing.radius(weightScale)
        val ringEdge = 60.0f - (50.0f * sin(Math.toRadians(halfAngle.toDouble())).toFloat())
        return pointEdge - ringEdge
    }

    @JvmStatic fun technologyCentreY(style: DualStyle?): Float {
        return if (style == DualStyle.GROUPED) 60.0f else 57.0f
    }

    @JvmStatic fun technologyCentreY(style: DualStyle?, showBatteryNumber: Boolean): Float {
        if (style == DualStyle.GROUPED) {
            return if (showBatteryNumber) 60.0f else 58.0f
        }
        if (style == DualStyle.LAYERED) {
            return if (showBatteryNumber) 57.0f else 55.0f
        }
        return 57.0f
    }

    @JvmStatic fun wifiCentreY(style: DualStyle?, showBatteryNumber: Boolean, normalY: Float): Float {
        return if (style == DualStyle.GROUPED && !showBatteryNumber) 58.0f else normalY
    }

    private fun layerBaseHalfAngle(): Float {
        val baselineRingEdge = 60.0f - (50.0f * sin(Math.toRadians(55.0)).toFloat())
        val targetRingEdge = baselineRingEdge + LAYER_SIDE_REDUCTION
        return Math.toDegrees(asin(((60.0f - targetRingEdge) / 50.0f).toDouble())).toFloat()
    }
}
