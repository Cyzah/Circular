package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Join
import android.graphics.Rect
import android.graphics.RectF
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DisplayPolicy

/**
 * Draws and measures technology labels without sharing temporary paint state with the main icon.
 */
class TechnologyRenderer {
    private val morphPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val morphSuffixPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val textMetrics = Paint.FontMetrics()
    private val glyphBounds = Rect()

    fun bounds(value: String?, textSize: Float, normalPaint: Paint, weightPercent: Int, out: RectF) {
        bounds(value, textSize, normalPaint, weightPercent, false, out)
    }

    fun bounds(
        value: String?, textSize: Float, normalPaint: Paint, weightPercent: Int,
        compressFiveGa: Boolean, out: RectF
    ) {
        out.setEmpty()
        if (value == null || value.isEmpty() || !java.lang.Float.isFinite(textSize) || textSize <= 0.0f) {
            return
        }
        val base = IconGeometry.technologyBase(value)
        val suffix: String = suffixAfter(value, base)
        val normal: String = normalBase(base, suffix, compressFiveGa)
        val condensed: String = condensedBase(base, suffix, compressFiveGa)
        configureForMeasurement(normalPaint, textSize)
        val strokeRatio = AppearanceScale.technologyWeightStrokeRatio(weightPercent)
        includeBounds(out, morphPaint, normal, 0.0f, 0.0f, strokeRatio)
        var cursor = morphPaint.measureText(normal)
        if (!condensed.isEmpty()) {
            morphPaint.setTextScaleX(TechnologyText.Companion.baseWidthScale(base, suffix, compressFiveGa))
            includeBounds(out, morphPaint, condensed, cursor, 0.0f, strokeRatio)
            cursor += morphPaint.measureText(condensed)
            morphPaint.setTextScaleX(1.0f)
        }
        if (!suffix.isEmpty()) {
            morphSuffixPaint.setTypeface(normalPaint.getTypeface())
            morphSuffixPaint.setFontFeatureSettings(normalPaint.getFontFeatureSettings())
            morphSuffixPaint.setTextSize(textSize * SUFFIX_SCALE)
            morphSuffixPaint.setTextScaleX(TechnologyText.Companion.suffixWidthScale(base, suffix, compressFiveGa))
            includeBounds(
                out, morphSuffixPaint, suffix, cursor,
                -textSize * SUFFIX_BASELINE_SHIFT, strokeRatio
            )
            morphSuffixPaint.setTextScaleX(1.0f)
        }
    }

    private fun includeBounds(
        out: RectF, paint: Paint, text: String,
        x: Float, y: Float, strokeRatio: Float
    ) {
        if (text.isEmpty()) {
            return
        }
        // getTextBounds supplies ink extents; advance width alone misses glyph
        // overhang, suffix baseline and the extra stroke used by heavier text.
        paint.setStyle(Paint.Style.FILL)
        paint.setStrokeWidth(0.0f)
        paint.getTextBounds(text, 0, text.length, this.glyphBounds)
        if (!this.glyphBounds.isEmpty()) {
            val pad = paint.getTextSize() * strokeRatio / 2.0f + 1.0f
            out.union(
                x + this.glyphBounds.left - pad, y + this.glyphBounds.top - pad,
                x + this.glyphBounds.right + pad, y + this.glyphBounds.bottom + pad
            )
        }
    }

    @JvmOverloads
    fun drawCentred(
        canvas: Canvas, value: String, centreX: Float, baseline: Float, tint: Int,
        opacity: Float, normalPaint: Paint, suffixPaint: Paint, weightPercent: Int,
        compressFiveGa: Boolean = false
    ) {
        val base = IconGeometry.technologyBase(value)
        val suffix: String = suffixAfter(value, base)
        val normalBase: String = normalBase(base, suffix, compressFiveGa)
        val condensedBase: String = condensedBase(base, suffix, compressFiveGa)
        val strokeRatio = AppearanceScale.technologyWeightStrokeRatio(weightPercent)

        normalPaint.setColor(tint)
        normalPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, opacity))
        normalPaint.setTextScaleX(1.0f)
        applyStroke(normalPaint, strokeRatio)
        if (suffix.isEmpty() && condensedBase.isEmpty()) {
            normalPaint.setTextAlign(Align.CENTER)
            canvas.drawText(value, centreX, baseline, normalPaint)
            return
        }

        val textSize = normalPaint.getTextSize()
        suffixPaint.setColor(tint)
        suffixPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, opacity))
        suffixPaint.setTextSize(textSize * SUFFIX_SCALE)
        suffixPaint.setTextScaleX(TechnologyText.Companion.suffixWidthScale(base, suffix, compressFiveGa))
        applyStroke(suffixPaint, strokeRatio)
        val normalWidth = normalPaint.measureText(normalBase)
        val condensedWidth = if (condensedBase.isEmpty())
            0.0f
        else
            (normalPaint.measureText(condensedBase)
                    * TechnologyText.Companion.baseWidthScale(base, suffix, compressFiveGa))
        val suffixWidth = if (suffix.isEmpty()) 0.0f else suffixPaint.measureText(suffix)
        var cursor = centreX - ((normalWidth + condensedWidth + suffixWidth) / 2.0f)
        normalPaint.setTextAlign(Align.LEFT)
        canvas.drawText(normalBase, cursor, baseline, normalPaint)
        cursor += normalWidth
        if (!condensedBase.isEmpty()) {
            normalPaint.setTextScaleX(TechnologyText.Companion.baseWidthScale(base, suffix, compressFiveGa))
            canvas.drawText(condensedBase, cursor, baseline, normalPaint)
            normalPaint.setTextScaleX(1.0f)
            cursor += condensedWidth
        }
        suffixPaint.setTextAlign(Align.LEFT)
        if (!suffix.isEmpty()) {
            canvas.drawText(
                suffix,
                cursor,
                baseline - (textSize * SUFFIX_BASELINE_SHIFT),
                suffixPaint
            )
        }
        suffixPaint.setTextScaleX(1.0f)
    }

    @JvmOverloads
    fun measure(
        value: String?,
        textSize: Float,
        normalPaint: Paint,
        compressFiveGa: Boolean = false
    ): Float {
        if (value == null || value.isEmpty()) {
            return 0.0f
        }
        val base = IconGeometry.technologyBase(value)
        val suffix: String = suffixAfter(value, base)
        val normalBase: String = normalBase(base, suffix, compressFiveGa)
        val condensedBase: String = condensedBase(base, suffix, compressFiveGa)

        configureForMeasurement(normalPaint, textSize)
        var width = morphPaint.measureText(normalBase)
        if (!condensedBase.isEmpty()) {
            width += (morphPaint.measureText(condensedBase)
                    * TechnologyText.Companion.baseWidthScale(base, suffix, compressFiveGa))
        }
        if (suffix.isEmpty()) {
            return width
        }
        morphSuffixPaint.setTypeface(normalPaint.getTypeface())
        morphSuffixPaint.setFontFeatureSettings(normalPaint.getFontFeatureSettings())
        morphSuffixPaint.setTextSize(textSize * SUFFIX_SCALE)
        morphSuffixPaint.setTextScaleX(TechnologyText.Companion.suffixWidthScale(base, suffix, compressFiveGa))
        width += morphSuffixPaint.measureText(suffix)
        morphSuffixPaint.setTextScaleX(1.0f)
        return width
    }

    fun baselineOffset(textSize: Float, normalPaint: Paint): Float {
        configureForMeasurement(normalPaint, textSize)
        morphPaint.getFontMetrics(textMetrics)
        return (-(textMetrics.ascent + textMetrics.descent)) / 2.0f
    }

    @JvmOverloads
    fun drawLeft(
        canvas: Canvas?, value: String?, left: Float, baseline: Float, textSize: Float,
        alpha: Float, tint: Int, normalPaint: Paint, weightPercent: Int,
        compressFiveGa: Boolean = false
    ) {
        if (value == null || value.isEmpty() || alpha <= 0.0f) {
            return
        }
        val base = IconGeometry.technologyBase(value)
        val suffix: String = suffixAfter(value, base)
        val normalBase: String = normalBase(base, suffix, compressFiveGa)
        val condensedBase: String = condensedBase(base, suffix, compressFiveGa)
        val strokeRatio = AppearanceScale.technologyWeightStrokeRatio(weightPercent)

        configureForMeasurement(normalPaint, textSize)
        morphPaint.setTextAlign(Align.LEFT)
        morphPaint.setColor(tint)
        morphPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, alpha))
        applyStroke(morphPaint, strokeRatio)
        canvas!!.drawText(normalBase, left, baseline, morphPaint)
        var cursor = left + morphPaint.measureText(normalBase)
        if (!condensedBase.isEmpty()) {
            morphPaint.setTextScaleX(TechnologyText.Companion.baseWidthScale(base, suffix, compressFiveGa))
            canvas.drawText(condensedBase, cursor, baseline, morphPaint)
            cursor += morphPaint.measureText(condensedBase)
            morphPaint.setTextScaleX(1.0f)
        }
        if (suffix.isEmpty()) {
            return
        }
        morphSuffixPaint.setTypeface(normalPaint.getTypeface())
        morphSuffixPaint.setFontFeatureSettings(normalPaint.getFontFeatureSettings())
        morphSuffixPaint.setTextSize(textSize * SUFFIX_SCALE)
        morphSuffixPaint.setTextScaleX(TechnologyText.Companion.suffixWidthScale(base, suffix, compressFiveGa))
        applyStroke(morphSuffixPaint, strokeRatio)
        morphSuffixPaint.setTextAlign(Align.LEFT)
        morphSuffixPaint.setColor(tint)
        morphSuffixPaint.setAlpha(DisplayPolicy.paintAlpha(tint, 255, alpha))
        canvas.drawText(
            suffix,
            cursor,
            baseline - (textSize * SUFFIX_BASELINE_SHIFT),
            morphSuffixPaint
        )
        morphSuffixPaint.setTextScaleX(1.0f)
    }

    private fun configureForMeasurement(normalPaint: Paint, textSize: Float) {
        morphPaint.setTypeface(normalPaint.getTypeface())
        morphPaint.setFontFeatureSettings(normalPaint.getFontFeatureSettings())
        morphPaint.setTextSize(textSize)
        morphPaint.setTextScaleX(1.0f)
    }

    companion object {
        private const val SUFFIX_SCALE = 0.75f
        private const val SUFFIX_BASELINE_SHIFT = 0.03f

        @JvmStatic @JvmOverloads
        fun normalBase(value: String?, compressFiveGa: Boolean = false): String {
            val base = IconGeometry.technologyBase(value)
            return normalBase(base, suffix(value), compressFiveGa)
        }

        private fun normalBase(base: String, suffix: String?, compressFiveGa: Boolean): String {
            return if (compressFiveGa && TechnologyText.Companion.isFiveGa(base, suffix))
                base.substring(0, base.length - 1)
            else
                base
        }

        @JvmStatic @JvmOverloads
        fun condensedBase(value: String?, compressFiveGa: Boolean = false): String {
            val base = IconGeometry.technologyBase(value)
            return condensedBase(base, suffix(value), compressFiveGa)
        }

        private fun condensedBase(base: String, suffix: String?, compressFiveGa: Boolean): String {
            return if (compressFiveGa && TechnologyText.Companion.isFiveGa(base, suffix))
                base.substring(base.length - 1)
            else
                ""
        }

        @JvmStatic fun suffix(value: String?): String {
            val base = IconGeometry.technologyBase(value)
            return if (value == null || value.length <= base.length)
                ""
            else
                value.substring(base.length)
        }

        private fun suffixAfter(value: String, base: String): String {
            return if (value.length > base.length) value.substring(base.length) else ""
        }

        private fun applyStroke(paint: Paint, ratio: Float) {
            if (ratio <= 0.0f) {
                paint.setStyle(Paint.Style.FILL)
                paint.setStrokeWidth(0.0f)
                return
            }
            paint.setStyle(Paint.Style.FILL_AND_STROKE)
            paint.setStrokeJoin(Join.ROUND)
            paint.setStrokeWidth(paint.getTextSize() * ratio)
        }
    }
}
