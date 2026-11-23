package com.cyzah.circular.core.diagnostics

object DiagnosticCopy {
    @JvmStatic fun isFresh(str: String?, str2: String?): Boolean {
        return if (str == null || str.isEmpty() || (str != str2)) false else true
    }

    @JvmStatic fun format(z: Boolean, str: String?, str2: String?, str3: String?, str4: String?): String {
        return ("Circular " + str + " · " + str2) + "\n" + (if (z) "本次刷新回报" else "本次刷新未收到 SystemUI 回报；以下为上次记录或排查信息") + "\n" + str3 + "\n" + str4
    }
}
