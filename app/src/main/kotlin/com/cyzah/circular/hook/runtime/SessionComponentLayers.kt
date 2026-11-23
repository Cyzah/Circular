package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import com.cyzah.circular.core.graphics.ControlCenterWifiPainter
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SharedWifiFrame
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Draws each native slot and its replacement, retaining only this frame's coverage.  */
class SessionComponentLayers {
    private val sharedWifi = SharedWifiFrame()
    private var canvasRowShift = 0f
    private var wifiPainted = false
    private var cellularPainted = false
    private var fallbackX = 0f
    private var wifiRightBoundary = 0f

    fun wifiRightBoundary(): Float {
        return wifiRightBoundary
    }

    @Throws(Throwable::class)
    fun draw(
        owner: IconSession, canvas: Canvas, renderer: IconSessionPainter,
        frame: SessionPaintFrame, geometry: SessionPaintGeometry
    ) {
        canvasRowShift = renderer.canvasRowShift()
        val spacing = owner.module.spacingField!!.getInt(owner.icons)
        var slotEdge = if (geometry.rtl)
            frame.stableStockWidth
        else
            geometry.width - frame.stableStockWidth
        geometry.prepareWifiAnchor(owner, frame)
        if (frame.useBaseWifi && frame.rowWifiVisible && frame.swappedWifiOwner
            && SessionPaintPolicy.drawsSwappedTopWifi(
                frame.ringTransferOwner, frame.ringHandoffHold
            )
        ) {
            owner.swappedWifiTransfer.draw(
                owner, canvas, frame.currentTextColor,
                geometry.trinityLeft, frame.minimum, geometry.wifiAnchorY,
                geometry.targetTrinityLeft, geometry.targetTrinityTop,
                geometry.progress, geometry.projectedTarget
            )
        }
        wifiRightBoundary = Float.NaN
        wifiPainted = false
        cellularPainted = false
        // With no native slots, the original fallback uses the battery centre.
        // Traversing any slot changes that fallback to the Wi-Fi source position.
        fallbackX = geometry.centerX
        for (index in owner.parts.indices.reversed()) {
            val view = owner.parts.get(index)
            var slotWidth = view.getMeasuredWidth().toFloat()
            val reservedWidth = owner.stackedSlotWidths.get(view)
            if (reservedWidth != null) {
                slotWidth = reservedWidth
            }
            val left: Float
            if (geometry.rtl) {
                left = slotEdge + view.getPaddingEnd()
                slotEdge = left + slotWidth + view.getPaddingStart() + spacing
            } else {
                left = (slotEdge - view.getPaddingEnd()) - slotWidth
                slotEdge = (left - view.getPaddingStart()) - spacing
            }
            val part = if ("com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
                == view.javaClass.getName()
            ) 1 else 2
            val sourceY = if (part == 2)
                ((frame.minimum * 45.5f) / 120.0f) + geometry.centerY
            else
                (if (geometry.projectedWifiTarget) geometry.targetWifiY else geometry.wifiAnchorY)
            val halfWidth = slotWidth / 2.0f
            val centerX = left + halfWidth
            captureSlot(owner, geometry, part, left, slotWidth)
            if (part == 1 && frame.rowWifiVisible && !wifiPainted && frame.moduleWifiOwner) {
                drawModuleWifi(owner, canvas, frame, geometry, sourceY, centerX)
                wifiPainted = true
            } else if (part == 2 && !cellularPainted && owner.module.mobileSubId!!.getInt(view) == owner.module.state.subId) {
                if (!frame.rowWifiVisible && !wifiPainted) {
                    renderer.paintPart(
                        owner, canvas, 1, geometry.wifiStartX,
                        geometry.wifiAnchorY, centerX, geometry.centerY,
                        frame.minimum, geometry.progress, geometry.iconAlpha,
                        frame.currentTextColor
                    )
                    wifiPainted = true
                }
                renderer.paintPart(
                    owner, canvas, part, geometry.wifiStartX, sourceY,
                    centerX, geometry.centerY, frame.minimum, geometry.progress,
                    geometry.iconAlpha, frame.currentTextColor
                )
                cellularPainted = true
            }
            if (SessionPaintPolicy.drawsNativeWifi(
                    (frame.moduleWifiActive || frame.solvedRowActive) && part == 1,
                    geometry.nativeAlpha
                )
                && !frame.nativeOwnershipHold && !(owner.stackedDrawing && part == 2)
            ) {
                NativeSessionDraw.view(
                    owner, canvas, view, geometry.nativeAlpha,
                    Motion.mix(geometry.wifiStartX - halfWidth, left, geometry.progress),
                    Motion.mix(sourceY, geometry.centerY, geometry.progress)
                            - (view.getHeight() / 2.0f),
                    geometry.width, geometry.height
                )
            }
            fallbackX = geometry.wifiStartX
        }
    }

    @Throws(Throwable::class)
    fun drawMissingParts(
        owner: IconSession, canvas: Canvas, renderer: IconSessionPainter,
        frame: SessionPaintFrame, geometry: SessionPaintGeometry
    ) {
        if (!wifiPainted) {
            if (frame.rowWifiVisible && frame.moduleWifiOwner) {
                val sourceY = if (geometry.projectedWifiTarget)
                    geometry.targetWifiY
                else
                    geometry.wifiAnchorY
                drawModuleWifi(owner, canvas, frame, geometry, sourceY, fallbackX)
            } else {
                renderer.paintPart(
                    owner, canvas, 1, fallbackX, geometry.wifiAnchorY,
                    fallbackX, geometry.centerY, frame.minimum, geometry.progress,
                    geometry.iconAlpha, frame.currentTextColor
                )
            }
        }
        if (!cellularPainted) {
            renderer.paintPart(
                owner, canvas, 2, fallbackX,
                geometry.centerY + ((frame.minimum * 45.5f) / 120.0f),
                fallbackX, geometry.centerY, frame.minimum, geometry.progress,
                geometry.iconAlpha, frame.currentTextColor
            )
        }
    }

    private fun drawModuleWifi(
        owner: IconSession, canvas: Canvas, frame: SessionPaintFrame,
        geometry: SessionPaintGeometry, sourceY: Float, nativeCenterX: Float
    ) {
        var wifiEndX = if (frame.solvedRowActive && frame.solvedRow!!.visible(ControlCenterRowComponent.WIFI))
            (SessionPaintGeometry.Companion.translatedRowX(
                frame.solvedRow!!.left(ControlCenterRowComponent.WIFI),
                frame.solvedRowTranslationX
            )
                    + (frame.solvedRow!!.width(ControlCenterRowComponent.WIFI)
                    / 2.0f))
        else
            (nativeCenterX
                    + (if (IconSessionPainter.Companion.appearanceSettings(owner).capsuleDual())
                (IconPainter.Companion.CAPSULE_WIFI_END_DX * frame.minimum) / 120.0f
            else
                0.0f))
        var wifiEndY = (geometry.centerY
                + (if (IconSessionPainter.Companion.appearanceSettings(owner).capsuleDual())
            (IconPainter.Companion.CAPSULE_WIFI_END_DY * frame.minimum) / 120.0f
        else
            0.0f))
        var sourceScale = (ControlCenterAppearance.WIFI_FINAL_CANVAS_SCALE
                * owner.module.controlCenterSettings().wifiScale() / 100.0f)
        if (sharedWifi.resolve(owner, frame, canvasRowShift)) {
            wifiEndX = sharedWifi.x
            wifiEndY = sharedWifi.y
            sourceScale *= sharedWifi.scale
        }
        if (geometry.captureCollapsedRow) {
            owner.captureWifiRowTarget(
                nativeCenterX,
                owner.painter.wifiHalfWidth(
                    SessionPaintGeometry.Companion.controlCenterWifiCanvasScale(
                        owner, 0.0f
                    )
                ) * 2.0f
            )
        }
        var wifiCenterY = Motion.mix(sourceY, wifiEndY, geometry.progress)
        val wifiCanvasScale: Float =
            SessionPaintGeometry.Companion.controlCenterWifiCanvasScale(owner, geometry.progress)
        wifiCenterY = SessionPaintPolicy.clampIconCentreY(
            wifiCenterY,
            owner.painter.wifiHalfHeight(wifiCanvasScale),
            0.0f,
            owner.battery.getHeight().toFloat()
        )
        val rawWifiCenterX =
            Motion.mix(geometry.wifiStartX, wifiEndX, geometry.progress)
        val wifiScaleBucket = min(
            4,
            max(
                0,
                floor(
                    (Motion.clamp(geometry.progress) * 4.0f
                            + 0.0001f).toDouble()
                ).toInt()
            )
        )
        if ((owner.from == 5 || owner.from == 6)
            && (owner.wifiScaleDiagnosticBucket
                    != wifiScaleBucket)
        ) {
            owner.wifiScaleDiagnosticBucket =
                wifiScaleBucket
            owner.battery.getLocationOnScreen(
                owner.batteryShapeFrameLocation
            )
            val endpointSettings =
                owner.module.sessionSettings()
            owner.module.log(
                6,
                ModuleMain.Companion.TAG,
                ("wifiScaleFrame host=" + owner.from
                        + " style="
                        + IconSessionPainter.Companion.appearanceSettings(owner)
                    .dualStyle()
                        + " progress=" + geometry.progress
                        + " control=" + owner.module.control
                        + " shade=" + owner.module.shade
                        + " owner=" + frame.moduleWifiOwner
                        + " canvasScale="
                        + wifiCanvasScale
                        + " endpointUnit="
                        + SessionPaintPolicy.endpointUnit(
                    owner.dp(endpointSettings.size()).toFloat(),
                    owner.battery.getHeight().toFloat()
                )
                        + " startScale="
                        + SessionPaintGeometry.Companion.controlCenterWifiCanvasScale(
                    owner,
                    0.0f
                )
                        + " statusBarWifiScale="
                        + endpointSettings.wifiScale()
                        + " nativeAlpha=" + geometry.nativeAlpha
                        + " rowTranslation=" + frame.solvedRowTranslationX + " screenOrigin=" + owner.batteryShapeFrameLocation[0]
                        + "," + owner.batteryShapeFrameLocation[1]
                        + " wifiStart=" + geometry.wifiStartX + " wifiEnd=" + wifiEndX
                        + " wifiCentre=" + rawWifiCenterX + " wifiY=" + wifiCenterY)
            )
        }
        val wifiHalfWidth =
            owner.painter.wifiHalfWidth(wifiCanvasScale)
        val wifiAvoidShift = if (frame.solvedRowActive)
            0.0f
        else
            SessionPaintGeometry.Companion.controlCenterWifiAvoidShift(
                owner,
                geometry.trinityLeft,
                frame.transferMinimum,
                rawWifiCenterX + wifiHalfWidth
            )
        val wifiCenterX =
            rawWifiCenterX - wifiAvoidShift
        ControlCenterWifiPainter.draw(
            owner, canvas, frame.currentTextColor,
            wifiCenterX, wifiCenterY, wifiCanvasScale,
            wifiEndX, wifiEndY, sourceScale,
            geometry.targetTrinityLeft, geometry.targetTrinityTop,
            geometry.progress, geometry.projectedTarget
        )
        wifiRightBoundary = if (frame.controlCenterBatteryHost)
            (owner.swappedWifiTransfer.lastCentreX()
                    + owner.swappedWifiTransfer.lastHalfWidth())
        else
            if (frame.solvedRowActive && frame.solvedRow!!.visible(
                    ControlCenterRowComponent.WIFI
                )
            )
                SessionPaintGeometry.Companion.translatedRowX(
                    frame.solvedRow!!.right(ControlCenterRowComponent.WIFI),
                    frame.solvedRowTranslationX
                )
            else
                wifiCenterX + wifiHalfWidth
    }

    companion object {
        private fun captureSlot(
            owner: IconSession, geometry: SessionPaintGeometry,
            part: Int,
            left: Float, width: Float
        ) {
            if (part != 2) {
                return
            }
            if (owner.stackedDrawing) {
                val right = left + width
                owner.stackedLeft = if (java.lang.Float.isNaN(owner.stackedLeft))
                    left
                else min(owner.stackedLeft, left)
                owner.stackedRight = if (java.lang.Float.isNaN(owner.stackedRight))
                    right
                else max(owner.stackedRight, right)
            }
            if (geometry.captureCollapsedRow) {
                owner.captureCellularRowTarget(left, left + width)
            }
        }
    }
}
