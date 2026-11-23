package com.cyzah.circular.core.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Join
import android.graphics.Path
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.DisplayPolicy

class DuoSignalRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crossPath = Path()

    fun draw(
        canvas: Canvas,
        state: IconState,
        tint: Int,
        weight: Float,
        opacity: Float,
        wifiProgress: Float,
        mask: Int
    ) {
        if ((mask and IconPainter.Companion.MASK_HIDE_SIGNALS) != 0) {
            return
        }
        val cardOneCross = state.card1Cross()
        val cardTwoCross = state.card2Cross()
        val cardOneLevel = state.card1Level()
        val cardTwoLevel = state.card2Level()
        val cardOneAvailable: Boolean = available(
            state.card1Present(),
            state.card1Known(),
            state.card1InService(),
            state.airplane
        )
        val cardTwoAvailable: Boolean = available(
            state.card2Present(),
            state.card2Known(),
            state.card2InService(),
            state.airplane
        )
        val dotRadius = weight * 4.9f
        for (row in 0..1) {
            val y = DuoLayout.dotY(row, wifiProgress, weight)
            val cross = if (row == 0) cardOneCross else cardTwoCross
            val rowAvailable: Boolean = rowAvailable(
                row,
                cardOneAvailable,
                cardTwoAvailable
            )
            val rowLevel: Int = rowLevel(
                row,
                cardOneLevel,
                cardTwoLevel
            )
            for (dot in 0..3) {
                val x = DuoLayout.dotX(dot, weight)
                if (!dotRemains(cross, x, y, 60.0f, y, dotRadius, weight)) {
                    continue
                }
                fill(
                    tint,
                    DuoLayout.dotAlpha(rowAvailable, rowLevel, dot),
                    opacity
                )
                canvas.drawCircle(x, y, dotRadius, this.paint)
            }
            if (cross) {
                drawCross(canvas, tint, 60.0f, y, weight, opacity)
            }
        }
    }

    private fun fill(color: Int, alpha: Int, opacity: Float) {
        this.paint.setColor(color)
        this.paint.setAlpha(DisplayPolicy.paintAlpha(color, alpha, opacity))
        this.paint.setStyle(Paint.Style.FILL)
    }

    fun drawCross(
        canvas: Canvas,
        tint: Int,
        centreX: Float,
        centreY: Float,
        weight: Float,
        opacity: Float
    ) {
        this.crossPath.reset()
        this.crossPath.moveTo(
            centreX - SignalCrossPolicy.ARM,
            centreY - SignalCrossPolicy.ARM
        )
        this.crossPath.lineTo(
            centreX + SignalCrossPolicy.ARM,
            centreY + SignalCrossPolicy.ARM
        )
        this.crossPath.moveTo(
            centreX + SignalCrossPolicy.ARM,
            centreY - SignalCrossPolicy.ARM
        )
        this.crossPath.lineTo(
            centreX - SignalCrossPolicy.ARM,
            centreY + SignalCrossPolicy.ARM
        )
        this.paint.setColor(tint)
        this.paint.setAlpha(
            DisplayPolicy.paintAlpha(
                tint,
                SignalCrossPolicy.ALPHA,
                opacity
            )
        )
        this.paint.setStyle(Paint.Style.STROKE)
        this.paint.setStrokeWidth(SignalCrossPolicy.STROKE_WIDTH * weight)
        this.paint.setStrokeCap(Paint.Cap.ROUND)
        this.paint.setStrokeJoin(Join.ROUND)
        canvas.drawPath(this.crossPath, this.paint)
    }

    companion object {
        @JvmStatic fun available(
            present: Boolean,
            known: Boolean,
            inService: Boolean,
            airplane: Boolean
        ): Boolean {
            return present && known && inService && !airplane
        }

        @JvmStatic fun dotRemains(
            cross: Boolean,
            dotX: Float,
            dotY: Float,
            centreX: Float,
            centreY: Float,
            dotRadius: Float,
            weight: Float
        ): Boolean {
            return !cross || !SignalCrossPolicy.coversDot(
                dotX,
                dotY,
                centreX,
                centreY,
                dotRadius,
                weight
            )
        }

        @JvmStatic fun rowAvailable(
            row: Int,
            firstCardAvailable: Boolean,
            secondCardAvailable: Boolean
        ): Boolean {
            return if (row <= 0) firstCardAvailable else secondCardAvailable
        }

        @JvmStatic fun rowLevel(
            row: Int,
            firstCardLevel: Int,
            secondCardLevel: Int
        ): Int {
            return if (row <= 0) firstCardLevel else secondCardLevel
        }
    }
}
