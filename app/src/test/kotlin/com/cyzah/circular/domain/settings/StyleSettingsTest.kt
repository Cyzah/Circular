package com.cyzah.circular.domain.settings

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.domain.settings.StyleSettingsTest.MemoryStore
import java.util.HashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class StyleSettingsTest {
    @Test
    fun unavailableSimSavesLatestDualAppearanceAndRestoresLayered() {
        for (style in arrayOf<DualStyle>(DualStyle.DUO, DualStyle.CAPSULE)) {
            val store = MemoryStore()
            val settings = StyleSettings(store)
            store.values.put("size", 21)
            store.values.put("weight_v2", 125)
            store.values.put("wifi_scale", 105)
            val layered = settings.currentProfile()
            Assert.assertTrue(settings.select(style, true))
            store.values.put("size", 28)
            store.values.put("weight_v2", 165)
            store.values.put("wifi_scale", 135)
            store.values.put("percent_weight_v2", 150)
            val edited = settings.currentProfile()

            val writes = store.writes
            Assert.assertTrue(settings.ensureAvailable(false))
            Assert.assertEquals((writes + 1).toLong(), store.writes.toLong())
            Assert.assertEquals(DualStyle.LAYERED, settings.selected())
            Assert.assertTrue(layered.sameAs(settings.currentProfile()))
            Assert.assertTrue(
                edited.sameAs(
                    StyleProfile.Companion.decode(
                        store.getString(
                            StyleSettings.Companion.profileKey(style), ""
                        )
                    )
                )
            )

            Assert.assertFalse(settings.select(style, false))
            Assert.assertEquals((writes + 1).toLong(), store.writes.toLong())
            Assert.assertTrue(settings.select(style, true))
            Assert.assertTrue(edited.sameAs(settings.currentProfile()))
            Assert.assertTrue(settings.select(DualStyle.LAYERED, true))
            Assert.assertTrue(layered.sameAs(settings.currentProfile()))
        }
    }

    @Test
    fun missingOrMalformedArchivesKeepExistingDefaultsAndSourceFallback() {
        val store = MemoryStore()
        val settings = StyleSettings(store)
        Assert.assertTrue(settings.select(DualStyle.CAPSULE, true))
        Assert.assertTrue(StyleProfile.Companion.defaults(DualStyle.CAPSULE)!!.sameAs(settings.currentProfile()))
        store.values.put("style_profile_duo", "bad archive")
        Assert.assertTrue(settings.select(DualStyle.DUO, true))
        val duo: StyleProfile? = StyleProfile.Companion.defaults(DualStyle.DUO)
        Assert.assertTrue(duo!!.sameAs(settings.currentProfile()))
        Assert.assertTrue(settings.select(DualStyle.GROUPED, true))
        Assert.assertTrue(duo.sameAs(settings.currentProfile()))
        Assert.assertFalse(settings.select(DualStyle.GROUPED, true))
    }

    @Test
    fun transitionNormalizesOnlySizeAndDuoSwapAndWritesLegacyAliases() {
        val store = MemoryStore()
        val settings = StyleSettings(store)
        val archived = StyleProfile(
            60, 135, 120, 110, 115, 130,
            true, 80, 145, true
        )
        store.values.put("style_profile_duo", archived.encode())
        Assert.assertTrue(settings.select(DualStyle.DUO, true))
        val actual = settings.currentProfile()
        Assert.assertEquals(29, actual.size.toLong())
        Assert.assertFalse(actual.swapContents)
        Assert.assertEquals(135, actual.weight.toLong())
        Assert.assertEquals(115, actual.technologyScale.toLong())
        Assert.assertEquals(AppearanceScale.weightToLegacy(135).toLong(), store.getInt("weight", -1).toLong())
        Assert.assertEquals(AppearanceScale.sizeToLegacy(115).toLong(), store.getInt("technology_scale", -1).toLong())
        Assert.assertEquals(
            AppearanceScale.weightToLegacy(130).toLong(),
            store.getInt("technology_weight", -1).toLong()
        )
        Assert.assertEquals(BatteryHeader.weightToLegacy(145).toLong(), store.getInt("percent_weight", -1).toLong())
        Assert.assertEquals(1, store.writes.toLong())
    }

    @Test
    fun sourceProfileUsesExistingProviderConversionRules() {
        val store = MemoryStore()
        store.values.put("weight", 125)
        store.values.put("percent_scale", 140)
        store.values.put("percent_weight", 120)
        val actual = StyleSettings(store).currentProfile()
        Assert.assertEquals(100, actual.weight.toLong())
        Assert.assertEquals(125, actual.wifiWeight.toLong())
        Assert.assertEquals(100, actual.technologyScale.toLong())
        Assert.assertEquals(100, actual.technologyWeight.toLong())
        Assert.assertEquals(BatteryHeader.fromLegacy(140).toLong(), actual.percentScale.toLong())
        Assert.assertEquals(BatteryHeader.weightFromLegacy(120).toLong(), actual.percentWeight.toLong())
    }

    @Test
    fun initializationRepairsCurrentConstraintsWithoutOverwritingArchives() {
        val store = MemoryStore()
        store.values.put("duo_dual", true)
        store.values.put("size", 12)
        store.values.put("swap_contents", true)
        store.values.put("style_profile_duo", "untouched")
        val settings = StyleSettings(store)
        Assert.assertTrue(settings.initialize(true))
        Assert.assertEquals(18, store.getInt("size", 0).toLong())
        Assert.assertFalse(store.getBoolean("swap_contents", true))
        Assert.assertEquals("untouched", store.getString("style_profile_duo", ""))
        Assert.assertFalse(settings.initialize(true))
    }

    class MemoryStore : StyleSettings.Store {
        @JvmField val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        @JvmField var writes: Int = 0

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

        override fun edit(): StyleSettings.Editor {
            return object : StyleSettings.Editor {
                private val pending: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                override fun putInt(key: String?, value: Int): StyleSettings.Editor {
                    pending.put(key, value)
                    return this
                }

                override fun putBoolean(key: String?, value: Boolean): StyleSettings.Editor {
                    pending.put(key, value)
                    return this
                }

                override fun putString(key: String?, value: String?): StyleSettings.Editor {
                    pending.put(key, value)
                    return this
                }

                override fun apply() {
                    values.putAll(pending)
                    writes++
                }
            }
        }
    }
}
