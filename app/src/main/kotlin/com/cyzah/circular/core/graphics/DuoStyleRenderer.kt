package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.StatusColors
import kotlin.math.min

class DuoStyleRenderer(private val owner: IconPainter, private val signals: DuoSignalRenderer) {
    private var enabled = false
    private var wifiProgress = 0f
    private var lastWifiLevel = -1
    private var lastTechnology = ""
    private val technologyCache = TextMetricCache()
    private val digitCache = TextMetricCache()
    private var preparedTechnologySource = ""
    private var preparedTechnologyBase = ""
    private var preparedTechnologySuffix = ""
    private var preparedTopHalfAngle = 0f
    private var preparedBottomHalfAngle = 0f
    private var preparedTopLeft = 0f
    private var preparedTopRight = 0f
    private var preparedBottomLeft = 0f
    private var preparedBottomRight = 0f
    private val batteryOpening = RectF()
    private val wifiOpening = RectF()
    private val signalOpening = RectF()
    private val technologyOpening = RectF()

    private fun batteryOffsetX(below: Float): Float {
        return contentOffset(owner.contentX(0), owner.contentX(2), below)
    }

    private fun batteryOffsetY(below: Float): Float {
        return contentOffset(owner.contentY(0), owner.contentY(2), below)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun enabled(): Boolean {
        return this.enabled
    }

    fun wifiLevelFor(state: IconState): Int {
        return if (state.showWifi()) state.wifiLevel else this.lastWifiLevel
    }

    fun setWifiProgress(progress: Float) {
        this.wifiProgress = Motion.clamp(progress)
    }

    fun technologyTextSize(state: IconState): Float {
        prepareTechnology(state.centreTechnology())
        return this.owner.networkPaint.getTextSize()
    }

    private fun prepareTechnology(technology: String): Float {
        val typeface: Any? = this.owner.networkPaint.getTypeface()
        if (this.technologyCache.matches(
                technology,
                this.owner.technologyPercent,
                this.owner.technologyWeightPercent,
                typeface
            )
        ) {
            this.owner.networkPaint.setTextSize(this.technologyCache.textSize())
            return this.technologyCache.width()
        }
        this.owner.networkPaint.setTextSize(
            AppearanceScale.technologyTextSize(this.owner.technologyPercent)
        )
        if (this.preparedTechnologySource != technology) {
            this.preparedTechnologySource = technology
            this.preparedTechnologyBase =
                IconGeometry.technologyBase(technology)
            this.preparedTechnologySuffix =
                if (technology.length > this.preparedTechnologyBase.length)
                    technology.substring(
                        this.preparedTechnologyBase.length
                    )
                else
                    ""
        }
        val base = this.preparedTechnologyBase
        val suffix = this.preparedTechnologySuffix
        if (!suffix.isEmpty()) {
            this.owner.suffixPaint.setTextSize(
                this.owner.networkPaint.getTextSize()
                        * IconPainter.Companion.TECHNOLOGY_SUFFIX_SCALE
            )
        }
        var width = this.owner.technologyWidth(base, suffix)
        this.owner.networkPaint.getTextBounds(
            base,
            0,
            base.length,
            this.owner.networkTextBounds
        )
        val fit = BatteryHeader.fit(
            width,
            this.owner.networkTextBounds.height().toFloat(),
            82.0f,
            34.0f
        )
        if (fit < 1.0f) {
            this.owner.networkPaint.setTextSize(
                this.owner.networkPaint.getTextSize() * fit
            )
            if (!suffix.isEmpty()) {
                this.owner.suffixPaint.setTextSize(
                    this.owner.networkPaint.getTextSize()
                            * IconPainter.Companion.TECHNOLOGY_SUFFIX_SCALE
                )
            }
            width = this.owner.technologyWidth(base, suffix)
        }
        this.technologyCache.store(
            technology,
            this.owner.technologyPercent,
            this.owner.technologyWeightPercent,
            typeface,
            width,
            this.owner.networkPaint.getTextSize(),
            0,
            0
        )
        return width
    }

    private fun batteryDigits(state: IconState): String {
        return (if (com.cyzah.circular.core.graphics.geometry.DuoLayout.showBatteryDigits(
                this.owner.showPercent,
                state.battery >= 0
            )
        )
            com.cyzah.circular.core.graphics.IconPainter.Companion.NUMBERS[kotlin.math.min(100, state.battery)]
        else
            "")!!
    }

    private fun prepareBatteryDigits(digits: String): Float {
        val typeface: Any? = this.owner.textPaint.getTypeface()
        val layoutFlags = owner.batteryWeightPercent() * 2 + (if (this.owner.swapContents) 1 else 0)
        if (this.digitCache.matches(
                digits,
                this.owner.percentScale,
                layoutFlags,
                typeface
            )
        ) {
            this.owner.textPaint.setTextSize(this.digitCache.textSize())
            this.owner.numberBounds.set(
                0,
                this.digitCache.top(),
                0,
                this.digitCache.bottom()
            )
            return this.digitCache.width()
        }
        this.owner.textPaint.setTextSize(
            BatteryHeader.textSize(this.owner.percentScale)
        )
        this.owner.textPaint.numberBounds(digits, this.owner.numberBounds)
        var width = this.owner.textPaint.measureNumber(digits)
        val fit = DuoLayout.fitBatteryReadout(
            width,
            this.owner.numberBounds.height().toFloat(),
            BatteryHeader.readoutWidth(this.owner.swapContents)
        )
        if (fit < 1.0f) {
            this.owner.textPaint.setTextSize(
                this.owner.textPaint.getTextSize() * fit
            )
            width = this.owner.textPaint.measureNumber(digits)
            this.owner.textPaint.numberBounds(digits, this.owner.numberBounds)
        }
        this.digitCache.store(
            digits,
            this.owner.percentScale,
            layoutFlags,
            typeface,
            width,
            this.owner.textPaint.getTextSize(),
            this.owner.numberBounds.top,
            this.owner.numberBounds.bottom
        )
        return width
    }

    private fun drawRing(
        canvas: Canvas,
        state: IconState,
        batteryTint: Int,
        statusTint: Int,
        technologyWidth: Float,
        digitsWidth: Float,
        progress: Float,
        batteryBelowProgress: Float
    ) {
        if (owner.hasContentOffsets()) {
            owner.drawPositionedRing(canvas, state, batteryTint, statusTint)
            return
        }
        val weight = this.owner.weight
        val bottomHalfAngle = this.preparedBottomHalfAngle
        val topHalfAngle = this.preparedTopHalfAngle
        val sideSweep = DuoLayout.sideArcSweep(
            topHalfAngle,
            bottomHalfAngle
        )
        val leftArcStart = 90.0f + bottomHalfAngle
        val rightArcStart = 270.0f + topHalfAngle
        val strokeWidth = 7.5f
        val topClosed = topHalfAngle <= 0.0f

        this.owner.stroke(
            if (state.battery == 0) batteryTint else statusTint,
            48,
            strokeWidth
        )
        if (topClosed) {
            canvas.drawArc(
                this.owner.ring,
                leftArcStart,
                sideSweep * 2.0f,
                false,
                this.owner.p
            )
        } else {
            canvas.drawArc(
                this.owner.ring,
                leftArcStart,
                sideSweep,
                false,
                this.owner.p
            )
            canvas.drawArc(
                this.owner.ring,
                rightArcStart,
                sideSweep,
                false,
                this.owner.p
            )
        }
        if (state.battery <= 0) {
            return
        }
        val fillSweep = ((sideSweep * 2.0f)
                * min(100, state.battery) / 100.0f)
        this.owner.stroke(batteryTint, 255, strokeWidth)
        if (topClosed) {
            canvas.drawArc(
                this.owner.ring,
                leftArcStart,
                fillSweep,
                false,
                this.owner.p
            )
            return
        }
        canvas.drawArc(
            this.owner.ring,
            leftArcStart,
            min(sideSweep, fillSweep),
            false,
            this.owner.p
        )
        if (fillSweep > sideSweep) {
            canvas.drawArc(
                this.owner.ring,
                rightArcStart,
                fillSweep - sideSweep,
                false,
                this.owner.p
            )
        }
    }

    private fun drawBattery(
        canvas: Canvas,
        state: IconState,
        digits: String,
        digitsWidth: Float,
        batteryTint: Int,
        batteryBelowProgress: Float
    ) {
        val textLeft = (60.0f + batteryOffsetX(batteryBelowProgress)
                - (DuoLayout.batteryReadoutWidth(digitsWidth, state.charging)
                / 2.0f))
        val baseline = batteryBaseline(batteryBelowProgress) + batteryOffsetY(batteryBelowProgress)
        if (digits.isEmpty()) {
            return
        }
        val numberTint = StatusColors.batteryNumberTint(batteryTint)
        this.owner.textPaint.setColor(numberTint)
        this.owner.textPaint.setAlpha(
            DisplayPolicy.paintAlpha(
                numberTint,
                255,
                this.owner.opacity
            )
        )
        canvas.drawText(
            digits,
            textLeft + this.owner.textPaint.leftPadding(),
            baseline,
            this.owner.textPaint
        )
    }

    fun batteryReadoutBounds(state: IconState, out: RectF) {
        out.setEmpty()
        val digits = batteryDigits(state)
        if (digits.isEmpty()) {
            return
        }
        val width = prepareBatteryDigits(digits)
        // The metric cache retains only vertical bounds, so measure the actual
        // left/right glyph overhang at the exact fitted size before projecting.
        this.owner.textPaint.numberBounds(digits, this.owner.numberBounds)
        val data: Boolean = dataAvailable(
            state.dataEnabled, state.mobileKnown, state.simPresent,
            state.inService, state.airplane
        )
        val below: Float = batteryBelowProgress(
            batteryBelowWanted(state.showWifi(), data),
            data, this.wifiProgress
        )
        val left = (60.0f + batteryOffsetX(below) - DuoLayout.batteryReadoutWidth(width, state.charging) / 2.0f
                + this.owner.textPaint.leftPadding())
        val baseline = batteryBaseline(below) + batteryOffsetY(below)
        out.set(
            left + this.owner.numberBounds.left, baseline + this.owner.numberBounds.top,
            left + this.owner.numberBounds.right, baseline + this.owner.numberBounds.bottom
        )
    }

    private fun batteryBaseline(belowProgress: Float): Float {
        return (Motion.mix(DuoLayout.batteryCentreY(), DuoLayout.batteryCentreY(true), belowProgress)
                - (this.owner.numberBounds.top + this.owner.numberBounds.bottom) / 2.0f)
    }

    private fun drawCentreContent(
        canvas: Canvas,
        state: IconState,
        statusTint: Int,
        mask: Int,
        gapTechnology: String,
        dataAvailable: Boolean,
        progress: Float
    ) {
        if ((mask and IconPainter.Companion.MASK_HIDE_CENTRE) != 0) {
            return
        }
        val wifiAlpha = DuoLayout.wifiAlpha(progress)
        if ((mask and IconPainter.Companion.MASK_HIDE_WIFI) == 0
            && wifiAlpha > 0.0f
        ) {
            val save = canvas.save()
            try {
                canvas.translate(owner.contentX(1), DuoLayout.wifiCentreY() - 57.0f + owner.contentY(1))
                canvas.scale(
                    this.owner.wifiScale,
                    this.owner.wifiScale,
                    60.0f,
                    57.0f
                )
                this.owner.wifiGlyphs.drawAtOrigin(
                    canvas,
                    statusTint,
                    wifiLevelFor(state),
                    this.owner.opacity,
                    wifiAlpha
                )
            } finally {
                canvas.restoreToCount(save)
            }
        }
        val technologyAlpha = DuoLayout.technologyAlpha(
            progress,
            dataAvailable
        )
        if (gapTechnology.isEmpty() || technologyAlpha <= 0.0f) {
            return
        }
        val savedOpacity = this.owner.opacity
        this.owner.opacity *= technologyAlpha
        try {
            this.owner.technologyText(
                canvas,
                gapTechnology,
                60.0f + owner.contentX(2),
                DuoLayout.technologyBaseline() + owner.contentY(2),
                statusTint
            )
        } finally {
            this.owner.opacity = savedOpacity
        }
    }

    fun draw(
        canvas: Canvas,
        state: IconState,
        batteryTint: Int,
        statusTint: Int,
        mask: Int
    ) {
        val technology = state.centreTechnology()
        if (!technology.isEmpty()) {
            this.lastTechnology = technology
        }
        if (state.showWifi() && state.wifiLevel >= 0) {
            this.lastWifiLevel = state.wifiLevel
        }
        val progress = Motion.clamp(this.wifiProgress)
        val dataAvailable: Boolean = dataAvailable(
            state.dataEnabled,
            state.mobileKnown,
            state.simPresent,
            state.inService,
            state.airplane
        )
        val batteryBelowWanted: Boolean = batteryBelowWanted(
            state.showWifi(),
            dataAvailable
        )
        val batteryBelowProgress: Float = batteryBelowProgress(
            batteryBelowWanted,
            dataAvailable,
            progress
        )
        val gapTechnology = if (dataAvailable)
            (if (technology.isEmpty()) this.lastTechnology else technology)
        else
            ""

        val technologyWidth = prepareTechnology(gapTechnology)
        val digits = batteryDigits(state)
        val digitsWidth = prepareBatteryDigits(digits)
        prepareRingGeometry(
            state,
            technologyWidth,
            digitsWidth,
            progress,
            batteryBelowProgress
        )

        if (BatteryTransferPolicy.drawsBatteryRing(mask)) {
            drawRing(
                canvas,
                state,
                batteryTint,
                statusTint,
                technologyWidth,
                digitsWidth,
                progress,
                batteryBelowProgress
            )
        }

        if (BatteryTransferPolicy.drawsBatteryContent(mask)) {
            drawBattery(
                canvas,
                state,
                digits,
                digitsWidth,
                batteryTint,
                batteryBelowProgress
            )
        }

        drawCentreContent(
            canvas,
            state,
            statusTint,
            mask,
            gapTechnology,
            dataAvailable,
            progress
        )

        drawSignals(canvas, state, statusTint, progress, mask)
    }

    private fun prepareRingGeometry(
        state: IconState,
        technologyWidth: Float,
        digitsWidth: Float,
        progress: Float,
        batteryBelowProgress: Float
    ) {
        val weight = this.owner.weight
        val dotRadius = weight * 4.9f
        val dockedSignalWidth = ((DuoLayout.dotX(3, weight) + dotRadius)
                - (DuoLayout.dotX(0, weight) - dotRadius))
        val batteryContentWidth = DuoLayout.batteryReadoutWidth(
            digitsWidth,
            state.charging
        ) + 9.0f
        val topContentWidth = DuoLayout.batteryReadoutWidth(
            digitsWidth,
            state.charging
        ) * (1.0f - batteryBelowProgress)
        val normalBottomContentWidth = DuoLayout.bottomContentWidth(
            technologyWidth,
            dockedSignalWidth,
            progress
        )
        val bottomContentWidth = Motion.mix(
            normalBottomContentWidth,
            batteryContentWidth,
            batteryBelowProgress
        )
        this.preparedBottomHalfAngle = DuoLayout.gapHalfAngle(
            bottomContentWidth,
            weight
        )
        this.preparedTopHalfAngle = DuoLayout.gapHalfAngle(
            topContentWidth,
            weight
        )
        owner.contentOpeningReference.setCircular(
            DualStyle.DUO, owner.ring.left, owner.ring.top,
            owner.ring.right, owner.ring.bottom, preparedTopHalfAngle, preparedBottomHalfAngle, 7.5f * weight
        )
        this.preparedTopRight = this.preparedTopHalfAngle
        this.preparedTopLeft = this.preparedTopRight
        this.preparedBottomRight = this.preparedBottomHalfAngle
        this.preparedBottomLeft = this.preparedBottomRight
        batteryOpening.setEmpty()
        wifiOpening.setEmpty()
        signalOpening.setEmpty()
        technologyOpening.setEmpty()
        if (owner.hasContentOffsets()) {
            val normalBottomWeight = 1.0f - batteryBelowProgress
            batteryReadoutBounds(state, this.batteryOpening)
            adaptOpening(
                this.batteryOpening, batteryOffsetX(batteryBelowProgress), batteryOffsetY(batteryBelowProgress),
                1.0f - batteryBelowProgress, batteryBelowProgress
            )
            val signalX: Float = contentOffset(owner.contentX(1), owner.contentX(2), progress)
            val signalY: Float = contentOffset(owner.contentY(1), owner.contentY(2), progress)
            this.signalOpening.set(
                DuoLayout.dotX(0, weight) - dotRadius + signalX,
                DuoLayout.dotY(0, progress, weight) - dotRadius + signalY,
                DuoLayout.dotX(3, weight) + dotRadius + signalX,
                DuoLayout.dotY(1, progress, weight) + dotRadius + signalY
            )
            adaptOpening(this.signalOpening, signalX, signalY, 0.0f, normalBottomWeight * progress)
            if (DuoLayout.wifiAlpha(progress) > 0.0f) {
                val glyph = owner.wifiGlyphs.bounds()
                wifiOpening.set(
                    60.0f + (glyph.left - 60.0f) * owner.wifiScale + owner.contentX(1),
                    DuoLayout.wifiCentreY() + (glyph.top - 57.0f) * owner.wifiScale + owner.contentY(1),
                    60.0f + (glyph.right - 60.0f) * owner.wifiScale + owner.contentX(1),
                    DuoLayout.wifiCentreY() + (glyph.bottom - 57.0f) * owner.wifiScale + owner.contentY(1)
                )
            }
            if (technologyWidth > 0.0f && progress < 1.0f) {
                var technology = state.centreTechnology()
                if (technology.isEmpty()) technology = this.lastTechnology
                owner.technologyBounds(technology, owner.networkPaint.getTextSize(), true, this.technologyOpening)
                this.technologyOpening.offset(
                    60.0f - technologyWidth / 2.0f + owner.contentX(2),
                    DuoLayout.technologyBaseline() + owner.contentY(2)
                )
                adaptOpening(
                    this.technologyOpening, owner.contentX(2), owner.contentY(2),
                    0.0f, normalBottomWeight * (1.0f - progress)
                )
            }
        }
        this.preparedTopHalfAngle = (this.preparedTopLeft + this.preparedTopRight) / 2.0f
        this.preparedBottomHalfAngle = (this.preparedBottomLeft + this.preparedBottomRight) / 2.0f
        this.owner.batteryRingGeometry.setCircularOpenings(
            DualStyle.DUO,
            this.owner.ring.left,
            this.owner.ring.top,
            this.owner.ring.right,
            this.owner.ring.bottom,
            this.preparedTopLeft,
            this.preparedTopRight,
            this.preparedBottomLeft,
            this.preparedBottomRight,
            7.5f * weight
        )
        owner.applyContentCutouts(
            batteryOpening, wifiOpening, signalOpening, technologyOpening,
            batteryOffsetX(batteryBelowProgress), batteryOffsetY(batteryBelowProgress),
            owner.contentX(1), owner.contentY(1),
            contentOffset(owner.contentX(1), owner.contentX(2), progress),
            contentOffset(owner.contentY(1), owner.contentY(2), progress), owner.contentX(2), owner.contentY(2)
        )
    }

    private fun adaptOpening(bounds: RectF, dx: Float, dy: Float, topWeight: Float, bottomWeight: Float) {
        if (bounds.isEmpty()) return
        if (topWeight > 0.0f) {
            preparedTopLeft = Motion.mix(
                preparedTopLeft,
                owner.adaptedOpeningEdge(preparedTopLeft, true, true, bounds, dx, dy), topWeight
            )
            preparedTopRight = Motion.mix(
                preparedTopRight,
                owner.adaptedOpeningEdge(preparedTopRight, true, false, bounds, dx, dy), topWeight
            )
        }
        if (bottomWeight > 0.0f) {
            preparedBottomLeft = Motion.mix(
                preparedBottomLeft,
                owner.adaptedOpeningEdge(preparedBottomLeft, false, true, bounds, dx, dy), bottomWeight
            )
            preparedBottomRight = Motion.mix(
                preparedBottomRight,
                owner.adaptedOpeningEdge(preparedBottomRight, false, false, bounds, dx, dy), bottomWeight
            )
        }
    }

    private fun drawSignals(
        canvas: Canvas,
        state: IconState,
        statusTint: Int,
        progress: Float,
        mask: Int
    ) {
        val save = canvas.save()
        try {
            canvas.translate(
                contentOffset(owner.contentX(1), owner.contentX(2), progress),
                contentOffset(owner.contentY(1), owner.contentY(2), progress)
            )
            this.signals.draw(
                canvas,
                state,
                statusTint,
                this.owner.weight,
                this.owner.opacity,
                progress,
                mask
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    fun updateRingGeometry(owner: IconPainter?, state: IconState) {
        val technology = state.centreTechnology()
        if (!technology.isEmpty()) {
            this.lastTechnology = technology
        }
        if (state.showWifi() && state.wifiLevel >= 0) {
            this.lastWifiLevel = state.wifiLevel
        }
        val progress = Motion.clamp(this.wifiProgress)
        val data: Boolean = dataAvailable(
            state.dataEnabled,
            state.mobileKnown,
            state.simPresent,
            state.inService,
            state.airplane
        )
        val below: Boolean = batteryBelowWanted(state.showWifi(), data)
        val belowProgress: Float = batteryBelowProgress(below, data, progress)
        val gapTechnology = if (data)
            (if (technology.isEmpty()) this.lastTechnology else technology)
        else
            ""
        val technologyWidth = prepareTechnology(gapTechnology)
        val digits = batteryDigits(state)
        val digitsWidth = prepareBatteryDigits(digits)
        prepareRingGeometry(
            state,
            technologyWidth,
            digitsWidth,
            progress,
            belowProgress
        )
    }

    companion object {
        @JvmStatic fun batteryBelowWanted(wifiConnected: Boolean, dataEnabled: Boolean): Boolean {
            return DuoLayout.batteryMovesBelow(wifiConnected, dataEnabled)
        }

        @JvmStatic fun dataAvailable(
            enabled: Boolean,
            known: Boolean,
            present: Boolean,
            inService: Boolean,
            airplane: Boolean
        ): Boolean {
            return enabled && known && present && inService && !airplane
        }

        @JvmStatic fun batteryBelowProgress(
            moveBelow: Boolean,
            dataAvailable: Boolean,
            wifiProgress: Float
        ): Float {
            return if (moveBelow)
                1.0f
            else
                (if (dataAvailable) 0.0f else 1.0f - Motion.clamp(wifiProgress))
        }

        @JvmStatic fun contentOffset(from: Float, to: Float, progress: Float): Float {
            return Motion.mix(from, to, progress)
        }
    }
}
