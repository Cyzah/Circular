package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.WifiGlyphRenderer
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SwappedWifiMotion
import com.cyzah.circular.core.graphics.geometry.TransitionGeometry
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Draws one Wi-Fi glyph between the expanded row and its static status-bar endpoint.
 */
class SwappedWifiTransfer {
    private val motion = SwappedWifiMotion()
    private val transferGlyphs = WifiGlyphRenderer()
    private var diagnosticBucket = -1
    private var lastCentreX = 0f
    private var lastScale = 0f
    private var lastHalfWidth = 0f
    private var controlCenterGlyph = false
    private var glyphAlpha = 0f

    fun draw(
        owner: IconSession,
        canvas: Canvas,
        tint: Int,
        sourceOriginX: Float,
        sourceMinimum: Float,
        sourceCentreY: Float,
        targetOriginX: Float,
        targetOriginY: Float,
        progress: Float,
        targetReady: Boolean
    ) {
        this.controlCenterGlyph = false
        this.glyphAlpha = 1.0f
        val endpointSettings = owner.module.sessionSettings()
        val endpointUnit = SessionPaintPolicy.endpointUnit(
            owner.dp(endpointSettings.size()).toFloat(),
            owner.battery.getHeight().toFloat()
        )
        val sourceScale = max(0.0f, sourceMinimum / 120.0f) * owner.painter.regularWifiScale()
        val targetScale = (endpointUnit
                * endpointSettings.wifiScale() / 100.0f)
        val anchorY = owner.painter.anchorY(
            1,
            owner.module.state
        )
        val dualOffset = if (owner.module.state.dualSim()) 2.0f else 0.0f
        val sourceCentreX = (sourceOriginX + (sourceMinimum / 2.0f)
                + owner.painter.contentX(owner.painter.wifiContentSlot(owner.module.state)) * sourceMinimum / 120.0f)
        val glyphWidth = owner.painter.wifiHalfWidth(1.0f) * 2.0f
        val glyphHeight = owner.painter.wifiHalfHeight(1.0f) * 2.0f
        val adjustedSourceY = (sourceCentreY
                - ((dualOffset * sourceMinimum) / 120.0f))
        val endpoint: IconPainter = SessionPaintGeometry.Companion.endpointRuler(owner)
        val targetCentreX = (targetOriginX
                + (endpoint.wifiReadoutCentreX(owner.module.state) * endpointUnit))
        val targetCentreY = (targetOriginY
                + (endpoint.wifiReadoutCentreY(owner.module.state) * endpointUnit))
        this.motion.resolve(
            sourceCentreX, adjustedSourceY, sourceScale,
            targetCentreX, targetCentreY, targetScale, glyphWidth, glyphHeight, progress, targetReady
        )

        drawRendered(
            owner,
            canvas,
            tint,
            sourceCentreX,
            adjustedSourceY,
            sourceScale,
            targetCentreX,
            targetCentreY,
            targetScale,
            glyphWidth,
            glyphHeight,
            progress,
            targetReady,
            "base"
        )
    }

    fun drawControlCenter(
        owner: IconSession,
        canvas: Canvas,
        tint: Int,
        sourceCentreX: Float,
        sourceCentreY: Float,
        sourceScale: Float,
        targetCentreX: Float,
        targetCentreY: Float,
        targetScale: Float,
        weight: Int,
        alpha: Float,
        progress: Float,
        targetReady: Boolean
    ) {
        this.controlCenterGlyph = true
        this.glyphAlpha = alpha
        this.transferGlyphs.setWeightPercent(weight)
        val glyphWidth = this.transferGlyphs.bounds().width()
        val glyphHeight = this.transferGlyphs.bounds().height()
        this.motion.resolve(
            sourceCentreX,
            sourceCentreY,
            sourceScale,
            targetCentreX,
            targetCentreY,
            targetScale,
            glyphWidth,
            glyphHeight,
            progress,
            targetReady
        )
        drawRendered(
            owner,
            canvas,
            tint,
            sourceCentreX,
            sourceCentreY,
            sourceScale,
            targetCentreX,
            targetCentreY,
            targetScale,
            glyphWidth,
            glyphHeight,
            progress,
            targetReady,
            "module"
        )
    }

    fun lastCentreX(): Float {
        return this.lastCentreX
    }

    fun reset() {
        this.motion.reset()
        this.diagnosticBucket = -1
        this.lastCentreX = 0.0f
        this.lastScale = 0.0f
        this.lastHalfWidth = 0.0f
    }

    fun lastScale(): Float {
        return this.lastScale
    }

    fun lastHalfWidth(): Float {
        return this.lastHalfWidth
    }

    private fun drawRendered(
        owner: IconSession,
        canvas: Canvas,
        tint: Int,
        sourceCentreX: Float,
        sourceCentreY: Float,
        sourceScale: Float,
        targetCentreX: Float,
        targetCentreY: Float,
        targetScale: Float,
        glyphWidth: Float,
        glyphHeight: Float,
        progress: Float,
        targetReady: Boolean,
        sourceKind: String?
    ) {
        val rendered = this.motion.rendered()
        val scale: Float = renderedScale(
            rendered,
            glyphWidth,
            glyphHeight
        )
        if (!(scale > 0.0f) || !java.lang.Float.isFinite(scale)) {
            return
        }
        this.lastCentreX = rendered.centreX
        this.lastScale = scale
        this.lastHalfWidth = rendered.width() / 2.0f
        val centreY = if (this.controlCenterGlyph || targetReady)
            rendered.centreY
        else
            SessionPaintPolicy.clampIconCentreY(
                rendered.centreY,
                owner.painter.wifiHalfHeight(scale),
                0.0f,
                owner.battery.getHeight().toFloat()
            )
        if (this.controlCenterGlyph) {
            this.transferGlyphs.drawCentred(
                canvas, tint, owner.painter.wifiGlyphLevel(owner.module.state),
                rendered.centreX, centreY, scale, this.glyphAlpha
            )
        } else {
            val savedOpacity = owner.painter.opacity
            owner.painter.opacity = 1.0f
            try {
                owner.painter.drawWifiOnly(
                    canvas,
                    owner.module.state,
                    tint,
                    rendered.centreX,
                    centreY,
                    scale,
                    1.0f
                )
            } finally {
                owner.painter.opacity = savedOpacity
            }
        }
        logFrame(
            owner,
            sourceCentreX,
            sourceCentreY,
            sourceScale,
            targetCentreX,
            targetCentreY,
            targetScale,
            rendered.centreX,
            centreY,
            scale,
            progress,
            targetReady,
            sourceKind
        )
    }

    private fun logFrame(
        owner: IconSession,
        sourceX: Float,
        sourceY: Float,
        sourceScale: Float,
        targetX: Float,
        targetY: Float,
        targetScale: Float,
        renderedX: Float,
        renderedY: Float,
        renderedScale: Float,
        progress: Float,
        targetReady: Boolean,
        sourceKind: String?
    ) {
        val bucket = min(
            4,
            max(
                0,
                floor(
                    (Motion.clamp(progress) * 4.0f
                            + 0.0001f).toDouble()
                ).toInt()
            )
        )
        if ((owner.from != 5 && owner.from != 6)
            || this.diagnosticBucket == bucket
        ) {
            return
        }
        this.diagnosticBucket = bucket
        owner.battery.getLocationOnScreen(
            owner.batteryShapeFrameLocation
        )
        val screenOriginX = owner.batteryShapeFrameLocation[0]
        val screenOriginY = owner.batteryShapeFrameLocation[1]
        owner.module.log(
            6,
            ModuleMain.Companion.TAG,
            ("swappedWifiFrame host=" + owner.from
                    + " style="
                    + owner.module.controlCenterSettings().dualStyle()
                    + " sourceKind=" + sourceKind
                    + " progress=" + progress
                    + " targetReady=" + targetReady
                    + " source=" + sourceX + "," + sourceY
                    + "/" + sourceScale
                    + " target=" + targetX + "," + targetY
                    + "/" + targetScale
                    + " rendered=" + renderedX + "," + renderedY
                    + "/" + renderedScale
                    + " screenOrigin=" + screenOriginX
                    + "," + screenOriginY
                    + " renderedScreen="
                    + (screenOriginX + renderedX)
                    + "," + (screenOriginY + renderedY))
        )
    }

    companion object {
        private fun renderedScale(
            geometry: TransitionGeometry?,
            glyphWidth: Float,
            glyphHeight: Float
        ): Float {
            if (geometry == null || !geometry.valid() || !(glyphWidth > 0.0f) || !(glyphHeight > 0.0f)) {
                return 0.0f
            }
            return min(
                geometry.width() / glyphWidth,
                geometry.height() / glyphHeight
            )
        }
    }
}
