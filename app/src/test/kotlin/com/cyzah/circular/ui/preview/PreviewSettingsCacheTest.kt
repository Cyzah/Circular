package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.ObservableSettingsValues
import com.cyzah.circular.domain.settings.StyleSettingsTest.MemoryStore
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class PreviewSettingsCacheTest {
    @Test
    fun reportAndSceneChangesKeepTheAppearanceSnapshot() {
        val source = Source()
        val cache = PreviewSettingsCache(source, source)
        cache.attach()
        val first = cache.settings()
        val reads = source.reads
        source.store.values.put("report", "fresh report")
        source.listener!!.onChanged("report")
        source.listener!!.onChanged("scene_state")
        source.listener!!.onChanged("preview_dark")
        Assert.assertSame(first, cache.settings())
        Assert.assertEquals(reads.toLong(), source.reads.toLong())
    }

    @Test
    fun drawsReuseBothSnapshotsUntilPreferenceNotification() {
        val source = Source()
        val cache = PreviewSettingsCache(source, source)
        cache.attach()
        val first = cache.settings()
        val expanded = cache.expanded()
        val reads = source.reads
        Assert.assertSame(first, cache.settings())
        Assert.assertSame(expanded, cache.expanded())
        Assert.assertEquals(reads.toLong(), source.reads.toLong())

        source.store.values.put("size", 27)
        source.store.values.put("weight_v2", 140)
        source.store.values.put("duo_dual", true)
        source.listener!!.onChanged(null)
        source.listener!!.onChanged(null)
        Assert.assertEquals(reads.toLong(), source.reads.toLong())
        Assert.assertEquals(27, cache.settings().size().toLong())
        Assert.assertEquals(140, cache.settings().weight().toLong())
        Assert.assertEquals(DualStyle.DUO, cache.settings().dualStyle())
        Assert.assertEquals(27, cache.expanded().size().toLong())
        Assert.assertNotSame(expanded, cache.expanded())
        Assert.assertEquals(3, source.invalidations.toLong())
    }

    @Test
    fun detachReleasesListenerAndReattachRefreshesUnobservedChanges() {
        val source = Source()
        val cache = PreviewSettingsCache(source, source)
        cache.attach()
        cache.attach()
        val first = cache.settings()
        Assert.assertEquals(1, source.registrations.toLong())
        cache.detach()
        cache.detach()
        Assert.assertEquals(1, source.removals.toLong())
        Assert.assertNull(source.listener)
        source.store.values.put("keep_merged_shade", true)
        source.store.values.put("battery_number_placement", 1)
        source.store.values.put("charge_color", -0xedcbaa)
        cache.attach()
        Assert.assertNotSame(first, cache.settings())
        Assert.assertSame(cache.settings(), cache.expanded())
        Assert.assertEquals(BatteryNumberPlacement.INSIDE, cache.settings().batteryNumberPlacement())
        Assert.assertEquals(-0xedcbaa, cache.settings().chargeColor().toLong())
        Assert.assertEquals(2, source.registrations.toLong())
    }

    @Test
    fun technologyRuleNotificationRefreshesMappingWithoutChangingSceneVersion() {
        val source = Source()
        val cache = PreviewSettingsCache(source, source)
        cache.attach()
        val raw =
            IconState()
        raw.mobileTechnology = "5G"
        val projection = PreviewTechnologyRules()
        val rendered =
            projection.update(raw, 3, cache.technologyText())
        Assert.assertEquals("5G", rendered.mobileTechnology)

        source.store.values.put("technology_text", "5G=5G+")
        source.listener!!.onChanged(null)
        Assert.assertSame(rendered, projection.update(raw, 3, cache.technologyText()))
        Assert.assertEquals("5G+", rendered.mobileTechnology)
        Assert.assertEquals("5G", raw.mobileTechnology)
    }

    private class Source : ObservableSettingsValues, Runnable {
        @JvmField val store: MemoryStore = MemoryStore()
        @JvmField var listener: ObservableSettingsValues.Listener? = null
        @JvmField var reads: Int = 0
        @JvmField var registrations: Int = 0
        @JvmField var removals: Int = 0
        @JvmField var invalidations: Int = 0
        override fun register(listener: ObservableSettingsValues.Listener) {
            this.listener = listener
            registrations++
        }

        override fun unregister() {
            listener = null
            removals++
        }

        override fun run() {
            invalidations++
        }

        override fun contains(key: String?): Boolean {
            reads++
            return store.contains(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            reads++
            return store.getInt(key, fallback)
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            reads++
            return store.getBoolean(key, fallback)
        }

        override fun getString(key: String?, fallback: String?): String? {
            reads++
            return store.getString(key, fallback)
        }
    }
}
