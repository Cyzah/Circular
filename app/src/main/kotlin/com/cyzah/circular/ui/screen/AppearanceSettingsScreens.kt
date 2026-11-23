package com.cyzah.circular.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyzah.circular.data.settings.ConfigProvider
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.SizePolicy
import com.cyzah.circular.domain.settings.StatusColors
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
internal fun IconScaleScreen(state: SettingsState) {
    val style = remember(state.revision) { state.style() }
    Section("图标比例") {
        IntSlider("整体尺寸", DisplayPolicy.size(state.int("size", 24)), SizePolicy.minimum(style), SizePolicy.maximum(style), suffix = " dp", onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("size", it) }) { state.setIntContinuously("size", it) }
        IntSlider("圆环与信号点粗细", ConfigProvider.ringWeight(state.prefs), 80, 180, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("weight_v2", it) }) { state.setIntContinuously("weight_v2", it) }
        IntSlider("Wi-Fi 大小", DisplayPolicy.centrePercent(state.int("wifi_scale", 100)), 50, DisplayPolicy.MAX_CENTRE_PERCENT, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("wifi_scale", it) }) { state.setIntContinuously("wifi_scale", it) }
        IntSlider("Wi-Fi 粗细", ConfigProvider.networkWeight(state.prefs, "wifi_weight"), 80, 180, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("wifi_weight", it) }) { state.setIntContinuously("wifi_weight", it) }
        IntSlider("网络制式文字大小", ConfigProvider.technologyScale(state.prefs), 50, DisplayPolicy.MAX_CENTRE_PERCENT, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("technology_scale_v2", it) }) { state.setIntContinuously("technology_scale_v2", it) }
        IntSlider("网络制式文字粗细", ConfigProvider.technologyWeight(state.prefs), 80, 180, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("technology_weight_v2", it) }) { state.setIntContinuously("technology_weight_v2", it) }
    }
}

@Composable
internal fun BatteryLayoutScreen(state: SettingsState) {
    val style = remember(state.revision) { state.style() }
    Section("电量与布局") {
        if (style.allowsShowPercent()) Toggle("显示电量数字", state.bool("show_percent", false)) { state.setBoolean("show_percent", it) }
        IntSlider("数字大小", ConfigProvider.percentScale(state.prefs), 70, 160, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("percent_scale_v2", it) }) { state.setIntContinuously("percent_scale_v2", it) }
        IntSlider("数字粗细", ConfigProvider.percentWeight(state.prefs), 50, 180, 5, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("percent_weight_v2", it) }) { state.setIntContinuously("percent_weight_v2", it) }
        if (style.allowsSwapContents()) Toggle("交换电量与网络位置", state.bool("swap_contents", false), "电量移到中央，Wi-Fi / 数据网络制式移到顶部。") { state.setBoolean("swap_contents", it) }
    }
}

@Composable
internal fun StatusColorsScreen(state: SettingsState) {
    var colorKey by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = listOf(
        Triple("充电", "charge_color", StatusColors.CHARGING),
        Triple("低电量", "low_color", StatusColors.LOW),
        Triple("省电模式", "saver_color", StatusColors.SAVER),
        Triple("性能模式", "performance_color", StatusColors.PERFORMANCE))
    Section("状态颜色") {
        Toggle("启用状态颜色", state.bool("colour", true)) { state.setBoolean("colour", it) }
        colors.forEach { (label, key, default) ->
            val color = StatusColors.opaque(state.int(key, default))
            BasicComponent(title = label, summary = StatusColors.hex(color), insideMargin = SettingsRowInsets,
                onClick = { colorKey = key }, endActions = {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(color)))
                })
        }
        IntSlider("低电量阈值", DisplayPolicy.lowBatteryThreshold(state.int("low_battery_threshold", DisplayPolicy.DEFAULT_LOW_BATTERY_THRESHOLD)), 1, 100, onChangeFinished = state::flushSettings, onDiscreteChange = { state.setInt("low_battery_threshold", it) }) { state.setIntContinuously("low_battery_threshold", it) }
    }
    colors.firstOrNull { it.second == colorKey }?.let { (label, key, default) ->
        ColorDialog("${label}颜色", state.int(key, default), default, { colorKey = null }) { state.setInt(key, StatusColors.opaque(it)) }
    }
}
