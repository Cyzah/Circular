package com.cyzah.circular.domain.settings

import java.util.Locale

object StatusColors {
    const val CHARGING: Int = -0xff6000
    const val LOW: Int = -0xd9da
    const val PERFORMANCE: Int = -0xd07f13
    const val SAVER: Int = -0x5b00

    @JvmStatic fun opaque(i: Int): Int {
        return i or (-16777216)
    }

    @JvmStatic fun batteryNumberTint(tint: Int): Int {
        return if ((tint and 0x00FFFFFF) == 0) -0x34000000 else tint
    }

    @JvmStatic fun batteryInnerNumberTint(tint: Int): Int {
        return if ((tint and 0x00FFFFFF) == 0) -0x57e5e5e6 else tint
    }

    @JvmStatic fun controlCenterInnerNumberTint(): Int {
        return -0xe5e5e6
    }

    @JvmStatic fun hex(i: Int): String {
        return String.format(Locale.ROOT, "#%06X", i and 16777215)
    }

    @JvmStatic fun parse(str: String?): Int? {
        if (str == null) {
            return null
        }
        var strTrim = str.trim { it <= ' ' }
        if (strTrim.startsWith("#")) {
            strTrim = strTrim.substring(1)
        }
        if (strTrim.length != 3 && strTrim.length != 6) {
            return null
        }
        for (i in 0..<strTrim.length) {
            val cCharAt = strTrim.get(i)
            if ((cCharAt < '0' || cCharAt > '9') && ((cCharAt < 'a' || cCharAt > 'f') && (cCharAt < 'A' || cCharAt > 'F'))) {
                return null
            }
        }
        if (strTrim.length == 3) {
            val cCharAt2 = strTrim.get(0)
            val cCharAt3 = strTrim.get(0)
            val cCharAt4 = strTrim.get(1)
            val cCharAt5 = strTrim.get(1)
            strTrim = StringBuilder().append(cCharAt2).append(cCharAt3).append(cCharAt4).append(cCharAt5)
                .append(strTrim.get(2)).append(strTrim.get(2)).toString()
        }
        return opaque(strTrim.toInt(16))
    }
}
