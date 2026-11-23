package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferProjection
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.domain.settings.StatusColors
import java.util.Locale

object BatteryTransferDiagnostics {
    @JvmStatic fun build(
        module: ModuleMain,
        transfer: BatteryTransferState,
        projection: BatteryTransferProjection
    ): String {
        val ringHost = module.transferHost(0)
        val controlHost = module.preferredBatteryPaintHost()
        val host = if (transfer.batteryShapeVisible()
            && controlHost != null
        )
            controlHost
        else
            ringHost
        val hostId = if (host == null) -1 else host.from
        val geometry = module.transferRingGeometry(module.state)
        if (ringHost != null) {
            projection.project(ringHost, geometry, ringHost)
            projection.projectPoint(
                ringHost,
                geometry,
                transfer.ringTraceProgress()
            )
        }
        val plainTint = if (host == null) -1 else host.lastTint
        val batteryTint = BatteryTransferPolicy.traceTint(
            module.state.battery,
            module.state.charging,
            module.state.powerSave,
            module.state.performanceMode,
            module.sessionSettings().coloured(),
            plainTint,
            module.sessionSettings().lowBatteryThreshold(),
            module.sessionSettings().chargeColor(),
            module.sessionSettings().lowColor(),
            module.sessionSettings().saverColor(),
            module.sessionSettings().performanceColor()
        )
        return String.format(
            Locale.ROOT,
            ("host=%d control=%.4f shade=%.4f morph=%.4f phase=%s "
                    + "style=%s battery=%d charging=%s batteryTint=%s "
                    + "pathLength=%.3f pathProgress=%.4f "
                    + "pathDurationMs=%d restoreDurationMs=%d "
                    + "pointScreenX=%.2f pointScreenY=%.2f "
                    + "ringStartX=%.2f ringStartY=%.2f "
                    + "ringEndX=%.2f ringEndY=%.2f "
                    + "ringTopHalfAngle=%.2f "
                    + "panelTracking=%s touchActive=%s fastChargeBadge=%s"),
            hostId,
            module.control,
            module.shade,
            module.morphValue(),
            transfer.phase().name,
            module.sessionSettings().dualStyle().name,
            module.state.battery,
            module.state.charging,
            StatusColors.hex(batteryTint),
            geometry.length,
            transfer.ringTraceProgress(),
            BatteryTransferConfig.PATH_DURATION_MS,
            BatteryTransferConfig.RESTORE_DURATION_MS,
            projection.pointScreenX,
            projection.pointScreenY,
            projection.startScreenX,
            projection.startScreenY,
            projection.endScreenX,
            projection.endScreenY,
            geometry.topHalfAngle,
            if (module.panelTrackingAvailable) "available" else "unavailable",
            module.panelTrackingActive,
            module.fastChargeBadgeStatus
        )
    }
}
