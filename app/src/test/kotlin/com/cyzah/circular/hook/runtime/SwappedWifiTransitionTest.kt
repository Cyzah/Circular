package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.SwappedWifiMotion
import com.cyzah.circular.core.graphics.animation.TransitionRetargetState
import com.cyzah.circular.core.graphics.geometry.TransitionGeometry
import com.cyzah.circular.hook.runtime.SwappedWifiTransitionTest
import org.junit.Assert
import org.junit.Test

class SwappedWifiTransitionTest {
    @Test
    fun geometryInterpolatesCenterAndUniformScale() {
        val source: TransitionGeometry = geometry(10.0f, 20.0f, 10.0f, 10.0f)
        val target: TransitionGeometry = geometry(30.0f, 60.0f, 20.0f, 20.0f)
        val output = TransitionGeometry()

        TransitionGeometry.Companion.interpolateSimilarity(
            source,
            target,
            0.5f,
            output
        )

        Assert.assertEquals(20.0f, output.centreX, EPSILON)
        Assert.assertEquals(40.0f, output.centreY, EPSILON)
        Assert.assertEquals(15.0f, output.width(), EPSILON)
        Assert.assertEquals(15.0f, output.height(), EPSILON)
    }

    @Test
    fun swappedWifiUsesTheProjectedRingEndpointAtCollapse() {
        val motion = SwappedWifiMotion()

        motion.resolve(
            110.0f,
            60.0f,
            1.0f,
            240.0f,
            80.0f,
            0.5f,
            40.0f,
            40.0f,
            0.0f,
            true
        )

        Assert.assertEquals(240.0f, motion.rendered().centreX, EPSILON)
        Assert.assertEquals(80.0f, motion.rendered().centreY, EPSILON)
        Assert.assertEquals(20.0f, motion.rendered().width(), EPSILON)
        Assert.assertEquals(20.0f, motion.rendered().height(), EPSILON)
    }

    @Test
    fun swappedWifiLayoutUsesTheActualRingOriginAndHeaderAnchor() {
        val motion = SwappedWifiMotion()

        motion.resolveLayout(
            100.0f,
            60.0f,
            1.0f,
            120.0f,
            240.0f,
            80.0f,
            0.5f,
            0.5f,
            40.0f,
            0.0f,
            40.0f,
            40.0f,
            0.0f,
            true
        )

        Assert.assertEquals(270.0f, motion.rendered().centreX, EPSILON)
        Assert.assertEquals(100.0f, motion.rendered().centreY, EPSILON)
    }

    @Test
    fun lateNativeTargetKeepsTheCurrentWifiPositionWithoutJump() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 20.0f)
        val target: TransitionGeometry = geometry(200.0f, 50.0f, 20.0f, 20.0f)
        val state = TransitionRetargetState()

        state.resolve(source, target, 0.6f, false)
        val beforeTarget = state.rendered().centreX
        state.resolve(source, target, 0.6f, true)

        Assert.assertEquals(beforeTarget, state.rendered().centreX, EPSILON)
        state.resolve(source, target, 0.0f, true)
        Assert.assertEquals(200.0f, state.rendered().centreX, EPSILON)
    }

    @Test
    fun readyProjectedTargetContinuesFollowingBaseMotion() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 20.0f)
        val firstTarget: TransitionGeometry = geometry(200.0f, 50.0f, 20.0f, 20.0f)
        val movedTarget: TransitionGeometry = geometry(220.0f, 50.0f, 20.0f, 20.0f)
        val state = TransitionRetargetState()

        state.resolve(source, firstTarget, 0.5f, true)
        Assert.assertEquals(150.0f, state.rendered().centreX, EPSILON)
        state.resolve(source, movedTarget, 0.4f, true)

        Assert.assertEquals(172.0f, state.rendered().centreX, EPSILON)
        state.resolve(source, movedTarget, 0.0f, true)
        Assert.assertEquals(220.0f, state.rendered().centreX, EPSILON)
        state.resolve(source, movedTarget, 1.0f, true)
        Assert.assertEquals(100.0f, state.rendered().centreX, EPSILON)
    }

    @Test
    fun reverseUsesTheSameProgressWithoutStateDrift() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 20.0f)
        val target: TransitionGeometry = geometry(200.0f, 50.0f, 40.0f, 40.0f)
        val state = TransitionRetargetState()

        state.resolve(source, target, 0.4f, true)
        val expectedX = state.rendered().centreX
        val expectedWidth = state.rendered().width()
        state.resolve(source, target, 0.8f, true)
        state.resolve(source, target, 0.4f, true)

        Assert.assertEquals(expectedX, state.rendered().centreX, EPSILON)
        Assert.assertEquals(expectedWidth, state.rendered().width(), EPSILON)
    }

    @Test
    fun aCompletedGestureDoesNotReplayThePreviousLateTargetCorrection() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 20.0f)
        val target: TransitionGeometry = geometry(200.0f, 50.0f, 20.0f, 20.0f)
        val state = TransitionRetargetState()

        state.resolve(source, target, 0.6f, false)
        state.resolve(source, target, 0.6f, true)
        state.resolve(source, target, 0.0f, true)
        state.resolve(source, target, 0.6f, true)

        Assert.assertEquals(140.0f, state.rendered().centreX, EPSILON)
    }

    @Test
    fun unavailableProjectionKeepsTheLatestReadyEndpoint() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 20.0f)
        val target: TransitionGeometry = geometry(200.0f, 50.0f, 20.0f, 20.0f)
        val state = TransitionRetargetState()

        state.resolve(source, target, 0.6f, true)
        target.setCentred(220.0f, 50.0f, 20.0f, 20.0f)
        state.resolve(source, target, 0.5f, true)
        state.resolve(source, target, 0.4f, false)

        Assert.assertEquals(172.0f, state.rendered().centreX, EPSILON)
    }

    @Test
    fun losingLateNativeTargetKeepsTheEstablishedCorrection() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 30.0f)
        val target: TransitionGeometry = geometry(200.0f, 80.0f, 40.0f, 60.0f)
        val state = TransitionRetargetState()

        for (ready in booleanArrayOf(false, true, false, false)) {
            state.resolve(source, target, 0.6f, ready)
            Assert.assertEquals(100.0f, state.rendered().centreX, EPSILON)
            Assert.assertEquals(50.0f, state.rendered().centreY, EPSILON)
            Assert.assertEquals(20.0f, state.rendered().width(), EPSILON)
            Assert.assertEquals(30.0f, state.rendered().height(), EPSILON)
        }

        state.resolve(source, target, 0.3f, false)
        Assert.assertEquals(150.0f, state.rendered().centreX, EPSILON)
        Assert.assertEquals(65.0f, state.rendered().centreY, EPSILON)
        Assert.assertEquals(30.0f, state.rendered().width(), EPSILON)
        Assert.assertEquals(45.0f, state.rendered().height(), EPSILON)
        state.resolve(source, target, 0.0f, false)
        Assert.assertEquals(200.0f, state.rendered().centreX, EPSILON)
        Assert.assertEquals(80.0f, state.rendered().centreY, EPSILON)
        Assert.assertEquals(40.0f, state.rendered().width(), EPSILON)
        Assert.assertEquals(60.0f, state.rendered().height(), EPSILON)
    }

    @Test
    fun lostNativeTargetCorrectionStillEndsAtTheExactSource() {
        val source: TransitionGeometry = geometry(100.0f, 50.0f, 20.0f, 30.0f)
        val target: TransitionGeometry = geometry(200.0f, 80.0f, 40.0f, 60.0f)
        val state = TransitionRetargetState()
        state.resolve(source, target, 0.6f, false)
        state.resolve(source, target, 0.6f, true)
        state.resolve(source, target, 0.6f, false)

        state.resolve(source, target, 1.0f, false)

        Assert.assertEquals(100.0f, state.rendered().centreX, EPSILON)
        Assert.assertEquals(50.0f, state.rendered().centreY, EPSILON)
        Assert.assertEquals(20.0f, state.rendered().width(), EPSILON)
        Assert.assertEquals(30.0f, state.rendered().height(), EPSILON)
    }

    @Test
    fun invalidGeometryIsRejected() {
        val invalid = TransitionGeometry()
        invalid.set(Float.NaN, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)

        Assert.assertFalse(invalid.valid())
        Assert.assertTrue(geometry(0.0f, 0.0f, 1.0f, 1.0f).valid())
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun geometry(
            centreX: Float,
            centreY: Float,
            width: Float,
            height: Float
        ): TransitionGeometry {
            val geometry = TransitionGeometry()
            geometry.setCentred(centreX, centreY, width, height)
            return geometry
        }
    }
}
