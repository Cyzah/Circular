package com.cyzah.circular.core.graphics

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.max
import kotlin.math.min

/**
 * Allocation-free Canvas renderer for the SVG-derived new battery.
 */
class BatteryShapeRenderer {
    private val bodyPath = Path()
    private val collapseBodyPath = Path()
    private var activeBodyPath: Path = bodyPath
    private var activeBodyWidthSvg = BatteryTransferConfig.BODY_WIDTH
    private val terminalPath = Path()
    private val innerVoid = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outsideNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val insideNumberMask = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numberInk = Rect()
    private val outsideNumberMetrics = Paint.FontMetrics()
    private val collapseFillMatrix = Matrix()
    private var collapseFill: LinearGradient? = null
    private var collapseFillBattery = -1
    private var collapseFillTint = 0

    init {
        this.bodyPath.moveTo(
            BatteryTransferConfig.BODY_RIGHT_CURVE_X,
            BatteryTransferConfig.BODY_Y
        )
        this.bodyPath.lineTo(
            BatteryTransferConfig.BODY_CORNER_X,
            BatteryTransferConfig.BODY_Y
        )
        this.bodyPath.cubicTo(
            BatteryTransferConfig.BODY_LEFT_CONTROL_X,
            BatteryTransferConfig.BODY_Y,
            BatteryTransferConfig.BODY_X,
            BatteryTransferConfig.BODY_TOP_CONTROL_Y,
            BatteryTransferConfig.BODY_X,
            BatteryTransferConfig.BODY_CORNER_Y
        )
        this.bodyPath.lineTo(
            BatteryTransferConfig.BODY_X,
            BatteryTransferConfig.BODY_HEIGHT
                    - BatteryTransferConfig.BODY_CORNER_Y
        )
        this.bodyPath.cubicTo(
            BatteryTransferConfig.BODY_X,
            BatteryTransferConfig.BODY_BOTTOM_CONTROL_Y,
            BatteryTransferConfig.BODY_LEFT_CONTROL_X,
            BatteryTransferConfig.BODY_HEIGHT,
            BatteryTransferConfig.BODY_CORNER_X,
            BatteryTransferConfig.BODY_HEIGHT
        )
        this.bodyPath.lineTo(
            BatteryTransferConfig.BODY_RIGHT_CURVE_X,
            BatteryTransferConfig.BODY_HEIGHT
        )
        this.bodyPath.cubicTo(
            BatteryTransferConfig.BODY_RIGHT_CONTROL_X,
            BatteryTransferConfig.BODY_HEIGHT,
            BatteryTransferConfig.BODY_WIDTH,
            BatteryTransferConfig.BODY_BOTTOM_CONTROL_Y,
            BatteryTransferConfig.BODY_WIDTH,
            BatteryTransferConfig.BODY_HEIGHT
                    - BatteryTransferConfig.BODY_CORNER_Y
        )
        this.bodyPath.lineTo(
            BatteryTransferConfig.BODY_WIDTH,
            BatteryTransferConfig.BODY_CORNER_Y
        )
        this.bodyPath.cubicTo(
            BatteryTransferConfig.BODY_WIDTH,
            BatteryTransferConfig.BODY_TOP_CONTROL_Y,
            BatteryTransferConfig.BODY_RIGHT_CONTROL_X,
            BatteryTransferConfig.BODY_Y,
            BatteryTransferConfig.BODY_RIGHT_CURVE_X,
            BatteryTransferConfig.BODY_Y
        )
        this.bodyPath.close()
        // Preserve the supplied SVG's five cubic segments, including its soft base.
        this.terminalPath.moveTo(166.75f, 29.0f)
        this.terminalPath.cubicTo(167.99f, 28.93f, 169.55f, 29.74f, 170.51f, 30.48f)
        this.terminalPath.cubicTo(175.87f, 34.6f, 176.18f, 44.65f, 172.78f, 50.06f)
        this.terminalPath.cubicTo(171.8f, 51.62f, 170.47f, 52.91f, 168.74f, 53.59f)
        this.terminalPath.cubicTo(167.38f, 54.13f, 165.11f, 54.47f, 165.0f, 52.42f)
        this.terminalPath.lineTo(165.0f, 30.76f)
        this.terminalPath.cubicTo(165.07f, 29.7f, 165.68f, 29.06f, 166.75f, 29.0f)
        this.terminalPath.close()
        val outlineScale = Matrix()
        outlineScale.setScale(
            BatteryTransferConfig.OUTLINE_SCALE_X,
            BatteryTransferConfig.OUTLINE_SCALE_Y
        )
        this.terminalPath.transform(outlineScale)
    }

    fun draw(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        geometry: BatteryShapeGeometry,
        placement: BatteryNumberPlacement?,
        batteryTint: Int,
        plainTint: Int,
        alpha: Float
    ) {
        draw(
            canvas,
            owner,
            state,
            geometry,
            placement,
            state.battery,
            batteryTint,
            plainTint,
            alpha,
            null
        )
    }

    @JvmOverloads
    fun draw(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        geometry: BatteryShapeGeometry,
        placement: BatteryNumberPlacement?,
        contentBattery: Int,
        batteryTint: Int,
        plainTint: Int,
        alpha: Float,
        opening: OpeningBatteryMorph? = null
    ) {
        val safeAlpha = Motion.clamp(alpha)
        if (safeAlpha <= 0.0f || geometry.width <= 0.0f || geometry.height <= 0.0f) {
            return
        }
        val scaleX = geometry.width / BatteryTransferConfig.SVG_WIDTH
        val scaleY = geometry.height / BatteryTransferConfig.SVG_HEIGHT
        val left = geometry.centreX - (geometry.width / 2.0f)
        val top = geometry.centreY - (geometry.height / 2.0f)
        activeBodyPath = bodyPath
        activeBodyWidthSvg = BatteryTransferConfig.BODY_WIDTH
        if (geometry.collapse > 0.0f) {
            activeBodyWidthSvg = geometry.bodyWidth / scaleX
            collapseBodyPath.reset()
            collapseBodyPath.addRoundRect(
                0.0f, 0.0f, activeBodyWidthSvg,
                BatteryTransferConfig.SVG_HEIGHT,
                geometry.cornerRadius / scaleX, geometry.cornerRadius / scaleY, Path.Direction.CW
            )
            activeBodyPath = collapseBodyPath
        }
        if (geometry.stage == BatteryShapeGeometry.Stage.POINT) {
            this.fillPaint.setStyle(Paint.Style.FILL)
            this.fillPaint.setColor(batteryTint)
            this.fillPaint.setAlpha(
                DisplayPolicy.paintAlpha(
                    batteryTint,
                    255,
                    safeAlpha
                )
            )
            canvas.drawCircle(
                geometry.centreX,
                geometry.centreY,
                geometry.width / 2.0f,
                this.fillPaint
            )
            return
        }

        if (geometry.collapse > 0.0f && opening == null) {
            drawCollapsingBattery(
                canvas, owner, state, geometry, placement, contentBattery,
                batteryTint, plainTint, safeAlpha, left, top, scaleX, scaleY
            )
            return
        }

        val save = canvas.save()
        try {
            canvas.translate(left, top)
            canvas.scale(scaleX, scaleY)
            run {
                // Erase glyphs only from this battery layer. The underlying
                // control-center background and neighbouring content stay intact.
                val layer = if (placement == BatteryNumberPlacement.INSIDE && contentBattery >= 0)
                    canvas.saveLayer(
                        0f, 0f, BatteryTransferConfig.SVG_WIDTH,
                        BatteryTransferConfig.SVG_HEIGHT, null
                    )
                else
                    -1
                try {
                    drawBattery(
                        canvas, owner, state, contentBattery,
                        batteryTint, plainTint, placement, safeAlpha,
                        if (opening == null)
                            safeAlpha * geometry.batteryContentAlpha
                        else
                            Motion.clamp(opening.contentAlpha), opening, scaleY / scaleX
                    )
                } finally {
                    if (layer >= 0) canvas.restoreToCount(layer)
                }
            }
        } finally {
            canvas.restoreToCount(save)
            owner.textPaint.setTextAlign(Align.LEFT)
        }
    }

    private fun drawCollapsingBattery(
        canvas: Canvas, owner: IconPainter, state: IconState,
        geometry: BatteryShapeGeometry, placement: BatteryNumberPlacement?,
        contentBattery: Int, batteryTint: Int, plainTint: Int, alpha: Float,
        left: Float, top: Float, scaleX: Float, scaleY: Float
    ) {
        val contentAlpha = alpha * geometry.batteryContentAlpha
        val layer = if (placement == BatteryNumberPlacement.INSIDE && contentBattery >= 0 && contentAlpha > 0)
            canvas.saveLayer(left, top, left + geometry.width, top + geometry.height, null)
        else
            -1
        try {
            val battery = max(0, min(100, state.battery))
            val ratio = if (geometry.stage == BatteryShapeGeometry.Stage.LINE)
                1.0f
            else
                BatteryTransferConfig.chargedBodyRightSvg(battery) / BatteryTransferConfig.BODY_WIDTH
            fillPaint.setStyle(Paint.Style.FILL)
            if (ratio <= 0 || ratio >= 1) {
                val tint = if (ratio <= 0) EMPTY_COLOR else batteryTint
                fillPaint.setColor(tint)
                fillPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, alpha))
            } else {
                if (collapseFill == null || collapseFillBattery != battery || collapseFillTint != batteryTint) {
                    collapseFill = LinearGradient(
                        0f, 0f, 1f, 0f,
                        intArrayOf(batteryTint, batteryTint, EMPTY_COLOR, EMPTY_COLOR),
                        floatArrayOf(0f, ratio, ratio, 1f), Shader.TileMode.CLAMP
                    )
                    collapseFillBattery = battery
                    collapseFillTint = batteryTint
                }
                collapseFillMatrix.setScale(geometry.bodyWidth, 1f)
                collapseFillMatrix.postTranslate(left, 0f)
                collapseFill!!.setLocalMatrix(collapseFillMatrix)
                fillPaint.setColor(Color.WHITE)
                fillPaint.setAlpha(Math.round(255 * alpha))
                fillPaint.setShader(collapseFill)
            }
            try {
                // One physical round-rect coverage mask for both charge colors.
                // No repeated AA edge or anisotropically tessellated SVG caps.
                canvas.drawRoundRect(
                    left, top, left + geometry.bodyWidth, top + geometry.height,
                    geometry.cornerRadius, geometry.cornerRadius, fillPaint
                )
            } finally {
                fillPaint.setShader(null)
            }
            if (contentAlpha > 0) {
                val save = canvas.save()
                try {
                    canvas.translate(left, top)
                    canvas.scale(scaleX, scaleY)
                    drawBatteryContent(
                        canvas, owner, contentBattery, battery, batteryTint, plainTint,
                        placement, alpha, contentAlpha, null, scaleY / scaleX
                    )
                } finally {
                    canvas.restoreToCount(save)
                }
            }
        } finally {
            if (layer >= 0) canvas.restoreToCount(layer)
            owner.textPaint.setTextAlign(Align.LEFT)
        }
    }

    private fun drawBattery(
        canvas: Canvas,
        owner: IconPainter,
        state: IconState,
        contentBattery: Int,
        batteryTint: Int,
        plainTint: Int,
        placement: BatteryNumberPlacement?,
        alpha: Float,
        contentAlpha: Float,
        opening: OpeningBatteryMorph?,
        numberAspectCorrection: Float
    ) {
        val battery = max(0, min(100, state.battery))
        val bodySave = canvas.save()
        try {
            if (opening != null && opening.interiorWidth > 0.0f && opening.interiorHeight > 0.0f && opening.totalWidth > 0.0f && opening.totalHeight > 0.0f) {
                // Opening dimensions are device pixels. This canvas is already
                // in SVG units, so use the complete projected frame for conversion.
                val pixelsPerSvgX = opening.totalWidth / BatteryTransferConfig.SVG_WIDTH
                val pixelsPerSvgY = opening.totalHeight / BatteryTransferConfig.SVG_HEIGHT
                val insetX = opening.interiorInset / pixelsPerSvgX
                val insetY = opening.interiorInset / pixelsPerSvgY
                this.innerVoid.reset()
                this.innerVoid.addRoundRect(
                    insetX, insetY,
                    insetX + opening.interiorWidth / pixelsPerSvgX,
                    insetY + opening.interiorHeight / pixelsPerSvgY,
                    opening.interiorRadiusX / pixelsPerSvgX,
                    opening.interiorRadiusY / pixelsPerSvgY, Path.Direction.CW
                )
                canvas.clipOutPath(this.innerVoid)
            }
            this.fillPaint.setStyle(Paint.Style.FILL)
            this.fillPaint.setColor(EMPTY_COLOR)
            this.fillPaint.setAlpha(DisplayPolicy.paintAlpha(EMPTY_COLOR, 255, alpha))
            canvas.drawPath(this.activeBodyPath, this.fillPaint)
            if (battery > 0) {
                val save = canvas.save()
                try {
                    if (BatteryShapeLayout.clipsChargedArea(battery)) {
                        canvas.clipRect(
                            BatteryShapeLayout.chargedClipLeftSvg(),
                            BatteryShapeLayout.chargedClipTopSvg(),
                            BatteryTransferConfig.chargedBodyRightSvg(battery)
                                    * activeBodyWidthSvg / BatteryTransferConfig.BODY_WIDTH,
                            BatteryShapeLayout.chargedClipBottomSvg()
                        )
                    }
                    this.fillPaint.setColor(batteryTint)
                    this.fillPaint.setAlpha(DisplayPolicy.paintAlpha(batteryTint, 255, alpha))
                    canvas.drawPath(this.activeBodyPath, this.fillPaint)
                } finally {
                    canvas.restoreToCount(save)
                }
            }
        } finally {
            // The interior cutout belongs only to the body. Digits and the
            // terminal must retain their independent transition opacity.
            canvas.restoreToCount(bodySave)
        }

        drawBatteryContent(
            canvas, owner, contentBattery, battery, batteryTint, plainTint, placement,
            alpha, contentAlpha, opening, numberAspectCorrection
        )
    }

    private fun drawBatteryContent(
        canvas: Canvas, owner: IconPainter, contentBattery: Int,
        battery: Int, batteryTint: Int, plainTint: Int,
        placement: BatteryNumberPlacement?, alpha: Float, contentAlpha: Float,
        opening: OpeningBatteryMorph?, numberAspectCorrection: Float
    ) {
        if (contentAlpha > 0.0f) {
            drawTerminal(canvas, EMPTY_COLOR, contentAlpha)
            if (battery > 0) {
                val save = canvas.save()
                try {
                    canvas.clipRect(
                        BatteryTransferConfig.TERMINAL_START_X,
                        BatteryTransferConfig.BODY_Y,
                        BatteryTransferConfig.chargedTerminalRightSvg(
                            battery
                        ),
                        BatteryTransferConfig.BODY_HEIGHT
                    )
                    drawTerminal(canvas, batteryTint, contentAlpha)
                } finally {
                    canvas.restoreToCount(save)
                }
            }
            val numberSave = canvas.save()
            try {
                if (placement == BatteryNumberPlacement.INSIDE) canvas.clipPath(this.activeBodyPath)
                // The shell deliberately changes aspect during collapse. Text
                // keeps the same horizontal and vertical scale around its anchor.
                val numberX = if (placement == BatteryNumberPlacement.INSIDE)
                    BatteryTransferConfig.NUMBER_INSIDE_X_SVG
                else
                    BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG
                canvas.scale(numberAspectCorrection, 1.0f, numberX, BatteryTransferConfig.SVG_HEIGHT / 2.0f)
                drawNumber(
                    canvas, owner, contentBattery, placement,
                    batteryTint, plainTint, if (placement == BatteryNumberPlacement.INSIDE)
                        if (opening == null) Motion.clamp(contentAlpha / alpha) else contentAlpha
                    else
                        contentAlpha
                )
            } finally {
                canvas.restoreToCount(numberSave)
            }
        }
    }

    private fun drawTerminal(canvas: Canvas, tint: Int, alpha: Float) {
        this.fillPaint.setStyle(Paint.Style.FILL)
        this.fillPaint.setColor(tint)
        this.fillPaint.setAlpha(
            DisplayPolicy.paintAlpha(
                tint,
                255,
                alpha
            )
        )
        canvas.drawPath(this.terminalPath, this.fillPaint)
    }

    private fun drawNumber(
        canvas: Canvas,
        owner: IconPainter,
        contentBattery: Int,
        placement: BatteryNumberPlacement?,
        batteryTint: Int,
        plainTint: Int,
        alpha: Float
    ) {
        drawReadoutGlyph(
            canvas, owner, contentBattery, placement, batteryTint, alpha,
            placement == BatteryNumberPlacement.INSIDE
        )
    }

    fun drawReadoutGlyph(
        canvas: Canvas, owner: IconPainter, contentBattery: Int,
        placement: BatteryNumberPlacement?, tint: Int, alpha: Float, cutout: Boolean
    ) {
        if (alpha <= 0.0f || placement == BatteryNumberPlacement.HIDDEN || contentBattery < 0) {
            return
        }
        val digits: String = IconPainter.Companion.NUMBERS[min(100, contentBattery)]
        val size = configuredNumberTextSizeSvg(owner, placement)
        owner.textPaint.setTextSize(size)
        owner.textPaint.getTextBounds(digits, 0, digits.length, owner.numberBounds)
        owner.textPaint.getFontMetrics(owner.textMetrics)
        val baseline = (BatteryTransferConfig.SVG_HEIGHT / 2.0f
                - ((owner.textMetrics.ascent + owner.textMetrics.descent) / 2.0f))
        if (placement == BatteryNumberPlacement.INSIDE) {
            owner.textPaint.setTextAlign(Align.CENTER)
            // Copy real typeface weight and supplemental stroke without leaking
            // the erasing blend mode into the shared status-bar text paint.
            this.insideNumberMask.set(owner.textPaint)
            this.insideNumberMask.setBlendMode(if (cutout) BlendMode.DST_OUT else BlendMode.SRC_OVER)
            drawText(
                canvas, this.insideNumberMask, digits,
                BatteryTransferConfig.NUMBER_INSIDE_X_SVG,
                baseline,
                if (cutout) Color.WHITE else tint,
                alpha
            )
        } else {
            prepareOutsideNumberPaint(owner)
            this.outsideNumberPaint.getFontMetrics(this.outsideNumberMetrics)
            val outsideBaseline = (BatteryTransferConfig.SVG_HEIGHT / 2.0f
                    - ((this.outsideNumberMetrics.ascent
                    + this.outsideNumberMetrics.descent) / 2.0f))
            drawText(
                canvas, this.outsideNumberPaint, digits,
                BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG,
                outsideBaseline,
                tint,
                alpha
            )
        }
    }

    fun numberBoundsSvg(owner: IconPainter, battery: Int, placement: BatteryNumberPlacement?, out: RectF) {
        out.setEmpty()
        if (battery < 0 || placement == BatteryNumberPlacement.HIDDEN) return
        val digits: String = IconPainter.Companion.NUMBERS[min(100, battery)]
        val savedSize = owner.textPaint.getTextSize()
        val savedAlign = owner.textPaint.getTextAlign()
        try {
            val paint: Paint?
            val left: Float
            if (placement == BatteryNumberPlacement.OUTSIDE) {
                prepareOutsideNumberPaint(owner)
                paint = this.outsideNumberPaint
                left = BatteryTransferConfig.NUMBER_OUTSIDE_X_SVG
            } else {
                owner.textPaint.setTextSize(configuredNumberTextSizeSvg(owner, placement))
                owner.textPaint.setTextAlign(Align.LEFT)
                paint = owner.textPaint
                left = BatteryTransferConfig.NUMBER_INSIDE_X_SVG - paint.measureText(digits) / 2.0f
            }
            paint.getFontMetrics(this.outsideNumberMetrics)
            val baseline = (BatteryTransferConfig.SVG_HEIGHT / 2.0f
                    - (this.outsideNumberMetrics.ascent + this.outsideNumberMetrics.descent) / 2.0f)
            paint.getTextBounds(digits, 0, digits.length, this.numberInk)
            val pad = paint.getStrokeWidth() / 2.0f
            out.set(
                left + this.numberInk.left - pad, baseline + this.numberInk.top - pad,
                left + this.numberInk.right + pad, baseline + this.numberInk.bottom + pad
            )
        } finally {
            owner.textPaint.setTextSize(savedSize)
            owner.textPaint.setTextAlign(savedAlign)
        }
    }

    fun outsideNumberOverflowUnits(owner: IconPainter, state: IconState): Float {
        return outsideNumberOverflowUnits(owner, state.battery)
    }

    fun outsideNumberOverflowUnits(owner: IconPainter, battery: Int): Float {
        if (battery < 0) {
            return 0.0f
        }
        prepareOutsideNumberPaint(owner)
        val digits: String? = IconPainter.Companion.NUMBERS[min(100, battery)]
        return BatteryShapeLayout.outsideNumberOverflowUnits(
            this.outsideNumberPaint.measureText(digits),
            this.outsideNumberPaint.measureText(
                SINGLE_DIGIT_REFERENCE
            )
        )
    }

    fun outsideNumberMaximumOverflowUnits(owner: IconPainter): Float {
        prepareOutsideNumberPaint(owner)
        return BatteryShapeLayout.outsideNumberOverflowUnits(
            this.outsideNumberPaint.measureText(
                IconPainter.Companion.NUMBERS[100]
            ),
            this.outsideNumberPaint.measureText(
                SINGLE_DIGIT_REFERENCE
            )
        )
    }

    fun outsideNumberAdaptiveBatteryShiftUnits(
        owner: IconPainter,
        state: IconState
    ): Float {
        return outsideNumberAdaptiveBatteryShiftUnits(
            owner,
            state.battery
        )
    }

    fun outsideNumberAdaptiveBatteryShiftUnits(
        owner: IconPainter,
        battery: Int
    ): Float {
        return (outsideNumberMaximumOverflowUnits(owner)
                - outsideNumberOverflowUnits(owner, battery))
    }

    fun prepareOutsideNumberTypeface(owner: IconPainter): Typeface? {
        prepareOutsideNumberPaint(owner)
        return this.outsideNumberPaint.getTypeface()
    }

    fun measureOutsideNumberSvg(
        owner: IconPainter,
        digits: String?
    ): Float {
        prepareOutsideNumberPaint(owner)
        return this.outsideNumberPaint.measureText(digits)
    }

    private fun prepareOutsideNumberPaint(owner: IconPainter) {
        val typeface = owner.textPaint.typeface
        if (outsideNumberPaint.typeface !== typeface) {
            outsideNumberPaint.typeface = typeface
        }
        this.outsideNumberPaint.setTextSize(
            configuredNumberTextSizeSvg(owner, BatteryNumberPlacement.OUTSIDE)
        )
        val stroke = (this.outsideNumberPaint.getTextSize()
                * StatusFontWeight.batteryStrokeRatio(owner.batteryWeightPercent()))
        this.outsideNumberPaint.setStrokeWidth(stroke)
        this.outsideNumberPaint.setStyle(if (stroke > 0) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL)
        this.outsideNumberPaint.setTextAlign(Align.LEFT)
    }

    fun configuredNumberTextSizeSvg(owner: IconPainter, placement: BatteryNumberPlacement?): Float {
        return BatteryTransferConfig.numberTextSizeSvg(placement) * owner.percentScale / 100.0f
    }

    private fun drawText(
        canvas: Canvas,
        paint: Paint,
        digits: String?,
        x: Float,
        baseline: Float,
        tint: Int,
        alpha: Float
    ) {
        paint.setColor(tint)
        paint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, alpha))
        canvas.drawText(digits!!, x, baseline, paint)
    }

    companion object {
        private const val SINGLE_DIGIT_REFERENCE = "0"
        private val EMPTY_COLOR = Color.rgb(
            Math.round(BatteryTransferConfig.EMPTY_COLOR_R * 255.0f),
            Math.round(BatteryTransferConfig.EMPTY_COLOR_G * 255.0f),
            Math.round(BatteryTransferConfig.EMPTY_COLOR_B * 255.0f)
        )

        private fun contrastTextColor(background: Int): Int {
            val luminance = (((Color.red(background) * 0.299f)
                    + (Color.green(background) * 0.587f)
                    + (Color.blue(background) * 0.114f))) / 255.0f
            return if (luminance > 0.58f) Color.BLACK else Color.WHITE
        }
    }
}
