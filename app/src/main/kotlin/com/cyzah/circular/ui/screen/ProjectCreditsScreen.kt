package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cyzah.circular.domain.settings.AppNotice
import com.cyzah.circular.ui.components.CircularIcons
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val APACHE_LICENSE = "Apache License 2.0"

@Composable
internal fun ProjectCreditsScreen(onLinkUnavailable: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val openLink: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: IllegalArgumentException) {
            onLinkUnavailable(url)
        }
    }

    Section("项目沿革与致谢") {
        Note(
            "Circular 最初由 lu_aiden 开发的「三合一状态图标」二次修改而来。" +
                "经过 ${AppNotice.DEVELOPER} 多次更新、重构和改版，现已发展为独立的 Circular 项目。" +
                "感谢 lu_aiden 大佬提供最初的项目基础。"
        )
    }
    Section("设计参考") {
        ProjectLink(
            "InstallerX Revived",
            "Circular 的主题设置参考了该开源项目。",
            onClick = { openLink("https://github.com/wxxsfxyzm/InstallerX-Revived") },
        )
    }
    Section("开源组件") {
        ProjectLink(
            "Miuix",
            "Glass 和 Preference 组件用于设置界面。版权归 compose-miuix-ui 贡献者。$APACHE_LICENSE",
            onClick = { openLink("https://github.com/compose-miuix-ui/miuix") },
        )
        ProjectLink(
            "Miuix Glass 返回图标",
            "使用 Regular 返回图标。© 2026 compose-miuix-ui contributors。$APACHE_LICENSE",
            onClick = {
                openLink("https://github.com/compose-miuix-ui/miuix/tree/4d2e718c18351c171987a9c9226c7e2d60c285d8")
            },
        )
        ProjectLink(
            "Compose Multiplatform",
            "Foundation 用于界面的布局、滚动和手势。$APACHE_LICENSE",
            onClick = { openLink("https://github.com/JetBrains/compose-multiplatform-core") },
        )
        ProjectLink(
            "AndroidX Activity Compose",
            "将 Compose 设置界面接入 Android Activity。$APACHE_LICENSE",
            onClick = { openLink("https://developer.android.com/jetpack/androidx/releases/activity") },
        )
        ProjectLink(
            "Kotlin",
            "应用使用 Kotlin 语言和标准库。$APACHE_LICENSE",
            onClick = { openLink("https://github.com/JetBrains/kotlin") },
        )
        ProjectLink(
            "libxposed API",
            "模块编译使用 API 102，运行时由 LSPosed 提供。$APACHE_LICENSE",
            onClick = { openLink("https://github.com/libxposed/api") },
        )
        ProjectLink(
            APACHE_LICENSE,
            "查看许可证全文。",
            onClick = { openLink("https://www.apache.org/licenses/LICENSE-2.0") },
        )
    }
    Section("字体版权") {
        Note(
            "状态栏绘制使用内置的 9 种 SF Pro Rounded 字重子集。" +
                "字体版权归 Apple Inc.，字体文件指向适用的 iOS 软件许可协议。"
        )
    }
}

@Composable
private fun ProjectLink(title: String, summary: String, onClick: () -> Unit) {
    BasicComponent(
        title = title,
        summary = summary,
        insideMargin = SettingsRowInsets,
        role = Role.Button,
        onClick = onClick,
        endActions = {
            Icon(
                CircularIcons.ChevronBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(180f),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
    )
}
