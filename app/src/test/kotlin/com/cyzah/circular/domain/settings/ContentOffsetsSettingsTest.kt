package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.ContentOffsetsSettingsTest
import com.cyzah.circular.domain.settings.StyleSettingsTest.MemoryStore
import java.util.HashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class ContentOffsetsSettingsTest {
    @Test
    fun eachOffsetOnlyChangeInvalidatesTheAppearanceSnapshot() {
        val before: SettingsSnapshot = SettingsSnapshot.Companion.from(Values())
        for (key in OFFSET_KEYS) {
            val changed = Values()
            changed.values.put(key, 7)
            Assert.assertFalse(key, before.sameAppearanceAs(SettingsSnapshot.Companion.from(changed)))
        }
    }

    @Test
    fun expandedControlCenterPreservesUserAppearanceAcrossEveryStyle() {
        for (style in DualStyle.entries) {
            val values = Values()
            values.values.put("dual_dots_grouped", style == DualStyle.GROUPED)
            values.values.put("capsule_dual", style == DualStyle.CAPSULE)
            values.values.put("duo_dual", style == DualStyle.DUO)
            values.values.put("weight_v2", 135)
            values.values.put("wifi_scale", 70)
            values.values.put("wifi_weight", 125)
            values.values.put("technology_scale_v2", 140)
            values.values.put("technology_weight_v2", 150)
            values.values.put("percent_scale_v2", 75)
            values.values.put("percent_weight_v2", 160)
            values.values.put("show_percent", false)
            values.values.put("swap_contents", true)
            val source: AppearanceSettings = AppearanceSettings.Companion.from(SettingsSnapshot.Companion.from(values))
            val expanded = source.controlCenter()
            Assert.assertEquals(style.toString() + " weight", source.weight().toLong(), expanded.weight().toLong())
            Assert.assertEquals(
                style.toString() + " wifiScale",
                source.wifiScale().toLong(),
                expanded.wifiScale().toLong()
            )
            Assert.assertEquals(
                style.toString() + " wifiWeight",
                source.wifiWeight().toLong(),
                expanded.wifiWeight().toLong()
            )
            Assert.assertEquals(
                style.toString() + " technologyScale",
                source.technologyScale().toLong(),
                expanded.technologyScale().toLong()
            )
            Assert.assertEquals(
                style.toString() + " technologyWeight",
                source.technologyWeight().toLong(),
                expanded.technologyWeight().toLong()
            )
            Assert.assertEquals(
                style.toString() + " percentScale",
                source.percentScale().toLong(),
                expanded.percentScale().toLong()
            )
            Assert.assertEquals(
                style.toString() + " percentWeight",
                source.percentWeight().toLong(),
                expanded.percentWeight().toLong()
            )
            Assert.assertEquals(style.toString() + " showPercent", source.showPercent(), expanded.showPercent())
            Assert.assertEquals(style.toString() + " swapContents", source.swapContents(), expanded.swapContents())
            Assert.assertEquals(style.toString() + " native battery size", 27, expanded.batterySize().toLong())
        }
    }

    @Test
    fun valuesDefaultToZeroAndClampEveryAxisAtTwentyFourPixels() {
        val empty: ContentOffsets = SettingsSnapshot.Companion.from(Values()).contentOffsets()
        Assert.assertEquals(ContentOffsets.DEFAULT, empty)
        Assert.assertEquals(ContentOffsets.DEFAULT.hashCode().toLong(), empty.hashCode().toLong())
        val clamped = ContentOffsets(
            Int.MIN_VALUE, Int.MAX_VALUE,
            -19, 49, -99, 99
        )
        Assert.assertEquals(-48, clamped.topX.toLong())
        Assert.assertEquals(48, clamped.topY.toLong())
        Assert.assertEquals(-19, clamped.centerX.toLong())
        Assert.assertEquals(48, clamped.centerY.toLong())
        Assert.assertEquals(-48, clamped.bottomX.toLong())
        Assert.assertEquals(48, clamped.bottomY.toLong())
        Assert.assertEquals(-48, ContentOffsets(0, -49, 0, -99, 0, Int.MIN_VALUE).bottomY.toLong())
    }

    @Test
    fun legacyPixelsMigrateWithoutDoublingPhysicalDisplacement() {
        val values = Values()
        val raw = intArrayOf(-99, -12, 23, 9, 20, 99)
        for (i in LEGACY_KEYS.indices) values.values.put(LEGACY_KEYS[i], raw[i])
        val migrated: ContentOffsets = SettingsSnapshot.Companion.from(values).contentOffsets()
        Assert.assertEquals(-48, migrated.topX.toLong())
        Assert.assertEquals(-24, migrated.topY.toLong())
        Assert.assertEquals(46, migrated.centerX.toLong())
        Assert.assertEquals(18, migrated.centerY.toLong())
        Assert.assertEquals(40, migrated.bottomX.toLong())
        Assert.assertEquals(48, migrated.bottomY.toLong())
        values.values.put(LEGACY_KEYS[0], Int.MAX_VALUE)
        values.values.put(LEGACY_KEYS[1], Int.MIN_VALUE)
        Assert.assertEquals(48, SettingsSnapshot.Companion.from(values).contentOffsets().topX.toLong())
        Assert.assertEquals(-48, SettingsSnapshot.Companion.from(values).contentOffsets().topY.toLong())
    }

    @Test
    fun canonicalHalfPixelsOverrideLegacyValuesIncludingZero() {
        val values = Values()
        for (key in LEGACY_KEYS) values.values.put(key, 8)
        val raw = intArrayOf(0, -47, 99, 49, -99, -49)
        for (i in OFFSET_KEYS.indices) values.values.put(OFFSET_KEYS[i], raw[i])
        val effective: ContentOffsets = SettingsSnapshot.Companion.from(values).contentOffsets()
        Assert.assertEquals(0, effective.topX.toLong())
        Assert.assertEquals(-47, effective.topY.toLong())
        Assert.assertEquals(48, effective.centerX.toLong())
        Assert.assertEquals(48, effective.centerY.toLong())
        Assert.assertEquals(-48, effective.bottomX.toLong())
        Assert.assertEquals(-48, effective.bottomY.toLong())
    }

    @Test
    fun halfPixelsAppendAfterAllLegacyColumnsAndRoundTripPrecisely() {
        val values = Values()
        val raw = intArrayOf(-47, -47, 43, 9, 47, 48)
        val legacyPixels = intArrayOf(-23, -23, 21, 4, 23, 24)
        for (i in OFFSET_KEYS.indices) values.values.put(OFFSET_KEYS[i], raw[i])
        val columns = SettingsSchema.CURSOR_COLUMNS
        Assert.assertEquals(46, columns.size.toLong())
        Assert.assertArrayEquals(
            arrayOf<String>(
                "mode", "size", "colour", "weight", "lockscreen", "aod",
                "diagnostic_request", "low_battery_threshold", "show_percent", "wifi_scale",
                "technology_scale", "percent_scale", "percent_weight_link", "percent_scale_v2",
                "charge_color", "low_color", "saver_color", "performance_color", "percent_weight",
                "swap_contents", "percent_weight_v2", "wifi_weight", "technology_weight", "weight_v2",
                "technology_scale_v2", "technology_weight_v2", "keep_merged_shade", "stacked_shade",
                "dual_dots_grouped", "duo_dual", "capsule_dual", "technology_text",
                "battery_number_placement", "style_profile_duo",
                "top_content_offset_x_px", "top_content_offset_y_px",
                "center_content_offset_x_px", "center_content_offset_y_px",
                "bottom_content_offset_x_px", "bottom_content_offset_y_px"
            ),
            columns.copyOf(40)
        )
        Assert.assertEquals("style_profile_duo", columns[33])
        val row = SettingsRowSerializer.create(values)!!
        val restored = Values()
        for (i in columns.indices) restored.values.put(columns[i], row[i])
        for (i in OFFSET_KEYS.indices) {
            Assert.assertEquals(LEGACY_KEYS[i], columns[34 + i])
            Assert.assertEquals("integer compatibility aliases truncate toward zero", legacyPixels[i], row[34 + i])
            Assert.assertEquals(OFFSET_KEYS[i], columns[40 + i])
            Assert.assertEquals(raw[i], row[40 + i])
            val definition = SettingsSchema.DEFINITIONS.get(40 + i)
            Assert.assertEquals(SettingsSchema.Kind.INTEGER, definition.kind())
            Assert.assertEquals(OFFSET_KEYS[i], definition.preferenceKey())
        }
        val decoded: ContentOffsets = SettingsSnapshot.Companion.from(restored).contentOffsets()
        Assert.assertEquals(-47, decoded.topX.toLong())
        Assert.assertEquals(-47, decoded.topY.toLong())
        Assert.assertEquals(43, decoded.centerX.toLong())
        Assert.assertEquals(9, decoded.centerY.toLong())
        Assert.assertEquals(47, decoded.bottomX.toLong())
        Assert.assertEquals(48, decoded.bottomY.toLong())
    }

    @Test
    fun v3ProfilesPersistHalfPixelsAndV1ProfilesRestoreZeroOffsets() {
        val offsets = ContentOffsets(-7, 9, 3, -2, 11, -13)
        val profile = StyleProfile(
            25, 130, 105, 115, 120, 125,
            true, 90, 150, true, offsets
        )
        Assert.assertTrue(profile.encode().startsWith("v3|"))
        Assert.assertEquals(17, profile.encode().split("\\|".toRegex()).toTypedArray().size.toLong())
        Assert.assertTrue(profile.sameAs(StyleProfile.Companion.decode(profile.encode())))
        Assert.assertFalse(
            profile.sameAs(
                StyleProfile(
                    25, 130, 105, 115, 120, 125,
                    true, 90, 150, true
                )
            )
        )
        val legacy: StyleProfile? = StyleProfile.Companion.decode("v1|25|130|105|115|120|125|1|90|150|1")
        Assert.assertNotNull(legacy)
        Assert.assertEquals(ContentOffsets.DEFAULT, legacy!!.contentOffsets)
        Assert.assertEquals(
            ContentOffsets.DEFAULT, StyleProfile(
                25, 130, 105, 115, 120, 125,
                true, 90, 150, true
            ).contentOffsets
        )
        Assert.assertNull(StyleProfile.Companion.decode("v2|25|130|105|115|120|125|1|90|150|1|0|0|0|0|0"))
        Assert.assertNull(StyleProfile.Companion.decode("v2|25|130|105|115|120|125|1|90|150|1|0|0|x|0|0|0"))
        Assert.assertNull(StyleProfile.Companion.decode("v3|25|130|105|115|120|125|1|90|150|1|0|0|x|0|0|0"))
    }

    @Test
    fun v2ProfilesMigrateIntegerPixelsWhileV3ProfilesReadHalfPixels() {
        val values = "|25|130|105|115|120|125|1|90|150|1|-7|24|12|-2|-24|-13"
        val old: StyleProfile? = StyleProfile.Companion.decode("v2" + values)
        val current: StyleProfile? = StyleProfile.Companion.decode("v3" + values)
        Assert.assertNotNull(old)
        Assert.assertNotNull(current)
        Assert.assertEquals("v3|25|130|105|115|120|125|1|90|150|1|-14|48|24|-4|-48|-26", old!!.encode())
        Assert.assertEquals(-24, current!!.contentOffsets.bottomX.toLong())
    }

    @Test
    fun styleChangesArchiveAndRestoreOffsetsIncludingNormalizedDuoProfiles() {
        val store = MemoryStore()
        val settings = StyleSettings(store)
        for (i in OFFSET_KEYS.indices) store.values.put(OFFSET_KEYS[i], i - 3)
        val layered = settings.currentProfile().contentOffsets
        Assert.assertTrue(settings.select(DualStyle.CAPSULE, true))
        Assert.assertEquals(ContentOffsets.DEFAULT, settings.currentProfile().contentOffsets)
        val saved = intArrayOf(-47, 11, 47, 13, 45, 15)
        for (i in OFFSET_KEYS.indices) store.values.put(OFFSET_KEYS[i], saved[i])
        val capsule = settings.currentProfile().contentOffsets
        Assert.assertTrue(settings.select(DualStyle.LAYERED, true))
        Assert.assertEquals(layered, settings.currentProfile().contentOffsets)
        Assert.assertEquals(-3, store.values.get(OFFSET_KEYS[0]))
        Assert.assertEquals(-1, store.values.get(LEGACY_KEYS[0]))
        Assert.assertTrue(settings.select(DualStyle.CAPSULE, true))
        Assert.assertEquals(capsule, settings.currentProfile().contentOffsets)
        Assert.assertEquals(-47, store.values.get(OFFSET_KEYS[0]))
        Assert.assertEquals(47, store.values.get(OFFSET_KEYS[2]))
        Assert.assertEquals(45, store.values.get(OFFSET_KEYS[4]))
        store.values.put(
            "style_profile_duo", StyleProfile(
                60, 135, 120, 110, 115, 130,
                true, 80, 145, true, capsule
            ).encode()
        )
        Assert.assertTrue(settings.select(DualStyle.DUO, true))
        Assert.assertEquals(29, settings.currentProfile().size.toLong())
        Assert.assertFalse(settings.currentProfile().swapContents)
        Assert.assertEquals(capsule, settings.currentProfile().contentOffsets)
        store.values.put("style_profile_layered", "v1|24|100|100|100|100|100|0|100|100|0")
        Assert.assertTrue(settings.select(DualStyle.LAYERED, true))
        Assert.assertEquals(ContentOffsets.DEFAULT, settings.currentProfile().contentOffsets)
    }

    @Test
    fun expandedNativeLayoutDoesNotReceiveMergedSlotOffsets() {
        val values = Values()
        for (i in OFFSET_KEYS.indices) values.values.put(OFFSET_KEYS[i], i + 1)
        val appearance: AppearanceSettings = AppearanceSettings.Companion.from(SettingsSnapshot.Companion.from(values))
        Assert.assertEquals(ContentOffsets(1, 2, 3, 4, 5, 6), appearance.contentOffsets())
        Assert.assertEquals(ContentOffsets.DEFAULT, appearance.controlCenter().contentOffsets())
        values.values.put("keep_merged_shade", true)
        val merged: AppearanceSettings = AppearanceSettings.Companion.from(SettingsSnapshot.Companion.from(values))
        Assert.assertSame(merged, merged.controlCenterAppearance())
        Assert.assertEquals(
            ContentOffsets(1, 2, 3, 4, 5, 6),
            merged.controlCenterAppearance().contentOffsets()
        )
    }

    private class Values : SettingsValues {
        @JvmField val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
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

    companion object {
        private val LEGACY_KEYS = arrayOf<String?>(
            "top_content_offset_x_px", "top_content_offset_y_px",
            "center_content_offset_x_px", "center_content_offset_y_px",
            "bottom_content_offset_x_px", "bottom_content_offset_y_px"
        )
        private val OFFSET_KEYS = arrayOf<String?>(
            "top_content_offset_x_half_px", "top_content_offset_y_half_px",
            "center_content_offset_x_half_px", "center_content_offset_y_half_px",
            "bottom_content_offset_x_half_px", "bottom_content_offset_y_half_px"
        )
    }
}
