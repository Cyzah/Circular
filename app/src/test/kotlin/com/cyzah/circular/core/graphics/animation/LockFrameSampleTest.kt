package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.LockFrameSampleTest
import org.junit.Assert
import org.junit.Test

class LockFrameSampleTest {
    @Test
    fun repeatedCaptureInFrameSkipsGeometry() {
        val sample = LockFrameSample()
        Assert.assertTrue(prepare(sample))
        sample.record(300, 320, false)

        Assert.assertFalse(prepare(sample))
    }

    @Test
    fun nextFrameResamplesAncestorTransforms() {
        val sample = LockFrameSample()
        prepare(sample)
        sample.record(300, 320, false)

        sample.beginFrame()

        Assert.assertTrue(prepare(sample))
        Assert.assertEquals(
            "h=1 p=250 show=true m=true bw=40 be=301 he=321"
                    + " bt=1 ht=2 kt=3 ks=1000 blocked=0 lr=false",
            sample.record(301, 321, false)
        )
    }

    @Test
    fun identicalRoundedSnapshotDoesNotCreateAnotherRecord() {
        val sample = LockFrameSample()
        prepare(sample)
        Assert.assertEquals(INITIAL, sample.record(300, 320, false))

        sample.beginFrame()
        prepare(sample)

        Assert.assertNull(sample.record(300, 320, false))
    }

    @Test
    fun diagnosticStateChangeIsRecordedWithinSameFrame() {
        val sample = LockFrameSample()
        prepare(sample)
        sample.record(300, 320, false)

        Assert.assertTrue(
            sample.prepare(
                0.25f, true, false, 40,
                1.0f, 2.0f, 3.0f, 1.0f, 2, true, false
            )
        )

        Assert.assertEquals(
            "h=1 p=250 show=true m=false bw=40 be=300 he=320"
                    + " bt=1 ht=2 kt=3 ks=1000 blocked=2 lr=true",
            sample.record(300, 320, false)
        )
    }

    @Test
    fun clearingTraceCanForceAnUnchangedSnapshot() {
        val sample = LockFrameSample()
        prepare(sample)
        sample.record(300, 320, false)

        Assert.assertFalse(prepare(sample))
        Assert.assertEquals(INITIAL, sample.record(300, 320, true))
    }

    @Test
    fun failedGeometryCanBeRetriedWithinTheSameFrame() {
        val sample = LockFrameSample()
        Assert.assertTrue(prepare(sample))
        // Geometry failed before record could commit the sample.
        Assert.assertTrue(prepare(sample))
    }

    @Test
    fun subpixelGeometryChangesStillResample() {
        val sample = LockFrameSample()
        prepare(sample)
        sample.record(300, 320, false)

        Assert.assertTrue(
            sample.prepare(
                0.25f, true, true, 40,
                1.1f, 2.0f, 3.0f, 1.0f, 0, false, false
            )
        )
        Assert.assertNull(sample.record(300, 320, false))
    }

    companion object {
        private val INITIAL = ("h=1 p=250 show=true m=true bw=40"
                + " be=300 he=320 bt=1 ht=2 kt=3 ks=1000 blocked=0 lr=false")

        private fun prepare(sample: LockFrameSample): Boolean {
            return sample.prepare(
                0.25f, true, true, 40,
                1.0f, 2.0f, 3.0f, 1.0f, 0, false, false
            )
        }
    }
}
