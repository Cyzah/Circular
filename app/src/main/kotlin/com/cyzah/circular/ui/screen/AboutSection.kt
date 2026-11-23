package com.cyzah.circular.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyzah.circular.Version
import com.cyzah.circular.domain.settings.AppNotice
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AboutSection(onNotice: () -> Unit, onProjectCredits: () -> Unit) {
    Section("关于 Circular") {
        Column(Modifier.padding(SettingsContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Circular", style = MiuixTheme.textStyles.title3)
            Text("版本 ${Version.NAME}", style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Text(AppNotice.DESCRIPTION, style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        BasicComponent(title = "开发与维护", summary = AppNotice.DEVELOPER, insideMargin = SettingsRowInsets)
        SettingsActionRow(
            SettingsSubpage.PROJECT_CREDITS.title,
            "项目沿革、致谢与第三方组件",
            onClick = onProjectCredits,
        )
        SettingsActionRow(AppNotice.TITLE, "开启体验前，先读一段关于 Circular 的约定。", onClick = onNotice)
    }
}
