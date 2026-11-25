package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.animation.OpeningTechnologyClearance
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.ControlCenterAppearance
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import kotlin.math.max
import kotlin.math.min

/**
 * Shared rendering for the device and settings-preview stacked signal overlays.
 * 
 * 
 * Instances own their mutable frame values and bar counters so the drawing path does not
 * allocate temporary arrays. Hook hosts and previews each create one renderer.
 */
class StackedOverlayRenderer {
    private val bars = IntArray(2)
    private val labels = arrayOfNulls<String>(2)
    private val rowLabelWidths = FloatArray(2)
    private val labelFrames = arrayOf<LabelFrame>(LabelFrame(), LabelFrame())

    private class LabelFrame {
        @JvmField val bounds: RectF = RectF()
        @JvmField var x: Float = 0f
        @JvmField var endpointX: Float = 0f
        @JvmField var baseline: Float = 0f
        @JvmField var size: Float = 0f
        @JvmField var hostAlpha: Float = 0f
        @JvmField var endpointAlpha: Float = 0f
        @JvmField var visible: Boolean = false
    }

    private var left = 0f
    private var right = 0f
    private var centreY = 0f
    private var morph = 0f
    private var signalXProgress = 0f
    private var trinityLeft = 0f
    private var trinityTop = 0f
    private var unit = 0f
    private var capsuleUnit = 0f
    private var technologyEndpointUnit = Float.NaN
    private var density = 0f
    private var motionProgress = 0f
    private var duoWifiProgress = 0f
    private var capsuleRadius = 0f
    private var tint = 0
    private var ringWeightPercent = 0
    private var rtl = false
    private var fitLabels = false
    private var centreBarsWhenLabelsHidden = false
    private var dualStyle: DualStyle? = null
    private var numberPlacement: BatteryNumberPlacement? = null
    private var labelsVisible = false
    private var labelFitWidth = 0f
    private var textWidth = 0f
    private var barLeft = 0f
    private var labelLeft = 0f
    private var labelsEnabled = true
    private var compactEndpoint = false
    private val batteryObstacle = RectF()
    private val labelBounds = RectF()
    private val fontBounds = RectF()
    private val batteryEndpoint = RectF()
    private val endpointLabelBounds = RectF()
    private var openingObstacle: OpeningBatteryMorph? = null
    private val openingSolidBounds = RectF()

    fun clearBatteryObstacle() {
        this.batteryObstacle.setEmpty()
        this.openingObstacle = null
        this.labelFrames[0].visible = false
        this.labelFrames[1].visible = false
    }

    fun setBatteryObstacle(bounds: RectF?, endpointBounds: RectF?) {
        this.batteryObstacle.set(bounds!!)
        this.batteryEndpoint.set(endpointBounds!!)
    }

    fun setOpeningBatteryObstacle(opening: OpeningBatteryMorph?, solidBounds: RectF?) {
        this.openingObstacle = opening
        this.openingSolidBounds.set(solidBounds!!)
    }

    fun setLabelsEnabled(enabled: Boolean) {
        this.labelsEnabled = enabled
    }

    fun setTechnologyEndpointUnit(endpointUnit: Float) {
        this.technologyEndpointUnit = endpointUnit
    }

    fun frame(
        left: Float,
        right: Float,
        centreY: Float,
        morph: Float,
        signalXProgress: Float,
        trinityLeft: Float,
        trinityTop: Float,
        unit: Float,
        capsuleUnit: Float,
        density: Float,
        motionProgress: Float,
        duoWifiProgress: Float,
        capsuleRadius: Float,
        tint: Int,
        ringWeightPercent: Int,
        rtl: Boolean,
        fitLabels: Boolean,
        centreBarsWhenLabelsHidden: Boolean,
        dualStyle: DualStyle?,
        numberPlacement: BatteryNumberPlacement?
    ) {
        this.left = left
        this.right = right
        this.centreY = centreY
        this.morph = morph
        this.signalXProgress = signalXProgress
        this.trinityLeft = trinityLeft
        this.trinityTop = trinityTop
        this.unit = unit
        this.capsuleUnit = capsuleUnit
        this.density = density
        this.motionProgress = motionProgress
        this.duoWifiProgress = duoWifiProgress
        this.capsuleRadius = capsuleRadius
        this.tint = tint
        this.ringWeightPercent = ringWeightPercent
        this.rtl = rtl
        this.fitLabels = fitLabels
        this.centreBarsWhenLabelsHidden = centreBarsWhenLabelsHidden
        this.dualStyle = dualStyle
        this.numberPlacement = numberPlacement
    }

    fun draw(
        canvas: Canvas?,
        painter: IconPainter,
        endpointPainter: IconPainter,
        stacked: StackedSignals,
        target: StackedSignals.Target,
        state: IconState,
        firstLabel: String?,
        secondLabel: String?
    ) {
        if (java.lang.Float.isNaN(left) || java.lang.Float.isNaN(right)) {
            return
        }
        val dual = state.dualSim()
        this.compactEndpoint = dual && dualStyle != DualStyle.CAPSULE
        val signalMotion = SignalMotionPolicy.signalProgress(morph)
        val signalXMotion =
            SignalMotionPolicy.signalProgress(signalXProgress)
        labels[0] = if (this.labelsEnabled) firstLabel else null
        labels[1] = if (this.labelsEnabled) secondLabel else null
        stacked.setTechnology(true)
        stacked.setRows(if (dual) 2 else 1)
        bars[0] = if (dual) state.card1Bars() else state.singleBars()
        bars[1] = if (dual) state.card2Bars() else 0

        val capsuleWifi = dualStyle == DualStyle.CAPSULE && state.showWifi()
        val centreRow = if (!this.labelsEnabled
            || (state.centreTechnology().isEmpty() && !capsuleWifi)
        )
            -1
        else
            (if (dual) (if (state.mobileIsSlot0()) 0 else 1) else 0)
        if (centreRow >= 0 && labels[centreRow] == null) {
            labels[centreRow] = StackedSignals.Companion.clip(state.centreTechnology())
        }

        val rowSize: Float = StackedSignals.Companion.labelSize(
            if (dual) painter.technologyScalePercent()
            else ControlCenterAppearance.SINGLE_TECHNOLOGY_SCALE
        ) * density
        val statusBarTechnology =
            StackedTechnologyStyle.usesStatusBarEndpoint(dualStyle)
        val centrePainter: IconPainter? = if (statusBarTechnology) endpointPainter else painter
        val technologyUnit = if (statusBarTechnology
            && java.lang.Float.isFinite(technologyEndpointUnit)
            && technologyEndpointUnit > 0.0f
        ) technologyEndpointUnit else unit
        this.textWidth = textWidth(painter, rowSize)
        var textWidth = this.textWidth
        var labelsVisible = textWidth > 0.0f
        this.labelFitWidth = StackedOverlayLayout.labelFitWidth(left, right)
        if (fitLabels
            && labelsVisible
            && StackedOverlayLayout.allowsLabelFitFallback(
                numberPlacement
            )
        ) {
            labelsVisible = StackedOverlayLayout.fitsLabels(
                this.labelFitWidth,
                textWidth,
                density,
                numberPlacement,
                singleRow = !dual
            )
            if (!labelsVisible) {
                textWidth = 0.0f
            }
        }
        this.labelsVisible = labelsVisible
        this.textWidth = textWidth
        this.barLeft = StackedOverlayLayout.barLeft(
            rtl,
            left,
            right,
            density,
            textWidth > 0.0f,
            centreBarsWhenLabelsHidden,
            numberPlacement
        )
        if (!dual && textWidth > 0.0f) {
            this.barLeft = StackedSignals.Companion.singleRowBarLeft(
                left, right, density, rtl, textWidth
            )
        }
        this.labelLeft = StackedOverlayLayout.labelLeft(
            rtl,
            this.barLeft,
            textWidth,
            density
        )

        val signalWeight = AppearanceScale.ringWeight(ringWeightPercent)
        val radius = IconGeometry.dotRadius(ringWeightPercent) * unit
        val secondaryAlpha = IconGeometry.innerDotAlpha(signalMotion)
        val span = 4.0f / 3.0f
        target.centreRow = -1
        target.centreSize = rowSize
        target.centreX = (trinityLeft
                + ((IconGeometry.centreX() + endpointPainter.contentX(
            endpointPainter.technologyContentSlot(state)
        )) * technologyUnit))
        if (dualStyle == DualStyle.CAPSULE) {
            target.centreX = (trinityLeft
                    + (endpointPainter.capsuleTextX(state) * capsuleUnit))
            target.centreY = (trinityTop
                    + (endpointPainter.capsuleTextBaseline(state)
                    * capsuleUnit))
        } else if (dualStyle == DualStyle.DUO) {
            target.centreY = trinityTop + ((DuoLayout.technologyBaseline()
                    + endpointPainter.contentY(2)) * technologyUnit)
        } else {
            target.centreY = (trinityTop
                    + (centrePainter!!.centreBaseline(state) * technologyUnit))
        }

        if (dual && dualStyle == DualStyle.DUO) {
            val cardOneOk = state.card1Present() && state.card1Known()
                    && state.card1InService() && !state.airplane
            val cardTwoOk = state.card2Present() && state.card2Known()
                    && state.card2InService() && !state.airplane
            for (element in 0..<StackedSignals.Companion.ELEMENTS) {
                val row: Int = element / StackedSignals.Companion.BARS
                val slot: Int = element % StackedSignals.Companion.BARS
                target.x[element] = trinityLeft + (DuoLayout.dotX(slot, signalWeight) * unit)
                target.y[element] = (trinityTop
                        + (DuoLayout.dotY(row, duoWifiProgress, signalWeight) * unit))
                target.radius[element] = radius
                target.alpha[element] = DuoLayout.dotAlpha(
                    if (row == 0) cardOneOk else cardTwoOk,
                    bars[row],
                    slot
                ) / 255.0f
                target.mergeFade[element] = false
            }
        } else if (dualStyle == DualStyle.CAPSULE) {
            val capsuleRows = if (dual) StackedSignals.Companion.ROWS else 1
            for (element in 0..<StackedSignals.Companion.ELEMENTS) {
                val row: Int = element / StackedSignals.Companion.BARS
                val slot: Int = element % StackedSignals.Companion.BARS
                if (row >= capsuleRows) {
                    target.x[element] = trinityLeft
                    target.y[element] = trinityTop
                    target.radius[element] = capsuleRadius
                    target.alpha[element] = 0.0f
                    target.mergeFade[element] = false
                    continue
                }
                val dot = min(slot, IconPainter.Companion.CAPSULE_DOTS - 1)
                val capsuleRow = if (dual) row else 1
                target.x[element] = (trinityLeft
                        + (endpointPainter.capsuleDotX(state, capsuleRow, dot)
                        * capsuleUnit))
                target.y[element] = (trinityTop
                        + (endpointPainter.capsuleDotY(state, capsuleRow)
                        * capsuleUnit))
                target.radius[element] = capsuleRadius
                val rowBars = if (dual) bars[row] else state.singleBars()
                val fill = Motion.clamp((rowBars - (dot * span)) / span)
                target.alpha[element] = (((48.0f + (fill * 207.0f)) / 255.0f)
                        * (if (row == 0) 1.0f else secondaryAlpha))
                target.mergeFade[element] = slot == StackedSignals.Companion.BARS - 1
            }
        } else if (dual && dualStyle == DualStyle.GROUPED) {
            val groupSecondary =
                IconGeometry.groupedSecondaryAlpha(signalMotion)
            for (element in 0..<StackedSignals.Companion.ELEMENTS) {
                val row: Int = element / StackedSignals.Companion.BARS
                val slot: Int = element % StackedSignals.Companion.BARS
                val dot = (row * 2) + (if (slot < 2) 0 else 1)
                target.x[element] = (trinityLeft
                        + (IconGeometry.groupedDotX(
                    dot,
                    signalXMotion,
                    signalWeight
                ) * unit))
                target.y[element] = (trinityTop
                        + (IconGeometry.groupedDotY(dot, signalWeight) * unit))
                target.radius[element] = radius
                val fill = Motion.clamp(
                    (bars[row] - IconGeometry.groupedDotRank(dot)) / 2.0f
                )
                target.alpha[element] = (((48.0f + (fill * 207.0f)) / 255.0f)
                        * (if (row == 0) 1.0f else groupSecondary))
                target.mergeFade[element] = slot == 1 || slot == 3
            }
        } else if (dual) {
            for (dot in 0..3) {
                target.x[dot + 4] = (trinityLeft
                        + (IconGeometry.outerDotX(dot, signalWeight) * unit))
                target.y[dot + 4] = (trinityTop
                        + (IconGeometry.outerDotY(dot, signalWeight) * unit))
                target.radius[dot + 4] = radius
                target.alpha[dot + 4] = if (bars[1] > dot) 1.0f else 0.188f
                target.mergeFade[dot + 4] = false

                val inner = min(dot, 2)
                target.x[dot] = (trinityLeft
                        + (IconGeometry.innerDotX(
                    inner,
                    signalXMotion,
                    signalWeight
                ) * unit))
                target.y[dot] = (trinityTop
                        + (IconGeometry.innerDotY(
                    inner, signalMotion, signalWeight
                ) * unit))
                target.radius[dot] = radius
                val fill = Motion.clamp((bars[0] - (inner * span)) / span)
                target.alpha[dot] = (((48.0f + (fill * 207.0f)) / 255.0f)
                        * secondaryAlpha)
                target.mergeFade[dot] = dot == 3
            }
        } else {
            for (dot in 0..3) {
                target.x[dot] = (trinityLeft
                        + (IconGeometry.singleDotX(dot, signalWeight) * unit))
                target.y[dot] = (trinityTop
                        + (IconGeometry.singleDotY(dot, signalWeight) * unit))
                target.radius[dot] = radius
                target.alpha[dot] = if (bars[0] > dot) 1.0f else 0.188f
                target.mergeFade[dot] = false
                target.mergeFade[dot + 4] = false
            }
        }

        var element = 0
        while (dual && element < StackedSignals.Companion.ELEMENTS
        ) {
            val row: Int = element / StackedSignals.Companion.BARS
            val slot: Int = element % StackedSignals.Companion.BARS
            val targetUnit = if (dualStyle == DualStyle.CAPSULE)
                capsuleUnit
            else
                unit
            target.x[element] =
                (trinityLeft
                        + (SessionPaintPolicy.controlCenterSignalTargetX(
                    dualStyle,
                    (target.x[element] - trinityLeft) / targetUnit,
                    row,
                    slot,
                    signalXMotion,
                    signalWeight
                ) * targetUnit))
            element++
        }
        if (dualStyle != DualStyle.CAPSULE) {
            for (element in 0..<(if (dual) StackedSignals.Companion.ELEMENTS else StackedSignals.Companion.BARS)) {
                var dx = endpointPainter.contentX(2)
                var dy = endpointPainter.contentY(2)
                if (dual && dualStyle == DualStyle.DUO) {
                    dx = Motion.mix(endpointPainter.contentX(1), endpointPainter.contentX(2), duoWifiProgress)
                    dy = Motion.mix(endpointPainter.contentY(1), endpointPainter.contentY(2), duoWifiProgress)
                }
                target.x[element] += dx * unit
                target.y[element] += dy * unit
            }
        }
        SignalMotionPolicy.updateRowAnchors(
            target,
            if (dual) StackedSignals.Companion.ROWS else 1
        )

        if (centreRow >= 0) {
            target.centreRow = centreRow
            target.centreSize = if (dualStyle == DualStyle.CAPSULE)
                endpointPainter.capsuleTextSize(state) * capsuleUnit
            else
                StackedTechnologyStyle.targetSize(
                    dualStyle,
                    (if (dualStyle == DualStyle.DUO)
                        endpointPainter.duoTechnologyTextSize(state)
                    else
                        centrePainter!!.centreTextSize()) * technologyUnit,
                    painter.centreTextSize() * unit
                )
        }
        if (textWidth > 0.0f) {
            drawLabels(
                canvas,
                painter,
                endpointPainter,
                stacked,
                target,
                rowSize,
                capsuleWifi,
                this.barLeft,
                state.showWifi(),
                dual
            )
        }
        target.hasBatteryObstacle = !this.batteryObstacle.isEmpty()
        // DUO's upper row crosses the battery bounds, so its side route must ease in and out.
        target.smoothBatteryAvoidance = dual && dualStyle == DualStyle.DUO
        target.openingBattery =
            if (target.smoothBatteryAvoidance && this.openingObstacle != null && this.openingObstacle!!.valid) this.openingObstacle else null
        target.solidBatteryLeft = this.openingSolidBounds.left
        target.solidBatteryTop = this.openingSolidBounds.top
        target.solidBatteryRight = this.openingSolidBounds.right
        target.solidBatteryBottom = this.openingSolidBounds.bottom
        target.batteryLeft = this.batteryObstacle.left
        target.batteryTop = this.batteryObstacle.top
        target.batteryRight = this.batteryObstacle.right
        target.batteryBottom = this.batteryObstacle.bottom
        target.batteryAnchorX = this.batteryEndpoint.centerX()
        stacked.drawElements(
            canvas!!,
            this.barLeft,
            centreY,
            density,
            signalMotion,
            signalXMotion,
            tint,
            bars,
            rtl,
            target
        )
    }

    fun diagnosticRowLeft(): Float {
        return left
    }

    fun diagnosticRowRight(): Float {
        return right
    }

    fun diagnosticLabelFitWidth(): Float {
        return labelFitWidth
    }

    fun diagnosticTextWidth(): Float {
        return textWidth
    }

    fun diagnosticBarLeft(): Float {
        return barLeft
    }

    fun diagnosticLabelLeft(): Float {
        return labelLeft
    }

    fun diagnosticLabelsVisible(): Boolean {
        return labelsVisible
    }

    private fun textWidth(painter: IconPainter, rowSize: Float): Float {
        var width = 0.0f
        for (row in labels.indices) {
            rowLabelWidths[row] = if (labels[row] == null)
                0.0f
            else
                painter.measureTechnology(labels[row], rowSize)
            width = max(width, rowLabelWidths[row])
        }
        return width
    }

    private fun drawLabels(
        canvas: Canvas?,
        painter: IconPainter,
        endpointPainter: IconPainter,
        stacked: StackedSignals,
        target: StackedSignals.Target,
        rowSize: Float,
        capsuleWifi: Boolean,
        barLeft: Float,
        wifiVisible: Boolean,
        dual: Boolean
    ) {
        val opening = this.openingObstacle != null
        val textMorph = if (opening)
            OpeningTechnologyClearance.textProgress(morph)
        else
            StackedOverlayLayout.technologyProgress(morph)
        val textXProgress = StackedOverlayLayout.technologyXProgress(morph)
        val fontMorph = if (opening) OpeningTechnologyClearance.fontProgress(morph) else textMorph
        val blendFonts =
            (endpointPainter.technologyWeightPercent != painter.technologyWeightPercent || this.compactEndpoint || opening)
                    && fontMorph < 1.0f
        for (row in labels.indices) {
            val labelFrame = this.labelFrames[row]
            labelFrame.visible = false
            labelFrame.bounds.setEmpty()
            val label = labels[row]
            if (label == null) {
                continue
            }
            val alpha = SessionPaintPolicy.stackedTechnologyAlpha(
                textMorph,
                row == target.centreRow,
                capsuleWifi,
                wifiVisible
            )
            if (alpha <= 0.0f) {
                continue
            }
            val rowX = if (!dual) {
                StackedSignals.Companion.singleRowLabelLeft(
                    barLeft, density, rtl, rowLabelWidths[row]
                )
            } else {
                StackedOverlayLayout.labelLeft(rtl, barLeft, rowLabelWidths[row], density)
            }
            val rowCentreOffset = if (!dual)
                StackedSignals.Companion.singleRowLabelCentreOffset(rowSize, density)
            else
                stacked.rowLabelCenter(row) * density
            val rowY = (centreY
                    + rowCentreOffset
                    + painter.technologyOffset(label, rowSize))
            val size = if (textMorph == 1.0f)
                rowSize
            else
                Motion.mix(target.centreSize, rowSize, textMorph)
            var y = Motion.mix(target.centreY, rowY, textMorph)
            val endpointAlpha = if (blendFonts)
                StackedTechnologyStyle.endpointAlpha(alpha, fontMorph)
            else
                0.0f
            val hostAlpha = if (blendFonts)
                StackedTechnologyStyle.hostAlpha(alpha, fontMorph)
            else
                alpha
            var endpointX = 0.0f
            var x = 0.0f
            this.labelBounds.setEmpty()
            if (blendFonts) {
                val endpointWidth = endpointPainter.measureTechnology(label, size, this.compactEndpoint)
                endpointX = Motion.mix(
                    target.centreX - (endpointWidth / 2.0f),
                    rowX,
                    textXProgress
                )
                endpointX = openingLabelX(endpointX, endpointWidth, target.centreX)
                if (endpointAlpha > 0.0f && !this.batteryObstacle.isEmpty()) {
                    endpointPainter.technologyBounds(label, size, this.compactEndpoint, this.fontBounds)
                    this.fontBounds.offset(endpointX, y)
                    this.labelBounds.union(this.fontBounds)
                }
            }
            if (hostAlpha > 0.0f) {
                val labelWidth = if (size == rowSize)
                    rowLabelWidths[row]
                else
                    painter.measureTechnology(label, size)
                x = Motion.mix(
                    target.centreX - (labelWidth / 2.0f),
                    rowX, textXProgress
                )
                x = openingLabelX(x, labelWidth, target.centreX)
                if (!this.batteryObstacle.isEmpty()) {
                    painter.technologyBounds(label, size, this.fontBounds)
                    this.fontBounds.offset(x, y)
                    this.labelBounds.union(this.fontBounds)
                }
            }
            if (!this.labelBounds.isEmpty() && !this.batteryObstacle.isEmpty()) {
                var above = row == 0
                if (row == target.centreRow) {
                    val targetPainter = if (StackedTechnologyStyle.usesStatusBarEndpoint(dualStyle)
                        || dualStyle == DualStyle.CAPSULE
                    ) endpointPainter else painter
                    targetPainter.technologyBounds(label, target.centreSize, this.compactEndpoint, this.fontBounds)
                    above = target.centreY + this.fontBounds.centerY() <= this.batteryEndpoint.centerY()
                }
                var endpointHorizontalGap = Float.NaN
                var endpointVerticalGap = Float.NaN
                if (row == target.centreRow) {
                    endpointBounds(painter, endpointPainter, label, target)
                    endpointHorizontalGap = max(
                        this.batteryEndpoint.left - this.endpointLabelBounds.right,
                        this.endpointLabelBounds.left - this.batteryEndpoint.right
                    )
                    endpointVerticalGap = if (above)
                        this.batteryEndpoint.top - this.endpointLabelBounds.bottom
                    else
                        this.endpointLabelBounds.top - this.batteryEndpoint.bottom
                }
                val shift = if (this.openingObstacle != null)
                    openingLabelShift(above)
                else
                    TechnologyAvoidance.motionBaselineShift(
                        this.labelBounds.left, this.labelBounds.top,
                        this.labelBounds.right, this.labelBounds.bottom,
                        this.batteryObstacle.left, this.batteryObstacle.top,
                        this.batteryObstacle.right, this.batteryObstacle.bottom,
                        density, max(this.labelBounds.height(), 4.0f * density), above,
                        endpointHorizontalGap, endpointVerticalGap, morph
                    )
                y += shift
                this.labelBounds.offset(0.0f, shift)
            }
            labelFrame.visible = true
            labelFrame.bounds.set(this.labelBounds)
            labelFrame.x = x
            labelFrame.endpointX = endpointX
            labelFrame.baseline = y
            labelFrame.size = size
            labelFrame.hostAlpha = hostAlpha
            labelFrame.endpointAlpha = endpointAlpha
        }
        separateLabelRows(target.centreRow)
        for (row in labels.indices) {
            val labelFrame = this.labelFrames[row]
            if (!labelFrame.visible) {
                continue
            }
            if (labelFrame.endpointAlpha > 0.0f) {
                endpointPainter.drawTechnology(
                    canvas, labels[row], labelFrame.endpointX,
                    labelFrame.baseline, labelFrame.size, labelFrame.endpointAlpha, tint, this.compactEndpoint
                )
            }
            if (labelFrame.hostAlpha > 0.0f) {
                painter.drawTechnology(
                    canvas, labels[row], labelFrame.x,
                    labelFrame.baseline, labelFrame.size, labelFrame.hostAlpha, tint
                )
            }
        }
    }

    private fun separateLabelRows(centreRow: Int) {
        if (this.batteryObstacle.isEmpty()
            || !this.labelFrames[0].visible || !this.labelFrames[1].visible
        ) {
            return
        }
        val primaryRow = if (centreRow == 1) 1 else 0
        val secondaryRow = 1 - primaryRow
        val primary = this.labelFrames[primaryRow]
        val secondary = this.labelFrames[secondaryRow]
        if (primary.bounds.isEmpty() || secondary.bounds.isEmpty()
            || primary.bounds.right <= secondary.bounds.left || secondary.bounds.right <= primary.bounds.left
        ) {
            return
        }
        val shift = TechnologyAvoidance.secondaryShift(
            primary.bounds.top, primary.bounds.bottom,
            secondary.bounds.top, secondary.bounds.bottom, density, secondaryRow == 0
        )
        secondary.baseline += shift
        secondary.bounds.offset(0.0f, shift)
    }

    private fun openingLabelShift(above: Boolean): Float {
        return OpeningTechnologyClearance.shift(
            labelBounds.left, labelBounds.top, labelBounds.right, labelBounds.bottom,
            batteryObstacle.left, batteryObstacle.top, batteryObstacle.right, batteryObstacle.bottom,
            openingSolidBounds.left, openingSolidBounds.top,
            openingSolidBounds.right, openingSolidBounds.bottom,
            morph, density, max(labelBounds.height(), 4.0f * density), above
        )
    }

    private fun openingLabelX(rowPathX: Float, width: Float, endpointCentre: Float): Float {
        if (this.openingObstacle == null) {
            return rowPathX
        }
        // Sample the already projected points, so copied and primary hosts use
        // the very same gap on screen. Capsule text starts in its original slot.
        val gapCentre = this.openingObstacle!!.gapAnchor[0]
        val slotOffset = ((endpointCentre - gapCentre)
                * (1.0f - Motion.ease(morph / OpeningTechnologyClearance.EXIT_START)))
        return Motion.mix(
            gapCentre + slotOffset - width / 2.0f, rowPathX,
            OpeningTechnologyClearance.horizontalJoin(morph)
        )
    }

    private fun endpointBounds(
        painter: IconPainter, endpointPainter: IconPainter,
        label: String?, target: StackedSignals.Target
    ) {
        painter.technologyBounds(label, target.centreSize, this.endpointLabelBounds)
        this.endpointLabelBounds.offset(
            target.centreX
                    - painter.measureTechnology(label, target.centreSize) / 2.0f, target.centreY
        )
        if (painter.technologyWeightPercent != endpointPainter.technologyWeightPercent
            || this.compactEndpoint
        ) {
            // Both fonts coexist until the handoff. Include the fading font in
            // endpoint clearance too so its last visible frame cannot jump.
            endpointPainter.technologyBounds(label, target.centreSize, this.compactEndpoint, this.fontBounds)
            this.fontBounds.offset(
                target.centreX
                        - endpointPainter.measureTechnology(label, target.centreSize, this.compactEndpoint) / 2.0f,
                target.centreY
            )
            this.endpointLabelBounds.union(this.fontBounds)
        }
    }
}
