package com.cyzah.circular.core.network

import com.cyzah.circular.core.graphics.animation.Motion

class SignalResourcePolicy(iArr: IntArray?, iArr2: IntArray?) {
    private val tables: Array<IntArray>

    init {
        this.tables = arrayOf(valid(iArr), valid(iArr2))
    }

    fun hasTable(): Boolean {
        return this.tables[0].size == 5 || this.tables[1].size == 5
    }

    fun level(i: Int, str: String?): Int {
        for (iArr in this.tables) {
            for (i2 in iArr.indices) {
                if (iArr[i2] == i) {
                    return i2
                }
            }
        }
        return Motion.nativeLevel(str)
    }

    companion object {
        private fun valid(iArr: IntArray?): IntArray {
            if (iArr == null || iArr.size != 5) {
                return IntArray(0)
            }
            for (i in 0..4) {
                if (iArr[i] <= 0) {
                    return IntArray(0)
                }
                for (i2 in 0..<i) {
                    if (iArr[i] == iArr[i2]) {
                        return IntArray(0)
                    }
                }
            }
            return iArr.copyOf()
        }
    }
}
