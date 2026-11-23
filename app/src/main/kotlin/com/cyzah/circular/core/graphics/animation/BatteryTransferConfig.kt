package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.max
import kotlin.math.min

/**
 * Central visual and timing constants for the control-center battery transfer.
 * Values use the existing 120-unit icon coordinate system unless named pixels.
 */
object BatteryTransferConfig {
    const val HORIZONTAL_COLLAPSE_END: Float = 0.65f

    /** Larger values spend more of the shared close timeline in horizontal shrink.  */
    const val BATTERY_HORIZONTAL_CLOSE_PROGRESS: Float = 0.90f
    const val ENDPOINT_HANDOFF_START: Float = 0.90f
    const val BATTERY_CONTENT_FADE_END: Float = 0.20f

    const val FULL_WIDTH_UNITS: Float = 112.0f
    const val FULL_HEIGHT_UNITS: Float = 53.1667f
    const val FULL_CENTRE_X_UNITS: Float = 60.0f
    const val FULL_CENTRE_Y_UNITS: Float = 53.0f
    @JvmField val CONTROL_CENTER_X_NUDGE_UNITS: Float = -9.0f
    const val CONTROL_CENTER_BATTERY_LEFT_SHIFT_DP: Float = 2.0f
    const val NON_OUTSIDE_BATTERY_LEFT_SHIFT_DP: Float = 3.0f
    const val POINT_DIAMETER_UNITS: Float = 12.0f
    const val CONTROL_CENTER_BATTERY_SCALE: Float = 1.13f
    const val CONTROL_CENTER_NUMBER_UNITS: Float = 49.0f
    const val OUTSIDE_NUMBER_UNITS: Float = 56.0f
    const val CONTROL_CENTER_WIFI_BATTERY_GAP_DP: Float = 6.0f
    const val OUTSIDE_BATTERY_GROUP_LEFT_SHIFT_DP: Float = 4.0f
    const val OUTSIDE_NUMBER_RIGHT_SHIFT_DP: Float = 7.0f
    const val BATTERY_COMPONENT_GAP_DP: Float = 3.0f
    @JvmField val OUTSIDE_NUMBER_LEFT_NUDGE_UNITS: Float = -20.0f
    const val COMBINED_NUMBER_GAP_REDUCTION_UNITS: Float = 2.0f
    const val OUTSIDE_NUMBER_MAX_FONT_WEIGHT: Int = 600
    @JvmField val BATTERY_LABEL_GAP_UNITS: Float = 16.0f / 3.0f

    /** Existing preview morph duration.  */
    const val PREVIEW_EXPANSION_DURATION_MS: Long = 180L

    const val SVG_WIDTH: Float = 198.44f
    const val SVG_HEIGHT: Float = 94.2f

    // Fit the new outline into the existing frame so layout and readouts stay put.
    const val OUTLINE_WIDTH: Float = 175.0f
    const val OUTLINE_HEIGHT: Float = 83.0f
    @JvmField val OUTLINE_SCALE_X: Float = SVG_WIDTH / OUTLINE_WIDTH
    @JvmField val OUTLINE_SCALE_Y: Float = SVG_HEIGHT / OUTLINE_HEIGHT
    const val BODY_X: Float = 0.0f
    const val BODY_Y: Float = 0.0f
    @JvmField val BODY_WIDTH: Float = 159.96f * OUTLINE_SCALE_X
    const val BODY_HEIGHT: Float = 94.2f

    /** Radius used by the existing opening and closing animations.  */
    const val BODY_RADIUS: Float = 38.0f
    @JvmField val BODY_CORNER_X: Float = 38.89f * OUTLINE_SCALE_X
    @JvmField val BODY_CORNER_Y: Float = 38.9f * OUTLINE_SCALE_Y
    @JvmField val BODY_RIGHT_CURVE_X: Float = 121.07f * OUTLINE_SCALE_X
    @JvmField val BODY_RIGHT_CONTROL_X: Float = 149.56f * OUTLINE_SCALE_X
    @JvmField val BODY_LEFT_CONTROL_X: Float = 10.4f * OUTLINE_SCALE_X
    @JvmField val BODY_TOP_CONTROL_Y: Float = 10.4f * OUTLINE_SCALE_Y
    @JvmField val BODY_BOTTOM_CONTROL_Y: Float = 72.6f * OUTLINE_SCALE_Y
    @JvmField val TERMINAL_START_X: Float = 165.0f * OUTLINE_SCALE_X
    const val TERMINAL_END_X: Float = 198.44f
    @JvmField val TERMINAL_CENTRE_Y: Float = 41.5f * OUTLINE_SCALE_Y
    @JvmField val TERMINAL_HALF_HEIGHT: Float = 12.5f * OUTLINE_SCALE_Y
    @JvmField val TERMINAL_BOTTOM_Y: Float = 54.0f * OUTLINE_SCALE_Y
    @JvmField val TERMINAL_TOP_Y: Float = 29.0f * OUTLINE_SCALE_Y
    private val CHARGED_SPAN_SVG = BODY_WIDTH + TERMINAL_END_X - TERMINAL_START_X
    const val OUTSIDE_SVG_WIDTH: Float = 271.2333f
    @JvmField val OUTSIDE_LAYOUT_TOTAL_WIDTH_UNITS: Float = FULL_WIDTH_UNITS * OUTSIDE_SVG_WIDTH / SVG_WIDTH
    @JvmField val OUTSIDE_LAYOUT_RIGHT_UNITS: Float = FULL_CENTRE_X_UNITS + (FULL_WIDTH_UNITS / 2.0f)
    @JvmField val OUTSIDE_BATTERY_CENTRE_X_UNITS: Float = (OUTSIDE_LAYOUT_RIGHT_UNITS
            - OUTSIDE_LAYOUT_TOTAL_WIDTH_UNITS
            + (FULL_WIDTH_UNITS / 2.0f))
    const val NUMBER_INSIDE_X_SVG: Float = 90.6804f
    const val NUMBER_OUTSIDE_X_SVG: Float = 204.0f
    @JvmField val NUMBER_FONT_SIZE_SVG: Float = (CONTROL_CENTER_NUMBER_UNITS
            * SVG_HEIGHT
            / FULL_HEIGHT_UNITS)
    @JvmField val OUTSIDE_NUMBER_FONT_SIZE_SVG: Float = (OUTSIDE_NUMBER_UNITS
            * SVG_HEIGHT
            / FULL_HEIGHT_UNITS)

    const val EMPTY_COLOR_R: Float = 0.69803923f
    const val EMPTY_COLOR_G: Float = 0.69803923f
    const val EMPTY_COLOR_B: Float = 0.69803923f

    const val USE_BLACK_CHARGED_AREA: Boolean = false
    const val BLACK_CHARGED_AREA: Int = -0x1000000
    const val CONTROL_CENTER_CHARGE_SYMBOL: Boolean = false

    const val PATH_DURATION_MS: Long = 650L
    const val RESTORE_DURATION_MS: Long = 320L
    const val FRAME_DELAY_MS: Long = 16L
    const val HANDOFF_MAX: Float = 0.0001f
    const val CLOSED_MORPH_MAX: Float = 0.002f
    const val OPEN_CONTROL_MIN: Float = 0.95f
    const val OPEN_MORPH_MIN: Float = 0.95f

    @JvmStatic fun chargedAreaTint(themedTint: Int): Int {
        return if (USE_BLACK_CHARGED_AREA) BLACK_CHARGED_AREA else themedTint
    }

    @JvmStatic fun chargeSymbolEnabled(): Boolean {
        return CONTROL_CENTER_CHARGE_SYMBOL
    }

    @JvmStatic fun numberTextSizeSvg(placement: BatteryNumberPlacement?): Float {
        return if (placement == BatteryNumberPlacement.OUTSIDE)
            OUTSIDE_NUMBER_FONT_SIZE_SVG
        else
            NUMBER_FONT_SIZE_SVG
    }

    @JvmStatic fun outsideNumberFontWeight(percentWeight: Int): Int {
        return min(
            OUTSIDE_NUMBER_MAX_FONT_WEIGHT,
            BatteryHeader.fontWeight(percentWeight)
        )
    }

    @JvmStatic fun chargedBodyRightSvg(battery: Int): Float {
        val safeBattery = max(0, min(100, battery))
        val chargedSpan = CHARGED_SPAN_SVG * safeBattery / 100.0f
        return BODY_X + min(BODY_WIDTH, chargedSpan)
    }

    @JvmStatic fun chargedTerminalRightSvg(battery: Int): Float {
        val safeBattery = max(0, min(100, battery))
        val terminalCharge =
            (CHARGED_SPAN_SVG * safeBattery / 100.0f) - BODY_WIDTH
        return (TERMINAL_START_X
                + max(
            0.0f,
            min(
                TERMINAL_END_X - TERMINAL_START_X,
                terminalCharge
            )
        ))
    }
}
