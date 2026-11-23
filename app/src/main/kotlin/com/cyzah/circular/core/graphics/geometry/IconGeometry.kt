package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.max
import kotlin.math.min

object IconGeometry {
    const val MOTION_SPREAD: Float = 0.35f
    const val MOTION_FADE: Float = 0.30f
    private const val DEFAULT_SIGNAL_WEIGHT = 1.2f
    private val TECHNOLOGY_BASES = arrayOf<String?>("5G", "4G", "3G", "2G")

    @JvmStatic @JvmOverloads
    fun outerDotX(index: Int, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return SignalSpacing.outerX(index, weightScale)
    }

    @JvmStatic @JvmOverloads
    fun outerDotY(index: Int, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return SignalSpacing.outerY(index, weightScale)
    }

    @JvmStatic @JvmOverloads
    fun singleDotX(index: Int, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return SignalSpacing.ringX(index, weightScale)
    }

    @JvmStatic @JvmOverloads
    fun singleDotY(index: Int, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return SignalSpacing.ringY(index, weightScale)
    }

    @JvmStatic @JvmOverloads
    fun innerDotX(index: Int, motion: Float, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return 60.0f + ((SignalSpacing.innerX(index, weightScale) - 60.0f)
                * (1.0f - (MOTION_SPREAD * Motion.clamp(motion))))
    }

    @JvmStatic @JvmOverloads
    fun innerDotY(index: Int, motion: Float, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        val y = SignalSpacing.innerY(index, weightScale)
        return 58.0f + ((y - 58.0f) * (1.0f - (MOTION_SPREAD * Motion.clamp(motion))))
    }

    @JvmStatic fun innerDotAlpha(motion: Float): Float {
        return 1.0f - (MOTION_FADE * Motion.clamp(motion))
    }

    @JvmStatic @JvmOverloads
    fun groupedDotX(index: Int, motion: Float, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        val safe = max(0, min(3, index))
        return SignalSpacing.groupedX(safe, weightScale)
    }

    @JvmStatic fun groupedDotRank(index: Int): Int {
        val inGroup = index % 2
        return (if (index < 2) 1 - inGroup else inGroup) * 2
    }

    @JvmStatic @JvmOverloads
    fun groupedDotY(index: Int, weightScale: Float = DEFAULT_SIGNAL_WEIGHT): Float {
        return SignalSpacing.ringY(index, weightScale)
    }

    @JvmStatic fun groupedSecondaryAlpha(motion: Float): Float {
        return 1.0f - (MOTION_FADE * Motion.ease(Motion.clamp(motion)))
    }

    @JvmStatic fun dotRadius(weightPercent: Int): Float {
        return SignalSpacing.dotRadiusFromPercent(weightPercent)
    }

    @JvmStatic fun centreX(): Float {
        return 60.0f
    }

    @JvmStatic fun technologyBase(value: String?): String {
        if (value == null) {
            return ""
        }
        for (base in TECHNOLOGY_BASES) {
            if (value.startsWith(base!!)) {
                val suffixLength = value.length - base.length
                return (if (suffixLength > 0 && suffixLength <= 2) base else value)
            }
        }
        return value
    }
}
