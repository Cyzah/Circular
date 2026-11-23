package com.cyzah.circular.ui.activity

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.cyzah.circular.framework.export.ExportFileProvider
import com.cyzah.circular.framework.export.ExportPolicy
import com.cyzah.circular.ui.components.CircularIcons
import com.cyzah.circular.ui.screen.SettingsApp
import com.cyzah.circular.ui.screen.ToolsScreen
import com.cyzah.circular.ui.state.GithubSettingsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.glass.GlassNavigationItem

private val toolsTab = GlassNavigationItem(CircularIcons.Tools, "工具", "工具")

class MainActivity : SettingsActivity<GithubSettingsState>() {
    private val exportDestination = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        settings.exportTo(if (result.resultCode == RESULT_OK) result.data?.data else null)
    }

    override fun createSettingsState(savedInstanceState: Bundle?) = GithubSettingsState(this, savedInstanceState)

    @Composable
    override fun SettingsContent() {
        SettingsApp(settings, toolsTab) { openCredits ->
            ToolsScreen(settings, ::requestExport, ::shareExport, openCredits)
        }
    }

    private fun requestExport() {
        if (settings.exportState.running || settings.choosingExport) return
        val fileName = ExportPolicy.archiveName(Build.MANUFACTURER, Build.MODEL,
            SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date()))
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip").putExtra(Intent.EXTRA_TITLE, fileName)
            .putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download"))
        try {
            settings.beginExportSelection()
            exportDestination.launch(intent)
        } catch (error: RuntimeException) {
            settings.cancelExportSelection()
            settings.info = "无法选择保存位置" to "系统中没有可用的文件选择器，请安装或启用系统文件管理应用后重试。"
        }
    }
    private fun shareExport() {
        val export = settings.exportState
        val file = export.file ?: return
        if (export.running) return
        try {
            val uri = ExportFileProvider.uri(this, file)
            val intent = Intent(Intent.ACTION_SEND).setType("application/zip")
                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newRawUri(file.name, uri)
            startActivity(Intent.createChooser(intent, "分享 SystemUI 适配包").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (error: Exception) {
            settings.info = "无法分享" to "请确认适配包仍存在且已安装可接收 ZIP 的应用，必要时重新导出。"
        }
    }
}
