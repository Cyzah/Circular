package com.cyzah.circular.domain.settings

import java.util.HashMap
import org.junit.Assert
import org.junit.Test

class SettingsSnapshotTest {
    @Test
    fun fontSelectionDefaultsSafelyAndInvalidatesAppearanceWhenReplaced() {
        val values = HashMap<String?, Any?>()
        val original = SettingsSnapshot.from(MapValues(values))
        Assert.assertEquals(StatusFontSelection.DEFAULT, original.statusFont())
        values["status_font_source"] = "custom:../../settings"
        Assert.assertTrue(original.sameAppearanceAs(SettingsSnapshot.from(MapValues(values))))
        values["status_font_source"] = "system"
        val system = SettingsSnapshot.from(MapValues(values))
        Assert.assertEquals(StatusFontSelection.SYSTEM, system.statusFont())
        Assert.assertFalse(original.sameAppearanceAs(system))
        values["status_font_source"] = "custom:${"a".repeat(32)}"
        val custom = SettingsSnapshot.from(MapValues(values))
        Assert.assertEquals(StatusFontMode.CUSTOM, custom.statusFont().mode)
        Assert.assertEquals(values["status_font_source"], custom.statusFont().encode())
        values["status_font_source"] = "custom:${"b".repeat(32)}"
        Assert.assertFalse(custom.sameAppearanceAs(SettingsSnapshot.from(MapValues(values))))
    }

    @Test
    fun defaultsAreClampedAndStable() {
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(HashMap<String?, Any?>()))

        Assert.assertEquals(0, snapshot.mode().toLong())
        Assert.assertEquals(24, snapshot.size().toLong())
        Assert.assertEquals(26, snapshot.batterySize().toLong())
        Assert.assertEquals(100, snapshot.weight().toLong())
        Assert.assertTrue(snapshot.coloured())
        Assert.assertFalse(snapshot.showPercent())
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            snapshot.batteryNumberPlacement()
        )
    }

    @Test
    fun invalidNumericValuesUsePolicyBounds() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("size", 999)
        values.put("weight_v2", 1)
        values.put("wifi_scale", -10)

        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        Assert.assertEquals(29, snapshot.size().toLong())
        Assert.assertEquals(80, snapshot.weight().toLong())
        Assert.assertEquals(50, snapshot.wifiScale().toLong())
    }

    @Test
    fun legacyWeightMigratesWhenV2IsAbsent() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("weight", 100)

        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        Assert.assertEquals(AppearanceScale.weightFromLegacy(100).toLong(), snapshot.weight().toLong())
    }

    @Test
    fun singleDualStyleFlagIsSelected() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("dual_dots_grouped", true)
        values.put("capsule_dual", true)
        values.put("duo_dual", true)

        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        Assert.assertEquals(DualStyle.DUO, snapshot.dualStyle())
        Assert.assertFalse(snapshot.groupedDots())
        Assert.assertFalse(snapshot.capsuleDual())
        Assert.assertTrue(snapshot.duoDual())
    }

    @Test
    fun batteryNumberPlacementDefaultsAndClamps() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("battery_number_placement", 1)

        Assert.assertEquals(
            BatteryNumberPlacement.INSIDE,
            SettingsSnapshot.Companion.from(MapValues(values))
                .batteryNumberPlacement()
        )

        values.put("battery_number_placement", -40)
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            SettingsSnapshot.Companion.from(MapValues(values))
                .batteryNumberPlacement()
        )
    }

    @Test
    fun batteryRenderingUsesDuoSizeWhenCapsuleIsActive() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("size", 22)
        values.put(
            "style_profile_duo",
            "v1|27|100|95|110|95|110|1|100|140|0"
        )

        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        Assert.assertEquals(22, snapshot.size().toLong())
        Assert.assertEquals(27, snapshot.batterySize().toLong())
    }

    @Test
    fun activeDuoUsesItsCurrentSizeEvenBeforeTheProfileIsSaved() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("duo_dual", true)
        values.put("size", 29)
        values.put(
            "style_profile_duo",
            "v1|27|100|95|110|95|110|1|100|140|0"
        )

        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        Assert.assertEquals(29, snapshot.size().toLong())
        Assert.assertEquals(29, snapshot.batterySize().toLong())
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
}
