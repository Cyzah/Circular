package com.cyzah.circular.data.settings

import android.content.SharedPreferences
import com.cyzah.circular.domain.settings.SettingsRowSerializer
import com.cyzah.circular.domain.settings.SettingsValues

object SettingsCursorRow {
    @JvmStatic fun create(prefs: SharedPreferences): Array<Any?>? {
        return SettingsRowSerializer.create(values(prefs))
    }

    @JvmStatic fun values(prefs: SharedPreferences): SettingsValues {
        return object : SettingsValues {
            override fun contains(key: String?): Boolean {
                return prefs.contains(key)
            }

            override fun getInt(key: String?, fallback: Int): Int {
                return prefs.getInt(key, fallback)
            }

            override fun getBoolean(key: String?, fallback: Boolean): Boolean {
                return prefs.getBoolean(key, fallback)
            }

            override fun getString(key: String?, fallback: String?): String? {
                return prefs.getString(key, fallback)
            }
        }
    }
}
