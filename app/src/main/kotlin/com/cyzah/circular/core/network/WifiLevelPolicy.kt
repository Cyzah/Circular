package com.cyzah.circular.core.network

import kotlin.math.min

class WifiLevelPolicy(calculator: Calculator?, calculator2: Calculator?, calculator3: Calculator?) {
    private val calculators: Array<Calculator?>
    private var source = "none"

    interface Calculator {
        @Throws(Exception::class)
        fun calculate(z: Boolean, i: Int): Int
    }

    init {
        this.calculators = arrayOf<Calculator?>(calculator, calculator2, calculator3)
    }

    fun calculate(i: Int, i2: Int): Int {
        this.source = "unavailable"
        if (!WifiRecovery.Companion.validRssi(i2)) {
            return -1
        }
        val z = i >= 2401 && i < 2500
        for (i3 in this.calculators.indices) {
            if (this.calculators[i3] != null) {
                try {
                    val iCalculate = this.calculators[i3]!!.calculate(z, i2)
                    if (iCalculate >= 0) {
                        this.source = SOURCES[i3]
                        return min(3, iCalculate)
                    }
                    continue
                } catch (e: Exception) {
                } catch (e: LinkageError) {
                }
            }
        }
        return -1
    }

    fun clear() {
        this.source = "none"
    }

    fun source(): String {
        return this.source
    }

    fun hasTracker(): Boolean {
        return this.calculators[0] != null
    }

    fun hasMiui(): Boolean {
        return this.calculators[1] != null
    }

    companion object {
        private val SOURCES = arrayOf<String>("systemui-tracker", "miui-rssi", "platform-rssi")
    }
}
