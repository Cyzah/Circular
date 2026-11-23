package com.cyzah.circular.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun restore(stored: String?, legacyLight: Boolean?): AppThemeMode =
            entries.firstOrNull { it.name == stored } ?: when (legacyLight) {
                true -> LIGHT
                false -> DARK
                null -> SYSTEM
            }
    }
}

/** App appearance is independent of the settings exported to SystemUI. */
@Immutable
data class AppThemeSettings(
    val mode: AppThemeMode = AppThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val customColor: Boolean = false,
    val dynamicColor: Boolean = true,
    val keyColor: Int = DEFAULT_KEY_COLOR,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.Spec2025,
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    fun oppositeMode(systemDark: Boolean) = if (isDark(systemDark)) AppThemeMode.LIGHT else AppThemeMode.DARK

    val supportsSpec2025: Boolean
        get() = paletteStyle in SPEC_2025_PALETTES

    val effectiveColorSpecYear: Int
        get() = when {
            paletteStyle == ThemePaletteStyle.Cmf -> 2026
            supportsSpec2025 && colorSpec == ThemeColorSpec.Spec2025 -> 2025
            else -> 2021
        }

    fun save(prefs: SharedPreferences) {
        prefs.edit {
            putString("mode", mode.name)
            putBoolean("amoled", amoled)
            putBoolean("custom_color", customColor)
            putBoolean("dynamic_color", dynamicColor)
            putInt("key_color", keyColor)
            putString("palette_style", paletteStyle.name)
            putString("color_spec", colorSpec.name)
        }
    }

    companion object {
        const val PREFERENCES = "app_theme"
        const val DEFAULT_KEY_COLOR = 0xFF3482FF.toInt()
        private val SPEC_2025_PALETTES = setOf(ThemePaletteStyle.TonalSpot, ThemePaletteStyle.Neutral,
            ThemePaletteStyle.Vibrant, ThemePaletteStyle.Expressive)

        fun read(prefs: SharedPreferences, legacy: SharedPreferences): AppThemeSettings {
            val defaults = AppThemeSettings()
            return AppThemeSettings(
                mode = AppThemeMode.restore(prefs.getString("mode", null),
                    if (legacy.contains("light_theme")) legacy.getBoolean("light_theme", true) else null),
                amoled = prefs.getBoolean("amoled", defaults.amoled),
                customColor = prefs.getBoolean("custom_color", defaults.customColor),
                dynamicColor = prefs.getBoolean("dynamic_color", defaults.dynamicColor),
                keyColor = prefs.getInt("key_color", defaults.keyColor) or 0xFF000000.toInt(),
                paletteStyle = ThemePaletteStyle.entries.firstOrNull {
                    it.name == prefs.getString("palette_style", null)
                } ?: defaults.paletteStyle,
                colorSpec = ThemeColorSpec.entries.firstOrNull {
                    it.name == prefs.getString("color_spec", null)
                } ?: defaults.colorSpec,
            )
        }
    }
}
