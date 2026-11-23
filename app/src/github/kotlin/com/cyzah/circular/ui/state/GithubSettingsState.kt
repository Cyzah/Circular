package com.cyzah.circular.ui.state

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cyzah.circular.Version
import com.cyzah.circular.core.diagnostics.DiagnosticCopy
import com.cyzah.circular.data.settings.SettingsMutation
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.framework.export.SystemUiExportController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GithubSettingsState(context: Context, restored: Bundle?) : SettingsState(context, restored) {
    var exportState by mutableStateOf(SystemUiExportController.state())
        private set
    var choosingExport by mutableStateOf(restored?.getBoolean("choosing_export", false) ?: false)
        private set
    var pendingReport by mutableStateOf(restored?.getString("pending_report"))
        private set
    private var reportDeadline = restored?.getLong("report_deadline", 0L) ?: 0L
    private val reportTimeout = Runnable { finishDiagnostic(false) }
    private val refreshExports = object : Runnable {
        override fun run() {
            if (!resumed || disposed) return
            exportState = SystemUiExportController.state()
            if (exportState.running) handler.postDelayed(this, 500L)
        }
    }

    init {
        SystemUiExportController.restore(app)
        exportState = SystemUiExportController.state()
    }

    fun requestDiagnostic() {
        if (pendingReport != null) return
        pendingReport = UUID.randomUUID().toString()
        reportDeadline = SystemClock.elapsedRealtime() + 8000L
        write(SettingsMutation.text(SettingsSchema.DIAGNOSTIC_REQUEST, pendingReport))
        handler.postDelayed(reportTimeout, 8000L)
    }

    override fun onReportChanged() {
        if (!disposed && resumed && DiagnosticCopy.isFresh(pendingReport, prefs.getString("report_token", ""))) {
            finishDiagnostic(true)
        }
    }

    private fun finishDiagnostic(received: Boolean) {
        val pending = pendingReport ?: return
        if (!resumed || disposed) return
        val fresh = DiagnosticCopy.isFresh(pending, prefs.getString("report_token", ""))
        if (received && !fresh) return
        handler.removeCallbacks(reportTimeout)
        pendingReport = null
        val reportTime = prefs.getLong("report_time", 0L)
        val reportDate = if (reportTime == 0L) "尚未收到 SystemUI 回报"
            else SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(Date(reportTime))
        val text = DiagnosticCopy.format(
            fresh, Version.NAME, Build.MANUFACTURER + " " + Build.MODEL, reportDate,
            prefs.getString("report", "未收到回报。请确认 LSPosed 已启用模块、作用域为系统界面，并在安装后重启手机。")
        )
        try {
            val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Circular 诊断", text))
        } catch (error: RuntimeException) {
            info = "复制失败" to "${error.message.orEmpty()}\n$text"
        }
        if (!fresh) info = "已复制诊断信息" to "8 秒内未收到新的 SystemUI 回报，已标明并复制现有诊断信息。"
    }

    fun beginExportSelection() {
        choosingExport = true
    }

    fun cancelExportSelection() {
        choosingExport = false
    }

    fun exportTo(uri: Uri?) {
        choosingExport = false
        if (uri?.scheme == "content") SystemUiExportController.start(app, uri)
        handler.removeCallbacks(refreshExports)
        refreshExports.run()
    }

    override fun resume() {
        super.resume()
        handler.removeCallbacks(refreshExports)
        refreshExports.run()
        if (pendingReport != null) {
            if (DiagnosticCopy.isFresh(pendingReport, prefs.getString("report_token", ""))) {
                finishDiagnostic(true)
            } else {
                handler.postDelayed(reportTimeout, (reportDeadline - SystemClock.elapsedRealtime()).coerceIn(0L, 8000L))
            }
        }
    }

    override fun stop() {
        super.stop()
        handler.removeCallbacks(reportTimeout)
        handler.removeCallbacks(refreshExports)
    }

    override fun save(out: Bundle) {
        super.save(out)
        out.putBoolean("choosing_export", choosingExport)
        out.putString("pending_report", pendingReport)
        out.putLong("report_deadline", reportDeadline)
    }
}
