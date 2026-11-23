package com.cyzah.circular.core.network

import kotlin.math.min

class IconState {
    @JvmField var airplane: Boolean = false
    @JvmField var charging: Boolean = false
    @JvmField var inService: Boolean = false
    @JvmField var mobileKnown: Boolean = false
    @JvmField var nativeMobile: Boolean = false
    @JvmField var performanceMode: Boolean = false
    @JvmField var powerSave: Boolean = false
    @JvmField var simPresent: Boolean = false
    @JvmField var wifiConnected: Boolean = false
    @JvmField var wifiEnabled: Boolean = false
    @JvmField var wifiKnown: Boolean = false
    @JvmField var dataEnabled: Boolean = true

    /** 副卡（非默认数据卡）相关状态：用于在主卡四点的缝隙上方画三个点  */
    @JvmField var secondaryKnown: Boolean = false
    @JvmField var secondarySimPresent: Boolean = false
    @JvmField var secondaryInService: Boolean = false
    @JvmField var secondaryLevel: Int = -1
    @JvmField var secondaryRawLevel: Int = -1
    @JvmField var secondarySubId: Int = -1

    /** 默认数据卡所在的卡槽（phoneId），-1 = 未知  */
    @JvmField var mobileSlot: Int = -1

    /** 另一张卡所在的卡槽（phoneId），-1 = 未知  */
    @JvmField var secondarySlot: Int = -1
    @JvmField var battery: Int = -1
    @JvmField var wifiLevel: Int = -1
    @JvmField var mobileLevel: Int = -1
    @JvmField var subId: Int = -1
    @JvmField var rawMobileLevel: Int = -1
    @JvmField var mobileTechnology: String = ""

    fun copyFrom(source: IconState?): Boolean {
        if (source == null) {
            return false
        }
        if (source == this) {
            return true
        }
        this.airplane = source.airplane
        this.charging = source.charging
        this.inService = source.inService
        this.mobileKnown = source.mobileKnown
        this.nativeMobile = source.nativeMobile
        this.performanceMode = source.performanceMode
        this.powerSave = source.powerSave
        this.simPresent = source.simPresent
        this.wifiConnected = source.wifiConnected
        this.wifiEnabled = source.wifiEnabled
        this.wifiKnown = source.wifiKnown
        this.dataEnabled = source.dataEnabled
        this.secondaryKnown = source.secondaryKnown
        this.secondarySimPresent = source.secondarySimPresent
        this.secondaryInService = source.secondaryInService
        this.secondaryLevel = source.secondaryLevel
        this.secondaryRawLevel = source.secondaryRawLevel
        this.secondarySubId = source.secondarySubId
        this.mobileSlot = source.mobileSlot
        this.secondarySlot = source.secondarySlot
        this.battery = source.battery
        this.wifiLevel = source.wifiLevel
        this.mobileLevel = source.mobileLevel
        this.subId = source.subId
        this.rawMobileLevel = source.rawMobileLevel
        this.mobileTechnology = source.mobileTechnology
        return true
    }

    fun showWifi(): Boolean {
        return this.wifiKnown && this.wifiEnabled && this.wifiConnected
    }

    fun showMobileCross(): Boolean {
        if (!this.simPresent) {
            return false
        }
        if (this.airplane) {
            return true
        }
        return this.mobileKnown && !this.inService
    }

    /** 双卡：内侧三个点 = 卡一（卡槽 0），外侧四个点 = 卡二（卡槽 1）  */
    fun dualSim(): Boolean {
        return this.simPresent && this.secondarySimPresent
    }

    /** 默认数据卡是否在卡槽 0（即卡一就是数据卡）  */
    fun mobileIsSlot0(): Boolean {
        return this.mobileSlot == 0
    }

    fun card1Present(): Boolean {
        return if (mobileIsSlot0()) this.simPresent else (this.secondarySlot == 0 && this.secondarySimPresent)
    }

    fun card1Known(): Boolean {
        return if (mobileIsSlot0()) this.mobileKnown else this.secondaryKnown
    }

    fun card1InService(): Boolean {
        return if (mobileIsSlot0()) this.inService else this.secondaryInService
    }

    fun card1Level(): Int {
        return if (mobileIsSlot0()) this.mobileLevel else this.secondaryLevel
    }

    fun card2Present(): Boolean {
        return if (mobileIsSlot0()) this.secondarySimPresent else this.simPresent
    }

    fun card2Known(): Boolean {
        return if (mobileIsSlot0()) this.secondaryKnown else this.mobileKnown
    }

    fun card2InService(): Boolean {
        return if (mobileIsSlot0()) this.secondaryInService else this.inService
    }

    fun card2Level(): Int {
        return if (mobileIsSlot0()) this.secondaryLevel else this.mobileLevel
    }

    /** 某张卡只要有卡但无服务（或飞行模式）就在它自己那条弧中间画叉  */
    fun card1Cross(): Boolean {
        return crossFor(card1Present(), card1Known(), card1InService())
    }

    fun card2Cross(): Boolean {
        return crossFor(card2Present(), card2Known(), card2InService())
    }

    private fun crossFor(z: Boolean, z2: Boolean, z3: Boolean): Boolean {
        if (!z) {
            return false
        }
        return this.airplane || (z2 && !z3)
    }

    fun centreTechnology(): String {
        return if (this.dataEnabled && this.wifiKnown && !showWifi() && this.mobileKnown && this.simPresent && this.inService && !this.airplane) this.mobileTechnology else ""
    }

    fun readyForMerge(): Boolean {
        if (this.battery < 0 || this.battery > 100 || !this.wifiKnown || !this.mobileKnown) {
            return false
        }
        if (!this.wifiConnected || this.wifiLevel >= 0) {
            return if (this.simPresent && this.inService && this.mobileLevel < 0) false else true
        }
        return false
    }

    /** 双排信号第 1 排（卡一）点亮的格数 0..4  */
    fun card1Bars(): Int {
        return stackedBars(card1Present(), card1Known(), card1InService(), card1Level())
    }

    /** 双排信号第 2 排（卡二）点亮的格数 0..4  */
    fun card2Bars(): Int {
        return stackedBars(card2Present(), card2Known(), card2InService(), card2Level())
    }

    /**
     * 控制中心双排信号是否可用：非飞行模式，且（双卡时）两张卡都读到信号、
     * （单卡时）唯一那张卡读到信号。单卡也走同一套形变动画，只是只有一排。
     */
    fun stackedReady(): Boolean {
        if (this.airplane) {
            return false
        }
        if (dualSim()) {
            return card1Known() && card2Known() && card1InService() && card2InService()
        }
        return this.simPresent && this.mobileKnown && this.inService
    }

    /** 单卡时那一排点亮的格数 0..4（对应三合一外侧四点）  */
    fun singleBars(): Int {
        if (!this.simPresent || !this.mobileKnown || !this.inService || this.airplane || this.mobileLevel <= 0) {
            return 0
        }
        return min(4, this.mobileLevel)
    }

    fun description(): String {
        val str: String?
        val str2: String?
        val str3: String?
        var str4 = ""
        if (this.battery < 0) {
            str = "电量未知"
        } else {
            str = "电量 " + this.battery + "%" + (if (this.charging) "，充电中" else "")
        }
        if (!this.wifiKnown) {
            str2 = "Wi-Fi 状态未知"
        } else if (!this.wifiEnabled) {
            str2 = "Wi-Fi 已关闭"
        } else if (this.wifiConnected) {
            str2 = "Wi-Fi 强度 " + (if (this.wifiLevel < 0) "未知" else (this.wifiLevel + 1).toString() + "/4")
        } else {
            str2 = "Wi-Fi 未连接"
        }
        if (this.airplane) {
            str3 = "飞行模式"
        } else if (!this.mobileKnown) {
            str3 = "蜂窝状态未知"
        } else if (!this.simPresent) {
            str3 = "无 SIM 卡"
        } else if (this.inService) {
            str3 = "蜂窝信号 " + (if (this.mobileLevel >= 0) this.mobileLevel.toString() + "/4" else "未知")
        } else {
            str3 = "无蜂窝服务"
        }
        val str5 = if (this.powerSave) "，省电模式" else ""
        val str6 = if (this.performanceMode) "，性能模式" else ""
        if (this.mobileKnown && this.simPresent && this.inService && !this.airplane && !this.mobileTechnology.isEmpty()) {
            str4 = "（" + this.mobileTechnology + "）"
        }
        val strBase = str + str5 + str6 + "；" + str2
        if (!this.secondarySimPresent) {
            return strBase + "\n" + str3 + str4
        }
        val strCardOne: String?
        if (this.airplane) {
            strCardOne = "卡一信号 无服务"
        } else if (!this.secondaryKnown) {
            strCardOne = "卡一信号 未知"
        } else if (!this.secondaryInService || this.secondaryLevel < 0) {
            strCardOne = "卡一信号 无服务"
        } else {
            strCardOne = "卡一信号 " + this.secondaryLevel + "/4"
        }
        val strCardTwo: String?
        if (this.airplane) {
            strCardTwo = "卡二信号 无服务"
        } else if (!this.mobileKnown) {
            strCardTwo = "卡二信号 未知"
        } else if (!this.simPresent) {
            strCardTwo = "卡二无 SIM 卡"
        } else if (!this.inService || this.mobileLevel < 0) {
            strCardTwo = "卡二信号 无服务"
        } else {
            strCardTwo = "卡二信号 " + this.mobileLevel + "/4" + str4
        }
        return strBase + "\n" + strCardOne + "；" + strCardTwo
    }

    companion object {
        private fun stackedBars(z: Boolean, z2: Boolean, z3: Boolean, i: Int): Int {
            if (!z || !z2 || !z3 || i <= 0) {
                return 0
            }
            return min(4, i)
        }
    }
}
