package com.cyzah.circular.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.cyzah.circular.ui.state.SettingsState

abstract class SettingsActivity<S : SettingsState> : ComponentActivity() {
    protected lateinit var settings: S
        private set

    protected abstract fun createSettingsState(savedInstanceState: Bundle?): S

    @Composable
    protected abstract fun SettingsContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = createSettingsState(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.navigationBarDividerColor = Color.TRANSPARENT
        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = settings.darkTheme
            SideEffect {
                settings.updateSystemDark(systemDark)
                applyBars(dark)
            }
            SettingsContent()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::settings.isInitialized) settings.resume()
    }

    override fun onPause() {
        if (::settings.isInitialized) settings.pause()
        super.onPause()
    }

    override fun onStop() {
        if (::settings.isInitialized) settings.stop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::settings.isInitialized) settings.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        settings.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::settings.isInitialized) applyBars(settings.darkTheme)
    }

    private fun applyBars(dark: Boolean) {
        val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (dark) 0 else flags, flags)
    }
}
