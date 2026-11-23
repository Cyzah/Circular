package com.cyzah.circular.domain.settings

import java.util.ArrayList
import java.util.Arrays

object NetworkTechnologyOptions {
    private val ALL = arrayOf<String>(
        "5GA", "5G+", "5G", "4G+", "4G"
    )

    @JvmStatic fun all(): Array<String> {
        return ALL.clone()
    }

    @JvmStatic fun supported(
        twoG: Boolean, threeG: Boolean,
        fourG: Boolean, fiveG: Boolean
    ): Array<String> {
        val result: MutableList<String> = ArrayList<String>()
        if (fiveG) {
            result.add("5GA")
            result.add("5G+")
            result.add("5G")
        }
        if (fourG) {
            result.add("4G+")
            result.add("4G")
        }
        return if (result.isEmpty()) all() else result.toTypedArray<String>()
    }

    @JvmStatic fun withSelected(options: Array<String>?, selected: String?): Array<String> {
        val safe = if (selected == null) "" else selected.trim { it <= ' ' }
        if (safe.isEmpty() || options == null || Arrays.asList<String?>(*options).contains(safe)
            || "2G" == safe
            || "3G" == safe
        ) {
            return if (options == null) all() else options.clone()
        }
        return Array(options.size + 1) { index -> if (index == 0) safe else options[index - 1] }
    }
}
