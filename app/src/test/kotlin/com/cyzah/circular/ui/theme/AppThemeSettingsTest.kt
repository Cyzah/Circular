package com.cyzah.circular.ui.theme

import org.junit.Assert.*
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

class AppThemeSettingsTest {
    @Test fun modeMigratesLegacyPreferenceAndDefaultsToSystem() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.restore(null, null))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.restore(null, true))
        assertEquals(AppThemeMode.DARK, AppThemeMode.restore(null, false))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.restore("SYSTEM", false))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.restore("unknown", true))
    }

    @Test fun explicitModeOverridesSystemAndToggleLeavesSystemMode() {
        assertFalse(AppThemeSettings(mode = AppThemeMode.LIGHT).isDark(true))
        assertTrue(AppThemeSettings(mode = AppThemeMode.DARK).isDark(false))
        val following = AppThemeSettings(mode = AppThemeMode.SYSTEM)
        assertFalse(following.isDark(false))
        assertTrue(following.isDark(true))
        assertEquals(AppThemeMode.LIGHT, following.oppositeMode(true))
        assertEquals(AppThemeMode.DARK, following.oppositeMode(false))
    }

    @Test fun effectiveSpecMatchesPaletteSupportWithoutLosingPreference() {
        ThemePaletteStyle.entries.forEach { palette ->
            val settings = AppThemeSettings(paletteStyle = palette, colorSpec = ThemeColorSpec.Spec2025)
            val expected = when (palette) {
                ThemePaletteStyle.Cmf -> 2026
                ThemePaletteStyle.TonalSpot, ThemePaletteStyle.Neutral,
                ThemePaletteStyle.Vibrant, ThemePaletteStyle.Expressive -> 2025
                else -> 2021
            }
            assertEquals(palette.name, expected, settings.effectiveColorSpecYear)
            assertEquals(ThemeColorSpec.Spec2025, settings.colorSpec)
        }
        assertEquals(2021, AppThemeSettings(colorSpec = ThemeColorSpec.Spec2021).effectiveColorSpecYear)
    }
}
