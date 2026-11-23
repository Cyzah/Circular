package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.graphics.geometry.WifiEndpointGeometry
import com.cyzah.circular.hook.runtime.IconSession
import com.cyzah.circular.hook.runtime.SessionPaintGeometry
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import kotlin.math.min

/**
 * Uses the static status-bar glyph as the collapsed endpoint for every style.
 */
object ControlCenterWifiPainter {
    @JvmStatic fun draw(
        owner: IconSession,
        canvas: Canvas?,
        tint: Int,
        regularCentreX: Float,
        regularCentreY: Float,
        regularScale: Float,
        controlCenterCentreX: Float,
        controlCenterCentreY: Float,
        controlCenterScale: Float,
        targetOriginX: Float,
        targetOriginY: Float,
        progress: Float,
        targetReady: Boolean
    ) {
        if (owner.from != 5 && owner.from != 6) {
            drawRegular(
                owner,
                canvas,
                tint,
                regularCentreX,
                regularCentreY,
                regularScale
            )
            return
        }

        val settings = owner.module.sessionSettings()
        val endpoint: IconPainter = SessionPaintGeometry.Companion.endpointRuler(owner)
        val fallbackUnit = SessionPaintPolicy.endpointUnit(
            owner.dp(settings.size()).toFloat(),
            owner.battery.getHeight().toFloat()
        )
        val unitX = if (targetReady) owner.batteryTransferProjection.ringUnitX else fallbackUnit
        val unitY = if (targetReady) owner.batteryTransferProjection.ringUnitY else fallbackUnit
        val targetCentreX = targetOriginX + endpoint.wifiReadoutCentreX(owner.module.state) * unitX
        val targetCentreY = targetOriginY + endpoint.wifiReadoutCentreY(owner.module.state) * unitY
        val targetScale = endpoint.wifiReadoutScale(owner.module.state) * min(unitX, unitY)
        val endpointAlpha = if (settings.duoDual() && owner.module.state.dualSim())
            DuoLayout.wifiAlpha(owner.module.currentDuoWifiProgress())
        else
            1.0f
        owner.swappedWifiTransfer.drawControlCenter(
            owner,
            canvas!!,
            tint,
            controlCenterCentreX,
            controlCenterCentreY,
            controlCenterScale,
            targetCentreX,
            targetCentreY,
            targetScale,
            WifiEndpointGeometry.weight(
                settings.wifiWeight(),
                owner.module.controlCenterSettings().wifiWeight(), progress
            ),
            WifiEndpointGeometry.alpha(endpointAlpha, progress),
            progress,
            targetReady
        )
    }

    private fun drawRegular(
        owner: IconSession,
        canvas: Canvas?,
        tint: Int,
        centreX: Float,
        centreY: Float,
        scale: Float
    ) {
        val savedOpacity = owner.painter.opacity
        owner.painter.opacity = 1.0f
        try {
            owner.painter.drawWifiOnly(
                canvas,
                owner.module.state,
                tint,
                centreX,
                centreY,
                scale,
                1.0f
            )
        } finally {
            owner.painter.opacity = savedOpacity
        }
    }
}
