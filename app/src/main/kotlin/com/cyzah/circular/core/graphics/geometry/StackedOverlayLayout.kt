package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.max

object StackedOverlayLayout {
    @JvmStatic fun labelFitWidth(left: Float, right: Float): Float {
        return max(0.0f, right - left)
    }

    @JvmStatic @JvmOverloads
    fun fitsLabels(
        availableWidth: Float,
        textWidth: Float,
        density: Float,
        placement: BatteryNumberPlacement? = BatteryNumberPlacement.HIDDEN,
        singleRow: Boolean = false
    ): Boolean {
        if (textWidth <= 0.0f) {
            return false
        }
        val requiredWidth = if (singleRow)
            StackedSignals.Companion.singleRowFillWidthPx(textWidth, density)
        else
            StackedSignals.Companion.fillWidthPx(textWidth, density, placement)
        return availableWidth >= requiredWidth
    }

    @JvmStatic fun allowsLabelFitFallback(
        placement: BatteryNumberPlacement?
    ): Boolean {
        return placement != BatteryNumberPlacement.OUTSIDE
    }

    @JvmStatic @JvmOverloads
    fun barLeft(
        rtl: Boolean,
        left: Float,
        right: Float,
        density: Float,
        labelsVisible: Boolean,
        centreWhenHidden: Boolean = true,
        placement: BatteryNumberPlacement? = BatteryNumberPlacement.HIDDEN
    ): Float {
        if (!labelsVisible && centreWhenHidden) {
            val available = right - left
            return (left
                    + ((available
                    - (StackedSignals.Companion.BLOCK_SPAN_DP * density))
                    / 2.0f))
        }
        return if (rtl)
            right - (StackedSignals.Companion.BAR_EDGE_DP * density)
        else
            left + (StackedSignals.Companion.BAR_INSET_DP * -density)
    }

    @JvmStatic fun labelLeft(
        rtl: Boolean,
        barLeft: Float,
        textWidth: Float,
        density: Float
    ): Float {
        if (rtl) {
            return (barLeft
                    + ((StackedSignals.Companion.BAR_INSET_DP
                    - StackedSignals.Companion.LEFT_PAD_DP
                    - StackedSignals.Companion.TEXT_GAP_DP) * density)
                    - textWidth)
        }
        return (barLeft
                + ((StackedSignals.Companion.BAR_EDGE_DP
                + StackedSignals.Companion.LEFT_PAD_DP
                + StackedSignals.Companion.TEXT_GAP_DP) * density))
    }

    @JvmStatic fun technologyProgress(expansion: Float): Float {
        val progress = Motion.clamp(expansion)
        // Expansion is 1 for the row and 0 for the merged icon. Cubing
        // closing progress delays text convergence without keeping timeline state.
        val closing = 1.0f - progress
        return 1.0f - (closing * closing * closing)
    }

    @JvmStatic fun technologyXProgress(expansion: Float): Float {
        val closing = 1.0f - Motion.clamp(expansion)
        // Text needs a shorter hold than the low signal rows; keep its vertical
        // placement and font-size channels on technologyProgress(expansion).
        return technologyProgress(SignalMotionPolicy.signalProgress(1.0f - closing * closing))
    }

    @JvmStatic fun signalXProgress(
        morph: Float,
        placement: BatteryNumberPlacement?
    ): Float {
        val progress = Motion.clamp(morph)
        // Closing runs from 1 to 0. Delay horizontal convergence until the
        // battery has narrowed; number placement must not accelerate this path.
        val closing = 1.0f - progress
        val square = closing * closing
        return 1.0f - square * square
    }
}
