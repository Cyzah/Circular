package com.cyzah.circular.hook.runtime

import android.widget.TextView
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.PainterConfigurator
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import kotlin.math.min

/** Reused per-session ownership and row sample, prepared before any native drawing.  */
class SessionPaintFrame {
    @JvmField var currentTextColor: Int = 0
    @JvmField var controlCenterBatteryHost: Boolean = false
    @JvmField var primaryRowHost: IconSession? = null
    @JvmField var reusesPrimaryRow: Boolean = false
    @JvmField var solvedRow: ControlCenterRowSnapshot? = null
    @JvmField var solvedRowActive: Boolean = false
    @JvmField var solvedRowTranslationX: Float = 0f
    @JvmField var minimum: Float = 0f
    @JvmField var transferMinimum: Float = 0f
    @JvmField var transferState: BatteryTransferState? = null
    @JvmField var batteryReplacementActive: Boolean = false
    @JvmField var ringHandoffHold: Boolean = false
    @JvmField var batteryShapeOwner: Boolean = false
    @JvmField var rowWifiVisible: Boolean = false
    @JvmField var useBaseWifi: Boolean = false
    @JvmField var swappedStatusBarWifiTarget: Boolean = false
    @JvmField var moduleWifiActive: Boolean = false
    @JvmField var moduleWifiOwner: Boolean = false
    @JvmField var swappedWifiOwner: Boolean = false
    @JvmField var ringTransferOwner: Boolean = false
    @JvmField var nativeOwnershipHold: Boolean = false
    @JvmField var suppressNativeBattery: Boolean = false
    @JvmField var hideTopRingContent: Boolean = false
    @JvmField var capsuleDetailedSignalOwner: Boolean = false
    @JvmField var stableStockWidth: Float = 0f

    @Throws(IllegalAccessException::class)
    fun prepare(owner: IconSession, settings: SessionSettings) {
        configurePainter(owner, settings)
        selectRow(owner, settings)
        val controlCenterLayoutSize =
            SessionPaintPolicy.controlCenterLayoutSize(
                settings.size(),
                controlCenterBatteryHost
            )
        minimum = min(
            owner.dp(controlCenterLayoutSize),
            owner.battery.getHeight()
        ).toFloat()
        transferMinimum = minimum
        owner.painter.setContentPixelUnit(minimum / 120.0f)
        val batteryPaintHost = prepareBatteryOwnership(owner, settings)
        prepareWifiOwnership(owner, settings, batteryPaintHost)
        prepareTransferOwnership(owner, settings, batteryPaintHost)
        prepareExtent(owner, settings)
    }

    @Throws(IllegalAccessException::class)
    private fun configurePainter(owner: IconSession, settings: SessionSettings) {
        owner.painterMask = 0
        PainterConfigurator.apply(
            owner.painter, settings.appearance(),
            owner.module.currentDuoWifiProgress()
        )
        owner.painter.setStatusFont(owner.module.statusFont)
        // 固定用设计宽高比，不跟宿主实测宽高比走：控制中心下拉时图标视图的宽度是逐帧插值的，
        // 跟着它算会让胶囊在下拉过程中被等比缩小、收手时又突然弹回用户设置的大小
        owner.painter.setCapsuleAspect(IconPainter.Companion.CAPSULE_ASPECT)
        val obj = owner.module.batteryText!!.get(owner.battery)
        currentTextColor = if (obj is TextView) obj.getCurrentTextColor() else -1
        owner.lastTint = currentTextColor
        controlCenterBatteryHost =
            owner.from == 5 || owner.from == 6
    }

    private fun selectRow(owner: IconSession, settings: SessionSettings) {
        if (owner.from == 6) {
            owner.synchronizeControlCenterRowTransition()
        }
        primaryRowHost =
            owner.module.preferredAttachedControlCenterHost()
        val primaryRow = if (primaryRowHost == null)
            null
        else
            primaryRowHost!!.controlCenterRow.currentSnapshot()
        val primaryRowAvailable =
            owner.module.stackedOn() && primaryRowHost != null && primaryRowHost != owner && primaryRowHost!!.from == 6 && primaryRowHost!!.battery.isAttachedToWindow()
                    && primaryRow != null && primaryRow.valid()
        reusesPrimaryRow = SessionPaintPolicy.reusesPrimaryRowSnapshot(
            owner.from, primaryRowAvailable,
            owner.battery.isAttachedToWindow()
        )
        solvedRow = if (reusesPrimaryRow)
            primaryRow
        else
            owner.controlCenterRow.currentSnapshot()
        solvedRowActive = reusesPrimaryRow
                || SessionPaintPolicy.usesSolvedControlCenterRow(
            owner.from, owner.module.stackedOn(), solvedRow!!.valid()
        )
        var liveRowOriginDelta = 0.0f
        if (reusesPrimaryRow && primaryRowHost != null) {
            owner.battery.getLocationOnScreen(owner.transitionRowOrigin)
            primaryRowHost!!.battery.getLocationOnScreen(primaryRowHost!!.transitionRowOrigin)
            liveRowOriginDelta = (primaryRowHost!!.transitionRowOrigin[0] - owner.transitionRowOrigin[0]).toFloat()
        }
        solvedRowTranslationX = if (reusesPrimaryRow)
            SessionPaintPolicy.rowTransitionTranslation(
                liveRowOriginDelta, owner.module.rowAlignHostShift(), owner.stackedAlpha
            )
        else
            0.0f
        val rowHost = (if (reusesPrimaryRow) primaryRowHost else owner)!!
        if (solvedRowActive && rowHost.controlCenterRow.usesMeasuredGeometry()) {
            solvedRowTranslationX += rowHost.battery.getWidth() - solvedRow!!.rowRight()
        }
        if (SessionPaintPolicy.logsControlCenterRowDiagnostic(
                owner.from,
                reusesPrimaryRow
            )
        ) {
            val fallbackReason =
                SessionPaintPolicy.controlCenterRowFallbackReason(
                    owner.from,
                    owner.module.stackedOn(),
                    primaryRowAvailable,
                    owner.battery.isAttachedToWindow(),
                    solvedRow!!.valid()
                )
            val diagnosticSignature = SessionPaintPolicy.controlCenterRowDiagnosticSignature(
                owner.from,
                if (solvedRow!!.valid())
                    solvedRow!!.generation()
                else
                    0L,
                settings.dualStyle(),
                solvedRow!!.valid() && solvedRow!!.visible(
                    ControlCenterRowComponent.BATTERY
                ),
                solvedRow!!.valid() && solvedRow!!.visible(
                    ControlCenterRowComponent.WIFI
                ),
                solvedRow!!.valid() && solvedRow!!.visible(
                    ControlCenterRowComponent.CELLULAR
                ),
                fallbackReason
            )
            if (owner.controlCenterRow.claimDiagnostic(
                    diagnosticSignature
                )
            ) {
                owner.module.log(
                    5,
                    ModuleMain.Companion.TAG,
                    SessionPaintPolicy.controlCenterRowDiagnostic(
                        owner.from,
                        settings.dualStyle(),
                        solvedRow,
                        fallbackReason
                    )
                )
            }
        }
        owner.controlCenterRowSolved = solvedRowActive
    }

    private fun prepareBatteryOwnership(owner: IconSession, settings: SessionSettings): IconSession? {
        transferState =
            owner.module.batteryTransferState()
        val batteryPaintHost =
            owner.module.preferredBatteryPaintHost()
        val batteryHostAvailable =
            SessionPaintPolicy.batteryHostPaintAvailable(
                owner.from,
                owner.module.transferHostAvailable(owner.from),
                owner.module.transferHostAttached(owner.from)
            )
        val panelEngaged = transferState!!.batteryShapeVisible()
                || (owner.module.control
                > BatteryTransferConfig.HANDOFF_MAX)
        val replacementEnabled = owner.module.stackedOn()
                && owner.module.mode == 2 && owner.module.state.battery >= 0 && owner.module.state.readyForMerge()
                && owner.module.state.stackedReady()
                && settings.compatible()
        batteryReplacementActive =
            SessionPaintPolicy.controlCenterReplacementActive(
                controlCenterBatteryHost,
                panelEngaged,
                replacementEnabled,
                batteryHostAvailable
            )
        ringHandoffHold =
            transferState!!.handoffHoldVisible(
                owner.module.control,
                0.0f,
                owner.module.morphValue()
            )
                    && owner.module.transferHostAvailable(0)
        batteryShapeOwner =
            transferState!!.batteryShapeVisible() && SessionPaintPolicy.isPreferredBatteryOwner(
                batteryReplacementActive,
                batteryPaintHost == owner,
                batteryHostAvailable,
                ringHandoffHold
            )
        return batteryPaintHost
    }

    private fun prepareWifiOwnership(
        owner: IconSession, settings: SessionSettings,
        batteryPaintHost: IconSession?
    ) {
        // At full expansion the regular icon fade is zero while the native view
        // alpha is one. Battery replacement must not leave Wi-Fi behind on that
        // stale native layer, so each engaged row keeps drawing the module glyph.
        rowWifiVisible = SessionPaintPolicy.solvedRowWifiVisible(
            owner.rowWifiVisible(owner.module.state.showWifi()),
            solvedRowActive,
            solvedRow!!.visible(ControlCenterRowComponent.WIFI)
        )
        useBaseWifi =
            SessionPaintPolicy.usesBaseWifiForSwappedFlat(
                settings.dualStyle(),
                settings.swapContents()
            )
        swappedStatusBarWifiTarget =
            SessionPaintPolicy.usesSwappedStatusBarWifiTarget(
                settings.dualStyle(),
                owner.module.sessionSettings().swapContents()
            )
        moduleWifiActive = SessionPaintPolicy.prefersModuleWifi(batteryReplacementActive, rowWifiVisible)
        val controlCenterWifiOwner = SessionPaintPolicy.ownsControlCenterWifi(
            owner.from,
            if (batteryPaintHost == null) -1 else batteryPaintHost.from, solvedRowActive
        )
        moduleWifiOwner = moduleWifiActive && SessionPaintPolicy.usesControlCenterWifiSlot(
            controlCenterBatteryHost,
            useBaseWifi
        ) && controlCenterWifiOwner
        swappedWifiOwner = moduleWifiActive && !SessionPaintPolicy.usesControlCenterWifiSlot(
            controlCenterBatteryHost,
            useBaseWifi
        ) && controlCenterWifiOwner
        if (!moduleWifiOwner && !swappedWifiOwner) {
            owner.wifiScaleDiagnosticBucket = -1
        }
    }

    private fun prepareTransferOwnership(
        owner: IconSession, settings: SessionSettings,
        batteryPaintHost: IconSession?
    ) {
        ringTransferOwner = owner.from == 0 && (transferState!!.ringTransferVisible()
                || ringHandoffHold)
                && owner.module.transferHostAvailable(0)
        nativeOwnershipHold =
            SessionPaintPolicy.forceHidesNativeIcons(
                owner.module.nativeStatusIconHold.active()
            )
        suppressNativeBattery =
            SessionPaintPolicy.suppressesNativeBattery(
                nativeOwnershipHold,
                batteryPaintHost != null
            )
        hideTopRingContent =
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                settings.dualStyle(),
                SessionPaintPolicy.topContentTransferActive(
                    transferState!!
                        .batteryShapeVisible(),
                    ringTransferOwner,
                    ringHandoffHold,
                    owner.progress
                ),
                owner.module.sessionSettings().swapContents()
            )
        val controlCenterActive = (owner.module.control
                > BatteryTransferConfig.HANDOFF_MAX)
        capsuleDetailedSignalOwner =
            SessionPaintPolicy.keepsCapsuleControlCenterSignalOwner(
                settings.dualStyle(),
                controlCenterBatteryHost,
                ringTransferOwner,
                controlCenterActive,
                owner.coversSignals(),
                owner.module.stackedOn()
            )
        if (batteryShapeOwner) {
            owner.module.markBatteryShapeOwner(owner.from)
        }
    }

    private fun prepareExtent(owner: IconSession, settings: SessionSettings) {
        stableStockWidth = if (controlCenterBatteryHost)
            SessionPaintPolicy.controlCenterBaseWidth(
                owner.stockWidth.toFloat(),
                minimum,
                batteryReplacementActive,
                owner.progress
            )
        else
            owner.stockWidth.toFloat()
        owner.stackedStableStockWidth = stableStockWidth
        owner.stackedStockWidth = owner.stockWidth
        owner.driftX = BurnInDrift.Companion.offset(
            owner.module.burnIn.x(),
            BurnInDrift.Companion.roomX(owner.battery.getWidth().toFloat(), minimum, settings.weight()),
            owner.ambient
        )
        owner.driftY = BurnInDrift.Companion.offset(
            owner.module.burnIn.y(),
            BurnInDrift.Companion.roomY(owner.battery.getHeight().toFloat(), minimum, settings.weight()),
            owner.ambient
        )
    }
}
