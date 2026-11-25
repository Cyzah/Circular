package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.cyzah.circular.data.settings.ConfigProvider
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.DualStyleAvailability
import com.cyzah.circular.ui.components.CircularIcons
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val dualStyleLabels = listOf("双层信号", "四点分组", "双卡胶囊", "DUO")
private val singleStyleLabels = listOf("简约圆环", "", "单卡胶囊", "")

@Composable
internal fun AppearanceScreen(state: SettingsState, onSubpage: (SettingsSubpage) -> Unit) {
    val revision = state.revision
    val style = remember(revision) { state.style() }
    val availableStyles = remember(state.dualSimAvailable) {
        DualStyle.values().filter { DualStyleAvailability.allows(it, state.dualSimAvailable) }
    }
    val availableStyleLabels = availableStyles.map {
        if (state.dualSimAvailable) dualStyleLabels[it.ordinal] else singleStyleLabels[it.ordinal]
    }
    var technology by rememberSaveable { mutableStateOf(ConfigProvider.technologyText(state.prefs)) }
    Section(if (state.dualSimAvailable) "信号样式" else "单卡样式") {
        ChoicePreference(
            title = "排列方式",
            options = availableStyleLabels,
            selected = availableStyles.indexOf(style).coerceAtLeast(0),
            darkTheme = state.darkTheme,
            onSelect = { state.selectStyle(availableStyles[it]) },
        )
    }
    Section("外观调整") {
        AppearanceSubpagePreference("图标比例", "图标尺寸、粗细与网络制式文字") {
            onSubpage(SettingsSubpage.ICON_SCALE)
        }
        AppearanceSubpagePreference("电量与布局", "电量数字与内容排列") {
            onSubpage(SettingsSubpage.BATTERY_LAYOUT)
        }
        AppearanceSubpagePreference("状态颜色", "充电、低电量、省电与性能模式") {
            onSubpage(SettingsSubpage.STATUS_COLORS)
        }
        AppearanceSubpagePreference("内容位置微调", "上下开口和电量环中的内容位置细调", onClick = {
            onSubpage(SettingsSubpage.CONTENT_POSITIONS)
        })
        BasicComponent(title = "网络制式文本",
            summary = "写「原=新」做替换，多条用分号隔开；「*=新」代表任何制式都显示它，留空恢复系统文本。",
            insideMargin = PaddingValues(start = SettingsContentPadding, top = SettingsContentPadding,
                end = SettingsContentPadding, bottom = 8.dp))
        Column(Modifier.fillMaxWidth().padding(start = SettingsContentPadding,
            end = SettingsContentPadding, bottom = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = technology, onValueChange = { technology = it }, label = "例如 5G=5G+;LTE=4G",
                modifier = Modifier.fillMaxWidth())
            TextButton(text = "保存网络制式文本", modifier = Modifier.fillMaxWidth(), onClick = {
                state.setTechnologyText(technology)
                technology = ConfigProvider.technologyText(state.prefs)
            })
        }
    }
}

@Composable
private fun AppearanceSubpagePreference(title: String, summary: String? = null, onClick: () -> Unit) {
    BasicComponent(title = title, summary = summary, insideMargin = SettingsRowInsets,
        onClick = onClick, endActions = {
            Icon(CircularIcons.ChevronDown, contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(-90f), tint = MiuixTheme.colorScheme.onSurface)
        })
}
