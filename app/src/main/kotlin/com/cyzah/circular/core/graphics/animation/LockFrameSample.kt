package com.cyzah.circular.core.graphics.animation


/** Reuses geometry within a traversal and formats only changed diagnostic values.  */
class LockFrameSample {
    private val inputs = IntArray(11)
    private val values = IntArray(12)
    private val recorded = IntArray(12)
    private var geometryCurrent = false
    private var hasRecord = false

    fun beginFrame() {
        geometryCurrent = false
    }

    fun prepare(
        control: Float, shown: Boolean, merge: Boolean, width: Int,
        batteryTranslation: Float, hostTranslation: Float,
        keyguardTranslation: Float, keyguardScale: Float, reflowBlocks: Int,
        layoutRequested: Boolean, rtl: Boolean
    ): Boolean {
        var changed = replace(0, java.lang.Float.floatToIntBits(control))
        changed = changed or replace(1, if (shown) 1 else 0)
        changed = changed or replace(2, if (merge) 1 else 0)
        changed = changed or replace(3, width)
        changed = changed or replace(4, java.lang.Float.floatToIntBits(batteryTranslation))
        changed = changed or replace(5, java.lang.Float.floatToIntBits(hostTranslation))
        changed = changed or replace(6, java.lang.Float.floatToIntBits(keyguardTranslation))
        changed = changed or replace(7, java.lang.Float.floatToIntBits(keyguardScale))
        changed = changed or replace(8, reflowBlocks)
        changed = changed or replace(9, if (layoutRequested) 1 else 0)
        changed = changed or replace(10, if (rtl) 1 else 0)
        geometryCurrent = geometryCurrent and !changed
        return !geometryCurrent
    }

    fun record(batteryEdge: Int, hostEdge: Int, force: Boolean): String? {
        values[0] = Math.round(java.lang.Float.intBitsToFloat(inputs[0]) * 1000.0f)
        values[1] = inputs[1]
        values[2] = inputs[2]
        values[3] = inputs[3]
        values[4] = batteryEdge
        values[5] = hostEdge
        values[6] = Math.round(java.lang.Float.intBitsToFloat(inputs[4]))
        values[7] = Math.round(java.lang.Float.intBitsToFloat(inputs[5]))
        values[8] = Math.round(java.lang.Float.intBitsToFloat(inputs[6]))
        values[9] = Math.round(java.lang.Float.intBitsToFloat(inputs[7]) * 1000.0f)
        values[10] = inputs[8]
        values[11] = inputs[9]
        geometryCurrent = true
        if (!force && hasRecord && values.contentEquals(recorded)) {
            return null
        }
        System.arraycopy(values, 0, recorded, 0, values.size)
        hasRecord = true
        return ("h=1 p=" + values[0]
                + " show=" + (values[1] != 0)
                + " m=" + (values[2] != 0)
                + " bw=" + values[3]
                + " be=" + values[4]
                + " he=" + values[5]
                + " bt=" + values[6]
                + " ht=" + values[7]
                + " kt=" + values[8]
                + " ks=" + values[9]
                + " blocked=" + values[10]
                + " lr=" + (values[11] != 0))
    }

    private fun replace(index: Int, value: Int): Boolean {
        val changed = inputs[index] != value
        inputs[index] = value
        return changed
    }
}
