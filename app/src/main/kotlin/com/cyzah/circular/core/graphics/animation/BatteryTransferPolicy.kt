package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.domain.settings.DisplayPolicy

object BatteryTransferPolicy {
    @JvmStatic fun gestureShapeAlpha(expansion: Float): Float {
        return Motion.ease(Motion.clamp(expansion) / 0.18f)
    }

    const val MASK_HIDE_CENTRE: Int = 8
    const val MASK_HIDE_SIGNALS: Int = 16
    const val MASK_HIDE_BATTERY: Int = 32
    const val MASK_HIDE_WIFI: Int = 64
    const val MASK_HIDE_TECHNOLOGY: Int = 128
    const val MASK_HIDE_BATTERY_RING: Int = 256

    @JvmStatic fun traceTint(
        battery: Int,
        charging: Boolean,
        powerSave: Boolean,
        performanceMode: Boolean,
        coloured: Boolean,
        plainTint: Int,
        lowThreshold: Int,
        chargeColor: Int,
        lowColor: Int,
        saverColor: Int,
        performanceColor: Int
    ): Int {
        return DisplayPolicy.batteryTint(
            battery,
            charging,
            powerSave,
            performanceMode,
            coloured,
            plainTint,
            lowThreshold,
            chargeColor,
            lowColor,
            saverColor,
            performanceColor
        )
    }

    @JvmStatic fun runtimeReady(
        batteryKnown: Boolean,
        screenInteractive: Boolean,
        keyguardVisible: Boolean,
        privacyBlocked: Boolean,
        hostAvailable: Boolean
    ): Boolean {
        return batteryKnown
                && screenInteractive
                && !keyguardVisible && !privacyBlocked && hostAvailable
    }

    @JvmStatic fun openSettled(control: Float, morph: Float): Boolean {
        return Motion.clamp(control) >= BatteryTransferConfig.OPEN_CONTROL_MIN
                && Motion.clamp(morph) >= BatteryTransferConfig.OPEN_MORPH_MIN
    }

    @JvmStatic @JvmOverloads
    fun ringTransferPaintMask(
        mask: Int,
        ringTransfer: Boolean,
        hideTopContent: Boolean = false
    ): Int {
        return if (ringTransfer)
            (mask
                    or MASK_HIDE_BATTERY_RING)
        else
            mask
    }

    @JvmStatic fun drawsBatteryRing(mask: Int): Boolean {
        return (mask and (MASK_HIDE_BATTERY or MASK_HIDE_BATTERY_RING)) == 0
    }

    @JvmStatic fun drawsBatteryContent(mask: Int): Boolean {
        return (mask and MASK_HIDE_BATTERY) == 0
    }

    @JvmStatic fun batteryContentAlpha(ringTransfer: Boolean, iconAlpha: Float): Float {
        return if (ringTransfer) 1.0f else Motion.clamp(iconAlpha)
    }

    @JvmStatic fun batteryAnchorX(startX: Float, endX: Float): Float {
        return startX
    }

    @JvmStatic fun batteryAnchorY(startY: Float, endY: Float): Float {
        return startY
    }

    @JvmStatic fun ringTrackTint(batteryTint: Int): Int {
        return batteryTint
    }

    @JvmStatic fun ringTraceTint(batteryTint: Int): Int {
        return batteryTint
    }

    @JvmStatic fun realFillTint(batteryTint: Int): Int {
        return batteryTint
    }

    @JvmStatic fun closedSettled(control: Float, shade: Float, morph: Float): Boolean {
        return Motion.clamp(control) <= BatteryTransferConfig.HANDOFF_MAX && Motion.clamp(shade) <= BatteryTransferConfig.HANDOFF_MAX && Motion.clamp(
            morph
        ) <= BatteryTransferConfig.CLOSED_MORPH_MAX
    }

    @JvmStatic fun stagedBatteryCollapse(closeProgress: Float): Float {
        val progress = Motion.clamp(closeProgress)
        val horizontalEnd =
            BatteryTransferConfig.BATTERY_HORIZONTAL_CLOSE_PROGRESS
        if (progress <= horizontalEnd) {
            return (BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
                    * progress
                    / horizontalEnd)
        }
        return Motion.mix(
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END,
            1.0f,
            (progress - horizontalEnd) / (1.0f - horizontalEnd)
        )
    }

    @JvmStatic fun verticalShrinkProgress(rawProgress: Float): Float {
        val progress = Motion.clamp(rawProgress)
        return progress * Motion.ease(progress)
    }

    @JvmStatic fun horizontalShrinkProgress(rawProgress: Float): Float {
        return verticalShrinkProgress(rawProgress)
    }
}
