package com.cyzah.circular.core.graphics

import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min

object BatteryHeader {
    const val DEFAULT_WEIGHT_PERCENT: Int = 100

    @JvmStatic fun scale(i: Int): Int {
        return max(70, min(160, i))
    }

    @JvmStatic fun textSize(i: Int): Float {
        return (scale(i) * 33.8f) / 100.0f
    }

    @JvmStatic fun weight(i: Int): Int {
        return max(50, min(180, i))
    }

    @JvmStatic fun fontWeight(i: Int): Int {
        return min(1000, Math.round(weight(i) * 6.5f))
    }

    @JvmStatic fun weightFromLegacy(i: Int): Int {
        return weight(Math.round(weight(i) / 6.5f) * 5)
    }

    @JvmStatic fun weightToLegacy(i: Int): Int {
        return weight(Math.round(weight(i) * 1.3f))
    }

    @JvmStatic fun fromLegacy(i: Int): Int {
        return scale(Math.round(scale(i) / 6.5f) * 5)
    }

    @JvmStatic fun toLegacy(i: Int): Int {
        return scale(Math.round(scale(i) * 1.3f))
    }

    @JvmStatic fun centreY(): Float {
        return 10.0f
    }

    @JvmStatic fun centreY(z: Boolean): Float {
        if (z) {
            return 57.0f
        }
        return centreY()
    }

    /**
     * 数字那一行的可用宽度（120 坐标系）。换位置布局（电量在环中央）原来是 70，
     * 三位数放大到 150% 以上会被判超宽而缩回，所以放到 92（约等于圆环内径）。
     */
    @JvmStatic fun readoutWidth(z: Boolean): Float {
        return (if (z) 92 else 82).toFloat()
    }

    @JvmStatic fun fit(f: Float, f2: Float, f3: Float, f4: Float): Float {
        if (!java.lang.Float.isFinite(f) || !java.lang.Float.isFinite(f2) || f < 0.0f || f2 < 0.0f) {
            return 0.0f
        }
        return max(0.0f, min(1.0f, min(if (f > 0.0f) f3 / f else 1.0f, if (f2 > 0.0f) f4 / f2 else 1.0f)))
    }

    @JvmStatic fun networkHeaderScale(): Float {
        return 0.65f
    }

    @JvmStatic fun networkHeaderY(f: Float): Float {
        return (f / 2.0f) + 2.0f
    }

    @JvmStatic fun halfGap(f: Float, f2: Float): Float {
        if (f <= 0.0f) {
            return 0.0f
        }
        return Math.toDegrees(asin(min(0.98f, (((f / 2.0f) + 6.0f) + (f2 * 2.0f)) / 50.0f).toDouble())).toFloat()
    }
}
