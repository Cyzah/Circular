package com.cyzah.circular.framework.export

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import kotlin.math.min

object ExportPolicy {
    const val MAIN: String = "com.android.systemui"
    const val MIUI_FALLBACK: String = "xiaomi.unregistered.systemui.plugin"
    const val MIUI_PLUGIN: String = "miui.systemui.plugin"

    @JvmStatic fun xiaomi(str: String): Boolean {
        return str.lowercase().matches(".*(xiaomi|redmi|poco).*".toRegex())
    }

    @JvmStatic fun reason(str: String, str2: String, str3: String): String? {
        val str4: String?
        if (MAIN == str2) {
            return "主 SystemUI"
        }
        val file = File(str3)
        val parentFile = file.getParentFile()
        if ((str2 + " " + file.getName() + " " + (if (parentFile == null) "" else parentFile.getName())).lowercase()
                .matches(".*(overlay|navbar|navigation|gesture|accessibility|\\brro\\b).*".toRegex())
        ) {
            return null
        }
        if (xiaomi(str)) {
            if (MIUI_PLUGIN == str2 || file.getName().equals("MIUISystemUIPlugin.apk", ignoreCase = true)) {
                return "小米核心 SystemUI 插件"
            }
            return null
        }
        val lowerCase = str.lowercase()
        if (lowerCase.matches(".*(oppo|oplus|oneplus|realme).*".toRegex())) {
            str4 = "oplus|oppo|oneplus|coloros|realme"
        } else {
            str4 = if (lowerCase.matches(".*(vivo|iqoo).*".toRegex())) "vivo|iqoo" else null
        }
        if (str4 == null) {
            return null
        }
        if (str2.lowercase()
                .matches(("(?:com\\.)?(?:" + str4 + ")\\.(?:systemui(?:\\.plugin|\\.controlcenter(?:\\.plugin)?)?|controlcenter(?:\\.plugin)?)").toRegex())
        ) {
            return "厂商核心 SystemUI/控制中心包"
        }
        if (file.getName().lowercase().replace("_", "").replace("-", "")
                .matches(("(?:" + str4 + ")(?:systemuiplugin|controlcenter(?:plugin)?)\\.apk").toRegex())
        ) {
            return "厂商核心插件 APK"
        }
        return null
    }

    @JvmStatic fun quote(str: String): String {
        return "'" + str.replace("'", "'\"'\"'") + "'"
    }

    @JvmStatic fun packageName(str: String): Boolean {
        return str.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+".toRegex())
    }

    @JvmStatic fun apkPath(str: String): Boolean {
        if (!str.startsWith("/") || !str.lowercase()
                .endsWith(".apk") || str.indexOf(0.toChar()) >= 0 || str.indexOf(10.toChar()) >= 0 || str.indexOf(13.toChar()) >= 0
        ) {
            return false
        }
        for (str2 in str.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (str2 == ".." || str2 == ".") {
                return false
            }
        }
        return true
    }

    @JvmStatic fun packages(str: String): LinkedHashMap<String, String> {
        val linkedHashMap = LinkedHashMap<String, String>()
        for (str2 in str.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val separator = str2.lastIndexOf('=')
            if (str2.startsWith("package:") && separator >= 8) {
                val strSubstring = str2.substring(8, separator)
                val strTrim = str2.substring(separator + 1).trim { it <= ' ' }
                if (packageName(strTrim) && apkPath(strSubstring)) {
                    linkedHashMap.put(strTrim, strSubstring)
                }
            }
        }
        return linkedHashMap
    }

    @JvmStatic fun paths(str: String): MutableList<String> {
        val linkedHashSet = LinkedHashSet<String>()
        for (str2 in str.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (str2.startsWith("package:")) {
                val strSubstring = str2.substring(8)
                if (apkPath(strSubstring)) {
                    linkedHashSet.add(strSubstring)
                }
            }
        }
        return ArrayList(linkedHashSet)
    }

    @JvmStatic fun safeName(str: String): String {
        var strReplaceAll = str.replace("[^A-Za-z0-9._-]".toRegex(), "_").replace("_+".toRegex(), "_")
        if (strReplaceAll.isEmpty() || strReplaceAll == "." || strReplaceAll == "..") {
            strReplaceAll = "unknown"
        }
        return strReplaceAll.substring(0, min(strReplaceAll.length, 90))
    }

    @JvmStatic fun archiveName(manufacturer: String, model: String, dateStamp: String): String {
        return archiveBaseName(manufacturer, model, dateStamp) + ".zip"
    }

    @JvmStatic fun archiveBaseName(manufacturer: String, model: String, dateStamp: String): String {
        return ("SystemUI_" + archivePart(manufacturer) + "_" + archivePart(model)
                + "_" + archivePart(dateStamp))
    }

    private fun archivePart(value: String): String {
        val safe = safeName(value).replace("^_+|_+$".toRegex(), "")
        return if (safe.isEmpty()) "unknown" else safe
    }

    @JvmStatic fun apkName(str: String, str2: String, i: Int, str3: String): String {
        val name = File(str3).getName()
        if (MAIN == str2 && (name.equals("base.apk", ignoreCase = true) || name.equals(
                "SystemUI.apk",
                ignoreCase = true
            ) || name.equals("MiuiSystemUI.apk", ignoreCase = true))
        ) {
            return if (xiaomi(str)) "MiuiSystemUI.apk" else "SystemUI.apk"
        }
        if (xiaomi(str) && ((MIUI_PLUGIN == str2 || MIUI_FALLBACK == str2 || name.equals(
                "MIUISystemUIPlugin.apk",
                ignoreCase = true
            )) && (name.equals("base.apk", ignoreCase = true) || name.equals(
                "MIUISystemUIPlugin.apk",
                ignoreCase = true
            )))
        ) {
            return "MIUISystemUIPlugin.apk"
        }
        return safeName(str2) + "__" + i + "__" + safeName(name.substring(0, name.length - 4)) + ".apk"
    }

    @JvmStatic @Throws(IOException::class)
    fun sharedFile(file: File, str: String): File {
        if (!str.matches("SystemUI_[A-Za-z0-9._-]+\\.zip".toRegex())) {
            throw IOException("无效的分享文件名。")
        }
        val canonicalFile = file.getCanonicalFile()
        val canonicalFile2 = File(canonicalFile, str).getCanonicalFile()
        if (canonicalFile != canonicalFile2.getParentFile() || !canonicalFile2.isFile()) {
            throw IOException("导出文件不存在，请重新导出。")
        }
        return canonicalFile2
    }
}
