package com.cyzah.circular.ui.preview

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.cyzah.circular.core.graphics.BatteryShapeRenderer
import com.cyzah.circular.core.graphics.BatteryTransferRingRenderer
import com.cyzah.circular.core.graphics.ClosingBatteryReadout
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.OpeningBatteryReadout
import com.cyzah.circular.core.graphics.OpeningBatteryRenderer
import com.cyzah.circular.core.graphics.PainterConfigurator
import com.cyzah.circular.core.graphics.StackedOverlayRenderer
import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.WifiGlyphRenderer
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.animation.SwappedWifiMotion
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.core.graphics.geometry.WifiEndpointGeometry
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.data.settings.SharedPreferencesSettingsStore
import com.cyzah.circular.domain.settings.AppearanceSettings
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.ControlCenterAppearance
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.hook.runtime.ControlCenterRowComponent
import com.cyzah.circular.hook.runtime.ControlCenterRowMeasurementPolicy
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import kotlin.math.max
import kotlin.math.min

/**
 * Standalone settings preview. All drawing state is owned by the view.
 */
@SuppressLint("ViewConstructor")
class SettingsPreviewView(
    context: Context?,
    private val rawState: IconState,
    prefs: SharedPreferences,
    private val runtime: PreviewRuntimeState,
    private val styleSource: DualStyleSource,
    onDarkChanged: Runnable
) : View(context) {
    fun interface DualStyleSource {
        fun current(): DualStyle?
    }

    private val technologyRules = PreviewTechnologyRules()
    private val state: IconState
    private var sceneRevision = 0
    private var renderFailed = false
    private var failureLogged = false
    private var lastFailureLogTime: Long = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val painter = IconPainter()
    private val rowPainter = IconPainter()
    private val settingsCache: PreviewSettingsCache
    private val row = PreviewRowGeometry()
    private val swappedWifi = SwappedWifiMotion()
    private val wifiTransferGlyphs = WifiGlyphRenderer()
    private val stacked = StackedSignals()
    private val stackedTarget = StackedSignals.Target()
    private val stackedOverlay = StackedOverlayRenderer()
    private val batteryShapeRenderer = BatteryShapeRenderer()
    private val batteryShapeGeometry = BatteryShapeGeometry()
    private val previewShapeBounds = RectF()
    private val previewPointBounds = RectF()
    private val previewSolidBounds = RectF()
    private val previewClipBounds = RectF()
    private val previewClipPath = Path()
    private val openingBattery = OpeningBatteryMorph()
    private val openingRenderer = OpeningBatteryRenderer()
    private val openingReadout = OpeningBatteryReadout()
    private val closingReadout = ClosingBatteryReadout()
    private var batteryShapeReady = false
    private var previewShapeX = 0f
    private var previewShapeY = 0f
    private var previewShapeUnit = 0f
    private var previewBatteryTint = 0
    private val batteryTransferGeometry = RingGeometry()
    private val batteryRingRenderer = BatteryTransferRingRenderer()

    init {
        this.settingsCache = PreviewSettingsCache(
            SharedPreferencesSettingsStore(prefs), object : Runnable {
                override fun run() {
                    renderFailed = false
                    invalidate()
                }
            })
        this.state = this.technologyRules.update(
            rawState, this.sceneRevision,
            this.settingsCache.technologyText()
        )
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
        setFocusable(true)
        setOnClickListener(object : OnClickListener {
            override fun onClick(view: View?) {
                runtime.toggleDark()
                onDarkChanged.run()
            }
        })
    }

    fun setSceneRevision(revision: Int) {
        if (this.sceneRevision != revision) {
            this.sceneRevision = revision
            this.renderFailed = false
            invalidate()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        this.previewClipPath.rewind()
        if (width > 0 && height > 0) {
            this.previewClipBounds.set(0.0f, 0.0f, width.toFloat(), height.toFloat())
            val radius = dp(24.0f).toFloat()
            this.previewClipPath.addRoundRect(
                this.previewClipBounds, radius, radius, Path.Direction.CW
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.settingsCache.attach()
    }

    override fun onDetachedFromWindow() {
        this.settingsCache.detach()
        super.onDetachedFromWindow()
    }

    public override fun onDraw(canvas: Canvas) {
        if (this.renderFailed) return
        var save = -1
        var failure: Throwable? = null
        try {
            save = canvas.save()
            // AndroidView can draw beyond its Compose bounds during native animation.
            // Clip before any renderer translation/scale, then restore in the existing finally.
            canvas.clipPath(this.previewClipPath)
            drawPreview(canvas)
        } catch (error: RuntimeException) {
            failure = error
            this.renderFailed = true
        } catch (error: Error) {
            failure = error
            throw error
        } finally {
            if (save >= 0) {
                try {
                    canvas.restoreToCount(save)
                } catch (cleanup: RuntimeException) {
                    if (failure != null) {
                        if (cleanup !== failure) failure.addSuppressed(cleanup)
                    } else {
                        failure = cleanup
                        this.renderFailed = true
                    }
                } catch (cleanup: Error) {
                    if (failure == null) throw cleanup
                    if (cleanup !== failure) failure.addSuppressed(cleanup)
                }
            }
            if (failure is RuntimeException) logPreviewFailure(failure)
        }
    }

    private fun logPreviewFailure(failure: RuntimeException?) {
        try {
            val now = SystemClock.uptimeMillis()
            if (!this.failureLogged || now - this.lastFailureLogTime >= 5000L) {
                this.failureLogged = true
                this.lastFailureLogTime = now
                Log.w("CircularPreview", "Preview drawing failed; waiting for changed inputs", failure)
            }
        } catch (diagnosticFailure: RuntimeException) {
            // Diagnostic delivery is not allowed to fail the settings Activity.
        }
    }

    private fun drawPreview(canvas: Canvas) {
        val settings = this.settingsCache.settings()
        val expanded = this.settingsCache.expanded()
        this.technologyRules.update(
            this.rawState, this.sceneRevision,
            this.settingsCache.technologyText()
        )
        configurePainter(this.painter, settings)
        configurePainter(this.rowPainter, expanded)

        val density = getResources().getDisplayMetrics().density
        val width = getWidth() * 0.68f
        val centreY = (getHeight() - dp(24.0f)) / 2.0f
        // The left stage demonstrates appearance at a stable baseline; only the
        // right endpoint sample follows the configured status-bar size.
        val iconSize = dp(if (this.styleSource.current() == DualStyle.CAPSULE) 18.0f else 25.0f).toFloat()
        val actualIconSize = dp(settings.size().toFloat()).toFloat()
        this.painter.setContentPixelUnit(iconSize / 120.0f)
        this.rowPainter.setContentPixelUnit(iconSize / 120.0f)
        this.painter.updateBatteryRingGeometry(this.state)
        val tint = this.runtime.iconTint()
        val configured = PreviewMorph.stackedConfigured(
            settings.stackedShade(), settings.keepMergedShade(),
            settings.mode(), this.state
        )
        val progress = if (configured) this.runtime.expansion() else 0.0f
        val rowVisible = configured && (progress > 0.0f
                || this.runtime.batteryTransfer().active())
        solvePreviewRow(
            dp(expanded.batterySize().toFloat()).toFloat(), density,
            expanded.batteryNumberPlacement(), progress
        )

        // Measure and draw in device pixels, then magnify the entire scene once.
        val ringCentre = if (configured && this.row.valid())
            this.row.stageRowWidth() / 2.0f - iconSize / 2.0f
        else
            0.0f
        val ringLeft = ringCentre - iconSize / 2.0f
        val ringTop = -iconSize / 2.0f
        val ring = this.painter.batteryRingGeometry
        val unit = iconSize / 120.0f
        val ringBoundLeft = if (ring.kind == RingGeometry.Kind.CAPSULE)
            ring.capsuleOriginX + ring.left * ring.capsuleScale
        else
            ring.left
        val ringBoundRight = if (ring.kind == RingGeometry.Kind.CAPSULE)
            ring.capsuleOriginX + ring.right * ring.capsuleScale
        else
            ring.right
        var contentLeft = ringLeft + (ringBoundLeft - ring.strokeWidth / 2.0f) * unit
        var contentRight = ringLeft + (ringBoundRight + ring.strokeWidth / 2.0f) * unit
        if (configured && this.row.valid()) {
            contentLeft = min(contentLeft, -this.row.stageRowWidth() / 2.0f)
            contentRight = max(contentRight, this.row.stageRowWidth() / 2.0f)
        }
        var contentHeight = max(iconSize, dp(expanded.batterySize().toFloat()).toFloat())
        contentHeight = max(
            contentHeight,
            this.rowPainter.wifiHalfHeight(1.0f) * 2.0f
        )
        var zoom: Float = PreviewRowGeometry.Companion.fitScale(
            max(0.0f, width - dp(16.0f)),
            max(0.0f, (getHeight() - dp(40.0f)).toFloat()),
            dp(62.0f) / iconSize,
            max(iconSize, contentRight - contentLeft),
            contentHeight
        )
        zoom *= Motion.mix(1.0f, 0.84f, Motion.ease(progress))
        val save = canvas.save()
        canvas.translate(width / 2.0f, centreY)
        canvas.scale(zoom, zoom)
        val mergedCentre = ringLeft + (ringBoundLeft + ringBoundRight) * unit / 2.0f
        val stageCentre = (contentLeft + contentRight) / 2.0f
        canvas.translate(-Motion.mix(mergedCentre, stageCentre, progress), 0.0f)
        if (rowVisible && this.row.valid()) {
            drawExpandedScene(
                canvas, settings, expanded, ringLeft, ringTop,
                iconSize, density, progress, tint
            )
        } else {
            this.painter.draw(
                canvas, ringLeft, ringTop, iconSize,
                this.state, tint, settings.coloured()
            )
        }
        canvas.restoreToCount(save)

        // Keep the size sample at the stationary status-bar endpoint.
        this.painter.setMotion(0.0f)
        this.painter.setCapsuleCentred(true)
        val rightCentre = (getWidth() + width) / 2.0f
        this.painter.draw(
            canvas, rightCentre - actualIconSize / 2.0f,
            centreY - actualIconSize / 2.0f, actualIconSize,
            this.state, tint, settings.coloured()
        )
        this.paint.setColor(this.runtime.captionTint())
        this.paint.setTextSize(getResources().getDisplayMetrics().scaledDensity * 12.0f)
        this.paint.setTextAlign(Align.CENTER)
        val hint = if (this.paint.measureText("实时预览 · 点按换背景") > width - dp(12.0f))
            "实时预览"
        else
            "实时预览 · 点按换背景"
        val baseline = getHeight() - dp(11.0f) - this.paint.descent()
        canvas.drawText(hint, width / 2.0f, baseline, this.paint)
        canvas.drawText(settings.size().toString() + " dp", rightCentre, baseline, this.paint)
        this.paint.setColor(if (this.runtime.dark()) -13090221 else -2892562)
        this.paint.setStrokeWidth(dp(1.0f).toFloat())
        canvas.drawLine(width, dp(20.0f).toFloat(), width, (getHeight() - dp(24.0f)).toFloat(), this.paint)
        if (this.row.active()) {
            postInvalidateOnAnimation()
        }
    }

    private fun configurePainter(target: IconPainter, settings: AppearanceSettings) {
        PainterConfigurator.apply(target, settings, this.runtime.duoWifiProgress())
        target.setCapsuleDual(settings.capsuleDual())
        target.setDuoDual(this.state.dualSim() && settings.duoDual())
        target.setCapsuleAspect(IconPainter.Companion.CAPSULE_ASPECT)
        target.setCapsuleCentred(false)
        target.setMotion(0.0f)
    }

    private fun drawExpandedScene(
        canvas: Canvas, settings: AppearanceSettings, expanded: AppearanceSettings,
        ringLeft: Float, ringTop: Float, iconSize: Float,
        density: Float, progress: Float, tint: Int
    ) {
        val transfer = this.runtime.batteryTransfer()
        val replacingBattery = progress > BatteryTransferConfig.HANDOFF_MAX
                || transfer.active()
        var mask: Int = (IconPainter.Companion.MASK_HIDE_SIGNALS or IconPainter.Companion.MASK_HIDE_CENTRE
                or IconPainter.Companion.MASK_HIDE_WIFI or IconPainter.Companion.MASK_HIDE_TECHNOLOGY)
        if (replacingBattery) {
            mask = mask or if (transfer.active())
                IconPainter.Companion.MASK_HIDE_BATTERY_RING
            else
                IconPainter.Companion.MASK_HIDE_BATTERY
        }
        this.painter.drawPartMasked(
            canvas, ringLeft, ringTop, iconSize,
            this.state, tint, settings.coloured(), 7 or mask, 1.0f
        )
        prepareBatteryTransfer(ringLeft, ringTop, iconSize, tint, transfer, settings)
        closingReadout.reset()
        if (this.batteryShapeReady && !transfer.drawState().openingShape()) {
            closingReadout.prepare(
                this.painter, this.rowPainter, this.batteryShapeRenderer,
                this.state, this.state.battery, settings.batteryNumberPlacement(),
                ringLeft, ringTop, iconSize / 120.0f, this.previewSolidBounds, progress
            )
        }
        drawStacked(
            canvas, settings, expanded, ringLeft, ringTop,
            iconSize, density, progress, tint
        )
        if (this.row.visible(ControlCenterRowComponent.WIFI)) {
            drawWifiOverlay(
                canvas, settings, ringLeft, ringTop,
                iconSize, progress, tint
            )
        }
        if (replacingBattery) {
            if (this.openingBattery.valid) {
                this.openingReadout.prepare(
                    this.painter, this.rowPainter, this.batteryShapeRenderer,
                    this.state, this.state.battery, settings.batteryNumberPlacement(),
                    ringLeft, ringTop, iconSize / 120.0f, this.openingBattery.totalLeft,
                    this.openingBattery.totalTop, this.openingBattery.totalWidth,
                    this.openingBattery.totalHeight, progress
                )
                val save = canvas.save()
                try {
                    this.openingRenderer.draw(
                        canvas, this.openingBattery, this.state.battery,
                        tint, this.previewBatteryTint, transfer.drawState().openingAlpha()
                    )
                } finally {
                    canvas.restoreToCount(save)
                }
            }
            if (transfer.drawState().openingShape() && this.openingBattery.valid) {
                this.openingReadout.draw(
                    canvas, this.painter, this.state, tint, settings.coloured(),
                    1.0f - this.openingBattery.solidAlpha
                )
            } else if (transfer.drawState().openingShape()) {
                this.painter.drawPartMasked(
                    canvas, ringLeft, ringTop, iconSize,
                    this.state, tint, settings.coloured(),
                    (1 or IconPainter.Companion.MASK_HIDE_CENTRE or IconPainter.Companion.MASK_HIDE_SIGNALS
                            or IconPainter.Companion.MASK_HIDE_WIFI or IconPainter.Companion.MASK_HIDE_TECHNOLOGY
                            or (if (this.openingBattery.valid) IconPainter.Companion.MASK_HIDE_BATTERY_RING else 0)),
                    if (this.openingBattery.valid)
                        1.0f - this.openingBattery.solidAlpha
                    else
                        1.0f - BatteryTransferPolicy.gestureShapeAlpha(progress)
                )
            }
            drawBatteryTransfer(
                canvas, ringLeft, ringTop, iconSize,
                tint, transfer, settings
            )
        }
    }

    private fun prepareBatteryTransfer(
        baseLeft: Float,
        baseTop: Float,
        baseSize: Float,
        plainTint: Int,
        transfer: PreviewBatteryTransfer,
        settings: AppearanceSettings
    ) {
        this.batteryShapeReady = false
        this.openingBattery.valid = false
        this.stackedOverlay.clearBatteryObstacle()
        PreviewBatteryTransfer.Companion.copyClosedRing(
            this.painter.batteryRingGeometry,
            this.batteryTransferGeometry
        )
        val geometry = this.batteryTransferGeometry
        if (!geometry.valid()) {
            return
        }
        val ringUnit = baseSize / 120.0f
        val batterySize = dp(ControlCenterAppearance.BATTERY_SIZE.toFloat()).toFloat()
        val canonicalMinimum = BatteryShapeLayout.minimumPx(batterySize)
        val shapeUnit = canonicalMinimum / 120.0f
        val placement = settings.batteryNumberPlacement()
        val density = getResources().getDisplayMetrics().density
        var leftAtZero = BatteryShapeLayout.fullBatteryLeftPx(
            0.0f, batterySize, batterySize, canonicalMinimum, placement, density
        )
        if (placement == BatteryNumberPlacement.OUTSIDE) {
            leftAtZero += this.batteryShapeRenderer.outsideNumberAdaptiveBatteryShiftUnits(
                this.rowPainter, this.state
            ) * shapeUnit
        }
        val shapeOriginX = SessionPaintPolicy.solvedBatteryOriginX(
            this.row.left(ControlCenterRowComponent.BATTERY),
            this.row.right(ControlCenterRowComponent.BATTERY), leftAtZero
        )
        val shapeOriginY = -batterySize / 2.0f
        val batteryTint = BatteryTransferPolicy.traceTint(
            this.state.battery,
            this.state.charging,
            this.state.powerSave,
            this.state.performanceMode,
            settings.coloured(),
            plainTint,
            settings.lowBatteryThreshold(),
            settings.chargeColor(),
            settings.lowColor(),
            settings.saverColor(),
            settings.performanceColor()
        )
        if (transfer.drawState().batteryShapeVisible()
            && this.runtime.expansion() > BatteryTransferConfig.HANDOFF_MAX
        ) {
            val fullWidth = BatteryShapeLayout.widthUnits(placement)
            val fullHeight = (fullWidth
                    * BatteryTransferConfig.SVG_HEIGHT
                    / BatteryTransferConfig.SVG_WIDTH)
            val targetX = baseLeft + (geometry.startX * ringUnit)
            val targetY = baseTop + (geometry.startY * ringUnit)
            val endX = (targetX - shapeOriginX) / shapeUnit
            val endY = (targetY - shapeOriginY) / shapeUnit
            var fullCentreX =
                BatteryShapeLayout.centreXUnits(placement)
            fullCentreX -= (BatteryShapeLayout.controlCenterBatteryLeftShiftPx(
                getResources().getDisplayMetrics().density
            )
                    / shapeUnit)
            fullCentreX -= (BatteryShapeLayout.nonOutsideBatteryLeftShiftPx(
                placement,
                getResources().getDisplayMetrics().density
            )
                    / shapeUnit)
            if (placement == BatteryNumberPlacement.OUTSIDE) {
                fullCentreX += this.batteryShapeRenderer
                    .outsideNumberAdaptiveBatteryShiftUnits(
                        this.rowPainter,
                        this.state
                    )
                fullCentreX -= (BatteryShapeLayout.outsideGroupLeftShiftPx(
                    getResources().getDisplayMetrics().density
                )
                        / shapeUnit)
                fullCentreX -= (BatteryShapeLayout.outsideNumberRightShiftPx(
                    getResources().getDisplayMetrics().density
                )
                        / shapeUnit)
            }
            val pointDiameter = BatteryShapeLayout.transferPointDiameterUnits(
                geometry.strokeWidth, ringUnit, shapeUnit
            )
            val fullLeft = shapeOriginX + (fullCentreX - fullWidth / 2.0f) * shapeUnit
            val fullTop = (shapeOriginY
                    + (BatteryTransferConfig.FULL_CENTRE_Y_UNITS - fullHeight / 2.0f) * shapeUnit)
            this.previewSolidBounds.set(
                fullLeft, fullTop,
                fullLeft + fullWidth * shapeUnit, fullTop + fullHeight * shapeUnit
            )
            this.batteryShapeGeometry.update(
                this.runtime.batteryCollapse(),
                fullCentreX,
                BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
                fullWidth,
                fullHeight,
                endX,
                endY,
                pointDiameter,
                pointDiameter
            )
            this.batteryShapeReady = true
            this.previewShapeX = shapeOriginX
            this.previewShapeY = shapeOriginY
            this.previewShapeUnit = shapeUnit
            this.previewBatteryTint = batteryTint
            this.previewShapeBounds.set(
                shapeOriginX + (this.batteryShapeGeometry.centreX - this.batteryShapeGeometry.width / 2.0f) * shapeUnit,
                shapeOriginY + (this.batteryShapeGeometry.centreY - this.batteryShapeGeometry.height / 2.0f) * shapeUnit,
                shapeOriginX + (this.batteryShapeGeometry.centreX + this.batteryShapeGeometry.width / 2.0f) * shapeUnit,
                shapeOriginY + (this.batteryShapeGeometry.centreY + this.batteryShapeGeometry.height / 2.0f) * shapeUnit
            )
            val pointRadius = pointDiameter * shapeUnit / 2.0f
            this.previewPointBounds.set(
                targetX - pointRadius, targetY - pointRadius,
                targetX + pointRadius, targetY + pointRadius
            )
            this.stackedOverlay.setBatteryObstacle(this.previewShapeBounds, this.previewPointBounds)
            if (transfer.drawState().openingShape()) {
                this.openingBattery.update(
                    geometry, baseLeft, baseTop, ringUnit, ringUnit,
                    fullLeft, fullTop, fullWidth * shapeUnit, fullHeight * shapeUnit,
                    this.runtime.expansion()
                )
                if (this.openingBattery.valid) {
                    this.previewSolidBounds.set(
                        fullLeft, fullTop,
                        fullLeft + fullWidth * shapeUnit, fullTop + fullHeight * shapeUnit
                    )
                    this.previewShapeBounds.set(
                        this.openingBattery.left, this.openingBattery.top,
                        this.openingBattery.right, this.openingBattery.bottom
                    )
                    this.stackedOverlay.setBatteryObstacle(this.previewShapeBounds, this.previewPointBounds)
                    this.stackedOverlay.setOpeningBatteryObstacle(this.openingBattery, this.previewSolidBounds)
                    this.batteryShapeGeometry.update(
                        0.0f,
                        fullCentreX, BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
                        fullWidth, fullHeight, endX, endY, pointDiameter, pointDiameter
                    )
                }
            }
        }
    }

    private fun drawBatteryTransfer(
        canvas: Canvas, baseLeft: Float, baseTop: Float, baseSize: Float,
        plainTint: Int, transfer: PreviewBatteryTransfer, settings: AppearanceSettings
    ) {
        if (this.batteryShapeReady) {
            val reopeningLayer = if (transfer.drawState().openingAlpha() < 1.0f)
                canvas.saveLayerAlpha(null, Math.round(255 * transfer.drawState().openingAlpha()))
            else
                -1
            val readoutLayer = if (closingReadout.cutout()) canvas.saveLayer(this.previewShapeBounds, null) else -1
            val save = canvas.save()
            try {
                canvas.translate(this.previewShapeX, this.previewShapeY)
                canvas.scale(this.previewShapeUnit, this.previewShapeUnit)
                this.batteryShapeRenderer.draw(
                    canvas, this.rowPainter, this.state,
                    this.batteryShapeGeometry, if (closingReadout.valid())
                        BatteryNumberPlacement.HIDDEN
                    else
                        settings.batteryNumberPlacement(),
                    this.state.battery,
                    BatteryTransferConfig.chargedAreaTint(this.previewBatteryTint), plainTint,
                    if (this.openingBattery.valid)
                        this.openingBattery.bodyAlpha
                    else
                        if (transfer.drawState().openingShape())
                            BatteryTransferPolicy.gestureShapeAlpha(this.runtime.expansion())
                        else
                            1.0f,
                    if (this.openingBattery.valid) this.openingBattery else null
                )
            } finally {
                canvas.restoreToCount(save)
                try {
                    closingReadout.drawSource(
                        canvas,
                        BatteryTransferConfig.chargedAreaTint(this.previewBatteryTint)
                    )
                } finally {
                    if (readoutLayer >= 0) canvas.restoreToCount(readoutLayer)
                    if (reopeningLayer >= 0) canvas.restoreToCount(reopeningLayer)
                }
            }
            closingReadout.drawTarget(canvas, this.state, plainTint, settings.coloured())
        }
        if (transfer.drawState().reopenAlpha() > 0) {
            this.batteryRingRenderer.drawReopen(
                canvas, this.batteryTransferGeometry,
                transfer.drawState(), this.state.battery, plainTint, this.previewBatteryTint,
                baseLeft, baseTop, baseSize / 120.0f
            )
        }
        if (transfer.active()) {
            val batteryTint = BatteryTransferPolicy.traceTint(
                this.state.battery,
                this.state.charging, this.state.powerSave, this.state.performanceMode,
                settings.coloured(), plainTint, settings.lowBatteryThreshold(),
                settings.chargeColor(), settings.lowColor(), settings.saverColor(), settings.performanceColor()
            )
            this.batteryRingRenderer.draw(
                canvas, this.batteryTransferGeometry, transfer.drawState(),
                this.state.battery, plainTint, batteryTint, baseLeft, baseTop, baseSize / 120.0f
            )
        }
    }

    private fun solvePreviewRow(
        batterySize: Float, density: Float, placement: BatteryNumberPlacement?,
        expansion: Float
    ) {
        val animateReflow = expansion >= 0.999f
        val outsideWidth = if (placement == BatteryNumberPlacement.OUTSIDE
            && this.state.battery >= 0
        )
            this.batteryShapeRenderer.measureOutsideNumberSvg(
                this.rowPainter, IconPainter.Companion.NUMBERS[min(100, this.state.battery)]
            )
        else
            0.0f
        val labelSize: Float =
            StackedSignals.Companion.labelSize(
                if (this.state.dualSim()) this.settingsCache.expanded().technologyScale()
                else com.cyzah.circular.domain.settings.ControlCenterAppearance.SINGLE_TECHNOLOGY_SCALE
            ) * density
        val labelWidth = max(
            this.rowPainter.measureTechnology(previewFirstLabel(), labelSize),
            this.rowPainter.measureTechnology(previewSecondLabel(), labelSize)
        )
        this.row.update(
            ControlCenterRowMeasurementPolicy.batteryWidth(
                batterySize, placement, outsideWidth
            ),
            this.rowPainter.wifiHalfWidth(
                ControlCenterAppearance.WIFI_FINAL_CANVAS_SCALE * this.settingsCache.expanded().wifiScale() / 100.0f
            ) * 2.0f,
            if (this.state.dualSim())
                StackedSignals.Companion.fillWidthPx(labelWidth, density, placement)
            else
                StackedSignals.Companion.singleRowFillWidthPx(labelWidth, density),
            this.state.battery >= 0,
            this.state.showWifi() || (!animateReflow && this.state.dualSim()
                    && this.styleSource.current() == DualStyle.DUO && this.runtime.duoWifiProgress() > 0.0f),
            this.state.stackedReady(), density,
            SystemClock.uptimeMillis(), animateReflow
        )
    }

    private fun previewFirstLabel(): String? {
        if (!this.state.dualSim()) {
            return previewDataLabel()
        }
        return if (this.state.mobileIsSlot0())
            previewDataLabel()
        else
            previewOtherLabel()
    }

    private fun previewSecondLabel(): String? {
        if (!this.state.dualSim()) {
            return null
        }
        return if (this.state.mobileIsSlot0())
            previewOtherLabel()
        else
            previewDataLabel()
    }

    private fun previewDataLabel(): String? {
        return if (PreviewMorph.shouldShowStackedTechnology(
                this.state.showWifi()
            )
            && this.state.mobileKnown
            && this.state.simPresent
            && this.state.inService
            && !this.state.airplane
        )
            StackedSignals.Companion.clip(this.state.mobileTechnology)
        else
            null
    }

    private fun previewOtherLabel(): String? {
        return if (PreviewMorph.shouldShowStackedTechnology(
                this.state.showWifi()
            )
            && this.state.secondaryKnown
            && this.state.secondarySimPresent
            && this.state.secondaryInService
            && !this.state.airplane
        )
            StackedSignals.Companion.clip(this.state.mobileTechnology)
        else
            null
    }

    private fun drawWifiOverlay(
        canvas: Canvas?, settings: AppearanceSettings,
        ringLeft: Float, ringTop: Float, size: Float, progress: Float, tint: Int
    ) {
        val style = settings.dualStyle()
        val unit = size / 120.0f
        val homeX = this.painter.wifiReadoutCentreX(this.state)
        val homeY = this.painter.wifiReadoutCentreY(this.state)
        val startScale = this.painter.wifiReadoutScale(this.state) * unit
        val endX = this.row.centre(ControlCenterRowComponent.WIFI)
        val endY = 0.0f
        val targetX = ringLeft + homeX * unit
        val targetY = ringTop + homeY * unit
        this.wifiTransferGlyphs.setWeightPercent(
            WifiEndpointGeometry.weight(
                settings.wifiWeight(), this.settingsCache.expanded().wifiWeight(), progress
            )
        )
        val glyphWidth = this.wifiTransferGlyphs.bounds().width()
        this.swappedWifi.resolve(
            endX, endY, this.settingsCache.expanded().wifiScale() / 100.0f, targetX, targetY, startScale,
            glyphWidth, this.wifiTransferGlyphs.bounds().height(), progress, true
        )
        val frame = this.swappedWifi.rendered()
        val endpointAlpha = if (style == DualStyle.DUO)
            DuoLayout.wifiAlpha(this.runtime.duoWifiProgress())
        else
            1.0f
        this.wifiTransferGlyphs.drawCentred(
            canvas, tint, this.painter.wifiGlyphLevel(this.state),
            frame.centreX, frame.centreY, frame.width() / glyphWidth,
            WifiEndpointGeometry.alpha(endpointAlpha, progress)
        )
    }

    private fun drawStacked(
        canvas: Canvas?, settings: AppearanceSettings, expanded: AppearanceSettings,
        ringLeft: Float, ringTop: Float, size: Float,
        density: Float, progress: Float, tint: Int
    ) {
        val unit = size / 120.0f
        val style = settings.dualStyle()
        this.stackedOverlay.frame(
            this.row.left(ControlCenterRowComponent.CELLULAR),
            this.row.right(ControlCenterRowComponent.CELLULAR),
            0.0f, progress,
            StackedOverlayLayout.signalXProgress(progress, expanded.batteryNumberPlacement()),
            ringLeft, ringTop, unit, unit, density, progress,
            this.runtime.duoWifiProgress(),
            this.painter.capsuleDotRadius(this.state) * unit,
            tint, settings.weight(), getLayoutDirection() == LAYOUT_DIRECTION_RTL,
            SessionPaintPolicy.fitsStackedLabels(true), true,
            style, expanded.batteryNumberPlacement()
        )
        this.stackedOverlay.setTechnologyEndpointUnit(unit)
        this.stackedOverlay.setLabelsEnabled(
            SessionPaintPolicy.showsStackedTechnology(true, progress)
        )
        this.stackedOverlay.draw(
            canvas, this.rowPainter, this.painter,
            this.stacked, this.stackedTarget, this.state,
            previewFirstLabel(), previewSecondLabel()
        )
    }

    private fun dp(value: Float): Int {
        return Math.round(value * getResources().getDisplayMetrics().density)
    }
}
