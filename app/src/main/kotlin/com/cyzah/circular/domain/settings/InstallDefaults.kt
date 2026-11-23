package com.cyzah.circular.domain.settings

import android.content.SharedPreferences

/** Seeds a new installation without changing preferences from an existing one.  */
object InstallDefaults {
    @JvmStatic fun applyIfEmpty(preferences: SharedPreferences) {
        // Check before the provider's legacy migrations add keys. An existing
        // installation keeps even its absent preferences and their old fallbacks.
        if (!preferences.getAll().isEmpty()) {
            return
        }

        // Captured user settings, 2026-11-17. Diagnostic and notice state is omitted.
        preferences.edit()
            .putInt(SettingsSchema.MODE, 2)
            .putInt(SettingsSchema.SIZE, 27)
            .putInt(SettingsSchema.WEIGHT, 120)
            .putInt(SettingsSchema.WEIGHT_V2, 100)
            .putInt(SettingsSchema.WIFI_SCALE, 95)
            .putInt(SettingsSchema.WIFI_WEIGHT, 110)
            .putInt(SettingsSchema.TECHNOLOGY_SCALE, 108)
            .putInt(SettingsSchema.TECHNOLOGY_SCALE_V2, 90)
            .putInt(SettingsSchema.TECHNOLOGY_WEIGHT, 156)
            .putInt(SettingsSchema.TECHNOLOGY_WEIGHT_V2, 130)
            .putBoolean(SettingsSchema.SHOW_PERCENT, true)
            .putInt(SettingsSchema.PERCENT_SCALE_V2, 100)
            .putInt(SettingsSchema.PERCENT_WEIGHT, 150)
            .putInt(SettingsSchema.PERCENT_WEIGHT_V2, 115)
            .putBoolean(SettingsSchema.SWAP_CONTENTS, false)
            .putBoolean(SettingsSchema.GROUPED_DOTS, false)
            .putBoolean(SettingsSchema.CAPSULE_DUAL, false)
            .putBoolean(SettingsSchema.DUO_DUAL, true)
            .putBoolean(SettingsSchema.COLOUR, true)
            .putInt(SettingsSchema.CHARGE_COLOR, -0xc636c0)
            .putInt(SettingsSchema.LOW_COLOR, -0xd9da)
            .putInt(SettingsSchema.SAVER_COLOR, -0x60e4)
            .putInt(SettingsSchema.PERFORMANCE_COLOR, -0xc97b10)
            .putInt(SettingsSchema.LOW_BATTERY_THRESHOLD, 20)
            .putBoolean(SettingsSchema.LOCKSCREEN, true)
            .putBoolean(SettingsSchema.AOD, true)
            .putBoolean(SettingsSchema.KEEP_MERGED_SHADE, false)
            .putBoolean(SettingsSchema.STACKED_SHADE, true)
            .putInt(SettingsSchema.BATTERY_NUMBER_PLACEMENT, BatteryNumberPlacement.INSIDE.value)
            .putString(SettingsSchema.TECHNOLOGY_TEXT, "")
            .putString(
                StyleSettings.Companion.profileKey(DualStyle.LAYERED),
                "v1|27|100|100|100|115|120|0|100|100|0"
            )
            .putString(
                StyleSettings.Companion.profileKey(DualStyle.GROUPED),
                "v1|27|100|100|100|115|130|0|100|135|0"
            )
            .putString(
                StyleSettings.Companion.profileKey(DualStyle.CAPSULE),
                "v1|22|135|120|110|130|135|0|100|130|0"
            )
            .putString(
                StyleSettings.Companion.profileKey(DualStyle.DUO),
                "v1|27|100|95|110|90|130|1|100|115|0"
            )
            .putBoolean("light_theme", false)
            .putBoolean("preview_dark", true)
            .putString("scene_state", "2;91,0,0,0,1,3,1,4,4,1,1,1,1,0,5G,0,1")
            .apply()
    }
}
