package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.SettingsRowSerializerTest
import com.cyzah.circular.domain.settings.SettingsRowSerializerTest.CursorValues
import java.util.HashMap
import org.junit.Assert
import org.junit.Test

class SettingsRowSerializerTest {
    @Test
    fun absentPreferencesKeepProviderDefaultsAndCompatibilityColumns() {
        Assert.assertArrayEquals(
            arrayOf<Any>(
                0, 24, 1, 120, 1, 1, "", 20, 0, 100, 120, 130, 0, 100,
                -0xff6000, -0xd9da, -0x5b00, -0xd07f13, 130, 0, 100, 100,
                120, 100, 100, 100, 0, 1, 0, 0, 0, "", 2, "", 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, "default"
            ), SettingsRowSerializer.create(MapValues(HashMap<String?, Any?>()))
        )
    }

    @Test
    fun distinctPreferencesProduceTheLegacyCompatibleWireRow() {
        val row = SettingsRowSerializer.create(MapValues(distinctPreferences()))!!

        // Literal wire contract catches swaps of same-type columns and legacy conversion drift.
        Assert.assertArrayEquals(
            arrayOf<Any>(
                2, 23, 0, 138, 0, 0, "request-37", 31, 1, 85, 114, 137, 0, 105,
                -0xddccbc, -0xaa9989, -0x776656, -0x443323, 180, 1, 145, 125,
                162, 115, 95, 135, 1, 0, 1, 0, 0, "LTE=4G;5G=5G+", 1,
                "v1|27|100|95|110|95|110|1|100|140|0", 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, "default"
            ), row
        )
    }

    @Test
    fun customFontSelectionSurvivesProviderAndStartupCacheRoundTrips() {
        val preferences = distinctPreferences()
        val selection = "custom:${"a".repeat(32)}"
        preferences["status_font_source"] = selection
        val wire = CursorValues(SettingsRowSerializer.create(MapValues(preferences))!!)
        val cache = com.cyzah.circular.data.settings.StartupAppearanceCodec
        val record = cache.appearance(wire, 1, true)

        Assert.assertEquals(selection, SettingsSnapshot.from(wire).statusFont().encode())
        Assert.assertEquals(selection, SettingsSnapshot.from(
            requireNotNull(cache.restoreAppearance(record, 1))
        ).statusFont().encode())
    }

    @Test
    fun serializedPreferencesRoundTripIntoTheRuntimeSnapshot() {
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(
            CursorValues(
                SettingsRowSerializer.create(MapValues(distinctPreferences()))!!
            )
        )

        Assert.assertEquals(2, snapshot.mode().toLong())
        Assert.assertEquals(23, snapshot.size().toLong())
        Assert.assertEquals(27, snapshot.batterySize().toLong())
        Assert.assertFalse(snapshot.coloured())
        Assert.assertEquals(115, snapshot.weight().toLong())
        Assert.assertEquals(125, snapshot.wifiWeight().toLong())
        Assert.assertEquals(135, snapshot.technologyWeight().toLong())
        Assert.assertEquals(85, snapshot.wifiScale().toLong())
        Assert.assertEquals(95, snapshot.technologyScale().toLong())
        Assert.assertEquals(105, snapshot.percentScale().toLong())
        Assert.assertEquals(145, snapshot.percentWeight().toLong())
        Assert.assertTrue(snapshot.swapContents())
        Assert.assertTrue(snapshot.keepMergedShade())
        Assert.assertFalse(snapshot.stackedShade())
        Assert.assertEquals(DualStyle.GROUPED, snapshot.dualStyle())
        Assert.assertTrue(snapshot.groupedDots())
        Assert.assertFalse(snapshot.duoDual())
        Assert.assertFalse(snapshot.capsuleDual())
        Assert.assertEquals(-0xddccbc, snapshot.chargeColor().toLong())
        Assert.assertEquals(-0xaa9989, snapshot.lowColor().toLong())
        Assert.assertEquals(-0x776656, snapshot.saverColor().toLong())
        Assert.assertEquals(-0x443323, snapshot.performanceColor().toLong())
        Assert.assertEquals(31, snapshot.lowBatteryThreshold().toLong())
        Assert.assertTrue(snapshot.showPercent())
        Assert.assertFalse(snapshot.lockscreen())
        Assert.assertFalse(snapshot.aod())
        Assert.assertEquals("request-37", snapshot.diagnosticRequest())
        Assert.assertEquals("LTE=4G;5G=5G+", snapshot.technologyText())
        Assert.assertEquals(BatteryNumberPlacement.INSIDE, snapshot.batteryNumberPlacement())
    }

    @Test
    fun legacyOnlyPercentSettingsAndWifiFallbackRetainProviderConversions() {
        val preferences: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        preferences.put("weight", 144)
        preferences.put("percent_scale", 117)
        preferences.put("percent_weight", 143)

        val wire = CursorValues(SettingsRowSerializer.create(MapValues(preferences))!!)
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(wire)

        Assert.assertEquals(100, snapshot.weight().toLong())
        Assert.assertEquals(144, snapshot.wifiWeight().toLong())
        Assert.assertEquals(100, snapshot.technologyScale().toLong())
        Assert.assertEquals(100, snapshot.technologyWeight().toLong())
        Assert.assertEquals(90, snapshot.percentScale().toLong())
        Assert.assertEquals(110, snapshot.percentWeight().toLong())
        Assert.assertEquals(117, wire.getInt("percent_scale", -1).toLong())
        Assert.assertEquals(143, wire.getInt("percent_weight", -1).toLong())
        Assert.assertEquals(0, wire.getInt("percent_weight_link", -1).toLong())
    }

    @Test
    fun rawFlagsAndDiagnosticTokenRemainWireValuesBeforeSnapshotNormalization() {
        val preferences: MutableMap<String?, Any?> = distinctPreferences()
        preferences.put("duo_dual", true)
        preferences.put("capsule_dual", true)
        val token = String(CharArray(129)).replace('\u0000', 'x')
        preferences.put("diagnostic_request", token)
        val wire = CursorValues(SettingsRowSerializer.create(MapValues(preferences))!!)

        Assert.assertTrue(wire.getBoolean("dual_dots_grouped", false))
        Assert.assertTrue(wire.getBoolean("duo_dual", false))
        Assert.assertTrue(wire.getBoolean("capsule_dual", false))
        Assert.assertEquals(token, wire.getString("diagnostic_request", ""))
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(wire)
        Assert.assertEquals(DualStyle.DUO, snapshot.dualStyle())
        Assert.assertEquals(23, snapshot.batterySize().toLong())
        Assert.assertEquals("", snapshot.diagnosticRequest())
    }

    @Test
    fun invalidPreferencesAreClampedAtTheWireBoundary() {
        val preferences: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        preferences.put("mode", -1)
        preferences.put("size", 1000)
        preferences.put("weight_v2", 1)
        preferences.put("wifi_weight", 1000)
        preferences.put("wifi_scale", -1)
        preferences.put("technology_scale_v2", 1000)
        preferences.put("technology_weight_v2", 1)
        preferences.put("percent_scale_v2", 1000)
        preferences.put("percent_weight_v2", -1)
        preferences.put("low_battery_threshold", 0)
        preferences.put("battery_number_placement", 42)
        val wire = CursorValues(SettingsRowSerializer.create(MapValues(preferences))!!)

        Assert.assertEquals(0, wire.getInt("mode", -1).toLong())
        Assert.assertEquals(29, wire.getInt("size", -1).toLong())
        Assert.assertEquals(80, wire.getInt("weight_v2", -1).toLong())
        Assert.assertEquals(180, wire.getInt("wifi_weight", -1).toLong())
        Assert.assertEquals(50, wire.getInt("wifi_scale", -1).toLong())
        Assert.assertEquals(150, wire.getInt("technology_scale_v2", -1).toLong())
        Assert.assertEquals(80, wire.getInt("technology_weight_v2", -1).toLong())
        Assert.assertEquals(160, wire.getInt("percent_scale_v2", -1).toLong())
        Assert.assertEquals(50, wire.getInt("percent_weight_v2", -1).toLong())
        Assert.assertEquals(1, wire.getInt("low_battery_threshold", -1).toLong())
        Assert.assertEquals(2, wire.getInt("battery_number_placement", -1).toLong())
    }

    private class MapValues(private val values: MutableMap<String?, Any?>) : SettingsValues {
        override fun contains(key: String?): Boolean {
            return values.containsKey(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val value = values.get(key)
            return if (value is Int) value else fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val value = values.get(key)
            return if (value is Boolean) value else fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            val value = values.get(key)
            return if (value is String) value else fallback
        }
    }

    /** Models the cursor boundary: booleans arrive as integers, not preference booleans.  */
    private class CursorValues(row: Array<Any?>) : SettingsValues {
        private val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()

        init {
            for (i in SettingsSchema.CURSOR_COLUMNS.indices) {
                values.put(SettingsSchema.CURSOR_COLUMNS[i], row[i])
            }
        }

        override fun contains(key: String?): Boolean {
            return values.containsKey(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val value = values.get(key)
            return if (value == null) fallback else value as Int
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val value = values.get(key)
            return if (value == null) fallback else value as Int != 0
        }

        override fun getString(key: String?, fallback: String?): String? {
            val value = values.get(key)
            return if (value == null) fallback else value as String
        }
    }

    companion object {
        private fun distinctPreferences(): MutableMap<String?, Any?> {
            val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            values.put("mode", 1)
            values.put("size", 23)
            values.put("colour", false)
            values.put("weight", 171)
            values.put("weight_v2", 115)
            values.put("wifi_weight", 125)
            values.put("technology_weight", 177)
            values.put("technology_weight_v2", 135)
            values.put("wifi_scale", 85)
            values.put("technology_scale", 149)
            values.put("technology_scale_v2", 95)
            values.put("percent_scale", 70)
            values.put("percent_scale_v2", 105)
            values.put("percent_weight", 55)
            values.put("percent_weight_v2", 145)
            values.put("swap_contents", true)
            values.put("keep_merged_shade", true)
            values.put("stacked_shade", false)
            values.put("dual_dots_grouped", true)
            values.put("duo_dual", false)
            values.put("capsule_dual", false)
            values.put("charge_color", 0x11223344)
            values.put("low_color", 0x22556677)
            values.put("saver_color", 0x338899AA)
            values.put("performance_color", 0x44BBCCDD)
            values.put("low_battery_threshold", 31)
            values.put("show_percent", true)
            values.put("lockscreen", false)
            values.put("aod", false)
            values.put("diagnostic_request", "request-37")
            values.put("technology_text", " LTE=4G;5G=5G+ ")
            values.put("battery_number_placement", 1)
            values.put("style_profile_duo", "v1|27|100|95|110|95|110|1|100|140|0")
            return values
        }
    }
}
