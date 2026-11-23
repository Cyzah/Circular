package com.cyzah.circular.data.settings

import com.cyzah.circular.domain.settings.StyleSettings
import java.util.HashMap
import java.util.LinkedHashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class SettingsMutationTest {
    @Test
    fun legacyPixelWritesKeepPixelSemanticsAndUpdateCanonicalAtomically() {
        for (slot in arrayOf<String>("top", "center", "bottom")) for (axis in arrayOf<String>("x", "y")) {
            val key = slot + "_content_offset_" + axis + "_px"
            val canonical = slot + "_content_offset_" + axis + "_half_px"
            val limit = 24
            val store = Store()
            SettingsMutation.Companion.integer(key, 100).apply(store, store)
            Assert.assertEquals(limit, store.values.get(key))
            Assert.assertEquals(limit * 2, store.values.get(canonical))
            Assert.assertEquals(2, store.lastTransaction!!.size.toLong())
            SettingsMutation.Companion.integer(key, -100).apply(store, store)
            Assert.assertEquals(-limit, store.values.get(key))
            Assert.assertEquals(-limit * 2, store.values.get(canonical))
            SettingsMutation.Companion.integer(key, -8).apply(store, store)
            Assert.assertEquals(-8, store.values.get(key))
            Assert.assertEquals(-16, store.values.get(canonical))
            Assert.assertEquals(3, store.publishes.toLong())
        }
    }

    @Test
    fun allHalfPixelWritesClampAtTwentyFourPixelsAndTruncateIntegerAliasesTowardZero() {
        for (slot in arrayOf<String>("top", "center", "bottom")) for (axis in arrayOf<String>("x", "y")) {
            val key = slot + "_content_offset_" + axis + "_half_px"
            val legacy = slot + "_content_offset_" + axis + "_px"
            val limit = 48
            val store = Store()
            Assert.assertTrue(SettingsMutation.Companion.integer(key, 100).apply(store, store))
            Assert.assertEquals(limit, store.values.get(key))
            Assert.assertEquals(limit / 2, store.values.get(legacy))
            Assert.assertTrue(SettingsMutation.Companion.integer(key, -100).apply(store, store))
            Assert.assertEquals(-limit, store.values.get(key))
            Assert.assertEquals(-limit / 2, store.values.get(legacy))
            for (sample in arrayOf<IntArray?>(
                intArrayOf(-47, -23),
                intArrayOf(-1, 0),
                intArrayOf(1, 0),
                intArrayOf(47, 23)
            )) {
                val halfSteps = sample!![0]
                Assert.assertTrue(SettingsMutation.Companion.integer(key, halfSteps).apply(store, store))
                Assert.assertEquals(halfSteps, store.values.get(key))
                Assert.assertEquals(sample[1], store.values.get(legacy))
                Assert.assertEquals(2, store.lastTransaction!!.size.toLong())
                Assert.assertFalse(SettingsMutation.Companion.integer(key, halfSteps).apply(store, store))
            }
            Assert.assertEquals(6, store.publishes.toLong())
        }
    }

    @Test
    fun legacyWriteReplacesAnExistingFractionAndCanonicalWriteRepairsAlias() {
        val store = Store()
        store.values.put("center_content_offset_x_half_px", 3)
        store.values.put("center_content_offset_x_px", 1)
        Assert.assertTrue(SettingsMutation.Companion.integer("center_content_offset_x_px", 1).apply(store, store))
        Assert.assertEquals(2, store.values.get("center_content_offset_x_half_px"))
        store.values.put("center_content_offset_x_px", 7)
        Assert.assertTrue(SettingsMutation.Companion.integer("center_content_offset_x_half_px", 2).apply(store, store))
        Assert.assertEquals(1, store.values.get("center_content_offset_x_px"))
        Assert.assertEquals(2, store.writes.toLong())
    }

    @Test
    fun equalCompleteTransactionDoesNotWriteOrPublish() {
        val store = Store()
        store.values.put("weight_v2", 100)
        store.values.put("weight", 120)
        Assert.assertFalse(SettingsMutation.Companion.integer("weight_v2", 100).apply(store, store))
        Assert.assertEquals(0, store.writes.toLong())
        Assert.assertEquals(0, store.publishes.toLong())
    }

    @Test
    fun equalCanonicalRepairsMissingOrIncorrectAliasAtomically() {
        for (alias in arrayOf<Any?>(null, 80)) {
            val store = Store()
            store.values.put("weight_v2", 100)
            if (alias != null) store.values.put("weight", alias)
            Assert.assertTrue(SettingsMutation.Companion.integer("weight_v2", 100).apply(store, store))
            Assert.assertEquals(1, store.writes.toLong())
            Assert.assertEquals(1, store.publishes.toLong())
            Assert.assertEquals(2, store.lastTransaction!!.size.toLong())
            Assert.assertEquals(100, store.values.get("weight_v2"))
            Assert.assertEquals(120, store.values.get("weight"))
        }
    }

    @Test
    fun missingFalseValueIsMaterializedAndOnlyThenDeduplicated() {
        val store = Store()
        Assert.assertTrue(SettingsMutation.Companion.bool("show_percent", false).apply(store, store))
        Assert.assertTrue(store.values.containsKey("show_percent"))
        Assert.assertFalse(SettingsMutation.Companion.bool("show_percent", false).apply(store, store))
        Assert.assertEquals(1, store.writes.toLong())
    }

    @Test
    fun mutuallyExclusiveTogglesShareOneTransaction() {
        val store = Store()
        store.values.put("stacked_shade", true)
        Assert.assertTrue(SettingsMutation.Companion.bool("keep_merged_shade", true).apply(store, store))
        Assert.assertEquals(2, store.lastTransaction!!.size.toLong())
        Assert.assertEquals(true, store.values.get("keep_merged_shade"))
        Assert.assertEquals(false, store.values.get("stacked_shade"))
        Assert.assertEquals(1, store.writes.toLong())
        Assert.assertFalse(SettingsMutation.Companion.bool("keep_merged_shade", true).apply(store, store))
        Assert.assertTrue(SettingsMutation.Companion.bool("stacked_shade", true).apply(store, store))
        Assert.assertEquals(false, store.values.get("keep_merged_shade"))
        Assert.assertEquals(true, store.values.get("stacked_shade"))
    }

    @Test
    fun eachScaledCanonicalPreservesItsExistingLegacyMapping() {
        val cases = arrayOf(
            arrayOf("technology_scale_v2", "technology_scale"),
            arrayOf("technology_weight_v2", "technology_weight"),
            arrayOf("percent_weight_v2", "percent_weight")
        )
        val expected = intArrayOf(120, 120, 130)
        for (index in cases.indices) {
            val store = Store()
            SettingsMutation.Companion.integer(cases[index][0], 100).apply(store, store)
            Assert.assertEquals(expected[index], store.values.get(cases[index][1]))
            Assert.assertEquals(2, store.lastTransaction!!.size.toLong())
        }
    }

    class Store : StyleSettings.Store, Runnable {
        @JvmField val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        @JvmField var lastTransaction: MutableMap<String?, Any?>? = null
        @JvmField var writes: Int = 0
        @JvmField var publishes: Int = 0
        override fun run() {
            publishes++
        }

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
                val pending: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
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
                    lastTransaction = pending
                    values.putAll(pending)
                    writes++
                }
            }
        }
    }
}
