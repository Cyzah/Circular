package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.IconPainter
import kotlin.math.max
import kotlin.math.min

object CapsuleGeometry {
    private const val ICON_UNIT = 120.0f

    @JvmStatic fun contentWidth(
        gap: Float,
        leftWidth: Float,
        digitsWidth: Float
    ): Float {
        val safeGap = safeLength(gap)
        var result = 0.0f
        val safeLeft = safeLength(leftWidth)
        val safeDigits = safeLength(digitsWidth)
        if (safeLeft > 0.0f) {
            result += safeLeft
        }
        if (safeDigits > 0.0f) {
            result += (if (result > 0.0f) safeGap else 0.0f) + safeDigits
        }
        return result
    }

    @JvmStatic fun signalTopHalfOpening(dualSim: Boolean, measuredHalfOpening: Float): Float {
        return if (dualSim) safeLength(measuredHalfOpening) else 0.0f
    }

    @JvmStatic fun minimumBodyWidth(weight: Float): Float {
        val safeWeight = safeLength(weight)
        val height: Float = IconPainter.Companion.CAPSULE_H
        val dotRadius: Float = IconPainter.Companion.CAPSULE_DOT_R * safeWeight
        val capInset: Float = (IconPainter.Companion.CAPSULE_STROKE * safeWeight) / 2.0f
        return max(
            height * IconPainter.Companion.CAPSULE_MIN_ASPECT,
            (height
                    + (2.0f * IconPainter.Companion.CAPSULE_DOTS * dotRadius)
                    + ((IconPainter.Companion.CAPSULE_DOTS + 1)
                    * IconPainter.Companion.CAPSULE_DOT_GAP_BOT)
                    + (2.0f * capInset))
        )
    }

    @JvmStatic fun bodyWidth(
        contentWidth: Float,
        weight: Float
    ): Float {
        val height: Float = IconPainter.Companion.CAPSULE_H
        val targetWidth: Float = (safeLength(contentWidth)
                + (2.0f * height * IconPainter.Companion.CAPSULE_PAD))
        return min(
            max(minimumBodyWidth(weight), targetWidth),
            height * IconPainter.Companion.CAPSULE_MAX_ASPECT
        )
    }

    @JvmStatic fun scale(bodyWidth: Float, capsuleAspect: Float): Float {
        val safeWidth = safeLength(bodyWidth)
        val safeAspect = safeLength(capsuleAspect)
        if (safeWidth <= 0.0f || safeAspect <= 0.0f) {
            return 0.0f
        }
        return min(
            1.0f,
            min(
                (ICON_UNIT * safeAspect) / safeWidth,
                ICON_UNIT / IconPainter.Companion.CAPSULE_H
            )
        )
    }

    @JvmStatic fun reserveWidth(bodyWidth: Float, weight: Float): Float {
        return (safeLength(bodyWidth)
                + ((IconPainter.Companion.CAPSULE_STROKE * safeLength(weight)) / 2.0f)
                + IconPainter.Companion.CAPSULE_SHIFT_X
                + IconPainter.Companion.CAPSULE_SIDE_GAP)
    }

    @JvmStatic fun wifiScale(
        contentWidth: Float,
        glyphWidth: Float,
        bodyScale: Float
    ): Float {
        val safeGlyphWidth = safeLength(glyphWidth)
        if (safeGlyphWidth <= 0.0f) {
            return 0.0f
        }
        return ((safeLength(contentWidth) / safeGlyphWidth)
                * safeLength(bodyScale))
    }

    @JvmStatic fun safeLength(value: Float): Float {
        return if (java.lang.Float.isFinite(value) && value > 0.0f) value else 0.0f
    }
}
