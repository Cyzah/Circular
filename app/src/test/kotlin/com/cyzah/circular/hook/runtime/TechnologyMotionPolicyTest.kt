package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.hook.runtime.TechnologyMotionPolicyTest
import org.junit.Assert
import org.junit.Test

class TechnologyMotionPolicyTest {
    @Test
    fun textKeepsExpandedAndMergedEndpoints() {
        Assert.assertEquals(
            1.0f,
            StackedOverlayLayout.technologyProgress(1.0f), EPSILON
        )
        Assert.assertEquals(
            0.0f,
            StackedOverlayLayout.technologyProgress(0.0f), EPSILON
        )
    }

    @Test
    fun closingTextHasTravelledLessAtTheSameGestureFraction() {
        val expansions = floatArrayOf(0.9f, 0.75f, 0.5f, 0.25f, 0.1f)
        for (expansion in expansions) {
            val oldDistance = 1.0f - expansion
            val textDistance = (1.0f
                    - StackedOverlayLayout.technologyProgress(expansion))
            Assert.assertTrue(
                "Text must keep moving toward the merged endpoint",
                textDistance > 0.0f
            )
            Assert.assertTrue(
                "Text must lag the old gesture channel at " + expansion,
                textDistance < oldDistance
            )
        }
    }

    @Test
    fun closingTextLagsFurtherThanThePreviousSquaredClosingCurve() {
        for (expansion in floatArrayOf(0.9f, 0.75f, 0.5f, 0.25f, 0.1f)) {
            val closing = 1.0f - expansion
            val previousDistance = closing * closing
            val textDistance = (1.0f
                    - StackedOverlayLayout.technologyProgress(expansion))
            Assert.assertTrue(
                "Text must travel less than the previous curve at " + expansion,
                textDistance < previousDistance
            )
        }
    }

    @Test
    fun halfwayClosedTextHasOnlyTravelledOneEighthOfItsPath() {
        Assert.assertEquals(
            0.125f,
            1.0f - StackedOverlayLayout.technologyProgress(0.5f), EPSILON
        )
    }

    @Test
    fun horizontalTextAlsoLagsForEveryBatteryNumberPlacement() {
        for (placement in BatteryNumberPlacement.values()) {
            for (expansion in floatArrayOf(0.9f, 0.75f, 0.5f, 0.25f)) {
                val oldX = StackedOverlayLayout.technologyProgress(expansion)
                val textX = StackedOverlayLayout.technologyXProgress(expansion)
                Assert.assertTrue(
                    "Text X must lag for " + placement + " at " + expansion,
                    textX >= oldX && textX <= 1.0f
                )
            }
        }
    }

    @Test
    fun textProgressRemainsBoundedAndFiniteForInvalidInputs() {
        Assert.assertEquals(
            0.0f,
            StackedOverlayLayout.technologyProgress(-1.0f), EPSILON
        )
        Assert.assertEquals(
            1.0f,
            StackedOverlayLayout.technologyProgress(2.0f), EPSILON
        )
        for (input in floatArrayOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)) {
            val actual = StackedOverlayLayout.technologyProgress(input)
            Assert.assertTrue(java.lang.Float.isFinite(actual))
            Assert.assertTrue(actual >= 0.0f && actual <= 1.0f)
        }
    }

    @Test
    fun closingTextMovesContinuouslyWithoutReversing() {
        var previous = StackedOverlayLayout.technologyProgress(1.0f)
        for (sample in 1..1000) {
            val expansion = 1.0f - (sample / 1000.0f)
            val current = StackedOverlayLayout.technologyProgress(expansion)
            Assert.assertTrue("Closing text must not reverse", current <= previous)
            Assert.assertTrue("Closing text must not jump", previous - current < 0.003f)
            previous = current
        }
        Assert.assertEquals(0.0f, previous, EPSILON)
    }

    @Test
    fun reversingAndReplayingGestureRetracesTheSameTextPositions() {
        val gesture = floatArrayOf(1.0f, 0.8f, 0.6f, 0.4f, 0.2f, 0.0f)
        val recorded = FloatArray(gesture.size)
        for (index in gesture.indices) {
            recorded[index] = StackedOverlayLayout.technologyProgress(gesture[index])
        }
        for (index in gesture.indices.reversed()) {
            Assert.assertEquals(
                recorded[index],
                StackedOverlayLayout.technologyProgress(gesture[index]), 0.0f
            )
        }
        for (index in gesture.indices) {
            Assert.assertEquals(
                recorded[index],
                StackedOverlayLayout.technologyProgress(gesture[index]), 0.0f
            )
        }
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
