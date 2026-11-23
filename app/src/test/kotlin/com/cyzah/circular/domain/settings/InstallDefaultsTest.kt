package com.cyzah.circular.domain.settings

import android.content.SharedPreferences
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.data.settings.SettingsCursorRow
import com.cyzah.circular.data.settings.SharedPreferencesSettingsStore
import com.cyzah.circular.data.settings.SharedPreferencesSettingsStoreTest
import com.cyzah.circular.domain.settings.InstallDefaultsTest
import com.cyzah.circular.ui.preview.PreviewSceneCodec
import java.util.HashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class InstallDefaultsTest {
    @Test
    fun emptyInstallPublishesCapturedDuoAppearanceThroughProviderRow() {
        val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()

        InstallDefaults.applyIfEmpty(preferences.proxy)

        Assert.assertEquals(1, preferences.applies.toLong())
        val snapshot: SettingsSnapshot = snapshot(preferences.proxy)
        Assert.assertEquals(DualStyle.DUO, snapshot.dualStyle())
        Assert.assertEquals(2, snapshot.mode().toLong())
        Assert.assertEquals(27, snapshot.size().toLong())
        Assert.assertEquals(27, snapshot.batterySize().toLong())
        Assert.assertEquals(95, snapshot.wifiScale().toLong())
        Assert.assertEquals(90, snapshot.technologyScale().toLong())
        Assert.assertEquals(130, snapshot.technologyWeight().toLong())
        Assert.assertTrue(snapshot.showPercent())
        Assert.assertEquals(115, snapshot.percentWeight().toLong())
        Assert.assertFalse(snapshot.swapContents())
        Assert.assertEquals(BatteryNumberPlacement.INSIDE, snapshot.batteryNumberPlacement())
        Assert.assertEquals("", snapshot.diagnosticRequest())
        Assert.assertFalse(preferences.values.containsKey("report"))
        Assert.assertFalse(preferences.values.containsKey("report_time"))
        Assert.assertFalse(preferences.values.containsKey("report_token"))
    }

    @Test
    fun initialPreviewRestoresCapturedThemeAndScene() {
        val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()
        InstallDefaults.applyIfEmpty(preferences.proxy)

        Assert.assertFalse(preferences.proxy.getBoolean("light_theme", true))
        Assert.assertTrue(preferences.proxy.getBoolean("preview_dark", false))
        val state = IconState()
        val encoded = preferences.proxy.getString("scene_state", "")
        val scene = PreviewSceneCodec.decode(encoded, state)
        Assert.assertNotNull(scene)
        Assert.assertEquals(91, state.battery.toLong())
        Assert.assertEquals(3, state.wifiLevel.toLong())
        Assert.assertEquals(4, state.secondaryLevel.toLong())
        Assert.assertTrue(state.secondarySimPresent)
        Assert.assertEquals("5G", state.mobileTechnology)
        Assert.assertEquals(0.0f, scene!!.expansion, 0.0f)
        Assert.assertTrue(scene.expanded)
        Assert.assertEquals(encoded, PreviewSceneCodec.encode(state, scene.expansion, scene.expanded))
    }

    @Test
    fun seededStyleArchivesRestoreIndependentCapturedAppearances() {
        val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()
        InstallDefaults.applyIfEmpty(preferences.proxy)
        val styles = StyleSettings(SharedPreferencesSettingsStore(preferences.proxy))
        Assert.assertEquals(
            "v3|27|100|95|110|90|130|1|100|115|0|0|0|0|0|0|0",
            styles.currentProfile().encode()
        )

        val targets = arrayOf(
            DualStyle.LAYERED, DualStyle.GROUPED, DualStyle.CAPSULE, DualStyle.DUO
        )
        val appearances = arrayOf<String?>(
            "v3|27|100|100|100|115|120|0|100|100|0|0|0|0|0|0|0",
            "v3|27|100|100|100|115|130|0|100|135|0|0|0|0|0|0|0",
            "v3|22|135|120|110|130|135|0|100|130|0|0|0|0|0|0|0",
            "v3|27|100|95|110|90|130|1|100|115|0|0|0|0|0|0|0"
        )
        for (i in targets.indices) {
            Assert.assertTrue(styles.select(targets[i], true))
            Assert.assertEquals(targets[i], snapshot(preferences.proxy).dualStyle())
            Assert.assertEquals(appearances[i], styles.currentProfile().encode())
        }
    }

    @Test
    fun laterInitializationDoesNotWriteOrRevertCustomChanges() {
        val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()
        InstallDefaults.applyIfEmpty(preferences.proxy)
        val installed = preferences.proxy.getAll()
        var writes = preferences.applies
        InstallDefaults.applyIfEmpty(preferences.proxy)
        Assert.assertEquals(installed, preferences.proxy.getAll())
        Assert.assertEquals(writes.toLong(), preferences.applies.toLong())

        preferences.proxy.edit().putInt("size", 28).putInt("weight_v2", 145)
            .putBoolean("colour", false).putBoolean("light_theme", true)
            .putString("technology_text", "LTE=4G+")
            .putString("style_profile_grouped", "custom archive")
            .putString("future_user_preference", "custom value").apply()
        val customized = preferences.proxy.getAll()
        writes = preferences.applies
        InstallDefaults.applyIfEmpty(preferences.proxy)
        Assert.assertEquals(customized, preferences.proxy.getAll())
        Assert.assertEquals(writes.toLong(), preferences.applies.toLong())
        val snapshot: SettingsSnapshot = snapshot(preferences.proxy)
        Assert.assertEquals(28, snapshot.size().toLong())
        Assert.assertEquals(145, snapshot.weight().toLong())
        Assert.assertFalse(snapshot.coloured())
        Assert.assertEquals("LTE=4G+", snapshot.technologyText())
    }

    @Test
    fun evenOneExistingPreferencePreventsAnyDefaultBackfill() {
        val oldPreferences = arrayOf<Array<Any?>?>(
            arrayOf<Any?>("mode", 0), arrayOf<Any?>("size", 24), arrayOf<Any?>("unknown_old_setting", "kept"),
            arrayOf<Any?>("ai_modification_notice_dismissed", false),
            arrayOf<Any?>("ai_modification_notice_install_stamp", "old install"),
            arrayOf<Any?>("report", "old diagnostic"), arrayOf<Any?>("report_time", 123L),
            arrayOf<Any?>("report_token", "old token")
        )
        for (entry in oldPreferences) {
            val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()
            preferences.values.put(entry!![0] as String?, entry[1])
            val original = preferences.proxy.getAll()

            InstallDefaults.applyIfEmpty(preferences.proxy)

            Assert.assertEquals(entry[0] as String?, original, preferences.proxy.getAll())
            Assert.assertEquals(entry[0] as String?, 0, preferences.applies.toLong())
        }
    }

    @Test
    fun oldLegacyPreferencesRemainAvailableToExistingMigration() {
        val preferences: SharedPreferencesSettingsStoreTest.Preferences = preferences()
        preferences.values.put("weight", 144)
        preferences.values.put("technology_scale", 120)
        preferences.values.put("technology_weight", 156)
        preferences.values.put("percent_scale", 130)
        preferences.values.put("percent_weight", 143)
        val original = preferences.proxy.getAll()

        InstallDefaults.applyIfEmpty(preferences.proxy)

        Assert.assertEquals(original, preferences.proxy.getAll())
        Assert.assertEquals(0, preferences.applies.toLong())
        val migrated = AppearanceScale.migration(preferences.proxy.getAll())
        Assert.assertEquals(120, migrated.get("weight_v2"))
        Assert.assertEquals(144, migrated.get("wifi_weight"))
        Assert.assertEquals(100, migrated.get("technology_scale_v2"))
        Assert.assertEquals(130, migrated.get("technology_weight_v2"))
        Assert.assertFalse(preferences.proxy.contains("percent_weight_v2"))
        val values = SettingsCursorRow.values(preferences.proxy)
        Assert.assertEquals(100, SettingsRowSerializer.percentScale(values).toLong())
        Assert.assertEquals(110, SettingsRowSerializer.percentWeight(values).toLong())
    }

    companion object {
        private fun preferences(): SharedPreferencesSettingsStoreTest.Preferences {
            return SharedPreferencesSettingsStoreTest.Preferences()
        }

        /** The provider's cursor transports preference booleans as integers.  */
        private fun snapshot(preferences: SharedPreferences): SettingsSnapshot {
            val row = SettingsCursorRow.create(preferences)!!
            val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            for (i in SettingsSchema.CURSOR_COLUMNS.indices) {
                values.put(SettingsSchema.CURSOR_COLUMNS[i], row[i])
            }
            return SettingsSnapshot.Companion.from(object : SettingsValues {
                override fun contains(key: String?): Boolean {
                    return values.containsKey(key)
                }

                override fun getInt(key: String?, fallback: Int): Int {
                    val value = values.get(key)
                    return if (value is Int) value else fallback
                }

                override fun getBoolean(key: String?, fallback: Boolean): Boolean {
                    val value = values.get(key)
                    return if (value is Int) value != 0 else fallback
                }

                override fun getString(key: String?, fallback: String?): String? {
                    val value = values.get(key)
                    return if (value is String) value else fallback
                }
            })
        }
    }
}
