package com.cyzah.circular.ui.screen

internal enum class SettingsSubpage(
    val destination: Int,
    val title: String,
    val showsPreview: Boolean,
) {
    THEME_SETTINGS(4, "主题设置", false),
    CONTENT_POSITIONS(5, "内容位置微调", true),
    ICON_SCALE(6, "图标比例", true),
    BATTERY_LAYOUT(7, "电量与布局", true),
    STATUS_COLORS(8, "状态颜色", true),
    PROJECT_CREDITS(9, "参考与开源说明", false),
}
