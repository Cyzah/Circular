package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.geometry.RingContentCutoutsTest
import com.cyzah.circular.domain.settings.DualStyle
import java.util.Arrays
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class RingContentCutoutsTest {
    @Test
    @Throws(Exception::class)
    fun calibratedCutoutsDoNotSuddenlyRemoveAllowedBaselineOverlap() {
        val reference: RingGeometry = circle()
        val moved: RingGeometry = circle()
        calibrated(
            moved, floatArrayOf(19.5f, 90f, 99.5f, 110f),
            floatArrayOf(20f, 90f, 100f, 110f), 8f, reference
        )
        Assert.assertEquals(reference.startX, moved.startX, .01f)
        Assert.assertEquals(reference.startY, moved.startY, .01f)
        Assert.assertEquals(reference.endX, moved.endX, .01f)
        Assert.assertEquals(reference.endY, moved.endY, .01f)
        Assert.assertEquals(reference.length, moved.length, .01f)
    }

    @Test
    @Throws(Exception::class)
    fun baselineToleranceNeverExemptsANewlyRestoredArc() {
        val reference: RingGeometry = circle()
        val moved = RingGeometry()
        moved.setCircularOpenings(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 25f, 25f, 60f, 50f, 9f)
        calibrated(
            moved, floatArrayOf(19.5f, 90f, 99.5f, 110f),
            floatArrayOf(20f, 90f, 100f, 110f), 8f, reference
        )
        Assert.assertTrue(moved.valid())
        for (i in 0..<moved.contourPointCount()) {
            Assert.assertFalse(
                "Restored right arc must retain full endcap clearance",
                moved.contourPointX(i) > 60 && moved.contourPointY(i) > reference.startY + .01f
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun calibrationMovesAnAlreadyOverlappingEndContinuously() {
        val reference: RingGeometry = circle()
        val moved: RingGeometry = circle()
        calibrated(
            moved, floatArrayOf(20f, 89.5f, 100f, 109.5f),
            floatArrayOf(20f, 90f, 100f, 110f), 8f, reference
        )
        Assert.assertEquals(.5f, reference.startY - moved.startY, .001f)
        Assert.assertEquals(.5f, reference.endY - moved.endY, .001f)
        Assert.assertTrue(reference.length - moved.length < 2)
        var starts = 0
        for (i in 0..<moved.contourPointCount()) if (moved.contourStartsAt(i)) starts++
        Assert.assertEquals(2, starts.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun missingReferenceKeepsLegacyFullPaddingCutouts() {
        val legacy: RingGeometry = circle()
        val calibrated: RingGeometry = circle()
        val moved = floatArrayOf(20f, 89.5f, 100f, 109.5f)
        legacy.setContentCutouts(moved, 8f)
        calibrated(calibrated, moved, floatArrayOf(20f, 90f, 100f, 110f), 8f, null)
        Assert.assertEquals(legacy.length, calibrated.length, .0001f)
        Assert.assertEquals(legacy.contourPointCount().toLong(), calibrated.contourPointCount().toLong())
        for (i in 0..<legacy.contourPointCount()) {
            Assert.assertEquals(legacy.contourPointX(i), calibrated.contourPointX(i), .0001f)
            Assert.assertEquals(legacy.contourPointY(i), calibrated.contourPointY(i), .0001f)
        }
    }

    @Test
    @Throws(Exception::class)
    fun calibrationSnapshotsSurviveCallerMutationAndTransfer() {
        val reference: RingGeometry = circle()
        val source: RingGeometry = circle()
        val original = floatArrayOf(20f, 90f, 100f, 110f)
        val moved = floatArrayOf(19.5f, 90f, 99.5f, 110f)
        calibrated(source, moved, original, 8f, reference)
        reference.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 80f, 80f, 9f)
        Arrays.fill(original, -1000f)
        Arrays.fill(moved, -1000f)
        val transfer = RingGeometry()
        transfer.copyForTransferFrom(source)
        Assert.assertEquals(source.length, transfer.length, .0001f)
        Assert.assertEquals(source.contourPointCount().toLong(), transfer.contourPointCount().toLong())
        for (i in 0..<source.contourPointCount()) {
            Assert.assertEquals(source.contourPointX(i), transfer.contourPointX(i), .0001f)
            Assert.assertEquals(source.contourPointY(i), transfer.contourPointY(i), .0001f)
            Assert.assertEquals(source.contourPointSourceDistance(i), transfer.contourPointSourceDistance(i), .0001f)
        }
    }

    @Test
    fun cutoutsKeepTheirSourceDistancesAcrossUnequalSidesAndTransfers() {
        val ring = RingGeometry()
        ring.setCircularOpenings(DualStyle.DUO, 10f, 8f, 110f, 108f, 20f, 20f, 55f, 25f, 7.5f)
        ring.setContentCutouts(floatArrayOf(95f, 30f, 120f, 85f), 5f)
        Assert.assertEquals(117.80973f, ring.firstContourLength(), .001f)
        Assert.assertEquals(209.43951f, ring.baseContourLength(), .001f)
        var starts = 0
        var previous = -1f
        for (i in 0..<ring.contourPointCount()) {
            val source = ring.contourPointSourceDistance(i)
            Assert.assertTrue(source >= previous)
            Assert.assertEquals(ring.rawPointX(source), ring.contourPointX(i), .0001f)
            Assert.assertEquals(ring.rawPointY(source), ring.contourPointY(i), .0001f)
            if (ring.contourStartsAt(i)) starts++
            previous = source
        }
        Assert.assertTrue(starts >= 3)
        val transfer = RingGeometry()
        transfer.copyForTransferFrom(ring)
        Assert.assertEquals(ring.contourPointCount().toLong(), transfer.contourPointCount().toLong())
        for (i in 0..<ring.contourPointCount()) {
            Assert.assertEquals(ring.contourPointSourceDistance(i), transfer.contourPointSourceDistance(i), .0001f)
        }
    }

    @Test
    fun sidewaysContentCanOpenTheCurvedSideAsWellAsTheTop() {
        val ring: RingGeometry = circle()
        val initial = ring.length
        ring.setContentCutouts(floatArrayOf(95f, 30f, 120f, 85f), 5f)
        Assert.assertTrue(ring.valid())
        Assert.assertTrue(ring.length < initial)
        assertClear(ring, 90f, 25f, 125f, 90f)
        Assert.assertEquals(ring.pointX(0f), ring.startX, .0001f)
        Assert.assertEquals(ring.pointY(ring.length), ring.endY, .0001f)
    }

    @Test
    fun transferCopiesTheSameVisibleContourAndCanRemoveOffsetsAgain() {
        val ring: RingGeometry = circle()
        ring.setContentCutouts(floatArrayOf(95f, 30f, 120f, 85f), 5f)
        val transfer = RingGeometry()
        transfer.copyForTransferFrom(ring)
        Assert.assertEquals(ring.length, transfer.length, .0001f)
        for (i in 0..100) {
            Assert.assertEquals(ring.pointX(ring.length * i / 100), transfer.pointX(transfer.length * i / 100), .0001f)
            Assert.assertEquals(ring.pointY(ring.length * i / 100), transfer.pointY(transfer.length * i / 100), .0001f)
        }
        ring.setContentCutouts(FloatArray(0), 0f)
        Assert.assertFalse(ring.hasContentCutouts())
        Assert.assertEquals(circle().length, ring.length, .0001f)
    }

    @Test
    fun capsuleSideCutoutsUseTheScaledActualContour() {
        val ring = RingGeometry()
        ring.setCapsule(240f, 104f, 22f, 30f, 8f, -60f, 34f, .5f)
        ring.setContentCutouts(floatArrayOf(48f, 35f, 65f, 85f), 3f)
        Assert.assertTrue(ring.valid())
        assertClear(ring, 45f, 32f, 68f, 88f)
    }

    @Test
    fun coveringTheWholeRingProducesNoImaginaryPath() {
        val ring: RingGeometry = circle()
        ring.setContentCutouts(floatArrayOf(-100f, -100f, 300f, 300f), 5f)
        Assert.assertFalse(ring.valid())
        Assert.assertEquals(0f, ring.length, 0f)
    }

    companion object {
        @Throws(Exception::class)
        private fun calibrated(
            ring: RingGeometry?, moved: FloatArray, original: FloatArray?,
            padding: Float, reference: RingGeometry?
        ) {
            try {
                RingGeometry::class.java.getMethod(
                    "setContentCutouts", FloatArray::class.java, FloatArray::class.java,
                    Float::class.javaPrimitiveType, RingGeometry::class.java
                ).invoke(ring, moved, original, padding, reference)
            } catch (missing: NoSuchMethodException) {
                throw AssertionError("Moved cutouts need immutable baseline calibration", missing)
            }
        }

        private fun circle(): RingGeometry {
            val ring = RingGeometry()
            ring.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 25f, 55f, 9f)
            return ring
        }

        private fun assertClear(ring: RingGeometry, l: Float, t: Float, r: Float, b: Float) {
            for (i in 0..1000) {
                val x = ring.pointX(ring.length * i / 1000)
                val y = ring.pointY(ring.length * i / 1000)
                Assert.assertFalse(
                    "Contour intersects moved content at " + x + "," + y,
                    x > l + .01f && x < r - .01f && y > t + .01f && y < b - .01f
                )
            }
        }
    }
}
