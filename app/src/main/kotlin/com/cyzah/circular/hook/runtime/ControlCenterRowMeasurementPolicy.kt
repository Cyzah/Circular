package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.max

/**
 * Pure width formulas for control-center row components.
 */
object ControlCenterRowMeasurementPolicy {
    @JvmStatic fun aggregateNativeVisibility(
        anyVisible: Boolean,
        unavailable: Boolean
    ): NativeVisibility {
        if (anyVisible) {
            return NativeVisibility.VISIBLE
        }
        return if (unavailable)
            NativeVisibility.UNAVAILABLE
        else
            NativeVisibility.HIDDEN
    }

    @JvmStatic fun applyNativeVisibility(
        input: ControlCenterRowInput?,
        wifi: NativeVisibility?,
        cellular: NativeVisibility?
    ): Boolean {
        if (input == null || wifi == null || cellular == null || wifi == NativeVisibility.UNAVAILABLE || cellular == NativeVisibility.UNAVAILABLE) {
            return false
        }
        input.setVisible(
            ControlCenterRowComponent.WIFI,
            wifi == NativeVisibility.VISIBLE
        )
        input.setVisible(
            ControlCenterRowComponent.CELLULAR,
            cellular == NativeVisibility.VISIBLE
        )
        return true
    }

    @JvmStatic fun batteryWidth(
        configuredMinimumPx: Float,
        placement: BatteryNumberPlacement?,
        outsideNumberSvgWidth: Float
    ): Float {
        val minimumPx = BatteryShapeLayout.minimumPx(
            configuredMinimumPx
        )
        if (minimumPx <= 0.0f) {
            return 0.0f
        }
        val batteryWidth = (BatteryShapeLayout.widthUnits(placement)
                * minimumPx
                / 120.0f)
        if (placement != BatteryNumberPlacement.OUTSIDE) {
            return batteryWidth
        }
        return (batteryWidth
                + outsideNumberWidthPx(
            configuredMinimumPx,
            outsideNumberSvgWidth
        ))
    }

    @JvmStatic fun outsideNumberWidthPx(
        configuredMinimumPx: Float,
        measuredSvgWidth: Float
    ): Float {
        val minimumPx = BatteryShapeLayout.minimumPx(
            configuredMinimumPx
        )
        if (minimumPx <= 0.0f || !java.lang.Float.isFinite(measuredSvgWidth) || measuredSvgWidth < 0.0f) {
            return 0.0f
        }
        val glyphUnits = (measuredSvgWidth
                * BatteryTransferConfig.FULL_WIDTH_UNITS
                / BatteryTransferConfig.SVG_WIDTH)
        return ((glyphUnits
                + BatteryTransferConfig.BATTERY_LABEL_GAP_UNITS)
                * minimumPx
                / 120.0f)
    }

    @JvmStatic fun occupiedWidth(
        content: Float,
        paddingLeft: Float,
        paddingRight: Float
    ): Float {
        if (!validLength(content) || !validLength(paddingLeft) || !validLength(paddingRight)) {
            return 0.0f
        }
        return content + paddingLeft + paddingRight
    }

    @JvmStatic fun cellularWidth(
        signalWidth: Float,
        internalGap: Float,
        cardOneTextWidth: Float,
        cardTwoTextWidth: Float
    ): Float {
        if (!validLength(signalWidth) || !validLength(internalGap) || !validLength(cardOneTextWidth) || !validLength(
                cardTwoTextWidth
            )
        ) {
            return 0.0f
        }
        return (signalWidth
                + internalGap
                + max(cardOneTextWidth, cardTwoTextWidth))
    }

    private fun validLength(value: Float): Boolean {
        return java.lang.Float.isFinite(value) && value >= 0.0f
    }

    enum class NativeVisibility {
        VISIBLE,
        HIDDEN,
        UNAVAILABLE
    }
}
