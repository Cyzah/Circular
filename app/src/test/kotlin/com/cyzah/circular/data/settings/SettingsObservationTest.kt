package com.cyzah.circular.data.settings

import com.cyzah.circular.domain.settings.ObservableSettingsValues
import com.cyzah.circular.domain.settings.SettingsObservation
import com.cyzah.circular.domain.settings.SettingsSnapshot
import java.util.ArrayList
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class SettingsObservationTest {
    @Test
    fun multiKeyTransactionPublishesOneEffectiveRevision() {
        val fixture = Fixture()
        fixture.source.store.values.put("weight_v2", 140)
        fixture.source.store.values.put("weight", 168)
        fixture.source.signal("weight_v2")
        fixture.source.signal("weight")
        Assert.assertEquals(1, fixture.queue.size.toLong())
        fixture.drain()
        Assert.assertEquals(1, fixture.appearances.toLong())
        Assert.assertEquals(140, fixture.observer.snapshot()!!.weight().toLong())
    }

    @Test
    fun knownUnrelatedWritesDoNotBuildAppearanceSnapshots() {
        val fixture = Fixture()
        val reads = fixture.source.iconReads
        for (key in arrayOf<String>(
            "scene_state", "preview_dark", "report", "report_time", "report_token",
            "ai_modification_notice_dismissed", "ai_modification_notice_install_stamp", "diagnostic_request"
        )) {
            fixture.source.signal(key)
        }
        fixture.drain()
        Assert.assertEquals(reads.toLong(), fixture.source.iconReads.toLong())
        Assert.assertEquals(0, fixture.appearances.toLong())
        Assert.assertEquals(1, fixture.scenes.toLong())
        Assert.assertEquals(1, fixture.reports.toLong())
    }

    @Test
    fun localRefreshDoesNotSwallowAQueuedReportOrAddAnotherRevision() {
        val fixture = Fixture()
        fixture.source.store.values.put("size", 28)
        fixture.source.signal("size")
        fixture.source.signal("report_token")
        fixture.observer.refreshAppearanceNow()
        fixture.drain()
        Assert.assertEquals(1, fixture.appearances.toLong())
        Assert.assertEquals(1, fixture.reports.toLong())
    }

    @Test
    fun equalEffectiveAliasChangesDoNotIncreaseRevision() {
        val fixture = Fixture(true)
        fixture.source.store.values.put("weight", 150)
        fixture.source.signal("weight")
        fixture.drain()
        Assert.assertEquals(0, fixture.appearances.toLong())
    }

    @Test
    fun nullUnknownAndReattachmentConservativelyReadLatestValues() {
        val fixture = Fixture()
        val reads = fixture.source.iconReads
        fixture.source.signal("future_setting")
        fixture.drain()
        Assert.assertTrue(fixture.source.iconReads > reads)
        fixture.source.store.values.put("size", 26)
        fixture.source.signal(null)
        fixture.drain()
        Assert.assertEquals(26, fixture.observer.snapshot()!!.size().toLong())
        fixture.observer.detach()
        fixture.source.store.values.put("size", 27)
        fixture.observer.attach()
        Assert.assertEquals(27, fixture.observer.snapshot()!!.size().toLong())
        fixture.observer.detach()
        Assert.assertNull(fixture.source.listener)
        Assert.assertEquals(0, fixture.queue.size.toLong())
    }

    @Test
    fun listenerIsInstalledBeforeInitialRead() {
        val fixture = Fixture()
        Assert.assertTrue(fixture.source.observedBeforeRead)
        fixture.source.store.values.put("style_profile_duo", "v1|28|100|95|110|95|115|1|90|145|0")
        fixture.source.signal("style_profile_duo")
        fixture.drain()
        Assert.assertEquals(28, fixture.observer.snapshot()!!.batterySize().toLong())
    }

    private class Fixture @JvmOverloads constructor(complete: Boolean = false) : SettingsObservation.Scheduler,
        SettingsObservation.Listener {
        @JvmField val source: Source = Source()
        @JvmField val queue: MutableList<Runnable?> = ArrayList<Runnable?>()
        @JvmField val observer: SettingsObservation = SettingsObservation(source, this, this)
        @JvmField var appearances: Int = 0
        @JvmField var scenes: Int = 0
        @JvmField var reports: Int = 0

        init {
            if (complete) {
                for (key in arrayOf<String>("weight_v2", "wifi_weight", "technology_weight_v2", "percent_weight_v2")) {
                    source.store.values.put(key, 100)
                }
            }
            observer.attach()
        }

        override fun post(work: Runnable) {
            queue.add(work)
        }

        override fun remove(work: Runnable) {
            queue.remove(work)
        }

        override fun onAppearanceChanged(snapshot: SettingsSnapshot) {
            appearances++
        }

        override fun onSceneChanged() {
            scenes++
        }

        override fun onReportChanged() {
            reports++
        }

        fun drain() {
            while (!queue.isEmpty()) queue.removeAt(0)!!.run()
        }
    }

    private class Source : ObservableSettingsValues {
        @JvmField val store: SettingsMutationTest.Store = SettingsMutationTest.Store()
        @JvmField var listener: ObservableSettingsValues.Listener? = null
        @JvmField var iconReads: Int = 0
        @JvmField var observedBeforeRead: Boolean = false
        fun signal(key: String?) {
            listener!!.onChanged(key)
        }

        override fun register(value: ObservableSettingsValues.Listener) {
            listener = value
        }

        override fun unregister() {
            listener = null
        }

        override fun contains(key: String?): Boolean {
            observedBeforeRead = listener != null
            iconReads++
            return store.contains(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            iconReads++
            return store.getInt(key, fallback)
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            iconReads++
            return store.getBoolean(key, fallback)
        }

        override fun getString(key: String?, fallback: String?): String? {
            iconReads++
            return store.getString(key, fallback)
        }
    }
}
