package com.cyzah.circular.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.StatusColors
import com.cyzah.circular.ui.state.SettingsState
import com.cyzah.circular.ui.theme.AppThemeMode
import com.cyzah.circular.ui.theme.AppThemeSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val paletteNames = listOf("柔和色调", "中性色调", "鲜艳", "表现力", "彩虹", "水果沙拉", "单色", "忠实", "内容", "CMF")
private val themeColorPresets = listOf(
    "蓝色" to AppThemeSettings.DEFAULT_KEY_COLOR,
    "紫色" to 0xFF8062D6.toInt(),
    "粉色" to 0xFFD35F91.toInt(),
    "橙色" to 0xFFE28336.toInt(),
    "绿色" to 0xFF3D9968.toInt(),
    "青色" to 0xFF238E9C.toInt(),
    "红色" to 0xFFBA1A1A.toInt(),
    "琥珀色" to 0xFFB87817.toInt(),
    "黄色" to 0xFFB49A25.toInt(),
    "酸橙色" to 0xFF829B33.toInt(),
    "蓝绿色" to 0xFF28887C.toInt(),
    "浅蓝色" to 0xFF388DC4.toInt(),
    "靛蓝色" to 0xFF5355A9.toInt(),
    "深紫色" to 0xFF7E42A4.toInt(),
    "蓝灰色" to 0xFF575D7E.toInt(),
    "棕色" to 0xFF7D524A.toInt(),
    "灰色" to 0xFF5F6162.toInt(),
    "橄榄色" to 0xFF4A672D.toInt(),
)

@Composable
internal fun ThemeSettingsScreen(state: SettingsState) {
    val settings = state.themeSettings
    var choosingColor by rememberSaveable { mutableStateOf(false) }
    Section("应用外观") {
        ChoicePreference("主题模式", listOf("跟随系统", "浅色", "深色"), settings.mode.ordinal,
            state.darkTheme, onSelect = { state.setThemeMode(AppThemeMode.entries[it]) })
        Toggle("纯黑背景", settings.amoled, "深色模式下为页面使用 AMOLED 纯黑背景。", onChange = state::setThemeAmoled)
    }
    Section("主题配色") {
        Toggle("自定义主题配色", settings.customColor, "为应用界面生成配色。", onChange = state::setThemeCustomColor)
        AnimatedVisibility(settings.customColor,
            enter = fadeIn(tween(220)) + expandVertically(tween(260)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))) {
            Column {
                Toggle("动态颜色", settings.dynamicColor, "基于壁纸的主题颜色。", onChange = state::setThemeDynamicColor)
                ChoicePreference("调色板风格", paletteNames, settings.paletteStyle.ordinal, state.darkTheme,
                    onSelect = { state.setThemePalette(ThemePaletteStyle.entries[it]) })
                val specOptions = when {
                    settings.paletteStyle == ThemePaletteStyle.Cmf -> listOf("2026（自动）")
                    settings.supportsSpec2025 -> listOf("2021", "2025")
                    else -> listOf("2021")
                }
                ChoicePreference("配色规范", specOptions,
                    if (settings.supportsSpec2025 && settings.colorSpec == ThemeColorSpec.Spec2025) 1 else 0,
                    state.darkTheme,
                    summary = when {
                        settings.paletteStyle == ThemePaletteStyle.Cmf -> "CMF 自动使用 2026 配色规范。"
                        !settings.supportsSpec2025 -> "此调色板使用 2021 配色规范。"
                        else -> null
                    }, enabled = settings.supportsSpec2025,
                    onSelect = { state.setThemeColorSpec(ThemeColorSpec.entries[it]) })
            }
        }
    }
    AnimatedVisibility(settings.customColor && !settings.dynamicColor,
        enter = fadeIn(tween(220)) + expandVertically(tween(300)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(260))) {
        Section("主题颜色") {
            ThemeColorChips(settings, state::setThemeKeyColor)
            BasicComponent(title = "自选主题色", summary = StatusColors.hex(settings.keyColor),
                insideMargin = SettingsRowInsets, onClick = { choosingColor = true }, endActions = {
                    Box(Modifier.size(28.dp).background(Color(settings.keyColor), CircleShape))
                })
        }
    }
    if (choosingColor) ColorDialog("主题颜色", settings.keyColor, AppThemeSettings.DEFAULT_KEY_COLOR,
        { choosingColor = false }, state::setThemeKeyColor)
}

@Composable
private fun ThemeColorChips(settings: AppThemeSettings, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(SettingsContentPadding).selectableGroup()) {
        val columns = (maxWidth / 88.dp).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themeColorPresets.chunked(columns).forEach { colors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { (label, color) ->
                        val selected = settings.keyColor == color
                        val previewController = remember(color, settings.paletteStyle, settings.colorSpec) {
                            ThemeController(colorSchemeMode = ColorSchemeMode.MonetLight, keyColor = Color(color),
                                paletteStyle = settings.paletteStyle, colorSpec = settings.colorSpec, isDark = false)
                        }
                        // Preview the generated palette, using the same seed/style/spec as the app.
                        val preview = previewController.currentColors()
                        Column(Modifier.weight(1f)
                            .selectable(selected, role = Role.RadioButton, onClick = { onSelect(color) })
                            .semantics { contentDescription = "${label}主题色" }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(64.dp).background(preview.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center) {
                                Canvas(Modifier.size(48.dp)) {
                                    drawArc(preview.primaryContainer.copy(alpha = 0.9f), 180f, 180f, useCenter = true)
                                    drawArc(preview.tertiaryContainer.copy(alpha = 0.9f), 90f, 90f, useCenter = true)
                                    drawArc(preview.secondaryContainer.copy(alpha = 0.6f), 0f, 90f, useCenter = true)
                                    drawCircle(preview.primary, radius = 13.dp.toPx())
                                    if (selected) {
                                        val centre = center
                                        drawLine(preview.onPrimary, centre + Offset(-5.dp.toPx(), 0f),
                                            centre + Offset(-1.dp.toPx(), 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                                        drawLine(preview.onPrimary, centre + Offset(-1.dp.toPx(), 4.dp.toPx()),
                                            centre + Offset(6.dp.toPx(), -4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                                    }
                                }
                            }
                            Text(label, style = MiuixTheme.textStyles.footnote1)
                        }
                    }
                    repeat(columns - colors.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
