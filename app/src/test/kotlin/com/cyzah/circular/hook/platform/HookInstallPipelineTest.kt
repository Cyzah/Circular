package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder
import com.cyzah.circular.hook.platform.HookInstallPipelineTest
import com.cyzah.circular.hook.platform.HookInstallPipelineTest.RecordingContext
import java.util.ArrayList
import java.util.Arrays
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable

class HookInstallPipelineTest {
    @Test
    @Throws(Throwable::class)
    fun installsUnitsInDeclaredStageOrder() {
        val context = RecordingContext()
        val recorder = HookOrderRecorder()

        HookInstallPipeline.install(
            Arrays.asList<HookInstallUnit>(
                unit(
                    HookOrderRecorder.Stage.CORE_VIEWS,
                    Compatibility.Capability.CORE, true,
                    HookInstallPipelineTest.Action { ignored: HookInstallContext? -> context.events.add("core") }),
                unit(
                    HookOrderRecorder.Stage.POWER,
                    Compatibility.Capability.BATTERY_POWER, false,
                    HookInstallPipelineTest.Action { ignored: HookInstallContext? -> context.events.add("power") })
            ),
            context,
            recorder
        )

        Assert.assertEquals(mutableListOf<String?>("core", "power"), context.events)
        Assert.assertEquals(HookOrderRecorder.Stage.POWER, recorder.last())
        Assert.assertTrue(recorder.valid())
    }

    @Test
    @Throws(Throwable::class)
    fun optionalFailureRollsBackAndContinues() {
        val context = RecordingContext()
        val recorder = HookOrderRecorder()

        HookInstallPipeline.install(
            Arrays.asList<HookInstallUnit>(
                unit(
                    HookOrderRecorder.Stage.POWER,
                    Compatibility.Capability.BATTERY_POWER, false,
                    HookInstallPipelineTest.Action { ignored: HookInstallContext? ->
                        throw IllegalStateException("power unavailable")
                    }),
                unit(
                    HookOrderRecorder.Stage.PERFORMANCE,
                    Compatibility.Capability.BATTERY_POWER, false,
                    HookInstallPipelineTest.Action { ignored: HookInstallContext? -> context.events.add("continued") })
            ),
            context,
            recorder
        )

        Assert.assertEquals(mutableListOf<String?>("continued"), context.events)
        Assert.assertEquals(0, context.lastRollback.toLong())
        Assert.assertEquals(HookOrderRecorder.Stage.POWER, context.failedStage)
        Assert.assertTrue(context.failure.toString().contains("power unavailable"))
    }

    @Test
    fun fatalFailureRollsBackAndStopsPipeline() {
        val context = RecordingContext()
        context.installed = 3

        val thrown = Assert.assertThrows<IllegalStateException>(
            IllegalStateException::class.java,
            ThrowingRunnable {
                HookInstallPipeline.install(
                    Arrays.asList<HookInstallUnit>(
                        unit(
                            HookOrderRecorder.Stage.CORE_VIEWS,
                            Compatibility.Capability.CORE, true,
                            HookInstallPipelineTest.Action { ignored: HookInstallContext? ->
                                throw IllegalStateException("core failed")
                            }),
                        unit(
                            HookOrderRecorder.Stage.POWER,
                            Compatibility.Capability.BATTERY_POWER, false,
                            HookInstallPipelineTest.Action { ignored: HookInstallContext? -> context.events.add("unexpected") })
                    ),
                    context,
                    HookOrderRecorder()
                )
            })

        Assert.assertEquals("core failed", thrown.message)
        Assert.assertEquals(3, context.lastRollback.toLong())
        Assert.assertFalse(context.events.contains("unexpected"))
    }

    private fun interface Action {
        @Throws(Throwable::class)
        fun run(context: HookInstallContext?)
    }

    private class RecordingContext : HookInstallContext {
        val events: MutableList<String?> = ArrayList<String?>()
        var installed = 0
        var lastRollback = -1
        var failedStage: HookOrderRecorder.Stage? = null
        var failure: Throwable? = null

        override fun mark(): Int {
            return installed
        }

        override fun rollbackTo(installedCount: Int) {
            lastRollback = installedCount
            installed = installedCount
        }

        override fun failed(
            capability: Compatibility.Capability?,
            stage: HookOrderRecorder.Stage?,
            error: Throwable?
        ) {
            failedStage = stage
            failure = error
        }

        override fun recordFailure(stage: HookOrderRecorder.Stage?, error: Throwable?) {
            failedStage = stage
            failure = error
        }
    }

    companion object {
        private fun unit(
            stage: HookOrderRecorder.Stage,
            capability: Compatibility.Capability,
            fatal: Boolean,
            action: Action
        ): HookInstallUnit {
            return object : HookInstallUnit {
                override fun stage(): HookOrderRecorder.Stage {
                    return stage
                }

                override fun capability(): Compatibility.Capability {
                    return capability
                }

                override fun fatalOnFailure(): Boolean {
                    return fatal
                }

                @Throws(Throwable::class)
                override fun install(context: HookInstallContext?) {
                    action.run(context)
                }
            }
        }
    }
}
