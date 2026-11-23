package com.cyzah.circular.hook.runtime

import android.view.View
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.BatteryRingFrame
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.min

/** Per-session geometry, reused only while its owner is drawing.  */
class SessionPaintGeometry {
    @JvmField var captureCollapsedRow: Boolean = false
    @JvmField var rtl: Boolean = false
    @JvmField var progress: Float = 0f
    @JvmField var nativeAlpha: Float = 0f
    @JvmField var signalXProgress: Float = 0f
    @JvmField var width: Float = 0f
    @JvmField var height: Float = 0f
    @JvmField var centerX: Float = 0f
    @JvmField var centerY: Float = 0f
    @JvmField var stockLeft: Float = 0f
    @JvmField var iconAlpha: Float = 0f
    @JvmField var staticSignalEndpoint: Boolean = false
    @JvmField var solvedBatteryOriginX: Float = 0f
    @JvmField var expandedTrinityCenterX: Float = 0f
    @JvmField var trinityLeft: Float = 0f
    private var signalTrinityLeft = 0f
    @JvmField var trinityTop: Float = 0f
    @JvmField var projectedTarget: Boolean = false
    @JvmField var targetTrinityLeft: Float = 0f
    @JvmField var targetTrinityTop: Float = 0f
    @JvmField var projectedWifiTarget: Boolean = false
    @JvmField var endpointUnit: Float = 0f
    private var targetWifiX = 0f
    @JvmField var targetWifiY: Float = 0f
    @JvmField var wifiStartX: Float = 0f
    @JvmField var wifiAnchorY: Float = 0f

    fun prepare(owner: IconSession, frame: SessionPaintFrame) {
        captureCollapsedRow =
            owner.beginCollapsedRowCapture()
        rtl = owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        progress = owner.progress
        val solvedRowDriftX = if (frame.reusesPrimaryRow
            && frame.primaryRowHost != null
        )
            frame.primaryRowHost!!.driftX
        else
            owner.driftX
        frame.solvedRowTranslationX =
            SessionPaintPolicy.rowTranslationWithDrift(
                frame.solvedRowTranslationX,
                solvedRowDriftX,
                BurnInDrift.Companion.fade(progress)
            )
        val numberPlacement =
            owner.rowNumberPlacement(
                IconSessionPainter.Companion.appearanceSettings(owner)
                    .batteryNumberPlacement()
            )
        // 两个控制中心宿主共用当前系统展开进度。
        val zCCHost = (owner.from == 5 || owner.from == 6) && owner.module.stackedOn()
        nativeAlpha = if (zCCHost) owner.module.morphValue() else PanelMotionFrame.nativeAlpha(progress)
        signalXProgress = StackedOverlayLayout.signalXProgress(
            nativeAlpha,
            numberPlacement
        )
        width = owner.battery.getWidth().toFloat()
        height = owner.battery.getHeight().toFloat()
        centerX = frame.minimum / 2.0f
        if (!rtl) {
            centerX = width - centerX
        }
        centerY = height / 2.0f
        if (!rtl) {
            stockLeft = width - frame.stableStockWidth
        } else {
            stockLeft = 0.0f
        }
        iconAlpha = 1.0f - nativeAlpha
        prepareContentMask(owner, frame)
        prepareBatteryOrigins(owner, frame)
        prepareTransferTarget(owner)
    }

    private fun prepareContentMask(owner: IconSession, frame: SessionPaintFrame) {
        // 双排信号：只有本宿主确实带着原生蜂窝图标时才接管，否则保持原样；
        // 收起尾部继续绘制到最后一段形变结束
        owner.stackedDrawing = SessionPaintPolicy.drawsStacked(
            owner.stackedActive,
            owner.module.morphValue(),
            owner.module.stackedOn(),
            owner.coversSignals()
        )
                || frame.capsuleDetailedSignalOwner
        staticSignalEndpoint =
            SessionPaintPolicy.keepsStaticSignalEndpoint(
                frame.controlCenterBatteryHost,
                owner.stackedDrawing
            )
        if (!owner.stackedDrawing) {
            // 一次接管会话结束时清掉盒宽记忆，下次下拉重新统计
            owner.stackedBoxWidth = 0.0f
        }
        owner.stackedAlpha = nativeAlpha
        // 形变期间信号点与中心制式文本由模块按形变进度绘制；只有模块真的在画时才屏蔽，
        // 否则形变还没开始就会出现「两边都不画」的空档
        owner.painterMask = if (owner.stackedDrawing
            && (owner.stackedAlpha > 0.0f || frame.capsuleDetailedSignalOwner
                    || staticSignalEndpoint)
        )
            SessionPaintPolicy.stackedContentPaintMask(
                frame.useBaseWifi
            )
        else
            0
        if (frame.batteryReplacementActive) {
            owner.painterMask = owner.painterMask or IconPainter.Companion.MASK_HIDE_BATTERY
        }
        owner.painterMask = BatteryTransferPolicy.ringTransferPaintMask(
            owner.painterMask,
            frame.ringTransferOwner,
            frame.hideTopRingContent
        )
        owner.painterMask = SessionPaintPolicy.capsuleRingPaintMask(
            owner.painterMask,
            IconSessionPainter.Companion.appearanceSettings(owner).dualStyle(),
            frame.ringTransferOwner
        )
        owner.painterMask = SessionPaintPolicy.moduleWifiPaintMask(
            owner.painterMask,
            frame.moduleWifiActive
        )
        owner.stackedLeft = Float.NaN
        owner.stackedRight = Float.NaN
    }

    private fun prepareBatteryOrigins(owner: IconSession, frame: SessionPaintFrame) {
        solvedBatteryOriginX = batteryShapeOriginX(
            owner, frame.transferMinimum,
            frame.stableStockWidth, frame.solvedRow, frame.solvedRowActive,
            frame.solvedRowTranslationX
        )
        expandedTrinityCenterX = solvedBatteryOriginX + frame.minimum / 2.0f
        // 三合一图标当前的画布位置与缩放（与 paintPart(owner, 0, …) 用的是同一组数）
        trinityLeft = (Motion.mix(
            centerX,
            expandedTrinityCenterX,
            progress
        )
                + (owner.driftX * BurnInDrift.Companion.fade(progress))
                - (frame.minimum / 2.0f))
        signalTrinityLeft = (Motion.mix(
            centerX,
            expandedTrinityCenterX,
            signalXProgress
        )
                + (owner.driftX * BurnInDrift.Companion.fade(signalXProgress))
                - (frame.minimum / 2.0f))
        trinityTop = batteryShapeOriginY(owner, frame.minimum)
        trinityLeft = BatteryRingFrame.originX(
            frame.ringTransferOwner,
            width, frame.minimum, rtl, owner.driftX, trinityLeft
        )
        trinityTop = BatteryRingFrame.originY(
            frame.ringTransferOwner,
            height, frame.minimum, owner.driftY, trinityTop
        )
        if (frame.ringTransferOwner) {
            // The ring and its readout must stay on the endpoint used by projection.
            // paintPart adds the fading drift below, so remove it from its anchors.
            val driftFade: Float = BurnInDrift.Companion.fade(progress)
            centerX = trinityLeft + frame.minimum / 2.0f - owner.driftX * driftFade
            centerY = trinityTop + frame.minimum / 2.0f - owner.driftY * driftFade
            expandedTrinityCenterX = centerX
            signalTrinityLeft = trinityLeft
        }
    }

    private fun prepareTransferTarget(owner: IconSession) {
        projectedTarget = false
        if (SessionPaintPolicy.usesControlCenterTransferProjection(
                owner.from, IconSessionPainter.Companion.appearanceSettings(owner).keepMergedShade()
            )
        ) {
            val ringHost = owner.module.transferHost(0)
            projectedTarget = owner.batteryTransferProjection.project(
                ringHost,
                owner.module.transferRingGeometry(owner.module.state),
                owner, owner.renderer.canvasRowShift()
            )
        }
        targetTrinityLeft =
            SessionPaintPolicy.transferTargetOrigin(
                owner.batteryTransferProjection.ringLocalX,
                signalTrinityLeft,
                projectedTarget
            )
        targetTrinityTop =
            SessionPaintPolicy.transferTargetOrigin(
                owner.batteryTransferProjection.ringLocalY,
                trinityTop,
                projectedTarget
            )
        projectedWifiTarget = projectedTarget
        endpointUnit = SessionPaintPolicy.endpointUnit(
            owner.dp(owner.module.sessionSettings().size()).toFloat(),
            owner.battery.getHeight().toFloat()
        )
        val endpoint: IconPainter = endpointRuler(owner)
        targetWifiX = if (projectedWifiTarget)
            targetTrinityLeft + (endpoint.wifiReadoutCentreX(owner.module.state)
                    * owner.batteryTransferProjection.ringUnitX)
        else
            centerX
        targetWifiY = if (projectedWifiTarget)
            targetTrinityTop + (endpoint.wifiReadoutCentreY(owner.module.state)
                    * owner.batteryTransferProjection.ringUnitY)
        else
            centerY
    }

    fun prepareWifiSource(owner: IconSession) {
        val capsuleWifiSourceX: Float = (trinityLeft
                + (endpointRuler(owner).capsuleTextX(owner.module.state)
                * capsuleEndpointUnit(owner)))
        wifiStartX = SessionPaintPolicy.controlCenterWifiSourceX(
            IconSessionPainter.Companion.appearanceSettings(owner).dualStyle(), centerX, capsuleWifiSourceX
        )
        if (projectedWifiTarget) {
            wifiStartX = targetWifiX
        }
    }

    fun prepareWifiAnchor(owner: IconSession, frame: SessionPaintFrame) {
        if (IconSessionPainter.Companion.appearanceSettings(owner).capsuleDual()) {
            val endpointSettings = owner.module.sessionSettings()
            val endpointMinimum = min(
                owner.dp(endpointSettings.size()), owner.battery.getHeight()
            ).toFloat()
            wifiAnchorY = (centerY
                    + (((endpointRuler(owner).anchorY(1, owner.module.state) - 60.0f)
                    * SessionPaintPolicy.endpointUnit(
                endpointMinimum, owner.battery.getHeight().toFloat()
            ))))
        } else {
            wifiAnchorY = (centerY
                    + (((owner.painter.anchorY(1, owner.module.state) - 60.0f)
                    * frame.minimum) / 120.0f))
        }
    }

    companion object {
        // Shared by the real shape and a second host's obstacle sample. These helpers
        // do not reset drawing masks, stacked bounds, or capture state on the source.
        @JvmStatic fun batteryShapeOriginX(
            owner: IconSession, minimum: Float,
            stockWidth: Float, row: ControlCenterRowSnapshot?,
            rowActive: Boolean, rowTranslation: Float
        ): Float {
            val rtl = owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
            if (rowActive && row != null && row.visible(ControlCenterRowComponent.BATTERY)) {
                var leftAtZero: Float = controlCenterBatteryLeftPx(owner, 0.0f, minimum)
                if (rtl) {
                    leftAtZero -= minimum - BatteryShapeLayout.frameMinimumPx(
                        owner.dp(IconSessionPainter.Companion.appearanceSettings(owner).batterySize()).toFloat()
                    )
                }
                val origin = SessionPaintPolicy.solvedBatteryOriginX(
                    translatedRowX(row.left(ControlCenterRowComponent.BATTERY), rowTranslation),
                    translatedRowX(row.right(ControlCenterRowComponent.BATTERY), rowTranslation),
                    leftAtZero
                )
                if (java.lang.Float.isFinite(origin)) {
                    return origin
                }
            }
            val stockLeft = if (rtl) 0.0f else owner.battery.getWidth() - stockWidth
            return stockWidth / 2.0f + stockLeft - minimum / 2.0f
        }

        @JvmStatic fun batteryShapeOriginY(owner: IconSession, minimum: Float): Float {
            return (owner.battery.getHeight() / 2.0f
                    + owner.driftY * BurnInDrift.Companion.fade(owner.progress)
                    - owner.painter.anchorY(0, owner.module.state) * minimum / 120.0f)
        }

        @JvmStatic fun endpointRuler(
            owner: IconSession
        ): IconPainter {
            return owner.module.capsuleRuler(
                owner.module.sessionSettings()
            )
        }

        @JvmStatic fun capsuleEndpointUnit(
            owner: IconSession
        ): Float {
            val endpointSettings =
                owner.module.sessionSettings()
            return SessionPaintPolicy.endpointUnit(
                owner.dp(endpointSettings.size()).toFloat(),
                owner.battery.getHeight().toFloat()
            )
        }

        @JvmStatic fun controlCenterWifiCanvasScale(
            owner: IconSession,
            expandedProgress: Float
        ): Float {
            val sourceSettings =
                owner.module.sessionSettings()
            val statusBarUnit = SessionPaintPolicy.endpointUnit(
                owner.dp(sourceSettings.size()).toFloat(),
                owner.battery.getHeight().toFloat()
            )
            val startScale =
                SessionPaintPolicy.controlCenterWifiStartCanvasScale(
                    IconSessionPainter.Companion.appearanceSettings(owner).dualStyle(),
                    statusBarUnit,
                    sourceSettings.wifiScale() / 100.0f,
                    endpointRuler(owner).capsuleWifiScale(
                        owner.module.state
                    )
                )
            return Motion.mix(
                startScale, owner.module.controlCenterSettings().wifiScale() / 100.0f,
                expandedProgress
            )
        }

        @JvmStatic fun controlCenterWifiAvoidShift(
            owner: IconSession,
            originX: Float,
            minimum: Float,
            wifiRight: Float
        ): Float {
            // Reserved for the legacy path; a solved row disables this shift.
            if ((owner.battery.getLayoutDirection()
                        == View.LAYOUT_DIRECTION_RTL)
                || !java.lang.Float.isFinite(wifiRight)
            ) {
                return 0.0f
            }
            val fullBatteryLeft: Float = controlCenterBatteryLeftPx(
                owner,
                originX,
                minimum
            )
            if (!java.lang.Float.isFinite(fullBatteryLeft)) {
                return 0.0f
            }
            return (BatteryShapeLayout.avoidWifiOverlapShiftPx(
                fullBatteryLeft,
                wifiRight,
                BatteryShapeLayout.wifiBatteryMinimumGapPx(
                    owner.battery.getResources()
                        .getDisplayMetrics().density
                )
            )
                    * (1.0f - Motion.ease(
                owner.module.batteryTransferState()
                    .batteryCollapse(
                        owner.module.control,
                        owner.module.morphValue()
                    )
            )))
        }

        @JvmStatic fun controlCenterBatteryLeftPx(
            owner: IconSession,
            originX: Float,
            minimum: Float
        ): Float {
            val canonicalMinimum = BatteryShapeLayout.minimumPx(
                owner.dp(IconSessionPainter.Companion.appearanceSettings(owner).batterySize()).toFloat()
            )
            val frameMinimum = BatteryShapeLayout.frameMinimumPx(
                owner.dp(IconSessionPainter.Companion.appearanceSettings(owner).batterySize()).toFloat()
            )
            if (canonicalMinimum <= 0.0f || minimum <= 0.0f) {
                return Float.NaN
            }
            val placement =
                owner.rowNumberPlacement(
                    IconSessionPainter.Companion.appearanceSettings(owner)
                        .batteryNumberPlacement()
                )
            var fullBatteryLeft =
                BatteryShapeLayout.fullBatteryLeftPx(
                    originX,
                    minimum,
                    frameMinimum,
                    canonicalMinimum,
                    placement,
                    owner.battery.getResources()
                        .getDisplayMetrics().density
                )
            if (placement == BatteryNumberPlacement.OUTSIDE) {
                fullBatteryLeft += (owner.batteryShapeRenderer
                    .outsideNumberAdaptiveBatteryShiftUnits(
                        owner.painter,
                        owner.rowBatteryForContent(
                            owner.module.state.battery
                        )
                    )
                        * canonicalMinimum
                        / 120.0f)
            }
            return fullBatteryLeft
        }

        @JvmStatic fun translatedRowX(x: Float, translationX: Float): Float {
            return ControlCenterRowHostPolicy.translateHorizontal(x, 0.0f, translationX)
        }
    }
}
