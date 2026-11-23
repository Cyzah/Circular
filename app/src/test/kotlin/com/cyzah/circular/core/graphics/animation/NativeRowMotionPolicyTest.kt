package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Test

class NativeRowMotionPolicyTest {
    @Test
    fun invalidatingGeometryAcceptsNewEndpointsOnTheFirstMovingFrame() {
        val endpoints = NativeRowMotionPolicy.Endpoints()
        endpoints.closed(1060f, 0f)
        endpoints.expanded(826f, 1f, false)
        endpoints.expanded(700f, 0.5f, true)
        Assert.assertEquals(826f, endpoints.expanded, 0f)
        endpoints.invalidate()
        endpoints.closed(900f, 0.2f)
        endpoints.expanded(650f, 0.2f, false)
        Assert.assertEquals(900f, endpoints.closed, 0f)
        Assert.assertEquals(650f, endpoints.expanded, 0f)
        Assert.assertEquals(
            775f, NativeRowMotionPolicy.speedBoundary(
                endpoints.closed, endpoints.expanded, 0.5f
            ), 0f
        )
    }

    @Test
    fun transientNativeLayoutsCannotRewriteAnActiveEndpoint() {
        val endpoints = NativeRowMotionPolicy.Endpoints()
        endpoints.closed(1060f, 0f)
        endpoints.expanded(826f, 1f, false)
        endpoints.closed(900f, 0.5f)
        endpoints.expanded(700f, 0.5f, false)
        Assert.assertEquals(1060f, endpoints.closed, 0f)
        Assert.assertEquals(826f, endpoints.expanded, 0f)
    }

    @Test
    fun closingFreesExpandedOnlySlotsOnItsFirstFrame() {
        val slots = NativeRowMotionPolicy.OpeningSlots()
        slots.sample(1f, false)
        Assert.assertEquals(1f, slots.space(), 0f)
        Assert.assertEquals(1f, slots.alpha(), 0f)
        slots.sample(0.999f, true)
        Assert.assertEquals(0f, slots.space(), 0f)
        Assert.assertEquals(0f, slots.alpha(), 0f)
    }

    @Test
    fun openingReservesFullWidthBeforeShowingExpandedOnlyIcons() {
        val slots = NativeRowMotionPolicy.OpeningSlots()
        slots.sample(0.5f, false)
        Assert.assertEquals(0f, slots.alpha(), 0f)
        slots.sample(0.7f, false)
        Assert.assertEquals(1f, slots.space(), 0f)
        Assert.assertEquals(0f, slots.alpha(), 0f)
        slots.sample(1f, false)
        Assert.assertEquals(1f, slots.alpha(), 0f)
    }

    @Test
    fun reversalRestoresExpandedSlotsFromTheCurrentCompactState() {
        val slots = NativeRowMotionPolicy.OpeningSlots()
        slots.sample(0.5f, true)
        slots.sample(0.5f, false)
        Assert.assertEquals(0f, slots.space(), 0f)
        slots.sample(0.6f, false)
        Assert.assertTrue(slots.space() > 0 && slots.space() < 1)
        Assert.assertEquals(0f, slots.alpha(), 0f)
        slots.sample(1f, false)
        Assert.assertEquals(1f, slots.space(), 0f)
        Assert.assertEquals(1f, slots.alpha(), 0f)
    }

    @Test
    fun temporaryAlphaPreservesItsNativeBaseAcrossFramesAndOverride() {
        val alpha = NativeRowMotionPolicy.Translation()
        Assert.assertEquals(0f, alpha.apply(0.7f, -0.7f), 0f)
        val base = 0 - alpha.applied(0f)
        Assert.assertEquals(0.35f, alpha.apply(0f, base * -0.5f), 0.0001f)
        Assert.assertEquals(0.7f, alpha.release(0.35f), 0.0001f)
        alpha.apply(0.7f, -0.7f)
        Assert.assertEquals(0.8f, alpha.release(0.8f), 0f)
    }

    @Test
    fun twoVisibleSpeedCopiesShareOneBoundaryDespiteDifferentTrailingSlots() {
        val desired = NativeRowMotionPolicy.speedBoundary(1060f, 767f, 1f)
        Assert.assertEquals(767f, desired, 0f)
        Assert.assertEquals(-59f, NativeRowMotionPolicy.iconCorrection(826f, desired), 0f)
        Assert.assertEquals(0f, NativeRowMotionPolicy.iconCorrection(767f, desired), 0f)
        var previous = desired
        for (step in 999 downTo 0) {
            val remaining = step / 1000.0f
            val next = NativeRowMotionPolicy.speedBoundary(1060f, 767f, remaining)
            Assert.assertEquals(next, 826 + NativeRowMotionPolicy.iconCorrection(826f, next), 0.0001f)
            Assert.assertEquals(next, 767 + NativeRowMotionPolicy.iconCorrection(767f, next), 0.0001f)
            Assert.assertTrue(next >= previous)
            previous = next
        }
        Assert.assertEquals(1060f, previous, 0f)
    }

    @Test
    fun nativeWidthFeedbackCannotPullTheExpandedRowLeft() {
        val nativeSource = floatArrayOf(850.0f, 848.61f, 846.59f, 843.67f, 840.99f, 844.55f, 875.0f)
        val correctedSource = floatArrayOf(850.0f, 850.0f, 850.0f, 850.0f, 850.0f, 850.0f, 875.0f)
        for (index in nativeSource.indices) {
            Assert.assertEquals(
                correctedSource[index], nativeSource[index]
                        + NativeRowMotionPolicy.sourceCorrection(850.0f, nativeSource[index]), 0.001f
            )
        }
    }

    @Test
    fun pendingWidthIsBridgedWithoutCancellingTheDraw() {
        Assert.assertEquals(-12.0f, NativeRowMotionPolicy.iconCorrection(862.0f, 850.0f), 0.0f)
        Assert.assertEquals(0.0f, NativeRowMotionPolicy.iconCorrection(850.0f, 850.0f), 0.0f)
        Assert.assertEquals(4.0f, NativeRowMotionPolicy.iconCorrection(846.0f, 850.0f), 0.0f)
    }

    @Test
    fun updatingAnOwnedTranslationNeverAccumulatesCorrections() {
        val lease = NativeRowMotionPolicy.Translation()
        Assert.assertEquals(16.0f, lease.apply(7.0f, 9.0f), 0.0f)
        Assert.assertEquals(16.0f, lease.apply(16.0f, 9.0f), 0.0f)
        Assert.assertEquals(10.0f, lease.apply(16.0f, 3.0f), 0.0f)
        Assert.assertEquals(7.0f, lease.release(10.0f), 0.0f)
    }

    @Test
    fun aNativeTranslationWriteBecomesTheNewBase() {
        val lease = NativeRowMotionPolicy.Translation()
        lease.apply(7.0f, 9.0f)
        Assert.assertEquals(23.0f, lease.apply(20.0f, 3.0f), 0.0f)
        Assert.assertEquals(20.0f, lease.release(23.0f), 0.0f)
    }

    @Test
    fun resetDoesNotUndoANativeOverride() {
        val lease = NativeRowMotionPolicy.Translation()
        lease.apply(7.0f, 9.0f)
        Assert.assertEquals(30.0f, lease.release(30.0f), 0.0f)
        Assert.assertEquals(0.0f, lease.applied(30.0f), 0.0f)
    }

    @Test
    fun rawCoordinatesExcludeOnlyTheCorrectionStillOwnedByTheModule() {
        val lease = NativeRowMotionPolicy.Translation()
        lease.apply(7.0f, 9.0f)
        Assert.assertEquals(9.0f, lease.applied(16.0f), 0.0f)
        Assert.assertEquals(0.0f, lease.applied(20.0f), 0.0f)
        Assert.assertEquals(7.0f, lease.release(16.0f), 0.0f)
        Assert.assertEquals(0.0f, lease.applied(7.0f), 0.0f)
    }

    @Test
    fun unknownGeometryDoesNotMoveNativeViews() {
        Assert.assertEquals(0.0f, NativeRowMotionPolicy.sourceCorrection(Float.NaN, 10.0f), 0.0f)
        Assert.assertEquals(0.0f, NativeRowMotionPolicy.sourceCorrection(10.0f, Float.POSITIVE_INFINITY), 0.0f)
        Assert.assertEquals(0.0f, NativeRowMotionPolicy.iconCorrection(Float.NaN, 10.0f), 0.0f)
        Assert.assertEquals(0.0f, NativeRowMotionPolicy.iconCorrection(10.0f, Float.NEGATIVE_INFINITY), 0.0f)
    }
}
