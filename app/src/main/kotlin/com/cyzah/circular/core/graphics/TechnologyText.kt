package com.cyzah.circular.core.graphics

import java.util.ArrayList

/**
 * 移动网络制式文本自定义规则。
 * 
 * 
 * 规则串形如 `5G=5G+;LTE=4G;*=5G`：
 * 
 *  * `原文本=新文本`：完全匹配（忽略大小写）时替换；
 *  * `*=新文本`：任何未被上面命中的文本都替换成它（等价于固定显示）；
 *  * `原文本=`：把该文本隐藏（替换成空串）；
 *  * 多条之间用半角或全角分号、逗号或换行分隔。
 * 
 */
class TechnologyText private constructor() {
    private val keys: MutableList<String?> = ArrayList<String?>()
    private val values: MutableList<String?> = ArrayList<String?>()

    val isEmpty: Boolean
        get() = this.keys.isEmpty()

    fun apply(str: String?): String? {
        if (str == null) {
            return ""
        }
        for (i in this.keys.indices) {
            if (this.keys.get(i).equals(str, ignoreCase = true)) {
                return this.values.get(i)
            }
        }
        for (i in this.keys.indices) {
            if (ANY == this.keys.get(i)) {
                return this.values.get(i)
            }
        }
        return str
    }

    fun spec(): String {
        if (this.keys.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        for (i in this.keys.indices) {
            if (i > 0) {
                sb.append(';')
            }
            sb.append(this.keys.get(i)).append('=').append(this.values.get(i))
        }
        return sb.toString()
    }

    companion object {
        const val MAX_LENGTH: Int = 400
        private const val ANY = "*"

        @JvmStatic fun parse(str: String?): TechnologyText {
            val technologyText = TechnologyText()
            if (str == null) {
                return technologyText
            }
            val strArrSplit: Array<String?> =
                str.split("[;,；，\n\r]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (str2 in strArrSplit) {
                if (str2 == null) {
                    continue
                }
                val strTrim = str2.trim { it <= ' ' }
                if (strTrim.isEmpty()) {
                    continue
                }
                val iIndexOf = strTrim.indexOf('=')
                if (iIndexOf < 0) {
                    // 只写了一个词：当成「任何制式都显示它」
                    technologyText.keys.add(ANY)
                    technologyText.values.add(strTrim)
                    continue
                }
                val strTrim2 = strTrim.substring(0, iIndexOf).trim { it <= ' ' }
                val strTrim3 = strTrim.substring(iIndexOf + 1).trim { it <= ' ' }
                if (strTrim2.isEmpty()) {
                    continue
                }
                technologyText.keys.add(strTrim2)
                technologyText.values.add(strTrim3)
            }
            return technologyText
        }

        @JvmStatic fun sanitize(str: String?): String {
            if (str == null) {
                return ""
            }
            val strTrim = str.trim { it <= ' ' }
            return if (strTrim.length > MAX_LENGTH) strTrim.substring(0, MAX_LENGTH) else strTrim
        }

        @JvmStatic @JvmOverloads
        fun suffixWidthScale(base: String?, suffix: String?, compressFiveGa: Boolean = false): Float {
            return if (compressFiveGa && isFiveGa(base, suffix)) 0.8f else 1.0f
        }

        @JvmStatic @JvmOverloads
        fun baseWidthScale(base: String?, suffix: String?, compressFiveGa: Boolean = false): Float {
            return if (compressFiveGa && isFiveGa(base, suffix)) 0.8f else 1.0f
        }

        @JvmStatic fun isFiveGa(base: String?, suffix: String?): Boolean {
            return "5G" == base && "A" == suffix
        }
    }
}
