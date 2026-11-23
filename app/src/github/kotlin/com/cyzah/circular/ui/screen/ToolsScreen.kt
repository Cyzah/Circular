package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.AppNotice
import com.cyzah.circular.ui.state.GithubSettingsState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ToolsScreen(
    state: GithubSettingsState,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onProjectCredits: () -> Unit,
) {
    DiagnosticsSection(state)
    ExportSection(state, onExport, onShare)
    AboutSection(
        onNotice = { state.info = AppNotice.TITLE to AppNotice.MESSAGE },
        onProjectCredits = onProjectCredits,
    )
}

@Composable
private fun DiagnosticsSection(state: GithubSettingsState) {
    val refreshing = state.pendingReport != null
    Section("诊断与重载") {
        SettingsActionRow(
            title = if (refreshing) "正在刷新诊断…" else "复制诊断信息",
            summary = if (refreshing) "等待 SystemUI 回报，最多 8 秒后复制可用信息。"
                else "刷新模块运行状态与设备信息，复制后用于问题反馈。",
            busy = refreshing,
            onClick = state::requestDiagnostic,
        )
        RestartSystemUiAction(state)
    }
}

@Composable
private fun ExportSection(state: GithubSettingsState, onExport: () -> Unit, onShare: () -> Unit) {
    val export = state.exportState
    Section("机型适配包") {
        SettingsActionRow(
            title = when {
                export.running -> "正在导出适配包…"
                state.choosingExport -> "请选择保存位置…"
                else -> "导出 SystemUI 适配包"
            },
            summary = "提取 SystemUI 与核心插件并打包为 ZIP，需要 Root 授权。",
            busy = export.running || state.choosingExport,
            onClick = onExport,
        )
        if (export.message.isNotEmpty()) {
            Column(
                Modifier.padding(SettingsContentPadding).semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (export.running) "导出进度" else "最近导出", style = MiuixTheme.textStyles.body1)
                Text(export.message, style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        if (!export.running && export.file?.isFile == true) {
            SettingsActionRow("分享适配包", "分享最近生成的 ZIP 文件。", onClick = onShare)
        }
        SettingsActionRow("导出说明", "查看提取范围、保存位置与文件安全说明。") {
            state.info = "导出说明" to (
                "此功能仅读取并复制 SystemUI 相关系统文件，不会修改系统文件。导出时会申请 Root 权限。\n\n"
                    + "选择保存位置后开始导出，文件选择器默认打开内部存储的 Download 文件夹。导出完成后可直接分享；若保存失败，仍可分享应用内保留的适配包。\n\n"
                    + "小米优先提取 SystemUI 与 MIUISystemUIPlugin；OPPO / OnePlus / vivo 仅选择明确的核心 SystemUI / 控制中心包，最多 3 个（含主包）。未识别的品牌只导出主 SystemUI，其他品牌的实际结果仍需实机确认。\n\n"
                    + "所有品牌均排除导航栏、手势、无障碍和资源覆盖包。必要的拆分 APK 会保留，因此 APK 数可能多于核心包数；设备信息、包清单与校验文件不计入 APK 数。"
                )
        }
    }
}

