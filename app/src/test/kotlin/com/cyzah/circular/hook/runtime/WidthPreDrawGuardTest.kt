package com.cyzah.circular.hook.runtime

import org.junit.Assert
import org.junit.Test

class WidthPreDrawGuardTest {
    @Test
    fun closedEndpointCorrectsAnOversizedReservationWithTheSameBoundedBudget() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(90, 60, 0.0f, true))
        Assert.assertTrue(guard.shouldDefer(90, 60, 0.0f, true))
        Assert.assertFalse(guard.shouldDefer(90, 60, 0.0f, true))
        Assert.assertFalse(guard.shouldDefer(60, 60, 0.0f, true))
        Assert.assertTrue(guard.shouldDefer(90, 60, 0.0f, true))
    }

    @Test
    fun missingSpaceDefersUntilTheRequestedWidthIsLaidOut() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(292, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
    }

    @Test
    fun anUnresponsiveLayoutCannotCancelEveryFrame() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
    }

    @Test
    fun movingRightEdgeCannotRenewTheRetryBudgetByGrowingWidth() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(292, 354, 0.98f))
        Assert.assertFalse(guard.shouldDefer(354, 416, 0.98f))
        Assert.assertFalse(guard.shouldDefer(416, 478, 0.98f))
    }

    @Test
    fun aMovingGestureCannotRenewTheBudgetOnEveryVsync() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 290, 0.97f))
        Assert.assertFalse(guard.shouldDefer(230, 288, 0.96f))
        Assert.assertFalse(guard.shouldDefer(230, 286, 0.95f))
        Assert.assertFalse(guard.shouldDefer(286, 286, 0.95f))
        Assert.assertTrue(guard.shouldDefer(230, 284, 0.94f))
    }

    @Test
    fun shrinkingReservationDrawsImmediatelyAndClearsOldRetries() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(292, 230, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
    }

    @Test
    fun explicitResetStartsANewCorrectionBudget() {
        val guard = WidthPreDrawGuard()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
        guard.reset()
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertTrue(guard.shouldDefer(230, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 292, 0.98f))
    }

    @Test
    fun unknownGeometryCannotBlockDrawing() {
        val guard = WidthPreDrawGuard()
        Assert.assertFalse(guard.shouldDefer(0, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(-1, 292, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, 0, 0.98f))
        Assert.assertFalse(guard.shouldDefer(230, -1, 0.98f))
    }

    @Test
    fun nonfiniteProgressCannotBlockDrawing() {
        val guard = WidthPreDrawGuard()
        Assert.assertFalse(guard.shouldDefer(230, 292, Float.NaN))
        Assert.assertFalse(guard.shouldDefer(230, 292, Float.POSITIVE_INFINITY))
        Assert.assertFalse(guard.shouldDefer(230, 292, Float.NEGATIVE_INFINITY))
    }
}
