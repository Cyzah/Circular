package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.max

/**
 * Pure layout values consumed by the SVG battery renderer.
 */
object BatteryShapeLayout {
    @JvmStatic fun transferPointDiameterUnits(ringStroke: Float, ringUnit: Float, shapeUnit: Float): Float {
        if (!java.lang.Float.isFinite(ringStroke) || ringStroke <= 0.0f || !java.lang.Float.isFinite(ringUnit) || ringUnit <= 0.0f || !java.lang.Float.isFinite(
                shapeUnit
            ) || shapeUnit <= 0.0f
        ) {
            return BatteryTransferConfig.POINT_DIAMETER_UNITS
        }
        return ringStroke * ringUnit / shapeUnit
    }

    @JvmStatic fun widthUnits(placement: BatteryNumberPlacement?): Float {
        return BatteryTransferConfig.FULL_WIDTH_UNITS
    }

    @JvmStatic fun minimumPx(configuredMinimumPx: Float): Float {
        if (!java.lang.Float.isFinite(configuredMinimumPx) || configuredMinimumPx <= 0.0f) {
            return 0.0f
        }
        return (configuredMinimumPx
                * BatteryTransferConfig.CONTROL_CENTER_BATTERY_SCALE)
    }

    @JvmStatic fun frameMinimumPx(configuredMinimumPx: Float): Float {
        if (!java.lang.Float.isFinite(configuredMinimumPx) || configuredMinimumPx <= 0.0f) {
            return 0.0f
        }
        return configuredMinimumPx
    }

    @JvmStatic fun leftOverflowUnits(placement: BatteryNumberPlacement?): Float {
        val left = centreXUnits(placement) - (widthUnits(placement) / 2.0f)
        return max(0.0f, -left)
    }

    @JvmStatic fun rowReservePx(
        configuredMinimumPx: Float,
        placement: BatteryNumberPlacement?,
        density: Float
    ): Float {
        val gap = max(
            0.0f,
            BatteryTransferConfig.BATTERY_LABEL_GAP_UNITS
                    - BatteryTransferConfig.COMBINED_NUMBER_GAP_REDUCTION_UNITS
        )
        var reserve = ((leftOverflowUnits(placement) + gap)
                * minimumPx(configuredMinimumPx)
                / 120.0f)
        if (java.lang.Float.isFinite(density)
            && density > 0.0f
        ) {
            reserve += BatteryTransferConfig.BATTERY_COMPONENT_GAP_DP * density
            reserve += controlCenterBatteryLeftShiftPx(density)
            reserve += nonOutsideBatteryLeftShiftPx(placement, density)
            if (placement == BatteryNumberPlacement.OUTSIDE) {
                reserve += outsideGroupLeftShiftPx(density)
                reserve += outsideNumberRightShiftPx(density)
            }
        }
        return reserve
    }

    @JvmStatic fun avoidWifiOverlapShiftPx(
        batteryLeft: Float,
        wifiRight: Float,
        minimumGapPx: Float
    ): Float {
        if (!java.lang.Float.isFinite(batteryLeft) || !java.lang.Float.isFinite(wifiRight) || !java.lang.Float.isFinite(minimumGapPx)) {
            return 0.0f
        }
        return max(
            0.0f,
            wifiRight + max(0.0f, minimumGapPx) - batteryLeft
        )
    }

    @JvmStatic fun outsideGroupLeftShiftPx(density: Float): Float {
        if (!java.lang.Float.isFinite(density) || density <= 0.0f) {
            return 0.0f
        }
        return BatteryTransferConfig.OUTSIDE_BATTERY_GROUP_LEFT_SHIFT_DP * density
    }

    @JvmStatic fun nonOutsideBatteryLeftShiftPx(
        placement: BatteryNumberPlacement?,
        density: Float
    ): Float {
        if (placement == BatteryNumberPlacement.OUTSIDE || !java.lang.Float.isFinite(density) || density <= 0.0f) {
            return 0.0f
        }
        return (BatteryTransferConfig.NON_OUTSIDE_BATTERY_LEFT_SHIFT_DP
                * density)
    }

    @JvmStatic fun controlCenterBatteryLeftShiftPx(density: Float): Float {
        if (!java.lang.Float.isFinite(density) || density <= 0.0f) {
            return 0.0f
        }
        return (BatteryTransferConfig.CONTROL_CENTER_BATTERY_LEFT_SHIFT_DP
                * density)
    }

    @JvmStatic fun outsideNumberRightShiftPx(density: Float): Float {
        if (!java.lang.Float.isFinite(density) || density <= 0.0f) {
            return 0.0f
        }
        return (BatteryTransferConfig.OUTSIDE_NUMBER_RIGHT_SHIFT_DP
                * density)
    }

    @JvmStatic fun outsideNumberOverflowUnits(
        actualWidthSvg: Float,
        referenceWidthSvg: Float
    ): Float {
        if (!java.lang.Float.isFinite(actualWidthSvg) || !java.lang.Float.isFinite(referenceWidthSvg) || referenceWidthSvg <= 0.0f) {
            return 0.0f
        }
        return (max(0.0f, actualWidthSvg - referenceWidthSvg) * BatteryTransferConfig.FULL_WIDTH_UNITS
                / BatteryTransferConfig.SVG_WIDTH)
    }

    @JvmStatic fun outsideNumberBatteryShiftUnits(
        widestNumberSvg: Float,
        currentNumberSvg: Float
    ): Float {
        if (!java.lang.Float.isFinite(widestNumberSvg)
            || !java.lang.Float.isFinite(currentNumberSvg)
        ) {
            return 0.0f
        }
        return (max(0.0f, widestNumberSvg - currentNumberSvg) * BatteryTransferConfig.FULL_WIDTH_UNITS
                / BatteryTransferConfig.SVG_WIDTH)
    }

    @JvmStatic fun outsideNumberOverflowReservePx(
        configuredMinimumPx: Float,
        overflowUnits: Float
    ): Float {
        if (!java.lang.Float.isFinite(overflowUnits) || overflowUnits <= 0.0f) {
            return 0.0f
        }
        return (overflowUnits
                * minimumPx(configuredMinimumPx)
                / 120.0f)
    }

    @JvmStatic fun wifiBatteryMinimumGapPx(density: Float): Float {
        if (!java.lang.Float.isFinite(density) || density <= 0.0f) {
            return 0.0f
        }
        return BatteryTransferConfig.CONTROL_CENTER_WIFI_BATTERY_GAP_DP * density
    }

    @JvmStatic fun fullBatteryLeftPx(
        originX: Float,
        minimumPx: Float,
        frameMinimumPx: Float,
        canonicalMinimumPx: Float,
        placement: BatteryNumberPlacement?,
        density: Float
    ): Float {
        if (!java.lang.Float.isFinite(originX) || !java.lang.Float.isFinite(minimumPx) || !java.lang.Float.isFinite(frameMinimumPx) || !java.lang.Float.isFinite(
                canonicalMinimumPx
            ) || canonicalMinimumPx <= 0.0f
        ) {
            return Float.NaN
        }
        val unit = canonicalMinimumPx / 120.0f
        val shapeOriginX = canonicalOriginX(
            originX,
            minimumPx,
            frameMinimumPx,
            false
        )
        var fullCentreX = centreXUnits(placement)
        fullCentreX -= controlCenterBatteryLeftShiftPx(density) / unit
        fullCentreX -= nonOutsideBatteryLeftShiftPx(placement, density) / unit
        if (placement == BatteryNumberPlacement.OUTSIDE) {
            fullCentreX -= outsideGroupLeftShiftPx(density) / unit
            fullCentreX -= outsideNumberRightShiftPx(density) / unit
        }
        return (shapeOriginX
                + ((fullCentreX - (bodyWidthUnits(placement) / 2.0f))
                * unit))
    }

    @JvmStatic fun centreXUnits(placement: BatteryNumberPlacement?): Float {
        val centre = if (placement == BatteryNumberPlacement.OUTSIDE)
            BatteryTransferConfig.OUTSIDE_BATTERY_CENTRE_X_UNITS
        else
            BatteryTransferConfig.FULL_CENTRE_X_UNITS
        return (centre
                + BatteryTransferConfig.CONTROL_CENTER_X_NUDGE_UNITS
                + (if (placement == BatteryNumberPlacement.OUTSIDE)
            BatteryTransferConfig.OUTSIDE_NUMBER_LEFT_NUDGE_UNITS
        else
            0.0f))
    }

    @JvmStatic fun canonicalOriginX(
        activeOriginPx: Float,
        activeMinimumPx: Float,
        canonicalMinimumPx: Float,
        rtl: Boolean
    ): Float {
        if (rtl) {
            return activeOriginPx
        }
        return activeOriginPx + activeMinimumPx - canonicalMinimumPx
    }

    @JvmStatic fun canonicalOriginY(
        activeOriginPx: Float,
        activeMinimumPx: Float,
        canonicalMinimumPx: Float
    ): Float {
        return (activeOriginPx
                + ((activeMinimumPx - canonicalMinimumPx) / 2.0f))
    }

    @JvmStatic fun clampBatteryCentreXToRightEdge(
        centreUnits: Float,
        shapeOriginPx: Float,
        additionalOffsetPx: Float,
        widthUnits: Float,
        unitPx: Float,
        maxRightPx: Float
    ): Float {
        if (!java.lang.Float.isFinite(centreUnits) || !java.lang.Float.isFinite(shapeOriginPx) || !java.lang.Float.isFinite(additionalOffsetPx) || !java.lang.Float.isFinite(
                widthUnits
            ) || !java.lang.Float.isFinite(unitPx) || !java.lang.Float.isFinite(maxRightPx) || unitPx <= 0.0f
        ) {
            return centreUnits
        }
        val rightPx = (shapeOriginPx
                + additionalOffsetPx
                + ((centreUnits + (widthUnits / 2.0f)) * unitPx))
        return (centreUnits
                - (max(0.0f, rightPx - maxRightPx) / unitPx))
    }

    @JvmStatic fun bodyWidthUnits(placement: BatteryNumberPlacement?): Float {
        return (widthUnits(placement)
                * BatteryTransferConfig.BODY_WIDTH
                / BatteryTransferConfig.SVG_WIDTH)
    }

    @JvmStatic fun bodyHeightUnits(placement: BatteryNumberPlacement?): Float {
        return (widthUnits(placement)
                * BatteryTransferConfig.BODY_HEIGHT
                / BatteryTransferConfig.SVG_WIDTH)
    }

    @JvmStatic fun chargedClipLeftSvg(): Float {
        return 0.0f
    }

    @JvmStatic fun chargedClipTopSvg(): Float {
        return 0.0f
    }

    @JvmStatic fun chargedClipBottomSvg(): Float {
        return BatteryTransferConfig.SVG_HEIGHT
    }

    @JvmStatic fun clipsChargedArea(battery: Int): Boolean {
        return battery > 0 && battery < 100
    }
}
