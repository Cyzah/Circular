package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.CapsuleGeometry
import com.cyzah.circular.core.graphics.geometry.CapsuleLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.StatusColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class CapsuleStyleRenderer {
    private val ringPath = Path()
    private val oval = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metrics = CapsuleMetrics()
    private val frame = CapsuleFrame()
    private val technologyCache = TextMetricCache()
    private val digitCache = TextMetricCache()
    private val readoutInk = Rect()
    private val contentInk = RectF()
    private val topOpening = RectF()
    private val centerOpening = RectF()
    private val bottomOpening = RectF()

    private class CapsuleMetrics {
        @JvmField var wifi: Boolean = false
        @JvmField var technology: String = ""
        @JvmField var digits: String = ""
        @JvmField var gap: Float = 0f
        @JvmField var leftWidth: Float = 0f
        @JvmField var technologySize: Float = 0f
        @JvmField var wifiWidth: Float = 0f
        @JvmField var digitSize: Float = 0f
        @JvmField var digitsWidth: Float = 0f
        @JvmField var contentWidth: Float = 0f
    }

    private class CapsuleFrame {
        @JvmField var width: Float = 0f
        @JvmField var height: Float = 0f
        @JvmField var scale: Float = 0f
        @JvmField var centreX: Float = 0f
        @JvmField var centreY: Float = 0f
        @JvmField var capInset: Float = 0f
        @JvmField var dotRadius: Float = 0f
        @JvmField var stepTop: Float = 0f
        @JvmField var stepBottom: Float = 0f
        @JvmField var firstTop: Float = 0f
        @JvmField var firstBottom: Float = 0f
        @JvmField var halfTop: Float = 0f
        @JvmField var halfBottom: Float = 0f
        @JvmField var originalHalfTop: Float = 0f
        @JvmField var originalHalfBottom: Float = 0f
        @JvmField var contentX: Float = 0f
        @JvmField var wifiContentCentreX: Float = 0f
        @JvmField var topX: Float = 0f
        @JvmField var topY: Float = 0f
        @JvmField var bottomX: Float = 0f
        @JvmField var bottomY: Float = 0f
    }

    fun measure(owner: IconPainter, state: IconState) {
        val height: Float = IconPainter.Companion.CAPSULE_H
        this.metrics.gap = safeLength(height * IconPainter.Companion.CAPSULE_GAP)
        this.metrics.wifi = state.showWifi()
        this.metrics.technology = if (this.metrics.wifi)
            ""
        else
            state.centreTechnology()
        this.metrics.digits = (if (state.battery < 0)
            ""
        else
            com.cyzah.circular.core.graphics.IconPainter.Companion.NUMBERS[kotlin.math.min(100, state.battery)])!!
        this.metrics.wifiWidth = if (this.metrics.wifi)
            safeLength((height * 0.40f) * owner.wifiScale)
        else
            0.0f
        this.metrics.technologySize = safeLength(
            (height * 0.40f) * (owner.technologyPercent / 100.0f)
        )
        val technologyStroke = (this.metrics.technologySize
                * AppearanceScale.technologyWeightStrokeRatio(
            owner.technologyWeightPercent
        ))
        if (this.metrics.wifi) {
            this.metrics.leftWidth = this.metrics.wifiWidth
        } else if (this.metrics.technology.isEmpty()) {
            this.metrics.leftWidth = 0.0f
        } else {
            val typeface: Any? = owner.networkPaint.getTypeface()
            if (this.technologyCache.matches(
                    this.metrics.technology,
                    owner.technologyPercent,
                    owner.technologyWeightPercent,
                    typeface
                )
            ) {
                this.metrics.leftWidth = this.technologyCache.width()
            } else {
                this.metrics.leftWidth = (safeLength(
                    owner.measureTechnology(
                        this.metrics.technology,
                        this.metrics.technologySize
                    )
                )
                        + max(0.0f, technologyStroke))
                this.technologyCache.store(
                    this.metrics.technology,
                    owner.technologyPercent,
                    owner.technologyWeightPercent,
                    typeface,
                    this.metrics.leftWidth,
                    this.metrics.technologySize,
                    0,
                    0
                )
            }
        }
        this.metrics.digitSize = safeLength(
            (height * 0.58f)
                    * (BatteryHeader.textSize(owner.percentScale)
                    / BatteryHeader.textSize(100))
        )
        val digitTypeface: Any? = owner.textPaint.getTypeface()
        if (this.digitCache.matches(
                this.metrics.digits,
                owner.percentScale,
                owner.batteryWeightPercent(),
                digitTypeface
            )
        ) {
            this.metrics.digitsWidth = this.digitCache.width()
            owner.textPaint.setTextSize(this.digitCache.textSize())
        } else {
            owner.textPaint.setTextSize(this.metrics.digitSize)
            this.metrics.digitsWidth = if (this.metrics.digits.isEmpty())
                0.0f
            else
                safeLength(owner.textPaint.measureNumber(this.metrics.digits))
            this.digitCache.store(
                this.metrics.digits,
                owner.percentScale,
                owner.batteryWeightPercent(),
                digitTypeface,
                this.metrics.digitsWidth,
                this.metrics.digitSize,
                0,
                0
            )
        }
        owner.textPaint.setTextAlign(Align.LEFT)
        this.metrics.contentWidth = CapsuleGeometry.contentWidth(
            this.metrics.gap,
            this.metrics.leftWidth,
            this.metrics.digitsWidth
        )
    }

    fun bodyWidth(owner: IconPainter): Float {
        return CapsuleGeometry.bodyWidth(
            this.metrics.contentWidth,
            owner.weight
        )
    }

    fun scale(owner: IconPainter): Float {
        return CapsuleGeometry.scale(
            bodyWidth(owner),
            owner.capsuleAspect
        )
    }

    private fun prepareFrame(owner: IconPainter) {
        this.frame.height = IconPainter.Companion.CAPSULE_H
        this.frame.width = bodyWidth(owner)
        this.frame.scale = CapsuleGeometry.scale(
            this.frame.width,
            owner.capsuleAspect
        )
        this.frame.centreX = this.frame.width / 2.0f
        this.frame.centreY = this.frame.height / 2.0f
        this.frame.capInset = (IconPainter.Companion.CAPSULE_STROKE * owner.weight) / 2.0f
        this.frame.dotRadius = IconPainter.Companion.CAPSULE_DOT_R * owner.weight
        this.frame.stepTop = ((2.0f * this.frame.dotRadius)
                + IconPainter.Companion.CAPSULE_DOT_GAP_TOP)
        this.frame.stepBottom = ((2.0f * this.frame.dotRadius)
                + IconPainter.Companion.CAPSULE_DOT_GAP_BOT)
        this.frame.firstTop = ((this.frame.width
                - (((IconPainter.Companion.CAPSULE_DOTS - 1) * this.frame.stepTop)
                + (2.0f * this.frame.dotRadius)))
                / 2.0f) + this.frame.dotRadius
        this.frame.firstBottom = ((this.frame.width
                - (((IconPainter.Companion.CAPSULE_DOTS - 1) * this.frame.stepBottom)
                + (2.0f * this.frame.dotRadius)))
                / 2.0f) + this.frame.dotRadius
        val maxHalf = ((this.frame.width / 2.0f)
                - (this.frame.height / 2.0f))
        this.frame.halfTop = min(
            (this.frame.width / 2.0f)
                    - (this.frame.firstTop
                    - this.frame.dotRadius
                    - IconPainter.Companion.CAPSULE_DOT_GAP_TOP
                    - this.frame.capInset),
            maxHalf
        )
        this.frame.halfBottom = min(
            (this.frame.width / 2.0f)
                    - (this.frame.firstBottom
                    - this.frame.dotRadius
                    - IconPainter.Companion.CAPSULE_DOT_GAP_BOT
                    - this.frame.capInset),
            maxHalf
        )
        this.frame.originalHalfTop = this.frame.halfTop
        this.frame.originalHalfBottom = this.frame.halfBottom
        this.frame.topX = localOffset(owner.contentX(0), this.frame.scale)
        this.frame.topY = localOffset(owner.contentY(0), this.frame.scale)
        this.frame.bottomX = localOffset(owner.contentX(2), this.frame.scale)
        this.frame.bottomY = localOffset(owner.contentY(2), this.frame.scale)
        this.frame.halfTop = adaptedSignalHalfGap(
            this.frame.halfTop, maxHalf,
            (IconPainter.Companion.CAPSULE_DOTS - 1) * this.frame.stepTop / 2.0f,
            this.frame.dotRadius, this.frame.capInset, this.frame.topX, this.frame.topY,
            IconPainter.Companion.CAPSULE_DOT_GAP_TOP
        )
        this.frame.halfBottom = adaptedSignalHalfGap(
            this.frame.halfBottom, maxHalf,
            (IconPainter.Companion.CAPSULE_DOTS - 1) * this.frame.stepBottom / 2.0f,
            this.frame.dotRadius, this.frame.capInset, this.frame.bottomX, this.frame.bottomY,
            IconPainter.Companion.CAPSULE_DOT_GAP_BOT
        )
    }

    fun reserveWidth(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        return CapsuleGeometry.reserveWidth(bodyWidth(owner), owner.weight)
    }

    fun dotX(
        owner: IconPainter,
        state: IconState,
        row: Int,
        dot: Int
    ): Float {
        measure(owner, state)
        val width = bodyWidth(owner)
        val scale = scale(owner)
        val dotRadius: Float = IconPainter.Companion.CAPSULE_DOT_R * owner.weight
        val step: Float = ((2.0f * dotRadius)
                + (if (row == 0) IconPainter.Companion.CAPSULE_DOT_GAP_TOP else IconPainter.Companion.CAPSULE_DOT_GAP_BOT))
        val first: Float = ((width
                - (((IconPainter.Companion.CAPSULE_DOTS - 1) * step) + (2.0f * dotRadius)))
                / 2.0f) + dotRadius
        val originX = CapsuleLayout.originX(
            width,
            scale,
            owner.capsuleCentred,
            IconPainter.Companion.CAPSULE_SHIFT_X
        )
        return CapsuleLayout.contentCentre(
            originX,
            first + (dot * step),
            scale
        ) + owner.contentX(if (row == 0) 0 else 2)
    }

    fun dotY(owner: IconPainter, state: IconState, row: Int): Float {
        measure(owner, state)
        val scale = scale(owner)
        return ((60.0f - ((IconPainter.Companion.CAPSULE_H * scale) / 2.0f))
                + (if (row == 0) 0.0f else (IconPainter.Companion.CAPSULE_H * scale)) + owner.contentY(if (row == 0) 0 else 2))
    }

    fun dotRadius(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        return CapsuleLayout.dotRadius(
            IconPainter.Companion.CAPSULE_DOT_R,
            owner.weight,
            scale(owner)
        )
    }

    fun drawWifiScale(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        return CapsuleGeometry.wifiScale(
            this.metrics.wifiWidth,
            owner.wifiGlyphs.bounds().width(),
            scale(owner)
        )
    }

    fun textX(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        val width = bodyWidth(owner)
        val scale = scale(owner)
        val centreX = width / 2.0f
        val localX = (centreX - (this.metrics.contentWidth / 2.0f)
                + (this.metrics.leftWidth / 2.0f))
        val originX = CapsuleLayout.originX(
            width,
            scale,
            owner.capsuleCentred,
            IconPainter.Companion.CAPSULE_SHIFT_X
        )
        return CapsuleLayout.contentCentre(originX, localX, scale) + owner.contentX(1)
    }

    fun textSize(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        return this.metrics.technologySize * scale(owner)
    }

    fun textBaseline(owner: IconPainter, state: IconState): Float {
        measure(owner, state)
        owner.networkPaint.setTextSize(this.metrics.technologySize)
        owner.networkPaint.getFontMetrics(owner.textMetrics)
        return (60.0f + owner.contentY(1)
                - ((owner.textMetrics.ascent + owner.textMetrics.descent) / 2.0f) * scale(owner))
    }

    fun updateRingGeometry(owner: IconPainter, state: IconState) {
        measure(owner, state)
        prepareFrame(owner)
        val originX = CapsuleLayout.originX(
            this.frame.width,
            this.frame.scale,
            owner.capsuleCentred,
            IconPainter.Companion.CAPSULE_SHIFT_X
        )
        val originY = (60.0f
                - ((this.frame.height * this.frame.scale) / 2.0f))
        owner.contentOpeningReference.setCapsule(
            frame.width, frame.height, frame.originalHalfTop,
            frame.originalHalfBottom, IconPainter.Companion.CAPSULE_STROKE * owner.weight * frame.scale,
            originX, originY, frame.scale
        )
        owner.batteryRingGeometry.setCapsuleOpenings(
            this.frame.width,
            this.frame.height,
            this.frame.centreX + this.frame.topX - this.frame.halfTop,
            this.frame.centreX + this.frame.topX + this.frame.halfTop,
            this.frame.centreX + this.frame.bottomX - this.frame.halfBottom,
            this.frame.centreX + this.frame.bottomX + this.frame.halfBottom,
            IconPainter.Companion.CAPSULE_STROKE * owner.weight * this.frame.scale,
            originX,
            originY,
            this.frame.scale
        )
        if (owner.hasContentOffsets()) {
            signalOpeningBounds(owner, state, topOpening, bottomOpening)
            centerOpeningBounds(owner, state, centerOpening)
        }
        owner.applyContentCutouts(topOpening, centerOpening, bottomOpening)
    }

    fun batteryReadoutBounds(owner: IconPainter, state: IconState, out: RectF) {
        out.setEmpty()
        measure(owner, state)
        prepareFrame(owner)
        if (this.metrics.digits.isEmpty()) {
            return
        }
        owner.textPaint.setTextSize(this.metrics.digitSize)
        owner.textPaint.numberBounds(this.metrics.digits, this.readoutInk)
        owner.textPaint.getFontMetrics(owner.textMetrics)
        val baseline = batteryDigitsBaseline(owner)
        val left = (batteryDigitsCentreX() - this.metrics.digitsWidth / 2.0f
                + owner.textPaint.leftPadding())
        val originX = CapsuleLayout.originX(
            this.frame.width, this.frame.scale,
            owner.capsuleCentred, IconPainter.Companion.CAPSULE_SHIFT_X
        )
        val originY = 60.0f - this.frame.height * this.frame.scale / 2.0f
        out.set(
            originX + (left + this.readoutInk.left) * this.frame.scale,
            originY + (baseline + this.readoutInk.top) * this.frame.scale,
            originX + (left + this.readoutInk.right) * this.frame.scale,
            originY + (baseline + this.readoutInk.bottom) * this.frame.scale
        )
        out.offset(owner.contentX(1), owner.contentY(1))
    }

    /** Painted row bounds in the same 120-unit frame as the exported dot endpoints.  */
    fun signalOpeningBounds(owner: IconPainter, state: IconState, top: RectF, bottom: RectF) {
        measure(owner, state)
        prepareFrame(owner)
        val originX =
            CapsuleLayout.originX(frame.width, frame.scale, owner.capsuleCentred, IconPainter.Companion.CAPSULE_SHIFT_X)
        val originY = 60.0f - frame.height * frame.scale / 2.0f
        rowOpeningBounds(top, originX, originY, frame.firstTop, frame.stepTop, frame.topX, frame.topY)
        rowOpeningBounds(
            bottom, originX, originY, frame.firstBottom, frame.stepBottom,
            frame.bottomX, frame.height + frame.bottomY
        )
    }

    private fun rowOpeningBounds(
        out: RectF, originX: Float, originY: Float,
        first: Float, step: Float, x: Float, y: Float
    ) {
        out.set(
            originX + (first + x - frame.dotRadius) * frame.scale,
            originY + (y - frame.dotRadius) * frame.scale,
            originX + (first + (IconPainter.Companion.CAPSULE_DOTS - 1) * step + x + frame.dotRadius) * frame.scale,
            originY + (y + frame.dotRadius) * frame.scale
        )
    }

    /** Includes the actual digits, technology suffix stroke, and fitted Wi-Fi glyph.  */
    fun centerOpeningBounds(owner: IconPainter, state: IconState, out: RectF) {
        batteryReadoutBounds(owner, state, out)
        val left = frame.centreX - metrics.contentWidth / 2.0f
        if (metrics.leftWidth <= 0.0f) return
        if (metrics.wifi) {
            val glyphScale = metrics.wifiWidth / owner.wifiGlyphs.bounds().width()
            val halfHeight = owner.wifiGlyphs.bounds().height() * glyphScale / 2.0f
            contentInk.set(left, frame.centreY - halfHeight, left + metrics.wifiWidth, frame.centreY + halfHeight)
        } else {
            owner.networkPaint.setTextSize(metrics.technologySize)
            owner.networkPaint.getFontMetrics(owner.textMetrics)
            owner.technologyBounds(metrics.technology, metrics.technologySize, contentInk)
            contentInk.offset(
                left + metrics.leftWidth / 2.0f
                        - owner.measureTechnology(metrics.technology, metrics.technologySize) / 2.0f,
                frame.centreY - (owner.textMetrics.ascent + owner.textMetrics.descent) / 2.0f
            )
        }
        val originX =
            CapsuleLayout.originX(frame.width, frame.scale, owner.capsuleCentred, IconPainter.Companion.CAPSULE_SHIFT_X)
        val originY = 60.0f - frame.height * frame.scale / 2.0f
        contentInk.set(
            originX + contentInk.left * frame.scale + owner.contentX(1),
            originY + contentInk.top * frame.scale + owner.contentY(1),
            originX + contentInk.right * frame.scale + owner.contentX(1),
            originY + contentInk.bottom * frame.scale + owner.contentY(1)
        )
        out.union(contentInk)
    }

    private fun batteryDigitsCentreX(): Float {
        var left = this.frame.centreX - this.metrics.contentWidth / 2.0f
        if (this.metrics.wifi || !this.metrics.technology.isEmpty()) {
            left += this.metrics.leftWidth + this.metrics.gap
        }
        return left + this.metrics.digitsWidth / 2.0f
    }

    private fun batteryDigitsBaseline(owner: IconPainter): Float {
        return this.frame.centreY - (owner.textMetrics.ascent + owner.textMetrics.descent) / 2.0f
    }

    fun drawRing(
        canvas: Canvas,
        width: Float,
        height: Float,
        halfTop: Float,
        halfBottom: Float,
        strokeWidth: Float,
        tint: Int,
        alpha: Int,
        from: Float,
        to: Float,
        weight: Float,
        opacity: Float
    ) {
        val radius = height / 2.0f
        val segmentTop = ((width / 2.0f) - halfTop) - radius
        val segmentBottom = ((width / 2.0f) - halfBottom) - radius
        val arc = (Math.PI * radius).toFloat()
        val circumference =
            (2.0f * segmentBottom) + (2.0f * arc) + (2.0f * segmentTop)
        if (circumference <= 0.0f) {
            return
        }
        val start = circumference * from
        val end = circumference * to
        this.paint.setColor(tint)
        this.paint.setAlpha(DisplayPolicy.paintAlpha(tint, alpha, opacity))
        this.paint.setStyle(Paint.Style.STROKE)
        this.paint.setStrokeWidth(strokeWidth * weight)
        this.paint.setStrokeCap(Paint.Cap.ROUND)
        this.paint.setStrokeJoin(Join.ROUND)
        this.ringPath.reset()
        var previousRun = -1
        var accumulated = 0.0f
        for (segment in 0..5) {
            val length = if (segment == 0 || segment == 5)
                segmentBottom
            else
                (if (segment == 1 || segment == 4) arc else segmentTop)
            val segmentStart = accumulated
            val segmentEnd = accumulated + length
            accumulated = segmentEnd
            if (segmentEnd <= start || segmentStart >= end || length <= 0.0f) {
                continue
            }
            val localStart = max(
                0.0f,
                (start - segmentStart) / length
            )
            val localEnd = min(
                1.0f,
                (end - segmentStart) / length
            )
            if (localEnd <= localStart) {
                continue
            }
            val run = if (segment <= 2) 0 else 1
            appendSegment(
                segment,
                localStart,
                localEnd,
                width,
                height,
                halfTop,
                halfBottom,
                radius,
                run != previousRun
            )
            previousRun = run
        }
        canvas.drawPath(this.ringPath, this.paint)
    }

    private fun appendSegment(
        segment: Int,
        from: Float,
        to: Float,
        width: Float,
        height: Float,
        halfTop: Float,
        halfBottom: Float,
        radius: Float,
        move: Boolean
    ) {
        if (segment == 1 || segment == 4) {
            if (segment == 1) {
                this.oval.set(0.0f, 0.0f, 2.0f * radius, 2.0f * radius)
                this.ringPath.arcTo(
                    this.oval,
                    90.0f + (180.0f * from),
                    180.0f * (to - from),
                    move
                )
            } else {
                this.oval.set(
                    width - (2.0f * radius),
                    0.0f,
                    width,
                    2.0f * radius
                )
                this.ringPath.arcTo(
                    this.oval,
                    270.0f + (180.0f * from),
                    180.0f * (to - from),
                    move
                )
            }
            return
        }
        val x1: Float
        val x2: Float
        val y: Float
        if (segment == 0) {
            x1 = (width / 2.0f) - halfBottom
            x2 = radius
            y = height
        } else if (segment == 5) {
            x1 = width - radius
            x2 = (width / 2.0f) + halfBottom
            y = height
        } else if (segment == 2) {
            x1 = radius
            x2 = (width / 2.0f) - halfTop
            y = 0.0f
        } else {
            x1 = (width / 2.0f) + halfTop
            x2 = width - radius
            y = 0.0f
        }
        val startX = x1 + ((x2 - x1) * from)
        val endX = x1 + ((x2 - x1) * to)
        if (move) {
            this.ringPath.moveTo(startX, y)
        } else {
            this.ringPath.lineTo(startX, y)
        }
        this.ringPath.lineTo(endX, y)
    }

    private fun drawRingSet(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        tint: Int
    ) {
        drawRing(
            canvas,
            this.frame.width,
            this.frame.height,
            this.frame.halfTop,
            this.frame.halfBottom,
            IconPainter.Companion.CAPSULE_STROKE,
            tint,
            IconPainter.Companion.CAPSULE_TRACK_ALPHA,
            0.0f,
            1.0f,
            owner.weight,
            owner.opacity
        )
        if (state.battery <= 0) {
            return
        }
        val level = min(100, state.battery) / 100.0f
        drawRing(
            canvas,
            this.frame.width,
            this.frame.height,
            this.frame.halfTop,
            this.frame.halfBottom,
            IconPainter.Companion.CAPSULE_STROKE,
            tint,
            255,
            0.0f,
            level,
            owner.weight,
            owner.opacity
        )
    }

    private fun drawSignalDots(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        tint: Int,
        mask: Int
    ) {
        if ((mask and IconPainter.Companion.MASK_HIDE_SIGNALS) != 0) {
            return
        }
        val topBars = state.card1Bars()
        val bottomBars = state.card2Bars()
        val topCross = state.card1Cross()
        val bottomCross = state.card2Cross()
        for (dot in 0..<IconPainter.Companion.CAPSULE_DOTS) {
            val topX = this.frame.firstTop + (dot * this.frame.stepTop) + this.frame.topX
            val bottomX = (this.frame.firstBottom
                    + (dot * this.frame.stepBottom) + this.frame.bottomX)
            if (owner.signalDotRemains(
                    topCross,
                    topX,
                    this.frame.topY,
                    this.frame.centreX + this.frame.topX,
                    this.frame.topY,
                    this.frame.dotRadius
                )
            ) {
                owner.fill(
                    tint,
                    dotFill(topBars, dot)
                )
                canvas.drawCircle(
                    topX,
                    this.frame.topY,
                    this.frame.dotRadius,
                    owner.p
                )
            }
            if (owner.signalDotRemains(
                    bottomCross,
                    bottomX,
                    this.frame.height + this.frame.bottomY,
                    this.frame.centreX + this.frame.bottomX,
                    this.frame.height + this.frame.bottomY,
                    this.frame.dotRadius
                )
            ) {
                owner.fill(
                    tint,
                    dotFill(bottomBars, dot)
                )
                canvas.drawCircle(
                    bottomX,
                    this.frame.height + this.frame.bottomY,
                    this.frame.dotRadius,
                    owner.p
                )
            }
        }
        if (topCross) {
            owner.drawSignalCross(
                canvas,
                tint,
                this.frame.centreX + this.frame.topX,
                this.frame.topY
            )
        }
        if (bottomCross) {
            owner.drawSignalCross(
                canvas,
                tint,
                this.frame.centreX + this.frame.bottomX,
                this.frame.height + this.frame.bottomY
            )
        }
    }

    private fun drawContent(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        tint: Int,
        plainTint: Int,
        mask: Int,
        batteryContentVisible: Boolean
    ) {
        val save = canvas.save()
        try {
            canvas.translate(localOffset(owner.contentX(1), frame.scale), localOffset(owner.contentY(1), frame.scale))
            this.frame.wifiContentCentreX = Float.NaN
            this.frame.contentX = (this.frame.centreX
                    - (this.metrics.contentWidth / 2.0f))
            drawLeftSlot(canvas, owner, state, plainTint, mask)
            if (batteryContentVisible) {
                drawBatteryDigits(
                    canvas,
                    owner,
                    tint
                )
            }
            drawWifiContent(canvas, owner, state, plainTint)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun drawLeftSlot(
        canvas: Canvas?,
        owner: IconPainter,
        state: IconState,
        tint: Int,
        mask: Int
    ) {
        if (this.metrics.wifi) {
            val hideWifi = (mask and IconPainter.Companion.MASK_HIDE_WIFI) != 0
            val hideCentre = (mask and IconPainter.Companion.MASK_HIDE_CENTRE) != 0
            if (!hideWifi && !hideCentre) {
                this.frame.wifiContentCentreX = (this.frame.contentX
                        + (this.metrics.leftWidth / 2.0f))
            } else if (hideWifi && !hideCentre) {
                val technology = state.centreTechnology()
                if (!technology.isEmpty()) {
                    owner.networkPaint.setTextSize(
                        this.metrics.technologySize
                    )
                    owner.networkPaint.getFontMetrics(owner.textMetrics)
                    owner.technologyText(
                        canvas!!,
                        technology,
                        this.frame.contentX
                                + (this.metrics.leftWidth / 2.0f),
                        this.frame.centreY
                                - ((owner.textMetrics.ascent
                                + owner.textMetrics.descent) / 2.0f),
                        tint
                    )
                }
            }
            this.frame.contentX += this.metrics.leftWidth + this.metrics.gap
            return
        }
        if (this.metrics.technology.isEmpty()) {
            return
        }
        if ((mask and IconPainter.Companion.MASK_HIDE_CENTRE) == 0) {
            owner.networkPaint.setTextSize(this.metrics.technologySize)
            owner.networkPaint.getFontMetrics(owner.textMetrics)
            owner.technologyText(
                canvas!!,
                this.metrics.technology,
                this.frame.contentX + (this.metrics.leftWidth / 2.0f),
                this.frame.centreY
                        - ((owner.textMetrics.ascent
                        + owner.textMetrics.descent) / 2.0f),
                tint
            )
        }
        this.frame.contentX += this.metrics.leftWidth + this.metrics.gap
    }

    private fun drawBatteryDigits(
        canvas: Canvas,
        owner: IconPainter,
        tint: Int
    ) {
        if (this.metrics.digits.isEmpty()) {
            return
        }
        val numberTint = StatusColors.batteryNumberTint(tint)
        owner.textPaint.setTextSize(this.metrics.digitSize)
        owner.textPaint.setColor(numberTint)
        owner.textPaint.setAlpha(
            DisplayPolicy.paintAlpha(
                numberTint,
                255,
                owner.opacity
            )
        )
        owner.textPaint.setTextAlign(Align.CENTER)
        owner.textPaint.getFontMetrics(owner.textMetrics)
        canvas.drawText(
            this.metrics.digits,
            batteryDigitsCentreX(),
            batteryDigitsBaseline(owner),
            owner.textPaint
        )
    }

    private fun drawWifiContent(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        tint: Int
    ) {
        if (java.lang.Float.isNaN(this.frame.wifiContentCentreX)) {
            return
        }
        val glyphScale = CapsuleGeometry.wifiScale(
            this.metrics.wifiWidth,
            owner.wifiGlyphs.bounds().width(),
            1.0f
        )
        val save = canvas.save()
        try {
            canvas.translate(
                this.frame.wifiContentCentreX
                        - (owner.wifiGlyphs.bounds().centerX() * glyphScale),
                this.frame.centreY
                        - (owner.wifiGlyphs.bounds().centerY() * glyphScale)
            )
            canvas.scale(glyphScale, glyphScale)
            owner.wifiGlyphs.drawAtOrigin(
                canvas,
                tint,
                state.wifiLevel,
                owner.opacity
            )
        } finally {
            canvas.restoreToCount(save)
        }
    }

    fun capsuleWifiOnly(owner: IconPainter, canvas: Canvas, iconState: IconState, iPlain: Int) {
        measure(owner, iconState)
        val content = this.metrics
        if (!content.wifi) {
            return
        }
        val fW = bodyWidth(owner)
        val fK = scale(owner)
        val fCx = fW / 2.0f
        val fLeftCx = (fCx - (content.contentWidth / 2.0f)
                + (content.leftWidth / 2.0f))
        val fS = CapsuleGeometry.wifiScale(
            content.wifiWidth,
            owner.wifiGlyphs.bounds().width(),
            1.0f
        )
        val iSave = canvas.save()
        canvas.translate(
            CapsuleLayout.originX(fW, fK, owner.capsuleCentred, IconPainter.Companion.CAPSULE_SHIFT_X),
            60.0f - ((IconPainter.Companion.CAPSULE_H * fK) / 2.0f)
        )
        canvas.scale(fK, fK)
        // 模块画 part 1 时会先按 anchorY(1) 把画布上移 —— 那是**圆环样式**里 Wi-Fi 的竖直锚点
        // （normalWifiY() 或换位置布局的表头 Y），胶囊的 Wi-Fi 不在那个高度，必须把这段偏移补回来，
        // 否则整枚 Wi-Fi 会跟着 anchorY 一起偏上/偏下
        canvas.translate(
            fLeftCx + localOffset(owner.contentX(1), fK) - (owner.wifiGlyphs.bounds().centerX() * fS),
            ((IconPainter.Companion.CAPSULE_H / 2.0f) + owner.anchorY(1, iconState) - owner.contentY(1)
                    + localOffset(owner.contentY(1), fK))
                    - (owner.wifiGlyphs.bounds().centerY() * fS)
        )
        canvas.scale(fS, fS)
        owner.wifiGlyphs.drawAtOrigin(
            canvas,
            iPlain,
            iconState.wifiLevel,
            owner.opacity
        )
        canvas.restoreToCount(iSave)
    }

    fun draw(owner: IconPainter, canvas: Canvas, iconState: IconState, iTint: Int, iPlain: Int, iMask: Int) {
        updateRingGeometry(owner, iconState)
        if (owner.hasContentOffsets() && BatteryTransferPolicy.drawsBatteryRing(iMask)) {
            owner.drawPositionedRing(canvas, iconState, iTint, iPlain)
        }
        val save = canvas.save()
        try {
            canvas.translate(
                CapsuleLayout.originX(
                    this.frame.width,
                    this.frame.scale,
                    owner.capsuleCentred,
                    IconPainter.Companion.CAPSULE_SHIFT_X
                ),
                60.0f - ((this.frame.height * this.frame.scale) / 2.0f)
            )
            canvas.scale(this.frame.scale, this.frame.scale)
            if (!owner.hasContentOffsets() && BatteryTransferPolicy.drawsBatteryRing(iMask)) {
                drawRingSet(canvas, owner, iconState, iTint)
            }
            drawSignalDots(canvas, owner, iconState, iPlain, iMask)
            drawContent(
                canvas,
                owner,
                iconState,
                iTint,
                iPlain,
                iMask,
                BatteryTransferPolicy.drawsBatteryContent(iMask)
            )
        } finally {
            canvas.restoreToCount(save)
            owner.textPaint.setTextAlign(Align.LEFT)
            owner.networkPaint.setTextAlign(Align.LEFT)
        }
    }

    companion object {
        private const val DOT_TRACK_ALPHA = 48
        @JvmStatic fun dotFill(bars: Int, dot: Int): Int {
            val span = 4.0f / 3.0f
            val fill = if (bars <= 0)
                0.0f
            else
                Motion.clamp((bars - (dot * span)) / span)
            return Math.round(
                DOT_TRACK_ALPHA
                        + (fill * (255.0f - DOT_TRACK_ALPHA))
            )
        }

        @JvmStatic fun safeLength(value: Float): Float {
            return CapsuleGeometry.safeLength(value)
        }

        @JvmStatic fun localOffset(offset: Float, scale: Float): Float {
            return if (java.lang.Float.isFinite(offset) && java.lang.Float.isFinite(scale) && scale > 0.0f) offset / scale else 0.0f
        }

        @JvmStatic fun adaptedSignalHalfGap(
            original: Float, maximum: Float, halfSpan: Float,
            radius: Float, capInset: Float, x: Float, y: Float, clearance: Float
        ): Float {
            // Horizontal motion moves both gap edges together in updateRingGeometry.
            if (y == 0.0f) return original
            val reach = radius + capInset + clearance
            if (abs(y) >= reach) return 0.0f
            val edge = sqrt(max(0.0f, reach * reach - y * y).toDouble()).toFloat()
            return min(maximum, max(0.0f, halfSpan + edge))
        }
    }
}
