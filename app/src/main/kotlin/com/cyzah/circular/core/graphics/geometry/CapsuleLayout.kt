package com.cyzah.circular.core.graphics.geometry

object CapsuleLayout {
    @JvmStatic fun originX(bodyWidth: Float, scale: Float, centred: Boolean, rightShift: Float): Float {
        return if (centred)
            60.0f - ((bodyWidth * scale) / 2.0f)
        else
            120.0f - (bodyWidth * scale) - rightShift
    }

    @JvmStatic fun contentCentre(originX: Float, localCentre: Float, scale: Float): Float {
        return originX + (localCentre * scale)
    }

    @JvmStatic fun dotRadius(baseRadius: Float, weight: Float, scale: Float): Float {
        return baseRadius * weight * scale
    }
}
