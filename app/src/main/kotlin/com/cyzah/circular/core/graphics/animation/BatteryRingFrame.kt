package com.cyzah.circular.core.graphics.animation

/** Closed host frame shared by ring transfer drawing and endpoint projection.  */
object BatteryRingFrame {
    @JvmStatic fun closedOriginX(width: Float, minimum: Float, rtl: Boolean, drift: Float): Float {
        return (if (rtl) 0.0f else width - minimum) + drift
    }

    @JvmStatic fun closedOriginY(height: Float, minimum: Float, drift: Float): Float {
        return (height - minimum) / 2.0f + drift
    }

    @JvmStatic fun originX(
        transfer: Boolean, width: Float, minimum: Float,
        rtl: Boolean, drift: Float, movingOrigin: Float
    ): Float {
        return if (transfer) closedOriginX(width, minimum, rtl, drift) else movingOrigin
    }

    @JvmStatic fun originY(
        transfer: Boolean, height: Float, minimum: Float,
        drift: Float, movingOrigin: Float
    ): Float {
        return if (transfer) closedOriginY(height, minimum, drift) else movingOrigin
    }
}
