package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import android.view.View
import com.cyzah.circular.core.graphics.BatteryShapeFrame
import com.cyzah.circular.core.graphics.ClosingBatteryReadout
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.OpeningBatteryReadout
import com.cyzah.circular.core.graphics.OpeningBatteryRenderer
import com.cyzah.circular.core.graphics.animation.BatteryRingFrame
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StatusColors
import kotlin.math.min

class IconSessionPainter {
    // Each IconSession owns its renderer. Native callbacks run under module.bypass,
    // so they cannot overwrite these samples by re-entering the module painter.
    private val frame = SessionPaintFrame()
    private val geometry = SessionPaintGeometry()
    private val components = SessionComponentLayers()
    private val batteryShape = BatteryShapeFrame()
    private val openingBattery = OpeningBatteryRenderer()
    private val openingReadout = OpeningBatteryReadout()
    private val closingReadout = ClosingBatteryReadout()
    private var canvasRowShift = 0f

    fun canvasRowShift(): Float {
        return canvasRowShift
    }

    @Throws(Throwable::class)
    fun paint(owner: IconSession, canvas: Canvas, nativeDraw: NativeDraw?) {
        // The outer hook samples this shift before prepare updates stackedAlpha.
        canvasRowShift = if (owner.from == 5)
            owner.module.rowAlignHostShift() * owner.stackedAlpha
        else
            0.0f
        val shapeHost = owner.module.preferredAttachedControlCenterHost()
        if (owner.from == 5 && shapeHost != null && shapeHost.from == 6) {
            // Both row selection and obstacle sampling must see this frame's row,
            // including when the copied host draws before the battery owner.
            shapeHost.synchronizeControlCenterRowTransition()
        }
        frame.prepare(owner, appearanceSettings(owner))
        if (owner.notificationCopy()) {
            drawMergedNotificationCopy(owner, canvas)
            recordPaint(owner)
            return
        }
        if (owner.from == 0 && frame.transferState!!.reopenAlpha() > 0.0f) {
            val left = BatteryRingFrame.closedOriginX(
                owner.battery.getWidth().toFloat(), frame.minimum,
                owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL, owner.driftX
            )
            val top = BatteryRingFrame.closedOriginY(owner.battery.getHeight().toFloat(), frame.minimum, owner.driftY)
            owner.batteryRingRenderer.drawReopen(
                canvas, owner.module.transferRingGeometry(owner.module.state),
                frame.transferState!!, owner.module.state.battery, frame.currentTextColor,
                batteryTint(owner, frame.currentTextColor), left, top, frame.minimum / 120.0f
            )
        }
        if (SessionPaintPolicy.defersClosedControlCenterCopy(
                frame.controlCenterBatteryHost,
                owner.module.transferHostAvailable(0), owner.module.control,
                owner.module.shade, owner.module.morphValue()
            )
        ) {
            owner.swappedWifiTransfer.reset()
            return
        }
        if (SessionPaintPolicy.usesMergedControlCenterEndpoint(
                frame.controlCenterBatteryHost,
                owner.module.transferHostAvailable(0), owner.module.control,
                owner.module.morphValue()
            )
        ) {
            drawClosedControlCenterEndpoint(owner, canvas)
            recordPaint(owner)
            return
        }
        if (SessionPaintPolicy.defersBatteryHostDuringTransfer(
                owner.from, owner.module.control,
                frame.transferState!!.batteryShapeVisible(), frame.ringTransferOwner
            )
        ) {
            return
        }
        val ringOnly = frame.ringTransferOwner
                && owner.module.control > BatteryTransferConfig.HANDOFF_MAX && owner.module.preferredBatteryPaintHost() != null
        if (ringOnly) {
            drawSimple(owner, canvas, SessionPaintPolicy.ringHostContentMask(true))
            recordPaint(owner)
            return
        }
        if (SessionPaintPolicy.usesSimpleIcon(
                owner.ambient, owner.merge, frame.capsuleDetailedSignalOwner
            )
        ) {
            drawSimple(owner, canvas)
            recordPaint(owner)
            return
        }
        geometry.prepare(owner, frame)
        drawBaseAndNativeBattery(owner, canvas, nativeDraw)
        components.draw(owner, canvas, this, frame, geometry)
        drawOverlays(owner, canvas)
        if (geometry.captureCollapsedRow) {
            owner.finishCollapsedRowCapture(
                geometry.solvedBatteryOriginX, frame.transferMinimum
            )
        }
        recordPaint(owner)
    }

    private fun drawClosedControlCenterEndpoint(owner: IconSession, canvas: Canvas) {
        // The real status row can remain transparent until the finger lifts.
        // Keep its projected endpoint and user appearance on the visible copy.
        val settings = owner.module.sessionSettings()
        val ring = owner.module.transferRingGeometry(owner.module.state)
        val projected = SessionPaintPolicy.usesControlCenterTransferProjection(
            owner.from, settings.keepMergedShade()
        ) && owner.batteryTransferProjection.project(
            owner.module.transferHost(0), ring, owner, canvasRowShift
        )
        val minimum = min(owner.dp(settings.size()), owner.battery.getHeight()).toFloat()
        val left = if (projected)
            owner.batteryTransferProjection.ringLocalX
        else
            (BatteryRingFrame.closedOriginX(
                owner.battery.getWidth().toFloat(), minimum,
                owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL, owner.driftX
            )
                    - canvasRowShift)
        val top = if (projected)
            owner.batteryTransferProjection.ringLocalY
        else
            BatteryRingFrame.closedOriginY(owner.battery.getHeight().toFloat(), minimum, owner.driftY)
        val unitX = if (projected) owner.batteryTransferProjection.ringUnitX else minimum / 120.0f
        val unitY = if (projected) owner.batteryTransferProjection.ringUnitY else minimum / 120.0f
        val transferRing = frame.transferState!!.pointVisible()
                || frame.transferState!!.ringTransferVisible()
        val endpoint: IconPainter = SessionPaintGeometry.Companion.endpointRuler(owner)
        val referenceUnit = if (projected) endpoint.contentPixelUnit() else unitY
        owner.stackedAlpha = 0.0f
        owner.stackedDrawing = false
        owner.painterMask = 0
        owner.swappedWifiTransfer.reset()
        endpoint.setMotion(0.0f)
        val save = canvas.save()
        try {
            canvas.translate(left, top)
            canvas.scale(unitX, unitY)
            endpoint.drawPartMaskedAtReferenceUnit(
                canvas, 0.0f, 0.0f, 120.0f,
                owner.module.state, frame.currentTextColor, settings.coloured(),
                7 or (if (transferRing) IconPainter.Companion.MASK_HIDE_BATTERY_RING else 0),
                1.0f, referenceUnit
            )
            if (transferRing) {
                owner.batteryRingRenderer.draw(
                    canvas, ring, frame.transferState!!,
                    owner.module.state.battery, frame.currentTextColor,
                    batteryTint(owner, frame.currentTextColor, settings),
                    0.0f, 0.0f, 1.0f
                )
            }
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun drawMergedNotificationCopy(owner: IconSession, canvas: Canvas) {
        val endpoint: IconPainter = SessionPaintGeometry.Companion.endpointRuler(owner)
        val minimum = min(owner.dp(owner.module.sessionSettings().size()), owner.battery.getHeight()).toFloat()
        val left = BatteryRingFrame.closedOriginX(
            owner.battery.getWidth().toFloat(), minimum,
            owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL, owner.driftX
        )
        val top = BatteryRingFrame.closedOriginY(owner.battery.getHeight().toFloat(), minimum, owner.driftY)
        owner.stackedAlpha = 0.0f
        owner.stackedDrawing = false
        owner.painterMask = 0
        endpoint.setMotion(0.0f)
        val save = canvas.save()
        try {
            canvas.translate(-this.canvasRowShift, 0.0f)
            endpoint.draw(
                canvas, left, top, minimum, owner.module.state,
                frame.currentTextColor, owner.module.sessionSettings().coloured()
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun drawSimple(owner: IconSession, canvas: Canvas, extraMask: Int = 0) {
        var left = ((owner.battery.getWidth() - frame.minimum) / 2.0f) + owner.driftX
        var top = ((owner.battery.getHeight() - frame.minimum) / 2.0f) + owner.driftY
        left = BatteryRingFrame.originX(
            frame.ringTransferOwner,
            owner.battery.getWidth().toFloat(), frame.minimum,
            owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL,
            owner.driftX, left
        )
        top = BatteryRingFrame.originY(
            frame.ringTransferOwner,
            owner.battery.getHeight().toFloat(), frame.minimum, owner.driftY, top
        )
        val mask = if (frame.ringTransferOwner)
            BatteryTransferPolicy.ringTransferPaintMask(1, true, frame.hideTopRingContent)
        else
            (if (frame.batteryReplacementActive) 1 or IconPainter.Companion.MASK_HIDE_BATTERY else 7)
        owner.painter.drawPartMasked(
            canvas, left, top, frame.minimum,
            owner.module.state, frame.currentTextColor,
            appearanceSettings(owner)!!.coloured() && !owner.ambient, mask or extraMask, 1.0f
        )
        if (frame.ringTransferOwner) {
            drawBatteryRingTransfer(
                owner, canvas, frame.currentTextColor,
                left, top, frame.minimum
            )
        }
    }

    @Throws(Throwable::class)
    private fun drawBaseAndNativeBattery(
        owner: IconSession, canvas: Canvas, nativeDraw: NativeDraw?
    ) {
        paintPart(
            owner, canvas, 0, geometry.centerX, geometry.centerY,
            geometry.expandedTrinityCenterX, geometry.centerY, frame.minimum,
            geometry.progress, BatteryTransferPolicy.batteryContentAlpha(
                frame.ringTransferOwner, geometry.iconAlpha
            ), frame.currentTextColor
        )
        geometry.prepareWifiSource(owner)
        if (SessionPaintPolicy.drawsNativeBattery(
                frame.batteryReplacementActive, geometry.nativeAlpha
            )
            && !frame.suppressNativeBattery
        ) {
            NativeSessionDraw.battery(
                owner, canvas, nativeDraw, geometry.nativeAlpha,
                Motion.mix(
                    geometry.centerX - (frame.stableStockWidth / 2),
                    geometry.stockLeft, geometry.progress
                ),
                geometry.width, geometry.height
            )
        }
    }

    @Throws(Throwable::class)
    private fun drawOverlays(owner: IconSession, canvas: Canvas) {
        closingReadout.reset()
        owner.stackedOverlay.clearBatteryObstacle()
        this.batteryShape.valid = false
        if (frame.batteryReplacementActive && frame.transferState!!.batteryShapeVisible()) {
            val shapeHost = owner.module.preferredBatteryPaintHost()
            val prepared = if (shapeHost == owner)
                this.batteryShape.prepare(
                    owner, geometry.solvedBatteryOriginX,
                    geometry.trinityTop, frame.transferMinimum
                )
            else
                this.batteryShape.prepareObstacle(shapeHost, owner, this.canvasRowShift)
            if (prepared) {
                owner.stackedOverlay.setBatteryObstacle(
                    this.batteryShape.bounds,
                    this.batteryShape.pointBounds
                )
                if (this.batteryShape.opening.valid) {
                    owner.stackedOverlay.setOpeningBatteryObstacle(
                        this.batteryShape.opening,
                        this.batteryShape.solidBounds
                    )
                }
            }
        }
        // 盒子宽度钉在「会话里出现过的最宽值」上：原生槽位在 settle 时会变宽，
        // 那个瞬间左缘会一下子左移十几 dp，块（贴左）或块中心都会被推走。
        // 右缘（贴着电池）稳定，所以只锁宽度、再由右缘反推左缘。
        if (owner.stackedDrawing && !java.lang.Float.isNaN(owner.stackedLeft) && owner.stackedRight > owner.stackedLeft) {
            val fBox = owner.stackedRight - owner.stackedLeft
            if (fBox > owner.stackedBoxWidth) {
                // 盒子变宽时按 1/4 逐帧吸收，别让这一下把条带整体推走
                owner.stackedBoxWidth = if (owner.stackedBoxWidth <= 0.0f)
                    fBox
                else
                    owner.stackedBoxWidth + ((fBox - owner.stackedBoxWidth) * 0.25f)
            } else if (owner.stackedBoxWidth > fBox) {
                if (geometry.rtl) {
                    owner.stackedRight = owner.stackedLeft + owner.stackedBoxWidth
                } else {
                    owner.stackedLeft = owner.stackedRight - owner.stackedBoxWidth
                }
            }
        }
        if (frame.solvedRowActive) {
            val cellularVisible = frame.solvedRow!!.visible(
                ControlCenterRowComponent.CELLULAR
            )
            owner.stackedLeft = if (cellularVisible)
                SessionPaintGeometry.Companion.translatedRowX(
                    frame.solvedRow!!.left(ControlCenterRowComponent.CELLULAR),
                    frame.solvedRowTranslationX
                )
            else Float.NaN
            owner.stackedRight = if (cellularVisible)
                SessionPaintGeometry.Companion.translatedRowX(
                    frame.solvedRow!!.right(ControlCenterRowComponent.CELLULAR),
                    frame.solvedRowTranslationX
                )
            else Float.NaN
        }
        components.drawMissingParts(owner, canvas, this, frame, geometry)
        if (SessionPaintPolicy.capsuleOverlaySignalAlpha(
                appearanceSettings(owner)!!.dualStyle(),
                frame.ringTransferOwner
            ) > 0.0f
        ) {
            owner.stackedOverlay.setTechnologyEndpointUnit(
                if (geometry.projectedTarget)
                    owner.batteryTransferProjection.ringUnitY
                else
                    geometry.endpointUnit
            )
            drawStackedFrame(
                owner,
                canvas,
                frame.currentTextColor,
                geometry.centerY,
                geometry.targetTrinityLeft,
                geometry.targetTrinityTop,
                frame.minimum / 120.0f,
                owner.stackedAlpha,
                geometry.signalXProgress,
                components.wifiRightBoundary(),
                frame.capsuleDetailedSignalOwner
                        || geometry.staticSignalEndpoint
            )
        }
        // Both native row copies carry the painted readout at identical screen
        // coordinates. Their parent alpha supplies the native crossfade.
        if (this.batteryShape.valid && !frame.transferState!!.openingShape() && geometry.projectedTarget && frame.controlCenterBatteryHost) {
            closingReadout.prepare(
                SessionPaintGeometry.Companion.endpointRuler(owner), owner.painter, owner.batteryShapeRenderer,
                owner.module.state, this.batteryShape.contentBattery, this.batteryShape.placement,
                geometry.targetTrinityLeft, geometry.targetTrinityTop,
                owner.batteryTransferProjection.ringUnitY, this.batteryShape.solidBounds,
                owner.module.morphValue()
            )
        }
        if (this.batteryShape.opening.valid && geometry.projectedTarget
            && frame.controlCenterBatteryHost
        ) {
            openingReadout.prepare(
                SessionPaintGeometry.Companion.endpointRuler(owner), owner.painter, owner.batteryShapeRenderer,
                owner.module.state, this.batteryShape.contentBattery, this.batteryShape.placement,
                geometry.targetTrinityLeft, geometry.targetTrinityTop,
                owner.batteryTransferProjection.ringUnitY,
                this.batteryShape.solidBounds.left, this.batteryShape.solidBounds.top,
                this.batteryShape.solidBounds.width(), this.batteryShape.solidBounds.height(),
                owner.module.morphValue()
            )
            drawOpeningRing(owner, canvas)
        }
        if (frame.batteryShapeOwner) {
            if (this.batteryShape.opening.valid) {
                val save = canvas.save()
                try {
                    openingBattery.draw(
                        canvas, this.batteryShape.opening, owner.module.state.battery,
                        frame.currentTextColor, batteryTint(owner, frame.currentTextColor),
                        frame.transferState!!.openingAlpha()
                    )
                } finally {
                    canvas.restoreToCount(save)
                }
            }
            if (!this.batteryShape.opening.valid) drawOpeningRing(owner, canvas)
            drawBatteryShape(
                owner,
                canvas,
                frame.currentTextColor,
                geometry.solvedBatteryOriginX,
                geometry.trinityTop,
                frame.transferMinimum,
                components.wifiRightBoundary()
            )
        } else if (frame.ringTransferOwner) {
            drawBatteryRingTransfer(
                owner,
                canvas,
                frame.currentTextColor,
                geometry.trinityLeft,
                geometry.trinityTop,
                frame.transferMinimum
            )
            if (SessionPaintPolicy.redrawsCapsuleContent(
                    appearanceSettings(owner)!!.dualStyle(),
                    true
                )
            ) {
                owner.painter.drawPartMasked(
                    canvas,
                    geometry.trinityLeft,
                    geometry.trinityTop,
                    frame.transferMinimum,
                    owner.module.state,
                    frame.currentTextColor,
                    appearanceSettings(owner)!!.coloured(),
                    SessionPaintPolicy.capsuleContentRedrawMask(),
                    1.0f
                )
            }
        }
        if (!closingReadout.cutout()) {
            closingReadout.drawSource(
                canvas, BatteryTransferConfig.chargedAreaTint(
                    batteryTint(owner, frame.currentTextColor)
                )
            )
        }
        closingReadout.drawTarget(
            canvas, owner.module.state, frame.currentTextColor,
            owner.module.sessionSettings().coloured()
        )
    }

    private fun drawOpeningRing(owner: IconSession, canvas: Canvas) {
        if (!frame.transferState!!.openingShape() || !geometry.projectedTarget) {
            return
        }
        val alpha = if (this.batteryShape.opening.valid)
            1.0f - this.batteryShape.opening.solidAlpha
        else
            1.0f - BatteryTransferPolicy.gestureShapeAlpha(owner.module.morphValue())
        if (alpha <= 0.0f) {
            return
        }
        val endpoint: IconPainter = SessionPaintGeometry.Companion.endpointRuler(owner)
        if (this.batteryShape.opening.valid) {
            openingReadout.draw(
                canvas, endpoint, owner.module.state, frame.currentTextColor,
                appearanceSettings(owner)!!.coloured(), alpha
            )
            return
        }
        endpoint.drawPartMaskedAtReferenceUnit(
            canvas,
            geometry.targetTrinityLeft,
            geometry.targetTrinityTop,
            owner.batteryTransferProjection.ringUnitY * 120.0f,
            owner.module.state,
            frame.currentTextColor,
            appearanceSettings(owner)!!.coloured(),
            (1 or IconPainter.Companion.MASK_HIDE_CENTRE or IconPainter.Companion.MASK_HIDE_SIGNALS
                    or IconPainter.Companion.MASK_HIDE_WIFI or IconPainter.Companion.MASK_HIDE_TECHNOLOGY
                    or (if (this.batteryShape.opening.valid) IconPainter.Companion.MASK_HIDE_BATTERY_RING else 0)),
            alpha,
            endpoint.contentPixelUnit()
        )
    }

    /**
     * 双排形变统一入口：tag 6（面板状态栏）把本帧的几何记成「主几何」，
     * tag 5（下拉时那段假状态栏副本）按两者在屏幕上的位置差换算后复用同一份几何，
     * 这样两块画布画出来的东西在屏幕上是同一个，不会看到两遍动画。
     */
    fun drawStackedFrame(
        owner: IconSession,
        canvas: Canvas,
        i: Int,
        fCenterY: Float,
        fTrinityLeft: Float,
        fTrinityTop: Float,
        fUnit: Float,
        fMorph: Float,
        fSignalXProgress: Float,
        wifiRight: Float,
        allowStaticEndpoint: Boolean
    ) {
        if (!owner.stackedDrawing
            || (fMorph <= 0.0f && !allowStaticEndpoint)
        ) {
            return
        }
        drawStacked(
            owner,
            canvas,
            i,
            owner.stackedLeft,
            owner.stackedRight,
            fCenterY,
            fMorph,
            fSignalXProgress,
            fTrinityLeft,
            fTrinityTop,
            fUnit,
            wifiRight,
            allowStaticEndpoint
        )
    }

    fun drawBatteryShape(
        owner: IconSession, canvas: Canvas, plainTint: Int, originX: Float,
        originY: Float, minimum: Float, wifiRightBoundary: Float
    ) {
        if (!this.batteryShape.valid) {
            return
        }
        val shapeOriginX = this.batteryShape.originX
        val shapeOriginY = this.batteryShape.originY
        val unit = this.batteryShape.unit
        val canonicalMinimum = this.batteryShape.canonicalMinimum
        val frameMinimum = this.batteryShape.frameMinimum
        val collapse = this.batteryShape.collapse
        val endX = this.batteryShape.endX
        val endY = this.batteryShape.endY
        val placement = this.batteryShape.placement
        val contentBattery = this.batteryShape.contentBattery
        val transferState = owner.module.batteryTransferState()
        if (owner.lastBatteryShapeStage
            != owner.batteryShapeGeometry.stage
        ) {
            owner.lastBatteryShapeStage = owner.batteryShapeGeometry.stage
            owner.module.log(
                5,
                ModuleMain.Companion.TAG,
                ("batteryShapeStage=" + owner.lastBatteryShapeStage
                        + " collapse=" + collapse
                        + " control=" + owner.module.control
                        + " morph=" + owner.module.morphValue()
                        + " size=" + owner.batteryShapeGeometry.width
                        + "x" + owner.batteryShapeGeometry.height
                        + " centre=" + owner.batteryShapeGeometry.centreX
                        + "," + owner.batteryShapeGeometry.centreY
                        + " end=" + endX + "," + endY)
            )
        }
        val batteryTint = batteryTint(owner, plainTint)
        if (!owner.batteryShapeFrameCaptured) {
            owner.batteryShapeFrameCaptured = true
            canvas.getClipBounds(owner.batteryShapeFrameClip)
            owner.battery.getLocationOnScreen(
                owner.batteryShapeFrameLocation
            )
            owner.module.log(
                4,
                ModuleMain.Companion.TAG,
                ("batteryShapeFrame host=" + owner.from
                        + " view=" + owner.battery.getWidth()
                        + "x" + owner.battery.getHeight()
                        + " screen="
                        + owner.batteryShapeFrameLocation[0] + ","
                        + owner.batteryShapeFrameLocation[1]
                        + " clip=" + owner.batteryShapeFrameClip
                        + " origin=" + shapeOriginX + ","
                        + shapeOriginY
                        + " minimum=" + minimum
                        + " batteryMinimum=" + canonicalMinimum
                        + " batteryFrame=" + frameMinimum
                        + " shape=" + owner.batteryShapeGeometry.width
                        + "x" + owner.batteryShapeGeometry.height
                        + " center=" + owner.batteryShapeGeometry.centreX
                        + "," + owner.batteryShapeGeometry.centreY
                        + " end=" + endX + "," + endY
                        + " stage=" + owner.batteryShapeGeometry.stage
                        + " battery=" + owner.module.state.battery
                        + " tint=" + StatusColors.hex(batteryTint)
                        + " number=" + placement
                        + " wifiKnown=" + owner.module.state.wifiKnown
                        + " showWifi=" + owner.module.state.showWifi()
                        + " wifiLevel=" + owner.module.state.wifiLevel
                        + " wifiPaint=module"
                        + " wifiScale="
                        + SessionPaintGeometry.Companion.controlCenterWifiCanvasScale(owner, 1.0f)
                        + " wifiRight=" + wifiRightBoundary
                        + " painterMask=" + owner.painterMask
                        + " numberTextSvg="
                        + BatteryTransferConfig.numberTextSizeSvg(
                    placement
                )
                        + " numberRendered120="
                        + (BatteryTransferConfig.numberTextSizeSvg(
                    placement
                )
                        * owner.batteryShapeGeometry.height
                        / BatteryTransferConfig.SVG_HEIGHT)
                        + " body120="
                        + BatteryShapeLayout.bodyWidthUnits(placement)
                        + "x"
                        + BatteryShapeLayout.bodyHeightUnits(placement)
                        + " cornerRadiusSvg="
                        + BatteryTransferConfig.BODY_RADIUS
                        + " bodyRightControlSvg="
                        + BatteryTransferConfig.BODY_RIGHT_CONTROL_X
                        + " chargedClipEnabled="
                        + BatteryShapeLayout.clipsChargedArea(
                    owner.module.state.battery
                )
                        + " chargedClip="
                        + BatteryShapeLayout.chargedClipLeftSvg()
                        + ","
                        + BatteryShapeLayout.chargedClipTopSvg()
                        + "-"
                        + BatteryTransferConfig.chargedBodyRightSvg(
                    owner.module.state.battery
                )
                        + ","
                        + BatteryShapeLayout.chargedClipBottomSvg()
                        + " terminalSvg="
                        + BatteryTransferConfig.TERMINAL_START_X
                        + ","
                        + BatteryTransferConfig.TERMINAL_TOP_Y
                        + "-"
                        + BatteryTransferConfig.TERMINAL_END_X
                        + ","
                        + BatteryTransferConfig.TERMINAL_BOTTOM_Y
                        + " batteryStroke=none terminalStroke=none")
            )
        }
        if ((owner.batteryShapeGeometry.stage
                    == BatteryShapeGeometry.Stage.POINT)
            && !owner.batteryPointFrameLogged
        ) {
            owner.batteryPointFrameLogged = true
            owner.battery.getLocationOnScreen(
                owner.batteryShapeFrameLocation
            )
            owner.module.log(
                4,
                ModuleMain.Companion.TAG,
                ("batteryPointFrame host=" + owner.from
                        + " pointScreenX="
                        + (owner.batteryShapeFrameLocation[0]
                        + (shapeOriginX
                        + (owner.batteryShapeGeometry.centreX * unit)))
                        + " pointScreenY="
                        + (owner.batteryShapeFrameLocation[1]
                        + (shapeOriginY
                        + (owner.batteryShapeGeometry.centreY * unit)))
                        + " ringStartX="
                        + owner.batteryTransferProjection.startScreenX
                        + " ringStartY="
                        + owner.batteryTransferProjection.startScreenY
                        + " centerX="
                        + owner.batteryShapeGeometry.centreX
                        + " startLocalX="
                        + owner.batteryTransferProjection.startLocalX
                        + " originX=" + shapeOriginX
                        + " unit=" + unit)
            )
        }

        owner.module.setFastChargeBadgeStatus("disabled")
        val reopeningLayer = if (transferState.openingAlpha() < 1.0f)
            canvas.saveLayerAlpha(null, Math.round(255 * transferState.openingAlpha()))
        else
            -1
        val readoutLayer = if (closingReadout.cutout()) canvas.saveLayer(this.batteryShape.bounds, null) else -1
        val save = canvas.save()
        try {
            canvas.translate(shapeOriginX, shapeOriginY)
            canvas.scale(unit, unit)
            owner.module.markBatteryShapeDraw(owner.from)
            owner.batteryShapeRenderer.draw(
                canvas,
                owner.painter,
                owner.module.state,
                owner.batteryShapeGeometry,
                if (closingReadout.valid()) BatteryNumberPlacement.HIDDEN else placement,
                contentBattery,
                BatteryTransferConfig.chargedAreaTint(batteryTint),
                plainTint,
                if (this.batteryShape.opening.valid)
                    this.batteryShape.opening.bodyAlpha
                else
                    if (transferState.openingShape())
                        BatteryTransferPolicy.gestureShapeAlpha(owner.module.morphValue())
                    else
                        1.0f,
                if (this.batteryShape.opening.valid) this.batteryShape.opening else null
            )
        } finally {
            canvas.restoreToCount(save)
            try {
                if (closingReadout.cutout()) {
                    closingReadout.drawSource(canvas, BatteryTransferConfig.chargedAreaTint(batteryTint))
                }
            } finally {
                if (readoutLayer >= 0) canvas.restoreToCount(readoutLayer)
                if (reopeningLayer >= 0) canvas.restoreToCount(reopeningLayer)
            }
        }
    }

    fun drawBatteryRingTransfer(
        owner: IconSession,
        canvas: Canvas,
        plainTint: Int,
        originX: Float,
        originY: Float,
        minimum: Float
    ) {
        if (minimum <= 0.0f) {
            return
        }
        owner.batteryRingRenderer.draw(
            canvas,
            owner.module.transferRingGeometry(owner.module.state),
            owner.module.batteryTransferState(), owner.module.state.battery,
            plainTint, batteryTint(owner, plainTint),
            originX, originY, minimum / 120.0f
        )
    }

    fun batteryTint(owner: IconSession, plainTint: Int): Int {
        return batteryTint(owner, plainTint, appearanceSettings(owner)!!)
    }

    private fun batteryTint(owner: IconSession, plainTint: Int, settings: SessionSettings): Int {
        return BatteryTransferPolicy.traceTint(
            owner.module.state.battery,
            owner.module.state.charging,
            owner.module.state.powerSave,
            owner.module.state.performanceMode,
            settings.coloured(),
            plainTint,
            settings.lowBatteryThreshold(),
            settings.chargeColor(),
            settings.lowColor(),
            settings.saverColor(),
            settings.performanceColor()
        )
    }

    /**
     * 画控制中心双排信号，并把每个元素按形变进度插值到三合一的信号点上。
     */
    fun drawStacked(
        owner: IconSession,
        canvas: Canvas,
        i: Int,
        fLeft0: Float,
        fRight0: Float,
        fCenterY: Float,
        fMorph: Float,
        fSignalXProgress: Float,
        fTrinityLeft: Float,
        fTrinityTop: Float,
        fUnit: Float,
        wifiRight: Float,
        allowStaticEndpoint: Boolean
    ) {
        if (!owner.stackedDrawing || (fMorph <= 0.0f && !allowStaticEndpoint)
            || java.lang.Float.isNaN(fLeft0) || java.lang.Float.isNaN(fRight0)
        ) {
            return
        }
        val iconState = owner.rowIconState(
            owner.module.state
        )
        val dual = owner.rowDualSim(iconState.dualSim())
        val firstLabel = owner.rowStackedLabel(iconState, true)
        val secondLabel = if (dual)
            owner.rowStackedLabel(iconState, false)
        else
            null
        val dualStyle: DualStyle? = DualStyle.Companion.from(
            appearanceSettings(owner)!!.groupedDots(),
            appearanceSettings(owner)!!.capsuleDual(),
            appearanceSettings(owner)!!.duoDual()
        )
        val density = owner.battery.getResources()
            .getDisplayMetrics().density
        val endpointSettings = owner.module.sessionSettings()
        val endpointUnit = if (this.geometry.projectedTarget)
            owner.batteryTransferProjection.ringUnitY
        else
            SessionPaintPolicy.endpointUnit(
                owner.dp(endpointSettings.size()).toFloat(),
                owner.battery.getHeight().toFloat()
            )
        val capsuleUnit: Float = SessionPaintGeometry.Companion.capsuleEndpointUnit(owner)
        val targetRadius = if (dualStyle == DualStyle.CAPSULE)
            (SessionPaintGeometry.Companion.endpointRuler(owner).capsuleDotRadius(iconState)
                    * capsuleUnit)
        else
            IconGeometry.dotRadius(
                endpointSettings.weight()
            ) * endpointUnit
        val rtl = (owner.battery.getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL)
        val signalRowShiftX =
            if (owner.controlCenterRowSolved)
                0.0f
            else
                SessionPaintPolicy.capsuleSignalRowShiftX(
                    dualStyle,
                    owner.from == 5 || owner.from == 6,
                    dual,
                    rtl,
                    density * ControlCenterAppearance.CAPSULE_WIFI_LEFT_GAP_DP,
                    fMorph
                )
        SignalMotionDiagnostics.maybeLog(owner, fMorph, fSignalXProgress)
        owner.stackedOverlay.frame(
            fLeft0 - signalRowShiftX,
            fRight0 - signalRowShiftX,
            fCenterY,
            fMorph,
            fSignalXProgress,
            fTrinityLeft,
            fTrinityTop,
            endpointUnit,
            capsuleUnit,
            density,
            owner.progress,
            owner.module.currentDuoWifiProgress(),
            targetRadius,
            i,
            endpointSettings.weight(),
            rtl,
            SessionPaintPolicy.fitsStackedLabels(
                owner.from == 5 || owner.from == 6
            ),
            true,
            dualStyle,
            owner.rowNumberPlacement(
                appearanceSettings(owner)!!
                    .batteryNumberPlacement()
            )
        )
        owner.stackedOverlay.setLabelsEnabled(
            SessionPaintPolicy.showsStackedTechnology(
                owner.from == 5 || owner.from == 6, fMorph
            )
        )
        owner.stackedOverlay.draw(
            canvas,
            owner.painter,
            SessionPaintGeometry.Companion.endpointRuler(owner),
            owner.stacked,
            owner.stackedTarget,
            iconState,
            firstLabel,
            secondLabel
        )
    }

    fun recordPaint(owner: IconSession) {
        if (owner.keyguard != null) {
            owner.captureKeyguard()
            val z = owner.ambient
            val moduleMain = owner.module
            if (!z) {
                moduleMain.lastLockPaint = System.currentTimeMillis()
                owner.captureRealLockFrame()
                if (!owner.returnCapturePending || owner.module.control != 0.0f || !owner.keyguard.isShown()) {
                    return
                }
                owner.module.firstLockReturn =
                    ModuleMain.Companion.stamp(owner.module.lastLockPaint) + " " + owner.lastRealFrame
                owner.returnCapturePending = false
                return
            }
            moduleMain.lastAodPaint = System.currentTimeMillis()
        }
    }

    @Throws(Throwable::class)
    fun paintPart(
        owner: IconSession,
        canvas: Canvas,
        i: Int,
        f: Float,
        f2: Float,
        f3: Float,
        f4: Float,
        f5: Float,
        f6: Float,
        f7: Float,
        i2: Int
    ) {
        if (f7 <= 0.0f) {
            return
        }
        owner.painter.setMotion(f6)
        owner.painter.drawPartMasked(
            canvas,
            (Motion.mix(f, f3, f6) + (owner.driftX * BurnInDrift.Companion.fade(f6))) - (f5 / 2.0f),
            (Motion.mix(f2, f4, f6) + (owner.driftY * BurnInDrift.Companion.fade(f6))) - ((owner.painter.anchorY(
                i,
                owner.module.state
            ) * f5) / 120.0f),
            f5,
            owner.module.state,
            i2,
            appearanceSettings(owner)!!.coloured(),
            (1 shl i) or owner.painterMask,
            f7
        )
    }

    companion object {
        @JvmStatic fun appearanceSettings(owner: IconSession): SessionSettings {
            return if (owner.from == 5 || owner.from == 6)
                owner.module.controlCenterSettings()
            else
                owner.module.sessionSettings()
        }
    }
}
