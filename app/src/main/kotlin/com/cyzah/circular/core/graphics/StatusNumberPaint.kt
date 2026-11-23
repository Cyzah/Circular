package com.cyzah.circular.core.graphics

import android.graphics.Paint
import android.graphics.Paint.Join
import android.graphics.Rect
import kotlin.math.ceil

/** Keeps added weight proportional through fitted sizes without squeezing digits.  */
class StatusNumberPaint : Paint(ANTI_ALIAS_FLAG or SUBPIXEL_TEXT_FLAG) {
    private var strokeRatio = 0f

    fun setWeightPercent(percent: Int) {
        this.strokeRatio = StatusFontWeight.batteryStrokeRatio(percent)
        updateStroke()
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        updateStroke()
    }

    fun measureNumber(text: String?): Float {
        return if (text == null || text.isEmpty()) 0.0f else measureText(text) + getStrokeWidth()
    }

    fun numberBounds(text: String, bounds: Rect) {
        getTextBounds(text, 0, text.length, bounds)
        if (!bounds.isEmpty()) {
            val pad = ceil((getStrokeWidth() / 2.0f).toDouble()).toInt()
            bounds.inset(-pad, -pad)
        }
    }

    fun leftPadding(): Float {
        return getStrokeWidth() / 2.0f
    }

    private fun updateStroke() {
        setStyle(if (strokeRatio > 0.0f) Style.FILL_AND_STROKE else Style.FILL)
        setStrokeJoin(Join.ROUND)
        setStrokeWidth(getTextSize() * strokeRatio)
    }
}
