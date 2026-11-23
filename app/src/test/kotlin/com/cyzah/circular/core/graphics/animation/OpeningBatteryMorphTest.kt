package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorphTest
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class OpeningBatteryMorphTest {
    @Test
    fun movedContentStartsFromEveryActualVisibleContourWithoutMirroringIt() {
        val ring: RingGeometry = circular()
        ring.setContentCutouts(floatArrayOf(95f, 30f, 120f, 85f), 5f)
        var frame: OpeningBatteryMorph = sample(ring, 0f)
        Assert.assertEquals((ring.contourPointCount() * 2).toLong(), frame.points.size.toLong())
        for (i in 0..<ring.contourPointCount()) {
            Assert.assertEquals(12 + ring.contourPointX(i) * .8f, frame.points[i * 2], .001f)
            Assert.assertEquals(-7 + ring.contourPointY(i) * 1.1f, frame.points[i * 2 + 1], .001f)
            Assert.assertEquals(ring.contourStartsAt(i), frame.contourStarts[i])
        }
        val preopening: OpeningBatteryMorph = sample(ring, .08f)
        Assert.assertTrue(
            "Moved content retains the gap-opening stage",
            abs(preopening.points[0] - frame.points[0]) > 1
        )
        Assert.assertTrue(java.lang.Float.isFinite(preopening.gapAnchor[0]))
        Assert.assertTrue(java.lang.Float.isFinite(preopening.gapAnchor[1]))
        frame = sample(ring, .65f)
        for (i in 1..<frame.contourStarts.size) if (frame.contourStarts[i]) {
            Assert.assertEquals(frame.points[i * 2 - 2], frame.points[i * 2], .001f)
            Assert.assertEquals(frame.points[i * 2 - 1], frame.points[i * 2 + 1], .001f)
        }
    }

    @Test
    fun configuredStrokeNeverThinsWhileTheRingChangesShape() {
        for (stroke in floatArrayOf(3.75f, 7.5f, 15.0f, 24.0f)) {
            val round = RingGeometry()
            round.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 30f, 20f, stroke)
            val capsule = RingGeometry()
            capsule.setCapsule(240f, 104f, 18f, 34f, stroke, 20f, 30f, 0.7f)
            for (ring in arrayOf<RingGeometry>(round, capsule)) {
                for (frame in 0..1000) {
                    val morph: OpeningBatteryMorph = sample(ring, frame / 1000.0f)
                    Assert.assertEquals(stroke * 0.8f, morph.strokeWidth, 0.0001f)
                }
            }
        }
    }

    @Test
    fun theHollowInteriorShrinksFromTheConfiguredStrokeToSolid() {
        val start: OpeningBatteryMorph = sample(circular(), 0.65f)
        Assert.assertEquals(6.0f, start.interiorInset, EPSILON)
        Assert.assertEquals(start.bodyWidth - 12.0f, start.interiorWidth, EPSILON)
        Assert.assertEquals(start.bodyHeight - 12.0f, start.interiorHeight, EPSILON)
        var width = start.interiorWidth
        var height = start.interiorHeight
        val forward = Array<FloatArray?>(201) { FloatArray(2) }
        for (step in 0..200) {
            val morph: OpeningBatteryMorph = sample(circular(), 0.65f + step / 1000.0f)
            Assert.assertTrue(morph.interiorWidth <= width + EPSILON)
            Assert.assertTrue(morph.interiorHeight <= height + EPSILON)
            Assert.assertTrue(morph.interiorWidth >= 0.0f && morph.interiorHeight >= 0.0f)
            Assert.assertEquals(100.0f, morph.bodyLeft, 0.0f)
            Assert.assertEquals(20.0f, morph.bodyTop, 0.0f)
            width = morph.interiorWidth
            forward[step]!![0] = width
            height = morph.interiorHeight
            forward[step]!![1] = height
        }
        Assert.assertEquals(0.0f, height, EPSILON)
        for (step in 200 downTo 0) {
            val morph: OpeningBatteryMorph = sample(circular(), 0.65f + step / 1000.0f)
            Assert.assertEquals(forward[step]!![0], morph.interiorWidth, 0.0f)
            Assert.assertEquals(forward[step]!![1], morph.interiorHeight, 0.0f)
        }
    }

    @Test
    fun theRemainingHoleStaysCentredAndRoundedUntilItDisappears() {
        for (stroke in floatArrayOf(3.75f, 7.5f, 15.0f, 24.0f, 32.0f)) {
            val ring = RingGeometry()
            ring.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 30f, 20f, stroke)
            for (step in 0..199) {
                val morph: OpeningBatteryMorph = sample(ring, 0.65f + step / 1000.0f)
                Assert.assertTrue(morph.interiorWidth > 0.0f && morph.interiorHeight > 0.0f)
                Assert.assertEquals(
                    morph.bodyLeft + morph.bodyWidth / 2.0f,
                    morph.bodyLeft + morph.interiorInset + morph.interiorWidth / 2.0f, EPSILON
                )
                Assert.assertEquals(
                    morph.bodyTop + morph.bodyHeight / 2.0f,
                    morph.bodyTop + morph.interiorInset + morph.interiorHeight / 2.0f, EPSILON
                )
                Assert.assertTrue(
                    "Square inner corner at stroke " + stroke + ", frame " + step,
                    morph.interiorRadiusX > 0.0f && morph.interiorRadiusY > 0.0f
                )
                Assert.assertTrue(morph.interiorRadiusX <= morph.interiorWidth / 2.0f)
                Assert.assertTrue(morph.interiorRadiusY <= morph.interiorHeight / 2.0f)
                // The nominal battery frame has circular corners in device pixels.
                Assert.assertEquals(morph.interiorRadiusX, morph.interiorRadiusY, EPSILON)
            }
            val full: OpeningBatteryMorph = sample(ring, 0.85f)
            Assert.assertEquals(0.0f, full.interiorHeight, EPSILON)
            Assert.assertEquals(0.0f, full.interiorRadiusX, EPSILON)
            Assert.assertEquals(0.0f, full.interiorRadiusY, EPSILON)
        }
    }

    @Test
    fun innerCornerCurvatureShrinksSmoothlyAndRetracesOnReversal() {
        val forward = FloatArray(201)
        for (step in 0..200) {
            val morph: OpeningBatteryMorph = sample(circular(), 0.65f + step / 1000.0f)
            forward[step] = morph.interiorRadiusY
            if (step > 0) {
                Assert.assertTrue(forward[step] <= forward[step - 1])
                Assert.assertTrue(forward[step - 1] - forward[step] < 0.2f)
            }
        }
        for (step in 200 downTo 0) {
            val morph: OpeningBatteryMorph = sample(circular(), 0.65f + step / 1000.0f)
            Assert.assertEquals(forward[step], morph.interiorRadiusY, 0.0f)
        }
    }

    @Test
    fun closedFrameKeepsTheExactCircularAndCapsuleRingPointsAndGaps() {
        val capsule = RingGeometry()
        capsule.setCapsule(240f, 104f, 18f, 34f, 7.5f, 20f, 30f, 0.7f)
        for (ring in arrayOf<RingGeometry>(circular(), capsule)) {
            val morph: OpeningBatteryMorph = sample(ring, 0.0f)
            Assert.assertTrue(morph.valid)
            for (index in 0..<OpeningBatteryMorph.Companion.SIDE_POINTS) {
                val distance: Float = (ring.length * 0.5f * index
                        / (OpeningBatteryMorph.Companion.SIDE_POINTS - 1))
                Assert.assertEquals(
                    12 + ring.pointX(distance) * 0.8f,
                    morph.points[2 * index], EPSILON
                )
                Assert.assertEquals(
                    -7 + ring.pointY(distance) * 1.1f,
                    morph.points[2 * index + 1], EPSILON
                )
            }
            val rightTop: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS - 1)
            val leftTop: Int = 2 * OpeningBatteryMorph.Companion.SIDE_POINTS
            val leftBottom: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS * 2 - 1)
            Assert.assertTrue(morph.points[rightTop] > morph.points[leftTop])
            Assert.assertEquals(morph.points[rightTop + 1], morph.points[leftTop + 1], EPSILON)
            Assert.assertEquals(12 + ring.endX * 0.8f, morph.points[leftBottom], EPSILON)
            Assert.assertEquals(-7 + ring.endY * 1.1f, morph.points[leftBottom + 1], EPSILON)
            Assert.assertEquals(0.0f, morph.solidAlpha, 0.0f)
            Assert.assertEquals(1.0f, morph.outlineAlpha, 0.0f)
            Assert.assertEquals(7.5f * 0.8f, morph.strokeWidth, EPSILON)
        }
    }

    @Test
    fun expandedContourHasNoGapsAndItsStrokeFitsTheExistingBody() {
        val morph: OpeningBatteryMorph = sample(circular(), 1.0f)
        Assert.assertTrue(morph.valid)
        val bodyWidth = 112.0f * 159.96f / 175.0f
        Assert.assertEquals(100.0f, morph.bodyLeft, EPSILON)
        Assert.assertEquals(20.0f, morph.bodyTop, EPSILON)
        Assert.assertEquals(bodyWidth, morph.bodyWidth, EPSILON)
        Assert.assertEquals(53.1667f, morph.bodyHeight, EPSILON)
        Assert.assertEquals(112.0f, morph.totalWidth, EPSILON)
        Assert.assertEquals(53.1667f, morph.totalHeight, EPSILON)
        val rightTop: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS - 1)
        val leftTop: Int = 2 * OpeningBatteryMorph.Companion.SIDE_POINTS
        val leftBottom: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS * 2 - 1)
        Assert.assertEquals(morph.points[rightTop], morph.points[leftTop], EPSILON)
        Assert.assertEquals(morph.points[rightTop + 1], morph.points[leftTop + 1], EPSILON)
        Assert.assertEquals(morph.points[0], morph.points[leftBottom], EPSILON)
        Assert.assertEquals(morph.points[1], morph.points[leftBottom + 1], EPSILON)
        var index = 0
        while (index < morph.points.size) {
            Assert.assertTrue(morph.points[index] - morph.strokeWidth / 2 >= 100 - EPSILON)
            Assert.assertTrue(morph.points[index] + morph.strokeWidth / 2 <= 100 + bodyWidth + EPSILON)
            Assert.assertTrue(morph.points[index + 1] - morph.strokeWidth / 2 >= 20 - EPSILON)
            Assert.assertTrue(morph.points[index + 1] + morph.strokeWidth / 2 <= 73.1667f + EPSILON)
            index += 2
        }
        Assert.assertEquals(1.0f, morph.solidAlpha, 0.0f)
        Assert.assertEquals(0.0f, morph.outlineAlpha, 0.0f)
        Assert.assertEquals(1.0f, morph.contentAlpha, 0.0f)
    }

    @Test
    fun everySampleIsContinuousAndRetracesOnGestureReversal() {
        val ring: RingGeometry = circular()
        val morph = OpeningBatteryMorph()
        val forward = arrayOfNulls<FloatArray>(1001)
        for (frame in 0..1000) {
            update(morph, ring, frame / 1000.0f)
            forward[frame] = morph.points.clone()
            for (value in morph.points.indices) {
                Assert.assertTrue(java.lang.Float.isFinite(morph.points[value]))
                if (frame > 0) {
                    Assert.assertTrue(abs(morph.points[value] - forward[frame - 1]!![value]) < 1.0f)
                }
            }
            Assert.assertTrue(morph.left < morph.right)
            Assert.assertTrue(morph.top < morph.bottom)
        }
        for (frame in 1000 downTo 0) {
            update(morph, ring, frame / 1000.0f)
            for (value in morph.points.indices) {
                Assert.assertEquals(forward[frame]!![value], morph.points[value], 0.0f)
            }
        }
    }

    @Test
    fun boundsEncloseStrokeAndTheVisibleSolidBattery() {
        for (frame in 0..1000) {
            val morph: OpeningBatteryMorph = sample(circular(), frame / 1000.0f)
            val radius = morph.strokeWidth / 2
            var index = 0
            while (index < morph.points.size) {
                Assert.assertTrue(morph.left <= morph.points[index] - radius + EPSILON)
                Assert.assertTrue(morph.right >= morph.points[index] + radius - EPSILON)
                Assert.assertTrue(morph.top <= morph.points[index + 1] - radius + EPSILON)
                Assert.assertTrue(morph.bottom >= morph.points[index + 1] + radius - EPSILON)
                index += 2
            }
            if (morph.solidAlpha > 0) {
                Assert.assertTrue(morph.left <= 100)
                Assert.assertTrue(morph.right >= 212)
                Assert.assertTrue(morph.top <= 20)
                Assert.assertTrue(morph.bottom >= 73.1667f)
            }
        }
    }

    @Test
    fun solidBatteryAppearsOnlyAfterTheContourReachesIt() {
        val contour: OpeningBatteryMorph = sample(circular(), 0.65f)
        val settled: OpeningBatteryMorph = sample(circular(), 1.0f)
        Assert.assertEquals(0.0f, contour.solidAlpha, 0.0f)
        for (index in contour.points.indices) {
            Assert.assertEquals(settled.points[index], contour.points[index], EPSILON)
        }
        val transition: OpeningBatteryMorph = sample(circular(), 0.75f)
        Assert.assertTrue(transition.solidAlpha > 0 && transition.solidAlpha < 1)
        Assert.assertEquals(1.0f, transition.bodyAlpha + transition.outlineAlpha, EPSILON)
        Assert.assertEquals(1.0f, transition.bodyAlpha, EPSILON)
        Assert.assertTrue(transition.interiorHeight > 0.0f)
        Assert.assertEquals(0.0f, sample(circular(), 0.4f).targetClearanceProgress, 0.0f)
        Assert.assertEquals(0.5f, sample(circular(), 0.525f).targetClearanceProgress, EPSILON)
        Assert.assertEquals(1.0f, contour.targetClearanceProgress, EPSILON)
        Assert.assertEquals(1.0f, transition.targetClearanceProgress, EPSILON)
    }

    @Test
    fun invalidInputsClearPreviousVisibleState() {
        val morph: OpeningBatteryMorph = sample(circular(), 0.5f)
        morph.update(null, 0f, 0f, 1f, 1f, 0f, 0f, 100f, 50f, 0.5f)
        Assert.assertFalse(morph.valid)
        Assert.assertEquals(0.0f, morph.outlineAlpha, 0.0f)
        Assert.assertEquals(0.0f, morph.solidAlpha, 0.0f)
        morph.update(circular(), 0f, 0f, 1f, 1f, 0f, 0f, -100f, 50f, 0.5f)
        Assert.assertFalse(morph.valid)
    }

    @Test
    fun existingGapsOpenBeforeTheOutlineTravelsToTheBattery() {
        val ring: RingGeometry = circular()
        val closed: OpeningBatteryMorph = sample(ring, 0f)
        val exit: OpeningBatteryMorph = sample(ring, OpeningTechnologyClearance.EXIT_END)
        val rightTop: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS - 1)
        val leftTop: Int = 2 * OpeningBatteryMorph.Companion.SIDE_POINTS
        val leftBottom: Int = 2 * (OpeningBatteryMorph.Companion.SIDE_POINTS * 2 - 1)
        Assert.assertTrue(
            exit.points[rightTop] - exit.points[leftTop]
                    > closed.points[rightTop] - closed.points[leftTop]
        )
        Assert.assertTrue(
            exit.points[0] - exit.points[leftBottom]
                    > closed.points[0] - closed.points[leftBottom]
        )
        Assert.assertEquals(closed.gapCentreX, exit.gapCentreX, EPSILON)
        Assert.assertEquals(
            exit.gapCentreX,
            (exit.points[rightTop] + exit.points[leftTop]) / 2.0f, EPSILON
        )
        Assert.assertEquals(
            exit.gapCentreX,
            (exit.points[0] + exit.points[leftBottom]) / 2.0f, EPSILON
        )
        Assert.assertEquals(0f, exit.solidAlpha, 0f)
        Assert.assertEquals(closed.strokeWidth, exit.strokeWidth, 0f)
    }

    @Test
    fun thinFramesKeepTheConfiguredStrokeAndClipAtTheTarget() {
        for (size in arrayOf<FloatArray?>(
            floatArrayOf(4f, 120f),
            floatArrayOf(200f, 2f),
            floatArrayOf(0.01f, 0.02f),
            floatArrayOf(100f, 100f)
        )) {
            val morph = OpeningBatteryMorph()
            morph.update(
                circular(), -10f, 7f, 0.01f, 0.02f,
                0f, 0f, size!![0], size[1], 1f
            )
            Assert.assertTrue(morph.valid)
            Assert.assertTrue(morph.contourAtTarget)
            Assert.assertEquals(0.075f, morph.strokeWidth, 0.0001f)
            Assert.assertEquals(0.0f, min(morph.interiorWidth, morph.interiorHeight), EPSILON)
            var index = 0
            while (index < morph.points.size) {
                Assert.assertTrue(java.lang.Float.isFinite(morph.points[index]))
                Assert.assertTrue(java.lang.Float.isFinite(morph.points[index + 1]))
                Assert.assertTrue(morph.points[index] >= -EPSILON)
                Assert.assertTrue(morph.points[index] <= morph.bodyWidth + EPSILON)
                Assert.assertTrue(morph.points[index + 1] >= -EPSILON)
                Assert.assertTrue(morph.points[index + 1] <= size[1] + EPSILON)
                index += 2
            }
        }
    }

    companion object {
        private const val EPSILON = 0.01f

        private fun sample(ring: RingGeometry?, expansion: Float): OpeningBatteryMorph {
            val morph = OpeningBatteryMorph()
            update(morph, ring, expansion)
            return morph
        }

        private fun update(morph: OpeningBatteryMorph, ring: RingGeometry?, expansion: Float) {
            morph.update(
                ring, 12f, -7f, 0.8f, 1.1f,
                100f, 20f, 112f, 53.1667f, expansion
            )
        }

        private fun circular(): RingGeometry {
            val ring = RingGeometry()
            ring.setCircular(DualStyle.LAYERED, 10f, 8f, 110f, 108f, 30f, 20f, 7.5f)
            return ring
        }
    }
}
