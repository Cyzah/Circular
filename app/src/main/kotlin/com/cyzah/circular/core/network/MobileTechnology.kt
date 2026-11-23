package com.cyzah.circular.core.network

import android.telephony.TelephonyDisplayInfo

class MobileTechnology {
    private var networkType = 0
    private var overrideType = 0

    fun clear() {
        this.overrideType = 0
        this.networkType = 0
    }

    fun networkChanged(i: Int) {
        if (this.networkType != i) {
            this.networkType = i
            this.overrideType = 0
        }
    }

    fun displayChanged(i: Int, i2: Int) {
        this.networkType = i
        this.overrideType = i2
    }

    fun networkType(): Int {
        return this.networkType
    }

    fun overrideType(): Int {
        return this.overrideType
    }

    fun label(): String {
        if (this.networkType == 13 || this.networkType == 19 || this.networkType == 20) {
            if (this.overrideType == 5) {
                // TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED：5G-Advanced，与系统的 5G+ 不是同一含义
                return "5GA"
            }
            if (this.overrideType == 3 || this.overrideType == 4) {
                return "5G"
            }
        }
        when (this.networkType) {
            1, 2, 4, 7, 11, 16 -> return "2G"
            3, 5, 6, 8, 9, 10, 12, 14, 15, 17 -> return "3G"
            13, 19 -> return "4G"
            18 -> return ""
            else -> return "5G"
        }
    }
}
