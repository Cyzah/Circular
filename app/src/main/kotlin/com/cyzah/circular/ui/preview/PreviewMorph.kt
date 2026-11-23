package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.max

object PreviewMorph {
    @JvmStatic fun shouldDrawStackedOverlay(stackedEnabled: Boolean, expansion: Float): Boolean {
        return stackedEnabled && Motion.clamp(expansion) > 0.002f
    }

    @JvmStatic fun iconOpacity(expansion: Float): Float {
        return 1.0f - Motion.clamp(expansion)
    }

    @JvmStatic fun positionProgress(expansion: Float): Float {
        return PanelMotionFrame.nativeAlpha(Motion.clamp(expansion))
    }

    @JvmStatic fun morphProgress(expansion: Float): Float {
        return PanelMotionFrame.morph(Motion.clamp(expansion))
    }

    @JvmStatic fun batteryCollapse(expansion: Float): Float {
        val morph = morphProgress(expansion)
        if (BatteryTransferPolicy.openSettled(expansion, morph)) {
            return 0.0f
        }
        val synchronizedExpansion = max(morph, Motion.ease(expansion))
        return BatteryTransferPolicy.stagedBatteryCollapse(
            (1.0f - synchronizedExpansion)
                    / (1.0f - BatteryTransferConfig.CLOSED_MORPH_MAX)
        )
    }

    @JvmStatic fun stackedConfigured(
        stackedShade: Boolean,
        keepMergedShade: Boolean,
        mode: Int,
        state: IconState
    ): Boolean {
        return stackedShade
                && !keepMergedShade && DisplayPolicy.mode(mode) == 2 && state.readyForMerge()
                && state.stackedReady()
    }

    @JvmStatic fun shouldDrawWifiOverlay(
        style: DualStyle?,
        connected: Boolean,
        duoWifiProgress: Float,
        expansion: Float
    ): Boolean {
        val fadingDuoWifi = style == DualStyle.DUO
                && Motion.clamp(duoWifiProgress) > 0.0f
        return (connected || fadingDuoWifi) && shouldDrawStackedOverlay(true, expansion)
    }

    @JvmStatic fun wifiStartX(style: DualStyle?): Float {
        return 60.0f
    }

    @JvmStatic fun wifiStartY(style: DualStyle?, regularWifiY: Float): Float {
        return regularWifiY
    }

    @JvmStatic fun wifiStartScale(style: DualStyle?, regularWifiScale: Float): Float {
        return regularWifiScale
    }

    @JvmStatic fun shouldShowStackedTechnology(wifiConnected: Boolean): Boolean {
        return true
    }

    @JvmStatic fun signalSourceScale(density: Float): Float {
        return max(0.0f, density)
    }

    @JvmStatic fun wifiHomeX(style: DualStyle?, regularX: Float, capsuleX: Float): Float {
        return if (style == DualStyle.CAPSULE) capsuleX else regularX
    }

    @JvmStatic fun wifiHomeY(style: DualStyle?, regularY: Float, capsuleY: Float, duoY: Float): Float {
        if (style == DualStyle.CAPSULE) {
            return capsuleY
        }
        return if (style == DualStyle.DUO) duoY else regularY
    }

    @JvmStatic fun wifiHomeScale(style: DualStyle?, regularScale: Float, capsuleScale: Float): Float {
        return if (style == DualStyle.CAPSULE) capsuleScale else regularScale
    }

    @JvmStatic fun wifiAlignedY(signalCentreY: Float, signalBottomOffset: Float, wifiBottomOffset: Float): Float {
        return signalCentreY + signalBottomOffset - wifiBottomOffset
    }
}
