package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder
import kotlin.math.max

object HookInstallPipeline {
    @JvmStatic @Throws(Throwable::class)
    fun install(
        units: MutableList<out HookInstallUnit>,
        context: HookInstallContext,
        recorder: HookOrderRecorder
    ) {
        for (unit in units) {
            val startedNanos = System.nanoTime()
            recorder.reached(unit.stage())
            val installedCount = context.mark()
            try {
                unit.install(context)
            } catch (error: Throwable) {
                context.recordFailure(unit.stage(), error)
                context.rollbackTo(installedCount)
                context.failed(unit.capability(), unit.stage(), error)
                if (unit.fatalOnFailure()) {
                    throw error
                }
            } finally {
                recorder.recordDuration(
                    max(0L, (System.nanoTime() - startedNanos) / 1000000L)
                )
            }
        }
    }
}
