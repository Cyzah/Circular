package com.cyzah.circular.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSubpageTest {
    @Test
    fun appearanceSubpagesHaveDistinctDestinationsAndKeepPreviewVisible() {
        val appearancePages = listOf(SettingsSubpage.ICON_SCALE, SettingsSubpage.BATTERY_LAYOUT,
            SettingsSubpage.STATUS_COLORS, SettingsSubpage.CONTENT_POSITIONS)
        val allPages = SettingsSubpage.values().toList()

        assertEquals(appearancePages.size, appearancePages.map { it.destination }.toSet().size)
        assertTrue(appearancePages.all { it.destination >= 4 })
        assertTrue(appearancePages.all { it.showsPreview })
        assertEquals(allPages.size, allPages.map { it.destination }.toSet().size)
    }

    @Test
    fun existingThemeSubpageKeepsItsTitleAndHidesPreview() {
        assertEquals("主题设置", SettingsSubpage.THEME_SETTINGS.title)
        assertFalse(SettingsSubpage.THEME_SETTINGS.showsPreview)
    }
}
