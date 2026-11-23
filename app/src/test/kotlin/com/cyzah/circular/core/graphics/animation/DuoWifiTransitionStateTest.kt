package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.DuoWifiTransitionStateTest
import org.junit.Assert
import org.junit.Test

class DuoWifiTransitionStateTest {
    @Test
    fun firstObservationSetsTheInitialTargetWithoutAnimation() {
        val state = DuoWifiTransitionState()

        Assert.assertFalse(state.begin(true, 1000L))
        Assert.assertEquals(1.0f, state.progress(), EPSILON)
    }

    @Test
    fun disconnectMovesWifiAndSignalsOverTheFullDuration() {
        val state = DuoWifiTransitionState()
        state.begin(true, 1000L)

        Assert.assertTrue(state.begin(false, 1100L))
        Assert.assertEquals(0.5f, state.advance(1230L, 260L), EPSILON)
        Assert.assertEquals(0.0f, state.advance(1360L, 260L), EPSILON)
        Assert.assertTrue(state.complete(1360L, 260L))
    }

    @Test
    fun reconnectReversesFromTheCurrentRenderedProgress() {
        val state = DuoWifiTransitionState()
        state.begin(true, 1000L)
        state.begin(false, 1100L)
        state.advance(1230L, 260L)

        Assert.assertTrue(state.begin(true, 1250L))
        Assert.assertEquals(0.75f, state.advance(1380L, 260L), EPSILON)
        Assert.assertEquals(1.0f, state.advance(1510L, 260L), EPSILON)
    }

    @Test
    fun duplicateTargetDoesNotStartASecondTimeline() {
        val state = DuoWifiTransitionState()
        state.begin(true, 1000L)
        state.begin(false, 1100L)
        state.advance(1160L, 260L)
        val before = state.progress()

        Assert.assertFalse(state.begin(false, 1170L))
        Assert.assertEquals(before, state.progress(), EPSILON)
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
