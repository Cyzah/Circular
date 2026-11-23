package com.cyzah.circular.ui.screen

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.cyzah.circular.BuildConfig
import com.cyzah.circular.R
import com.cyzah.circular.data.settings.ConfigProvider
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
internal fun DisplayScreen(state: SettingsState, onThemeSettings: () -> Unit) {
    val revision = state.revision
    val placement = remember(revision) { ConfigProvider.batteryNumberPlacement(state.prefs).value }
    val labels = listOf("不显示数字", "电池内", "电池右外")
    Section("应用主题") {
        BasicComponent(title = "主题设置", summary = "深浅模式、主题色与调色板",
            insideMargin = SettingsRowInsets, onClick = onThemeSettings)
    }
    Section("下拉显示") {
        Toggle("下拉后保持三合一", state.bool("keep_merged_shade", false), "下拉时不展开。") { state.setBoolean("keep_merged_shade", it) }
        Toggle("控制中心双排信号", state.bool("stacked_shade", true), "控制中心信号排成两排。") { state.setBoolean("stacked_shade", it) }
        ChoicePreference("电池电量数字", labels, placement, state.darkTheme) {
            state.setInt(SettingsSchema.BATTERY_NUMBER_PLACEMENT, it)
        }
    }
    Section("锁屏与息屏") {
        Toggle("锁屏显示", state.bool("lockscreen", true)) { state.setBoolean("lockscreen", it) }
        Toggle("全屏 AOD 显示", state.bool("aod", true), "使用单色，跟随系统电池图标的位置和显隐。") { state.setBoolean("aod", it) }
        Note("三星、一加和 OPPO 支持状态栏、亮屏锁屏及已适配的下拉图标区域；AOD 暂时使用系统原生显示。小米保留已适配的展开与 AOD 功能。不含状态栏的独立息屏样式需要另行适配。")
    }
    Section("应用入口") {
        Toggle("隐藏桌面图标", state.launcherHidden, "隐藏后可从 LSPosed 的模块设置进入。", onChange = state::changeLauncherHidden)
    }
    Section("使用提示") {
        Note(stringResource(R.string.system_ui_restart_hint))
        Action("遇到问题如何恢复") {
            state.info = "恢复系统显示" to "先在“预览”关闭合并显示。若系统界面异常，在 LSPosed 停用本模块后重启手机；也可使用电脑 ADB 卸载 ${BuildConfig.APPLICATION_ID}。"
        }
    }
}

