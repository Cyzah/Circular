package com.cyzah.circular.framework.export

import android.content.Context
import android.net.Uri
import android.os.Build
import com.cyzah.circular.Version
import com.cyzah.circular.framework.device.RootShell
import com.cyzah.circular.framework.export.SystemUiExporter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.Volatile

object SystemUiExportController {
    const val NOTICE: String = "此功能仅读取并复制 SystemUI 相关系统文件用于机型适配，不会修改系统文件。"

    @Volatile
    private var state = State(false, "", null)

    @JvmStatic fun state(): State {
        return state
    }

    @JvmStatic fun directory(context: Context): File {
        return File(context.getCacheDir(), "systemui-exports")
    }

    @JvmStatic @Synchronized
    fun restore(context: Context) {
        if (!state.running && state.file == null) {
            val string: String = context.getSharedPreferences("systemui-export", 0).getString("last_file", "")!!
            if (!string.isEmpty()) {
                try {
                    state = State(
                        false,
                        context.getSharedPreferences("systemui-export", 0)
                            .getString("last_message", "上次导出：" + string) ?: ("上次导出：" + string),
                        ExportPolicy.sharedFile(
                            directory(context), string
                        )
                    )
                } catch (e: IOException) {
                }
            }
        }
    }

    @JvmStatic @Synchronized
    fun start(context: Context, destination: Uri?): Boolean {
        if (state.running
            || destination == null || ("content" != destination.getScheme())
        ) {
            return false
        }
        val applicationContext = context.getApplicationContext()
        val target: Uri? = destination
        state = State(true, "正在申请 Root 权限…", null)
        Thread(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                try {
                    `lambda$0`(applicationContext, target)
                } catch (th: Throwable) {
                    state = State(false, "导出失败：" + SystemUiExporter.Companion.message(Exception(th)), null)
                }
            }
        }, "systemui-export").start()
        return true
    }

    @JvmStatic @Throws(Throwable::class)
    fun `lambda$0`(context: Context, destination: Uri?) {
        try {
            val resultExport = SystemUiExporter(RootShell(), object : SystemUiExporter.Progress {
                // com.cyzah.circular.framework.export.SystemUiExporter.Progress
                override fun update(str: String) {
                    `lambda$1`(str)
                }
            }).export(directory(context), Build.MANUFACTURER, Build.MODEL, deviceInfo())
            val summary = (resultExport.packageCount.toString() + " 个核心包 · "
                    + resultExport.count + " 个 APK · "
                    + String.format(
                Locale.ROOT,
                "%.1f",
                resultExport.file.length() / 1048576.0
            ) + " MiB\n" + resultExport.file.getName())
            var saveStatus: String?
            try {
                state = State(true, "正在保存到所选位置…", null)
                copyToDestination(context, resultExport.file, destination)
                saveStatus = "已保存到所选位置"
            } catch (saveError: Exception) {
                saveStatus = ("保存到所选位置失败：" + SystemUiExporter.Companion.message(saveError)
                        + "\n适配包仍保留在应用内，可继续分享或重新导出。")
            }
            var str = "导出完成：" + summary + "\n" + saveStatus
            if (resultExport.count > resultExport.packageCount) {
                str = str + "\n包含核心包所需的拆分 APK。"
            }
            if (!resultExport.warnings.isEmpty()) {
                str = str + "\n注意：" + resultExport.warnings.joinToString("\n")
            }
            context.getSharedPreferences("systemui-export", 0).edit()
                .putString("last_file", resultExport.file.getName()).putString("last_message", str).apply()
            state = State(false, str, resultExport.file)
        } catch (e: Exception) {
            state = State(false, "导出失败：" + SystemUiExporter.Companion.message(e), null)
        }
    }

    @JvmStatic fun `lambda$1`(str: String) {
        state = State(true, str, null)
    }

    @Throws(IOException::class)
    private fun copyToDestination(context: Context, source: File, destination: Uri?) {
        val rawOutput: OutputStream = context.getContentResolver()
            .openOutputStream(destination!!, "wt")
            ?: throw IOException("文件选择器未返回可写入的位置。")
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(rawOutput, 65536).use { output ->
                ExportCopy.copy(input, output)
            }
        }
    }

    private fun deviceInfo(): String {
        return "manufacturer=" + Build.MANUFACTURER + "\nmodel=" + Build.MODEL + "\ndevice=" + Build.DEVICE + "\nAndroid version=" + Build.VERSION.RELEASE + "\nSDK=" + Build.VERSION.SDK_INT + "\nincremental=" + Build.VERSION.INCREMENTAL + "\nbuild version=" + Build.DISPLAY + "\nbuild id=" + Build.ID + "\nfingerprint=" + Build.FINGERPRINT + "\nexporter=" + Version.NAME + "\nexported_at=" + SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.ROOT
        ).format(
            Date()
        ) + "\n\n此功能仅读取并复制 SystemUI 相关系统文件用于机型适配，不会修改系统文件。\n"
    }

    class State(@JvmField val running: Boolean, @JvmField val message: String, @JvmField val file: File?)
}
