package com.cyzah.circular.framework.export

import android.annotation.SuppressLint
import com.cyzah.circular.framework.device.RootShell
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.min

@SuppressLint("UsableSpace")
class SystemUiExporter(private val shell: RootShell, private val progress: Progress) {
    private var total: Long = 0
    private val warnings = ArrayList<String>()
    private val selected = LinkedHashMap<String, MutableList<String>>()
    private val reasons = LinkedHashMap<String, String>()
    private val seen = HashSet<String>()
    private val exported = ArrayList<File>()
    private val packageLog =
        StringBuilder("SystemUI adaptation export (core packages only)\nXiaomi: com.android.systemui + miui.systemui.plugin. Other OEMs: exact core SystemUI/control-center candidates only.\nOverlays, navigation/gesture/accessibility helpers and generic notification/status-bar packages are excluded. Necessary splits of selected packages are retained.\n\n")

    interface Progress {
        fun update(str: String)
    }

    class Result(@JvmField val file: File, @JvmField val count: Int, @JvmField val packageCount: Int, list: MutableList<String>) {
        @JvmField val warnings: MutableList<String>

        init {
            this.warnings = List.copyOf<String>(list)
        }
    }
    @Throws(Throwable::class)
    fun export(file: File, str: String, str2: String, str3: String): Result {
        if (!file.isDirectory() && !file.mkdirs()) {
            throw IOException("无法创建导出目录，请检查存储空间。")
        }
        val listFiles = file.listFiles(object : FileFilter {
            // java.io.FileFilter
            override fun accept(file2: File): Boolean {
                return `lambda$0`(file2)
            }
        })
        if (listFiles != null) {
            for (file2 in listFiles) {
                cleanup(file2)
            }
        }
        val workDir = Files.createTempDirectory(file.toPath(), "work-").toFile()
        val partial = File(workDir, "export.partial")
        try {
            this.progress.update("正在申请 Root 权限…")
            if ("0" != this.shell.text("id -u").trim { it <= ' ' }) {
                throw IOException("未获得 Root 权限，请在 Root 管理器中允许本应用后重试。")
            }
            this.progress.update("正在定位 SystemUI…")
            val paths = ExportPolicy.paths(this.shell.text("pm path " + ExportPolicy.MAIN))
            if (paths.isEmpty()) {
                throw IOException("pm path 未返回有效的 SystemUI APK，未生成适配包。")
            }
            this.selected.put(ExportPolicy.MAIN, paths)
            this.reasons.put(ExportPolicy.MAIN, "主 SystemUI（包含全部 split APK）")
            discover(str)
            val extraPackages = this.selected.size - 1
            var packageCount = 0
            for (entry in this.selected.entries) {
                val isMain = ExportPolicy.MAIN == entry.key
                val copies = ArrayList<File>()
                val sources = ArrayList<String>()
                this.packageLog.append("package=").append(entry.key).append("\nreason=")
                    .append(this.reasons.get(entry.key)).append('\n')
                try {
                    if (this.exported.size + entry.value.size > MAX_APKS) {
                        throw IOException("相关 APK 数量超过 " + MAX_APKS + " 个限制。")
                    }
                    var index = 0
                    var addedBytes: Long = 0
                    for (source in entry.value) {
                        this.packageLog.append("source=").append(source).append('\n')
                        if (this.seen.contains(source)) {
                            this.packageLog.append("status=duplicate source already exported\n")
                            continue
                        }
                        if (!ExportPolicy.apkPath(source)) {
                            throw IOException("无效的 APK 来源路径。")
                        }
                        index++
                        this.progress.update("正在复制 " + entry.key + "（" + index + "/" + entry.value.size + "）…")
                        val target = File(
                            workDir,
                            ExportPolicy.apkName(str, entry.key, this.exported.size + copies.size + 1, source)
                        )
                        if (target.exists()) {
                            throw IOException("APK 文件名冲突，未覆盖其他文件。")
                        }
                        copies.add(target)
                        sources.add(source)
                        val remaining: Long = MAX_TOTAL - this.total - addedBytes
                        if (remaining <= 0) {
                            throw IOException("APK 总大小超过 2 GiB 限制。")
                        }
                        BufferedOutputStream(FileOutputStream(target)).use { bufferedOutputStream ->
                            this.shell.copy(
                                "/system/bin/cat " + ExportPolicy.quote(source), bufferedOutputStream, min(
                                    MAX_APK, remaining
                                ), 180
                            )
                        }
                        ZipFile(target).use { zipFile ->
                            if (zipFile.getEntry("AndroidManifest.xml") == null) {
                                throw IOException("读取到的文件不是有效 APK。")
                            }
                        }
                        addedBytes += target.length()
                    }
                    this.exported.addAll(copies)
                    this.seen.addAll(sources)
                    this.total += addedBytes
                    if (!copies.isEmpty()) {
                        packageCount++
                    }
                    this.packageLog.append("status=exported\n")
                    for (file3 in copies) {
                        this.packageLog.append("archive=apk/").append(file3.getName()).append(" size=")
                            .append(file3.length()).append('\n')
                    }
                } catch (e: IOException) {
                    for (file4 in copies) {
                        Files.deleteIfExists(file4.toPath())
                    }
                    if (isMain) {
                        throw IOException("主 SystemUI 导出失败：" + message(e), e)
                    }
                    warn(entry.key + " 未完整导出：" + message(e))
                    this.packageLog.append("status=failed; no APK from this package included\n")
                }
                this.packageLog.append('\n')
            }
            if (extraPackages == 0) {
                warn(if (ExportPolicy.xiaomi(str)) "未找到 MIUISystemUIPlugin.apk，本次仅导出主 SystemUI；请将 packages.txt 发给开发者检查插件路径。" else "未找到可确认的独立核心插件，本次仅导出主 SystemUI；请结合清单确认该系统是否需要独立插件。")
            }
            for (str4 in this.warnings) {
                this.packageLog.append("warning=").append(str4.replace('\n', ' ')).append('\n')
            }
            this.progress.update("正在计算 SHA256 并生成 ZIP…")
            if (file.getUsableSpace() < this.total + 8388608L) {
                throw IOException("存储空间不足，打包至少还需要约 " + ((this.total / 1024) / 1024 + 8) + " MiB。")
            }
            val sb = StringBuilder()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(partial))).use { zipOutputStream ->
                zipOutputStream.setLevel(1)
                zipOutputStream.putNextEntry(ZipEntry("apk/"))
                zipOutputStream.closeEntry()
                val bArr = ByteArray(65536)
                for (file5 in this.exported) {
                    val messageDigestSha256: MessageDigest = sha256()
                    zipOutputStream.putNextEntry(ZipEntry("apk/" + file5.getName()))
                    BufferedInputStream(FileInputStream(file5)).use { bufferedInputStream ->
                        while (true) {
                            val read = bufferedInputStream.read(bArr)
                            if (read == -1) {
                                break
                            }
                            zipOutputStream.write(bArr, 0, read)
                            messageDigestSha256.update(bArr, 0, read)
                        }
                    }
                    zipOutputStream.closeEntry()
                    sb.append(hex(messageDigestSha256.digest())).append("  apk/").append(file5.getName()).append('\n')
                }
                entry(zipOutputStream, "device_info.txt", str3)
                entry(zipOutputStream, "packages.txt", this.packageLog.toString())
                entry(zipOutputStream, "checksums.txt", sb.toString())
            }
            val archiveBase = ExportPolicy.archiveBaseName(
                str,
                str2,
                SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
            )
            var file6 = File(file, archiveBase + ".zip")
            var i = 2
            while (file6.exists()) {
                file6 = File(file, archiveBase + "_" + i + ".zip")
                i++
            }
            Files.move(partial.toPath(), file6.toPath())
            val result = Result(file6, this.exported.size, packageCount, this.warnings)
            cleanup(workDir)
            return result
        } catch (th: Throwable) {
            cleanup(workDir)
            throw th
        }
    }

    private fun discover(str: String) {
        this.progress.update("正在查找核心 SystemUI 插件…")
        val zXiaomi = ExportPolicy.xiaomi(str)
        if (zXiaomi) {
            try {
                val listPaths =
                    ExportPolicy.paths(this.shell.text("pm path " + ExportPolicy.quote("miui.systemui.plugin")))
                if (!listPaths.isEmpty()) {
                    this.selected.put("miui.systemui.plugin", listPaths)
                    this.reasons.put("miui.systemui.plugin", "直接定位小米核心插件（包含必要 split APK）")
                    return
                }
                this.packageLog.append("discovery=miui.systemui.plugin returned no paths; trying exact filename fallback\n")
            } catch (e: IOException) {
                this.packageLog.append("discovery=miui.systemui.plugin lookup failed: ").append(message(e)).append('\n')
            }
        }
        val linkedHashMap = LinkedHashMap<String, String>()
        try {
            for (entry in ExportPolicy.packages(this.shell.text("pm list packages -s -f")).entries) {
                val key = entry.key
                if (key == ExportPolicy.MAIN) continue
                val reason = ExportPolicy.reason(str, key, entry.value) ?: continue
                if (zXiaomi) {
                    linkedHashMap.put(key, reason)
                } else if (this.selected.size >= MAX_PACKAGES) {
                    warn("相关系统包数量超过 3 个限制，跳过：" + key)
                } else {
                    try {
                        val listPaths2 = ExportPolicy.paths(this.shell.text("pm path " + ExportPolicy.quote(key)))
                        if (listPaths2.isEmpty()) {
                            throw IOException("pm path 未返回 APK 路径。")
                        }
                        this.selected.put(key, listPaths2)
                        this.reasons.put(key, reason)
                    } catch (e2: IOException) {
                        warn(key + " 定位失败：" + message(e2))
                    }
                }
            }
        } catch (e3: IOException) {
            warn("相关系统包列表读取失败：" + message(e3))
        }
        if (zXiaomi) {
            if (linkedHashMap.size > 1) {
                warn("发现多个小米核心插件候选，无法确认当前版本，未额外导出；请检查 packages.txt。")
                this.packageLog.append("plugin_candidates=").append(linkedHashMap.keys).append('\n')
                return
            }
            for (entry2 in linkedHashMap.entries) {
                try {
                    val listPaths3 = ExportPolicy.paths(this.shell.text("pm path " + ExportPolicy.quote(entry2.key)))
                    if (!listPaths3.isEmpty()) {
                        this.selected.put(entry2.key, listPaths3)
                        this.reasons.put(entry2.key, entry2.value)
                        return
                    }
                    continue
                } catch (e4: IOException) {
                    this.packageLog.append("discovery=").append(entry2.key).append(": ").append(message(e4))
                        .append('\n')
                }
            }
            try {
                val linkedHashSet = LinkedHashSet<String>()
                for (str2 in this.shell.text(XIAOMI_FIND).split("\\R".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    if (ExportPolicy.apkPath(str2) && File(str2).getName()
                            .equals("MIUISystemUIPlugin.apk", ignoreCase = true)
                    ) {
                        linkedHashSet.add(str2)
                    }
                }
                if (linkedHashSet.size == 1) {
                    this.selected.put("xiaomi.unregistered.systemui.plugin", ArrayList(linkedHashSet))
                    this.reasons.put(
                        "xiaomi.unregistered.systemui.plugin",
                        "按 MIUISystemUIPlugin.apk 精确文件名找到唯一文件；未确认包名，供离线分析"
                    )
                } else if (linkedHashSet.size > 1) {
                    warn("发现多个 MIUISystemUIPlugin.apk，无法确认当前使用版本，未批量导出；请检查 packages.txt。")
                    this.packageLog.append("plugin_candidate_paths=").append(linkedHashSet).append('\n')
                }
            } catch (e5: IOException) {
                warn("MIUISystemUIPlugin.apk 补充搜索失败：" + message(e5))
            }
        }
    }

    private fun warn(str: String?) {
        this.warnings.add(str!!)
    }

    companion object {
        const val MAX_APK: Long = 805306368
        const val MAX_APKS: Int = 48
        const val MAX_PACKAGES: Int = 3
        const val MAX_TOTAL: Long = 2147483648L
        const val XIAOMI_FIND: String =
            "for dir in /system/app /system/priv-app /system_ext/app /system_ext/priv-app /product/app /product/priv-app /vendor/app /vendor/priv-app /odm/app /odm/priv-app; do if [ -d \"\$dir\" ]; then /system/bin/find \"\$dir\" -maxdepth 4 -type f -iname 'MIUISystemUIPlugin.apk' || exit 1; fi; done"

        @JvmStatic fun `lambda$0`(file: File): Boolean {
            return file.getName().startsWith("work-") && file.isDirectory() && !Files.isSymbolicLink(file.toPath())
        }

        @JvmStatic fun message(exc: Exception): String {
            val message = exc.message
            return if (message == null) exc.javaClass.getSimpleName() else message
        }

        @Throws(IOException::class)
        private fun entry(zipOutputStream: ZipOutputStream, str: String, str2: String) {
            zipOutputStream.putNextEntry(ZipEntry(str))
            zipOutputStream.write(str2.toByteArray(StandardCharsets.UTF_8))
            zipOutputStream.closeEntry()
        }

        private fun sha256(): MessageDigest {
            try {
                return MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw AssertionError(e)
            }
        }

        @JvmStatic fun hex(bArr: ByteArray): String {
            val sb = StringBuilder()
            for (b in bArr) {
                sb.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255))
            }
            return sb.toString()
        }

        private fun cleanup(file: File) {
            val fileArrListFiles = file.listFiles()
            if (fileArrListFiles != null) {
                for (file2 in fileArrListFiles) {
                    file2.delete()
                }
            }
            file.delete()
        }
    }
}
