package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.BatteryHeader
import kotlin.math.max
import kotlin.math.min

object DuoLayout {
    private const val DEFAULT_WEIGHT = 1.2f
    private const val DOT_EDGE_GAP = 4.4f
    private const val ROW_WIDTH = 84.0f
    private const val RING_CENTRE_X = 60.0f
    private const val CENTRE_ROWS_Y = 58.0f
    private const val DOCKED_ROWS_Y = 95.5f
    private val SIGNAL_OFFSET_Y = -1.0f

    @JvmStatic @JvmOverloads
    fun dotX(dot: Int, weightScale: Float = DEFAULT_WEIGHT): Float {
        return SignalSpacing.rowCentre(
            dot,
            4,
            RING_CENTRE_X,
            weightScale,
            DOT_EDGE_GAP,
            ROW_WIDTH
        )
    }

    @JvmStatic @JvmOverloads
    fun dotY(row: Int, wifiProgress: Float, weightScale: Float = DEFAULT_WEIGHT): Float {
        val safeRow = max(0, min(1, row))
        val progress = clamp(wifiProgress)
        val centreRow = SignalSpacing.pairCentre(
            safeRow,
            CENTRE_ROWS_Y,
            weightScale,
            DOT_EDGE_GAP
        )
        val dockedRow = SignalSpacing.pairCentre(
            safeRow,
            DOCKED_ROWS_Y,
            weightScale,
            DOT_EDGE_GAP
        )
        return (centreRow + ((dockedRow - centreRow) * progress)
                + SIGNAL_OFFSET_Y)
    }

    @JvmStatic fun wifiAlpha(wifiProgress: Float): Float {
        return clamp(wifiProgress)
    }

    @JvmStatic fun technologyAlpha(wifiProgress: Float): Float {
        return 1.0f - clamp(wifiProgress)
    }

    @JvmStatic fun technologyAlpha(wifiProgress: Float, dataEnabled: Boolean): Float {
        return if (dataEnabled) technologyAlpha(wifiProgress) else 0.0f
    }

    @JvmStatic fun batteryMovesBelow(wifiConnected: Boolean, dataEnabled: Boolean): Boolean {
        return !wifiConnected && !dataEnabled
    }

    @JvmStatic fun dotAlpha(available: Boolean, level: Int, dot: Int): Int {
        return if (available && dot < max(0, min(4, level))) 255 else 48
    }

    @JvmStatic fun batteryCentreY(): Float {
        return 14.0f
    }

    @JvmStatic fun batteryCentreY(below: Boolean): Float {
        return if (below) 102.0f else batteryCentreY()
    }

    @JvmStatic fun wifiCentreY(): Float {
        return 55.0f
    }

    @JvmStatic fun wifiCentreX(): Float {
        return RING_CENTRE_X
    }

    @JvmStatic fun technologyBaseline(): Float {
        return 112.0f
    }

    @JvmStatic fun fitBatteryReadout(measuredWidth: Float, measuredHeight: Float, maxWidth: Float): Float {
        return BatteryHeader.fit(measuredWidth, measuredHeight, maxWidth, 44.0f)
    }

    @JvmStatic fun batteryReadoutWidth(digitsWidth: Float, charging: Boolean): Float {
        return digitsWidth
    }

    @JvmStatic fun showBatteryDigits(showPercent: Boolean, batteryKnown: Boolean): Boolean {
        return showPercent && batteryKnown
    }

    @JvmStatic fun gapHalfAngle(contentWidth: Float, weightScale: Float): Float {
        return BatteryHeader.halfGap(max(0.0f, contentWidth), weightScale)
    }

    @JvmStatic fun bottomContentWidth(technologyWidth: Float, dockedSignalsWidth: Float, wifiProgress: Float): Float {
        val technologyBase = technologyWidth + 9.0f
        val dockedBase = dockedSignalsWidth + 9.0f
        return technologyBase + ((dockedBase - technologyBase) * clamp(wifiProgress))
    }

    @JvmStatic fun sideArcSweep(topHalfAngle: Float, bottomHalfAngle: Float): Float {
        return max(0.0f, 180.0f - topHalfAngle - bottomHalfAngle)
    }

    private fun clamp(value: Float): Float {
        return if (java.lang.Float.isFinite(value)) max(0.0f, min(1.0f, value)) else 0.0f
    }
}
