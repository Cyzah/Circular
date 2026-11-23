package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.RectF
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.RoundedWifiGeometry
import com.cyzah.circular.domain.settings.DisplayPolicy
import kotlin.math.abs
import kotlin.math.max

class WifiGlyphRenderer {
    private val original: Array<Path?> = RoundedWifiGeometry.create()
    private val weighted = arrayOf<Path?>(Path(), Path(), Path())
    private val outline = Path()
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private var weightPercent = -1

    fun setWeightPercent(percent: Int) {
        val normalized = DisplayPolicy.weight(percent)
        if (this.weightPercent == normalized) {
            return
        }
        this.weightPercent = normalized
        val scale = normalized / 100.0f
        this.outlinePaint.setStyle(Paint.Style.STROKE)
        this.outlinePaint.setStrokeJoin(Join.ROUND)
        this.outlinePaint.setStrokeCap(Paint.Cap.ROUND)
        for (index in original.indices) {
            this.weighted[index]!!.reset()
            this.weighted[index]!!.set(this.original[index]!!)
            if (normalized != 100) {
                this.outlinePaint.setStrokeWidth(abs(scale - 1.0f) * 7.0f)
                this.outline.reset()
                this.outlinePaint.getFillPath(this.original[index], this.outline)
                if (!this.weighted[index]!!.op(
                        this.outline,
                        if (scale > 1.0f) Path.Op.UNION else Path.Op.DIFFERENCE
                    )
                ) {
                    this.weighted[index]!!.set(this.original[index]!!)
                }
            }
        }
        val expansion = scale * 3.5f
        this.bounds.set(
            39.0f - expansion,
            42.0f - expansion,
            expansion + 81.0f,
            (max(0.0f, scale - 1.0f) * 3.5f) + 75.85f
        )
    }

    fun bounds(): RectF {
        return this.bounds
    }

    fun drawCentred(
        canvas: Canvas?, tint: Int, level: Int,
        centreX: Float, centreY: Float, scale: Float, opacity: Float
    ) {
        if (!java.lang.Float.isFinite(scale) || scale <= 0.0f || opacity <= 0.0f) {
            return
        }
        val save = canvas!!.save()
        try {
            canvas.translate(
                centreX - (this.bounds.centerX() * scale),
                centreY - (this.bounds.centerY() * scale)
            )
            canvas.scale(scale, scale)
            drawAtOrigin(canvas, tint, level, Motion.clamp(opacity))
        } finally {
            canvas.restoreToCount(save)
        }
    }

    @JvmOverloads
    fun drawAtOrigin(
        canvas: Canvas,
        tint: Int,
        level: Int,
        opacity: Float,
        alphaScale: Float = 1.0f
    ) {
        for (arc in this.weighted.indices) {
            val alpha: Int = Math.round(arcAlpha(level, arc) * alphaScale)
            this.fillPaint.setColor(tint)
            this.fillPaint.setAlpha(DisplayPolicy.paintAlpha(tint, alpha, opacity))
            this.fillPaint.setStyle(Paint.Style.FILL)
            canvas.drawPath(this.weighted[arc]!!, this.fillPaint)
        }
    }

    companion object {
        private const val LIT_ALPHA = 255
        private const val TRACK_ALPHA = 102

        @JvmStatic fun arcAlpha(level: Int, arc: Int): Int {
            return if (level > arc) LIT_ALPHA else TRACK_ALPHA
        }
    }
}
