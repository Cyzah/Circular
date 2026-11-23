package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import android.view.View

/**
 * Runs native SystemUI drawing only for the duration of an optional alpha
 * layer, while routing through the module's existing bypass state.
 */
object NativeSessionDraw {
    @JvmStatic @Throws(Throwable::class)
    fun battery(
        owner: IconSession,
        canvas: Canvas,
        nativeDraw: NativeDraw?,
        alpha: Float,
        translationX: Float,
        width: Float,
        height: Float
    ) {
        val save = beginLayer(canvas, alpha, width, height)
        val previousBypass = owner.module.bypass
        var drawFailure: Throwable? = null
        try {
            canvas.translate(translationX, 0.0f)
            owner.module.bypass = true
            nativeDraw!!.run()
        } catch (error: Throwable) {
            drawFailure = error
            throw error
        } finally {
            owner.module.bypass = previousBypass
            NativeSessionDraw.restoreCanvas(canvas, save, drawFailure)
        }
    }

    @JvmStatic fun view(
        owner: IconSession,
        canvas: Canvas,
        view: View,
        alpha: Float,
        translationX: Float,
        translationY: Float,
        width: Float,
        height: Float
    ) {
        val save = beginLayer(canvas, alpha, width, height)
        val previousBypass = owner.module.bypass
        var drawFailure: Throwable? = null
        try {
            canvas.translate(translationX, translationY)
            owner.module.bypass = true
            view.draw(canvas)
        } catch (error: Throwable) {
            drawFailure = error
            throw error
        } finally {
            owner.module.bypass = previousBypass
            NativeSessionDraw.restoreCanvas(canvas, save, drawFailure)
        }
    }

    @JvmStatic fun restoreCanvas(canvas: Canvas, save: Int, drawFailure: Throwable?) {
        try {
            canvas.restoreToCount(save)
        } catch (cleanup: Throwable) {
            if (drawFailure == null) {
                throw cleanup
            }
            if (drawFailure !== cleanup) {
                drawFailure.addSuppressed(cleanup)
            }
        }
    }

    private fun beginLayer(
        canvas: Canvas,
        alpha: Float,
        width: Float,
        height: Float
    ): Int {
        return if (SessionPaintPolicy.needsAlphaLayer(alpha))
            canvas.saveLayerAlpha(
                0.0f,
                0.0f,
                width,
                height,
                Math.round(alpha * 255.0f)
            )
        else
            canvas.save()
    }
}
