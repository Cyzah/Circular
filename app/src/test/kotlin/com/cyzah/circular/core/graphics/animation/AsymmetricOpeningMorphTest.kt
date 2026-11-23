package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.AsymmetricOpeningMorphTest
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class AsymmetricOpeningMorphTest {
    @Test
    fun initialFrameSamplesBothRealSidesInsteadOfMirroringOne() {
        val ring = RingGeometry()
        ring.setCircularOpenings(DualStyle.DUO, 10f, 8f, 110f, 108f, 45f, 15f, 60f, 25f, 9f)
        val frame: OpeningBatteryMorph = frame(ring, 0f)
        val rightTop: Int = (OpeningBatteryMorph.Companion.SIDE_POINTS - 1) * 2
        val leftTop: Int = OpeningBatteryMorph.Companion.SIDE_POINTS * 2
        Assert.assertEquals(72.94095f, frame.points[rightTop], .001f)
        Assert.assertEquals(9.703709f, frame.points[rightTop + 1], .001f)
        Assert.assertEquals(24.64466f, frame.points[leftTop], .001f)
        Assert.assertEquals(22.64466f, frame.points[leftTop + 1], .001f)
        Assert.assertEquals(ring.endX, frame.points[frame.points.size - 2], .001f)
        Assert.assertEquals((72.94095f + 24.64466f) / 2, frame.gapAnchor[0], .001f)
        Assert.assertEquals((9.703709f + 22.64466f) / 2, frame.gapAnchor[1], .001f)
    }

    @Test
    fun unequalCutSidesMeetAtTheCorrectBatteryTopAtTheHandoff() {
        val ring = RingGeometry()
        ring.setCircularOpenings(DualStyle.DUO, 10f, 8f, 110f, 108f, 80f, 75f, 80f, 78f, 9f)
        ring.setContentCutouts(floatArrayOf(105f, 45f, 120f, 56f), 0f)
        val frame: OpeningBatteryMorph = frame(ring, .65f)
        Assert.assertTrue(frame.valid)
        assertContains(
            frame, frame.bodyLeft + frame.bodyWidth / 2,
            frame.bodyTop + frame.strokeWidth / 2, .01f
        )
        assertTargetBreaksAreClosed(frame)
    }

    @Test
    fun eitherFullyHiddenSideStillConvergesToTheCompleteTargetOutline() {
        for (box in arrayOf<FloatArray?>(floatArrayOf(60f, -100f, 200f, 200f), floatArrayOf(-100f, -100f, 60f, 200f))) {
            val ring: RingGeometry = circular()
            ring.setContentCutouts(box, 0f)
            Assert.assertTrue(ring.valid())
            val source: OpeningBatteryMorph = frame(ring, 0f)
            Assert.assertEquals((ring.contourPointCount() * 2).toLong(), source.points.size.toLong())
            for (i in 0..<ring.contourPointCount()) {
                Assert.assertEquals(ring.contourPointX(i), source.points[i * 2], .001f)
                Assert.assertEquals(ring.contourPointY(i), source.points[i * 2 + 1], .001f)
                Assert.assertEquals(ring.contourStartsAt(i), source.contourStarts[i])
            }
            val target: OpeningBatteryMorph = frame(ring, .65f)
            Assert.assertEquals(0f, target.solidAlpha, 0f)
            Assert.assertEquals(1f, target.outlineAlpha, 0f)
            assertFullTarget(target)
            assertTargetBreaksAreClosed(target)
        }
    }

    @Test
    fun aVeryShortRemainingArcHasEnoughSamplesToTraceEveryTargetCorner() {
        val ring: RingGeometry = circular()
        ring.setContentCutouts(
            floatArrayOf(
                60f, -100f, 200f, 200f,
                -100f, -100f, 200f, 57f, -100f, 59f, 200f, 200f
            ), 0f
        )
        Assert.assertTrue(ring.valid())
        Assert.assertTrue(ring.contourPointCount() < 20)
        val almostTarget: OpeningBatteryMorph = frame(ring, .649f)
        Assert.assertTrue(
            "Sparse source must be augmented along its real visible segments",
            almostTarget.points.size / 2 >= 129
        )
        for (i in 1..<almostTarget.contourStarts.size) {
            if (!almostTarget.contourStarts[i]) {
                val segment = hypot(
                    (almostTarget.points[i * 2] - almostTarget.points[i * 2 - 2]).toDouble(),
                    (almostTarget.points[i * 2 + 1] - almostTarget.points[i * 2 - 1]).toDouble()
                )
                Assert.assertTrue("Target corners must not become long diagonal chords: " + segment, segment < 5)
            }
        }
        assertFullTarget(frame(ring, .65f))
    }

    @Test
    fun cutoutBreaksRemainSeparateDuringTravelAndCloseAtTheTarget() {
        val ring: RingGeometry = circular()
        ring.setContentCutouts(floatArrayOf(60f, -100f, 200f, 200f, -100f, 54f, 60f, 62f), 0f)
        val moving: OpeningBatteryMorph = frame(ring, .4f)
        var starts = 0
        for (start in moving.contourStarts) if (start) starts++
        Assert.assertEquals(2, starts.toLong())
        assertTargetBreaksAreClosed(frame(ring, .65f))
    }

    @Test
    fun gapAnchorUsesTheActualCutEndpointsRatherThanTheBaseOpening() {
        val ring: RingGeometry = circular()
        ring.setContentCutouts(floatArrayOf(60f, -100f, 200f, 40f), 0f)
        val source: OpeningBatteryMorph = frame(ring, 0f)
        Assert.assertEquals(70.8238f, source.gapAnchor[0], .01f)
        Assert.assertEquals(27.3494f, source.gapAnchor[1], .01f)
        val target: OpeningBatteryMorph = frame(ring, .65f)
        Assert.assertEquals(201.1872f, target.gapAnchor[0], .001f)
        Assert.assertEquals(23.75f, target.gapAnchor[1], .001f)
    }

    @Test
    fun gapAnchorFallsBackToTheRemainingRealSideWhenTheOtherIsHidden() {
        val left: RingGeometry = circular()
        left.setContentCutouts(floatArrayOf(60f, -100f, 200f, 200f), 0f)
        Assert.assertEquals(35f, frame(left, 0f).gapAnchor[0], .01f)
        Assert.assertEquals(14.6987f, frame(left, 0f).gapAnchor[1], .01f)
        val right: RingGeometry = circular()
        right.setContentCutouts(floatArrayOf(-100f, -100f, 60f, 200f), 0f)
        Assert.assertEquals(85f, frame(right, 0f).gapAnchor[0], .01f)
        Assert.assertEquals(14.6987f, frame(right, 0f).gapAnchor[1], .01f)
    }

    @Test
    fun settledCutoutMorphUsesOneClosedContourWithoutOverlappingCapSeams() {
        val ring: RingGeometry = circular()
        ring.setContentCutouts(floatArrayOf(95f, 30f, 120f, 85f), 5f)
        val target: OpeningBatteryMorph = frame(ring, .65f)
        var starts = 0
        for (start in target.contourStarts) if (start) starts++
        Assert.assertEquals(1, starts.toLong())
        Assert.assertEquals(target.points[0], target.points[target.points.size - 2], .001f)
        Assert.assertEquals(target.points[1], target.points[target.points.size - 1], .001f)
    }

    companion object {
        private fun circular(): RingGeometry {
            val ring = RingGeometry()
            ring.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 30f, 20f, 7.5f)
            return ring
        }

        private fun assertFullTarget(frame: OpeningBatteryMorph) {
            Assert.assertTrue(frame.valid)
            assertContains(frame, 201.1872f, 23.75f, .001f)
            assertContains(frame, 201.1872f, 69.4167f, .001f)
            var minimumX = Float.POSITIVE_INFINITY
            var maximumX = Float.NEGATIVE_INFINITY
            var i = 0
            while (i < frame.points.size) {
                minimumX = min(minimumX, frame.points[i])
                maximumX = max(maximumX, frame.points[i])
                i += 2
            }
            Assert.assertEquals(153.75f, minimumX, .001f)
            Assert.assertEquals(248.6244f, maximumX, .001f)
        }

        private fun assertContains(frame: OpeningBatteryMorph, x: Float, y: Float, epsilon: Float) {
            var i = 0
            while (i < frame.points.size) {
                if (abs(frame.points[i] - x) <= epsilon && abs(frame.points[i + 1] - y) <= epsilon) return
                i += 2
            }
            Assert.fail("Contour does not contain target point (" + x + ", " + y + ")")
        }

        private fun assertTargetBreaksAreClosed(frame: OpeningBatteryMorph) {
            for (i in 1..<frame.contourStarts.size) if (frame.contourStarts[i]) {
                Assert.assertEquals(frame.points[i * 2 - 2], frame.points[i * 2], .001f)
                Assert.assertEquals(frame.points[i * 2 - 1], frame.points[i * 2 + 1], .001f)
            }
        }

        private fun frame(ring: RingGeometry?, expansion: Float): OpeningBatteryMorph {
            val frame = OpeningBatteryMorph()
            frame.update(ring, 0f, 0f, 1f, 1f, 150f, 20f, 112f, 53.1667f, expansion)
            return frame
        }
    }
}
