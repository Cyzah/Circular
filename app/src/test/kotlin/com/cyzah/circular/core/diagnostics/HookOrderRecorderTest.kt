package com.cyzah.circular.core.diagnostics

import org.junit.Assert
import org.junit.Test

class HookOrderRecorderTest {
    @Test
    fun acceptsMonotonicStages() {
        val recorder = HookOrderRecorder()

        recorder.reached(HookOrderRecorder.Stage.COMPATIBILITY)
        recorder.reached(HookOrderRecorder.Stage.CORE_VIEWS)
        recorder.reached(HookOrderRecorder.Stage.COMPLETE)

        Assert.assertTrue(recorder.valid())
        Assert.assertEquals(HookOrderRecorder.Stage.COMPLETE, recorder.last())
    }

    @Test
    fun reportsReversedStagesWithoutThrowing() {
        val recorder = HookOrderRecorder()

        recorder.reached(HookOrderRecorder.Stage.MOTION, 3)
        recorder.reached(HookOrderRecorder.Stage.POWER, 4)

        Assert.assertFalse(recorder.valid())
        Assert.assertEquals(1, recorder.violations().toLong())
        Assert.assertEquals(HookOrderRecorder.Stage.MOTION, recorder.last())
        Assert.assertEquals(HookOrderRecorder.Stage.POWER, recorder.firstViolation())
        Assert.assertEquals(2, recorder.history().size.toLong())
        Assert.assertTrue(recorder.summary().contains("durationMs=7"))
        Assert.assertTrue(recorder.summary().contains("first=POWER"))
        Assert.assertTrue(recorder.summary().contains("violations=1"))
    }

    @Test
    fun requiredStagesAreOrdered() {
        Assert.assertEquals(0, HookOrderRecorder.Stage.COMPATIBILITY.ordinal.toLong())
        Assert.assertTrue(
            HookOrderRecorder.Stage.CORE_VIEWS.ordinal
                    < HookOrderRecorder.Stage.MOTION.ordinal
        )
        Assert.assertTrue(
            HookOrderRecorder.Stage.KEYGUARD.ordinal
                    < HookOrderRecorder.Stage.COMPLETE.ordinal
        )
    }
}
