package com.cyzah.circular.domain.settings

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.max
import kotlin.math.min

object DisplayPolicy {
    const val DEFAULT_LOW_BATTERY_THRESHOLD: Int = 20
    const val DEFAULT_SIZE_DP: Int = 24
    const val MAX_CENTRE_PERCENT: Int = 150
    const val MAX_SIZE_DP: Int = 28
    const val MIN_CENTRE_PERCENT: Int = 50
    const val MIN_SIZE_DP: Int = 12

    @JvmStatic fun mode(i: Int): Int {
        return if (i == 1 || i == 2) 2 else 0
    }

    @JvmStatic fun lowBatteryThreshold(i: Int): Int {
        return max(1, min(100, i))
    }

    @JvmStatic fun batteryTint(i: Int, z: Boolean, z2: Boolean, i2: Int, i3: Int): Int {
        return batteryTint(i, z, false, z2, i2, i3)
    }

    @JvmStatic @JvmOverloads
    fun batteryTint(
        i: Int,
        z: Boolean,
        z2: Boolean,
        z3: Boolean,
        i2: Int,
        i3: Int,
        i4: Int = StatusColors.CHARGING,
        i5: Int = StatusColors.LOW,
        i6: Int = StatusColors.SAVER
    ): Int {
        return batteryTint(i, z, z2, false, z3, i2, i3, i4, i5, i6, StatusColors.PERFORMANCE)
    }

    @JvmStatic fun batteryTint(
        battery: Int, charging: Boolean, saver: Boolean, performance: Boolean,
        coloured: Boolean, plainTint: Int, threshold: Int, chargeColor: Int,
        lowColor: Int, saverColor: Int, performanceColor: Int
    ): Int {
        if (!coloured) return plainTint
        if (charging) return StatusColors.opaque(chargeColor)
        if (saver) return StatusColors.opaque(saverColor)
        if (performance) return StatusColors.opaque(performanceColor)
        return if (battery >= 0 && battery < lowBatteryThreshold(threshold))
            StatusColors.opaque(lowColor)
        else
            plainTint
    }

    @JvmStatic fun paintAlpha(i: Int, i2: Int, f: Float): Int {
        return Math.round((((i ushr 24) * max(0, min(255, i2))) / 255.0f) * Motion.clamp(f))
    }

    @JvmStatic fun weight(i: Int): Int {
        return max(80, min(180, i))
    }

    @JvmStatic fun size(i: Int): Int {
        return max(12, min(29, i))
    }

    @JvmStatic fun centrePercent(i: Int): Int {
        return max(50, min(MAX_CENTRE_PERCENT, i))
    }

    @JvmStatic fun textWeight(i: Int): Int {
        return weight(i) * 5
    }

    @JvmStatic fun technologyTextWeight(i: Int): Int {
        return min(1000, weight(i) * 6)
    }

    @JvmStatic fun attach(i: Int, z: Boolean, z2: Boolean): Boolean {
        return i == 0 || (i == 1 && z2) || ((i == 5 || i == 6) && z)
    }

    /**
     * 
     * @param i  宿主角色（0=状态栏，1=亮屏锁屏，5/6=控制中心与面板副本）
     * @param i2 状态栏状态（mStatusBarState）
     * @param z  ambient（AOD 常亮显示中）
     * @param z2 用户锁屏开关
     * @param z3 用户 AOD 开关
     * @param z4 锁屏渲染器已就绪（KeyguardHandoff.prepare 的返回值）
     */
    @JvmStatic fun enabled(i: Int, i2: Int, z: Boolean, z2: Boolean, z3: Boolean, z4: Boolean): Boolean {
        if (i == 0) {
            return i2 == 0
        }
        if (i == 1) {
            if (!z4) {
                return false
            }
            if (z) {
                return z3
            }
            return z2
        }
        if (i == 5 || i == 6) {
            if (i2 == 0) {
                return true
            }
            if (i2 == 1 || i2 == 2) {
                return z2 && !z
            }
            return false
        }
        return false
    }

    @JvmStatic fun expansion(i: Int, z: Boolean, f: Float, f2: Float): Float {
        if (z || i == 1) {
            return 0.0f
        }
        // Notification-shade visibility belongs to the native status-icon row.
        // Only the control center changes the merged icon's internal geometry.
        return Motion.ease(f2)
    }

    @JvmStatic fun privacyTransition(str: String?): Boolean {
        return "START_SHOW_PRIVACY" == str || "COMPLETE_SHOW_PRIVACY" == str || "START_HOME_TO_DOT" == str || "START_HIDE_PRIVACY" == str
    }

    @JvmStatic fun keepPrivacyRenderer(i: Int, z: Boolean, str: String?, z2: Boolean, z3: Boolean): Boolean {
        if (i == 0 && z && privacyTransition(str)) {
            return z2 || z3
        }
        return false
    }
}
