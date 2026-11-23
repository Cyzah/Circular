package com.cyzah.circular.data.settings

import com.cyzah.circular.data.settings.SettingsPublicationIntegrationTest.Pipeline
import com.cyzah.circular.domain.settings.SettingsPublishScheduler
import java.util.ArrayList
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class SettingsPublicationIntegrationTest {
    @Test
    fun repeatedSliderEventsPersistDistinctValuesImmediatelyAndPublishTheLatestRow() {
        val pipeline = Pipeline()
        for (time in 0..1000) {
            pipeline.advance(time.toLong())
            val value = time % 100 + 1
            pipeline.change(value)
            pipeline.change(value)
            Assert.assertEquals(value.toLong(), pipeline.store.getInt("low_battery_threshold", -1).toLong())
        }
        Assert.assertEquals(1001, pipeline.store.writes.toLong())
        Assert.assertEquals(21, pipeline.providerReads.size.toLong())
        pipeline.publisher.flush()
        Assert.assertEquals(22, pipeline.providerReads.size.toLong())
        Assert.assertEquals(1, pipeline.providerReads.get(21))
        pipeline.publisher.flush()
        Assert.assertEquals(22, pipeline.providerReads.size.toLong())
        println("2002 slider inputs: writes=1001 notify/query/apply(model)=22 including final flush")
    }

    @Test
    fun pendingPublicationReadsAnExternalLatestValueWithoutOverwritingIt() {
        val pipeline = Pipeline()
        pipeline.change(10)
        pipeline.advance(1)
        pipeline.change(20)
        pipeline.store.values.put("low_battery_threshold", 30)
        pipeline.advance(50)
        Assert.assertEquals(mutableListOf<Int?>(10, 30), pipeline.providerReads)
        Assert.assertEquals(30, pipeline.store.getInt("low_battery_threshold", -1).toLong())
    }

    @Test
    fun diagnosticCommandIsImmediateAndDoesNotClassifyAsAppearance() {
        val pipeline = Pipeline()
        pipeline.change(10)
        pipeline.advance(1)
        pipeline.change(20)
        val command: SettingsMutation = SettingsMutation.Companion.text("diagnostic_request", "new-request")
        Assert.assertFalse(command.affectsAppearance())
        command.apply(pipeline.store, object : Runnable {
            override fun run() {
                pipeline.publisher.publishNow()
            }
        })
        Assert.assertEquals(mutableListOf<Int?>(10, 20), pipeline.providerReads)
        Assert.assertNull(pipeline.work)
        Assert.assertEquals("new-request", pipeline.store.getString("diagnostic_request", ""))
    }

    private class Pipeline : SettingsPublishScheduler.Scheduler, Runnable {
        @JvmField val store: SettingsMutationTest.Store = SettingsMutationTest.Store()
        @JvmField val providerReads: MutableList<Int?> = ArrayList<Int?>()
        @JvmField val publisher: SettingsPublishScheduler = SettingsPublishScheduler(this, this, 50)
        @JvmField var now: Long = 0
        @JvmField var due: Long = 0
        @JvmField var work: Runnable? = null
        override fun nowMillis(): Long {
            return now
        }

        override fun post(task: Runnable, delay: Long) {
            Assert.assertNull(work)
            work = task
            due = now + delay
        }

        override fun remove(task: Runnable) {
            if (work === task) work = null
        }

        override fun run() {
            providerReads.add(store.getInt("low_battery_threshold", -1))
        }

        fun change(value: Int) {
            SettingsMutation.Companion.integer("low_battery_threshold", value).apply(store, object : Runnable {
                override fun run() {
                    publisher.changedContinuously()
                }
            })
        }

        fun advance(time: Long) {
            if (work != null && due <= time) {
                now = due
                val pending = work
                work = null
                pending!!.run()
            }
            now = time
        }
    }
}
