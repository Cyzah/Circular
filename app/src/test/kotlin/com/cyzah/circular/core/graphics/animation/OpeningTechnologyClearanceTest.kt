package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.OpeningTechnologyClearanceTest
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class OpeningTechnologyClearanceTest {
    @Test
    fun closedRingKeepsItsInternalTechnologyAtTheOriginalPosition() {
        for (above in booleanArrayOf(true, false)) {
            Assert.assertEquals(
                0.0f, OpeningTechnologyClearance.shift(
                    40f, 40f, 80f, 65f, 10f, 8f, 110f, 108f,
                    100f, 20f, 212f, 73.1667f, 0f, 1f, 25f, above
                ), 0.0f
            )
        }
    }

    @Test
    fun hollowContourIsClearedBeforeTheSolidTargetStartsAppearing() {
        val frame: OpeningBatteryMorph = sample(circular(DualStyle.LAYERED), 0.35f)
        Assert.assertEquals(0.0f, frame.solidAlpha, 0.0f)
        Assert.assertEquals(0.0f, frame.targetClearanceProgress, 0.0f)
        val label: FloatArray = reproducedLabel(0.35f)
        val shift: Float = shift(frame, label, 0.35f, true)
        Assert.assertTrue("The passing hollow side must already have clearance", shift < 0.0f)
        Assert.assertTrue(label[3] + shift < frame.top)
    }

    @Test
    fun reproducedNarrowGlyphNeverCrossesEitherSampledContour() {
        val ring: RingGeometry = circular(DualStyle.LAYERED)
        for (step in 0..1000) {
            val expansion = step / 1000.0f
            val frame: OpeningBatteryMorph = sample(ring, expansion)
            val label: FloatArray = reproducedLabel(expansion)
            val shift: Float = shift(frame, label, expansion, true)
            label[1] += shift
            label[3] += shift
            Assert.assertTrue(
                "Contour touched the reproduced glyph at " + expansion,
                !intersectsStroke(frame, label)
            )
        }
    }

    @Test
    fun variedGlyphSizesAndStylesClearTheContourAfterTheExitPhase() {
        // These geometry tables verify clearance after the early exit phase.
        // They do not claim that arbitrary fonts fit through every source-ring gap.
        for (ring in rings()) {
            for (width in floatArrayOf(8f, 20f, 32f, 48f)) {
                for (height in floatArrayOf(6f, 12f, 24f)) {
                    for (above in booleanArrayOf(true, false)) {
                        val closed: OpeningBatteryMorph = sample(ring, 0f)
                        val forward = FloatArray(1001)
                        for (step in 0..1000) {
                            val expansion = step / 1000.0f
                            val frame: OpeningBatteryMorph = sample(ring, expansion)
                            val label: FloatArray = movingLabel(closed, width, height, expansion)
                            val shift: Float = shift(frame, label, expansion, above)
                            forward[step] = shift
                            Assert.assertTrue(java.lang.Float.isFinite(shift))
                            Assert.assertTrue(if (above) shift <= 0 else shift >= 0)
                            if (step > 0) {
                                Assert.assertTrue(
                                    "Abrupt motion for " + ring.style + " at " + expansion,
                                    abs(shift - forward[step - 1]) < 2.0f
                                )
                            }
                            if (expansion >= OpeningTechnologyClearance.EXIT_END && label[2] >= frame.left && label[0] <= frame.right) {
                                Assert.assertTrue(
                                    if (above)
                                        label[3] + shift <= frame.top + EPSILON
                                    else
                                        label[1] + shift >= frame.bottom - EPSILON
                                )
                            }
                            if (expansion >= 0.65f && label[2] >= 100 && label[0] <= 212) {
                                Assert.assertTrue(
                                    if (above)
                                        label[3] + shift <= 20 + EPSILON
                                    else
                                        label[1] + shift >= 73.1667f - EPSILON
                                )
                            }
                        }
                        Assert.assertEquals(0f, forward[0], 0f)
                        Assert.assertEquals(0f, forward[1000], 0f)
                        for (step in 1000 downTo 0) {
                            val expansion = step / 1000.0f
                            Assert.assertEquals(
                                forward[step], shift(
                                    sample(ring, expansion),
                                    movingLabel(closed, width, height, expansion), expansion, above
                                ), 0f
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun invalidExpansionDoesNotMoveTheGlyph() {
        for (invalid in floatArrayOf(
            Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )) {
            Assert.assertEquals(
                0f, OpeningTechnologyClearance.shift(
                    40f, 40f, 80f, 65f, 10f, 8f, 110f, 108f,
                    100f, 20f, 212f, 73.1667f, invalid, 1f, 25f, true
                ), 0f
            )
        }
    }

    @Test
    fun openingPhasesKeepTheEndpointFontUntilTheGlyphLeaves() {
        Assert.assertEquals(0f, OpeningTechnologyClearance.textProgress(0.06f), 0f)
        Assert.assertEquals(0f, OpeningTechnologyClearance.fontProgress(0.16f), 0f)
        Assert.assertEquals(0f, OpeningTechnologyClearance.horizontalJoin(0.16f), 0f)
        Assert.assertEquals(1f, OpeningTechnologyClearance.horizontalJoin(0.35f), EPSILON)
        Assert.assertEquals(0f, OpeningTechnologyClearance.shapeProgress(0.16f), 0f)
        Assert.assertEquals(1f, OpeningTechnologyClearance.shapeProgress(0.65f), EPSILON)
        Assert.assertEquals(1f, OpeningTechnologyClearance.gapProgress(0.08f), EPSILON)
        Assert.assertEquals(1f, OpeningTechnologyClearance.textProgress(1f), 0f)
        Assert.assertEquals(1f, OpeningTechnologyClearance.fontProgress(1f), 0f)
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun shift(
            frame: OpeningBatteryMorph, label: FloatArray,
            expansion: Float, above: Boolean
        ): Float {
            return OpeningTechnologyClearance.shift(
                label[0], label[1], label[2], label[3],
                frame.left, frame.top, frame.right, frame.bottom,
                100f, 20f, 212f, 73.1667f, expansion,
                1f, max(4f, label[3] - label[1]), above
            )
        }

        private fun reproducedLabel(expansion: Float): FloatArray {
            val progress = OpeningTechnologyClearance.textProgress(expansion)
            val xProgress = StackedOverlayLayout.technologyXProgress(expansion)
            val width = Motion.mix(20f, 25f, progress)
            val gapCentre: Float = sample(circular(DualStyle.LAYERED), expansion).gapCentreX
            val left = Motion.mix(
                gapCentre - width / 2.0f,
                Motion.mix(50f, 65f, xProgress), OpeningTechnologyClearance.horizontalJoin(expansion)
            )
            val top = Motion.mix(45f, 25f, progress)
            return floatArrayOf(
                left, top, left + width,
                top + Motion.mix(10f, 12f, progress)
            )
        }

        private fun movingLabel(
            closed: OpeningBatteryMorph,
            width: Float, height: Float, expansion: Float
        ): FloatArray {
            val progress = StackedOverlayLayout.technologyProgress(expansion)
            val xProgress = StackedOverlayLayout.technologyXProgress(expansion)
            val sourceLeft = (closed.left + closed.right - width) / 2.0f
            val sourceTop = (closed.top + closed.bottom - height) / 2.0f
            val finalWidth = width / 2.0f + 5
            val left = Motion.mix(sourceLeft, 90 - finalWidth, xProgress)
            val top = Motion.mix(sourceTop, 40f, progress)
            return floatArrayOf(
                left, top, left + Motion.mix(width, finalWidth, progress),
                top + Motion.mix(height, height / 2.0f + 3, progress)
            )
        }

        private fun sample(ring: RingGeometry?, expansion: Float): OpeningBatteryMorph {
            val result = OpeningBatteryMorph()
            result.update(
                ring, 12f, -7f, 0.8f, 1.1f,
                100f, 20f, 112f, 53.1667f, expansion
            )
            return result
        }

        private fun rings(): Array<RingGeometry> {
            val capsule = RingGeometry()
            capsule.setCapsule(240f, 104f, 18f, 34f, 7.5f, 20f, 30f, 0.7f)
            return arrayOf<RingGeometry>(
                circular(DualStyle.LAYERED), circular(DualStyle.GROUPED),
                circular(DualStyle.DUO), capsule
            )
        }

        private fun circular(style: DualStyle?): RingGeometry {
            val ring = RingGeometry()
            ring.setCircular(style, 10f, 8f, 110f, 108f, 30f, 20f, 7.5f)
            return ring
        }

        private fun intersectsStroke(frame: OpeningBatteryMorph, label: FloatArray): Boolean {
            val radius = frame.strokeWidth / 2
            var index = 2
            while (index < frame.points.size) {
                if (index == OpeningBatteryMorph.Companion.SIDE_POINTS * 2) {
                    index += 2
                    continue
                }
                if (segmentIntersects(
                        frame.points[index - 2], frame.points[index - 1],
                        frame.points[index], frame.points[index + 1],
                        label[0] - radius, label[1] - radius,
                        label[2] + radius, label[3] + radius
                    )
                ) {
                    return true
                }
                index += 2
            }
            return false
        }

        private fun segmentIntersects(
            x1: Float, y1: Float, x2: Float, y2: Float,
            left: Float, top: Float, right: Float, bottom: Float
        ): Boolean {
            var from = 0f
            var to = 1f
            val first = floatArrayOf(x1, y1)
            val delta = floatArrayOf(x2 - x1, y2 - y1)
            val minimum = floatArrayOf(left, top)
            val maximum = floatArrayOf(right, bottom)
            for (axis in 0..1) {
                if (abs(delta[axis]) < 0.000001f) {
                    if (first[axis] < minimum[axis] || first[axis] > maximum[axis]) {
                        return false
                    }
                } else {
                    val start = (minimum[axis] - first[axis]) / delta[axis]
                    val end = (maximum[axis] - first[axis]) / delta[axis]
                    from = max(from, min(start, end))
                    to = min(to, max(start, end))
                    if (from > to) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
