package com.cyzah.circular.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.cyzah.circular.BuildConfig
import com.cyzah.circular.Version
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues

/** Last provider-confirmed appearance in the current SystemUI user's own storage.  */
object StartupAppearanceCache {
    private const val LEGACY_APPLICATION_ID = "com.cyzah.circular"
    private const val APPEARANCE = "circluar_startup_appearance"
    private const val TOPOLOGY = "circluar_startup_topology"

    /** Caller must still validate this process's runtime hook compatibility.  */
    @JvmStatic fun restore(context: Context?): SettingsSnapshot? {
        if (context == null) {
            return null
        }
        try {
            val values = StartupAppearanceCodec.restoreAppearance(
                preferences(context, APPEARANCE).getAll(), Version.CODE
            )
            return if (values == null) null else SettingsSnapshot.Companion.from(values)
        } catch (unavailable: RuntimeException) {
            return null
        }
    }

    /** Invoke on the configuration worker before closing its successful provider cursor.  */
    @JvmStatic fun save(context: Context?, values: SettingsValues?, compatible: Boolean) {
        if (context == null || values == null) {
            return
        }
        try {
            val record = StartupAppearanceCodec.appearance(
                values, Version.CODE, compatible
            )
            val preferences = preferences(context, APPEARANCE)
            if (record != preferences.getAll()) {
                // A completed save survives an immediate SystemUI restart.
                write(preferences, record).commit()
            }
        } catch (unavailable: RuntimeException) {
            // A cache failure must not invalidate the freshly read provider settings.
        }
    }

    /** Seed shape only, before live network callbacks; no measured state is restored.  */
    @JvmStatic fun restoreTopology(context: Context?, state: IconState?): Boolean {
        if (context == null) {
            return false
        }
        try {
            return StartupAppearanceCodec.restoreTopology(
                preferences(context, TOPOLOGY).getAll(), Version.CODE, state
            )
        } catch (unavailable: RuntimeException) {
            return false
        }
    }

    /** Call after a complete live subscription refresh, never during source teardown.  */
    @JvmStatic fun saveTopology(context: Context?, state: IconState?) {
        if (context == null) {
            return
        }
        try {
            val record = StartupAppearanceCodec.topology(state, Version.CODE)
            if (record.isEmpty()) {
                return
            }
            val preferences = preferences(context, TOPOLOGY)
            if (record != preferences.getAll()) {
                write(preferences, record).apply()
            }
        } catch (unavailable: RuntimeException) {
            // Topology is only a startup hint; live callbacks remain authoritative.
        }
    }

    internal fun preferenceName(name: String): String =
        if (BuildConfig.APPLICATION_ID == LEGACY_APPLICATION_ID) name else "${BuildConfig.APPLICATION_ID}.$name"

    private fun preferences(context: Context, name: String): SharedPreferences {
        return context.createDeviceProtectedStorageContext()
            .getSharedPreferences(preferenceName(name), Context.MODE_PRIVATE)
    }

    private fun write(
        preferences: SharedPreferences,
        record: MutableMap<String?, Any?>
    ): SharedPreferences.Editor {
        val editor = preferences.edit().clear()
        for (entry in record.entries) {
            val value = entry.value
            if (value is Int) {
                editor.putInt(entry.key, value)
            } else if (value is Boolean) {
                editor.putBoolean(entry.key, value)
            } else if (value is String) {
                editor.putString(entry.key, value)
            }
        }
        return editor
    }
}
