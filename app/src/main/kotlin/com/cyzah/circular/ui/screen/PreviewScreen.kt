package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.ui.preview.PreviewPowerMode
import com.cyzah.circular.ui.preview.PreviewSceneOperations
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.Card

private val PreviewSliderInsets = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 0.dp)
private val PreviewSliderFirstInsets = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 0.dp)
private val PreviewSliderLastInsets = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp)

@Composable
internal fun PreviewScreen(state: SettingsState) {
    DisposableEffect(state) { onDispose { state.finishExpansionPreview() } }
    val revision = state.revision
    val sceneRevision = state.sceneRevision
    val scene = state.scene
    val description = remember(sceneRevision) { scene.description() }
    val style = remember(revision) { state.style() }
    val technologies = remember(state) { state.technologies() }
    val merged = DisplayPolicy.mode(state.int("mode", 0)) == 2
    Card(modifier = Modifier.fillMaxWidth()) {
        Toggle("启用Circular", merged,
            summary = if (merged) "合并显示已开启 · 开始享受" else "合并显示已关闭 · 保留系统原生图标") {
            state.setInt("mode", if (it) 2 else 0)
        }
    }
    Section("动画与场景", description) {
        IntSlider("展开动画", (state.expansion * 100f).toInt(), 0, 100,
            onChangeFinished = state::finishExpansionPreview,
            contentPadding = PreviewSliderLastInsets) { state.updateExpansion(it / 100f) }
    }
    ExpandableSection("模拟场景", expanded = state.scenesExpanded,
        darkTheme = state.darkTheme, onToggle = state::toggleScenes) {
        IntSlider("电量", scene.battery, 0, 100,
            contentPadding = PreviewSliderFirstInsets) { state.editScene { s -> s.battery = it } }
        Toggle("充电", scene.charging) { state.editScene { s -> s.charging = it } }
        Toggle("省电模式", scene.powerSave) { state.editScene { s -> PreviewPowerMode.setSaver(s, it) } }
        Toggle("性能模式", scene.performanceMode) { state.editScene { s -> PreviewPowerMode.setPerformance(s, it) } }
        Toggle("Wi-Fi", scene.wifiEnabled) { state.editScene { s -> PreviewSceneOperations.setWifiEnabled(s, it) } }
        IntSlider("Wi-Fi 强度（0~3 级）", scene.wifiLevel, 0, 3, suffix = "", enabled = scene.wifiEnabled,
            contentPadding = PreviewSliderInsets) { state.editScene { s -> s.wifiLevel = it } }
        Toggle("双卡", scene.dualSim()) { state.editScene { s -> PreviewSceneOperations.setDual(s, it) } }
        if (scene.dualSim()) {
            val labels = when (style) {
                DualStyle.CAPSULE, DualStyle.DUO -> "卡一信号（上排四点）" to "卡二信号（下排四点）"
                DualStyle.GROUPED -> "卡一信号（左组两点）" to "卡二信号（右组两点）"
                else -> "卡一信号（内侧三点）" to "卡二信号（外侧四点）"
            }
            IntSlider(labels.first, scene.secondaryLevel.coerceAtLeast(0), 0, 4, suffix = "/4",
                contentPadding = PreviewSliderInsets) { state.editScene { s -> s.secondaryLevel = it } }
            Toggle("卡一无服务", !scene.secondaryInService) { state.editScene { s -> PreviewSceneOperations.setCard1NoService(s, it) } }
            IntSlider(labels.second, scene.mobileLevel.coerceAtLeast(0), 0, 4, suffix = "/4",
                contentPadding = PreviewSliderInsets) { state.editScene { s -> s.mobileLevel = it } }
            Toggle("卡二无服务", !scene.inService) { state.editScene { s -> PreviewSceneOperations.setCard2NoService(s, it) } }
        } else {
            IntSlider("信号", scene.mobileLevel, 0, 4, suffix = "/4",
                contentPadding = PreviewSliderInsets) { state.editScene { s -> s.mobileLevel = it } }
            Toggle("无服务", !scene.inService) { state.editScene { s -> PreviewSceneOperations.setCard2NoService(s, it) } }
        }
        Toggle("数据网络", scene.dataEnabled) { state.editScene { s -> s.dataEnabled = it } }
        ChoicePreference("数据网络制式", technologies, technologies.indexOf(scene.mobileTechnology), state.darkTheme,
            summary = if (scene.mobileTechnology.isBlank()) "未选择" else null) { index ->
            state.editScene { it.mobileTechnology = technologies[index] }
        }
        Toggle("飞行模式", scene.airplane) { state.editScene { s -> s.airplane = it } }
    }
    Note("首次使用请先在 LSPosed 启用模块，并重新加载系统界面。")
}

