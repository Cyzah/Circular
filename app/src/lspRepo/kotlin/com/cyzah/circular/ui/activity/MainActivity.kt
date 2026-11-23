package com.cyzah.circular.ui.activity

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.cyzah.circular.domain.settings.AppNotice
import com.cyzah.circular.ui.components.CircularIcons
import com.cyzah.circular.ui.screen.AboutSection
import com.cyzah.circular.ui.screen.RestartSystemUiAction
import com.cyzah.circular.ui.screen.Section
import com.cyzah.circular.ui.screen.SettingsApp
import com.cyzah.circular.ui.state.SettingsState
import top.yukonga.miuix.kmp.glass.GlassNavigationItem

private val aboutTab = GlassNavigationItem(CircularIcons.About, "关于", "关于")

class MainActivity : SettingsActivity<SettingsState>() {
    override fun createSettingsState(savedInstanceState: Bundle?) = SettingsState(this, savedInstanceState)

    @Composable
    override fun SettingsContent() {
        SettingsApp(settings, aboutTab) { openCredits ->
            AboutSection(
                onNotice = { settings.info = AppNotice.TITLE to AppNotice.MESSAGE },
                onProjectCredits = openCredits,
            )
            Section("系统界面") { RestartSystemUiAction(settings) }
        }
    }
}
