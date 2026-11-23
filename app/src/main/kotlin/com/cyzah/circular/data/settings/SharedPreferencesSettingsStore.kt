package com.cyzah.circular.data.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.cyzah.circular.domain.settings.ObservableSettingsValues
import com.cyzah.circular.domain.settings.StyleSettings

/** Android persistence/listener adapter; style and preview policy stay platform-free.  */
class SharedPreferencesSettingsStore(private val preferences: SharedPreferences) : StyleSettings.Store,
    ObservableSettingsValues {
    private var listener: OnSharedPreferenceChangeListener? = null

    override fun contains(key: String?): Boolean {
        return preferences.contains(key)
    }

    override fun getInt(key: String?, fallback: Int): Int {
        return preferences.getInt(key, fallback)
    }

    override fun getBoolean(key: String?, fallback: Boolean): Boolean {
        return preferences.getBoolean(key, fallback)
    }

    override fun getString(key: String?, fallback: String?): String? {
        return preferences.getString(key, fallback)
    }

    override fun edit(): StyleSettings.Editor {
        val editor = preferences.edit()
        return object : StyleSettings.Editor {
            override fun putInt(key: String?, value: Int): StyleSettings.Editor {
                editor.putInt(key, value)
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): StyleSettings.Editor {
                editor.putBoolean(key, value)
                return this
            }

            override fun putString(key: String?, value: String?): StyleSettings.Editor {
                editor.putString(key, value)
                return this
            }

            override fun apply() {
                editor.apply()
            }
        }
    }

    override fun register(changed: ObservableSettingsValues.Listener) {
        if (listener != null) {
            return
        }
        listener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
                changed.onChanged(key)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregister() {
        if (listener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            listener = null
        }
    }
}
