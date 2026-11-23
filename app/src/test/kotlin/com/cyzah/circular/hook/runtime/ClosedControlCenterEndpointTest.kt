package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline
import com.cyzah.circular.hook.runtime.ClosedControlCenterEndpointTest
import org.junit.Assert
import org.junit.Test

/** Closing must not expose the control-center template while host 0 is transparent.  */
class ClosedControlCenterEndpointTest {
    @Test
    fun transparentStatusHostKeepsAUserConfiguredMergedEndpoint() {
        Assert.assertTrue(usesEndpoint(true, false, 0.0f, 0.0f))
    }

    @Test
    fun endpointUsesTheExistingClosedThresholds() {
        Assert.assertTrue(
            usesEndpoint(
                true, false,
                BatteryTransferConfig.HANDOFF_MAX,
                BatteryTransferConfig.CLOSED_MORPH_MAX
            )
        )
        Assert.assertFalse(
            usesEndpoint(
                true, false,
                Math.nextUp(BatteryTransferConfig.HANDOFF_MAX),
                BatteryTransferConfig.CLOSED_MORPH_MAX
            )
        )
        Assert.assertFalse(
            usesEndpoint(
                true, false,
                BatteryTransferConfig.HANDOFF_MAX,
                Math.nextUp(BatteryTransferConfig.CLOSED_MORPH_MAX)
            )
        )
    }

    @Test
    fun activeClosingGeometryContinuesToOwnIntermediateFrames() {
        Assert.assertFalse(usesEndpoint(true, false, 0.01f, 0.01f))
        Assert.assertFalse(usesEndpoint(true, false, 1.0f, 1.0f))
        Assert.assertFalse(usesEndpoint(true, false, 0.0f, 0.01f))
    }

    @Test
    fun statusAndLockscreenHostsDoNotBecomeControlCenterCopies() {
        Assert.assertFalse(usesEndpoint(false, false, 0.0f, 0.0f))
        Assert.assertFalse(usesEndpoint(false, true, 0.0f, 0.0f))
    }

    @Test
    fun visibleStatusHostExclusivelyOwnsTheClosedIcon() {
        Assert.assertFalse(usesEndpoint(true, true, 0.0f, 0.0f))
        Assert.assertTrue(
            SessionPaintPolicy.defersClosedControlCenterCopy(
                true, true, 0.0f, 0.0f, 0.0f
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.defersClosedControlCenterCopy(
                true, false, 0.0f, 0.0f, 0.0f
            )
        )
    }

    @Test
    fun ordinaryNotificationsKeepTheirLocalMergedCopy() {
        Assert.assertTrue(usesLocalNotificationCopy(true, false, false))
    }

    @Test
    fun frozenControlCenterTransitionDoesNotUseLocalNotificationGeometry() {
        Assert.assertFalse(usesLocalNotificationCopy(true, true, false))
        Assert.assertFalse(usesLocalNotificationCopy(true, true, true))
    }

    @Test
    fun waitingAndRingTransferDoNotLoseTheirProjectedEndpointToNotifications() {
        Assert.assertFalse(usesLocalNotificationCopy(true, false, true))
    }

    @Test
    fun absentNotificationCopyNeverCreatesALocalOwner() {
        for (rowTransition in booleanArrayOf(false, true)) {
            for (transferActive in booleanArrayOf(false, true)) {
                Assert.assertFalse(usesLocalNotificationCopy(false, rowTransition, transferActive))
            }
        }
    }

    @Test
    fun heldEndpointSurvivesWaitingAndReleaseUntilTheStatusHostCanPaint() {
        val fixture = Fixture()
        fixture.update(0.0f, true)
        fixture.update(1.0f, false)
        fixture.update(0.0f, false)

        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertTrue(fixture.state.handoffHoldVisible(0.0f, 0.0f, 0.0f))
        Assert.assertTrue(fixture.state.pointVisible())
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertTrue(usesEndpoint(true, false, 0.0f, 0.0f))

        fixture.now += 5000L
        fixture.update(0.0f, false)
        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), 0.0f)
        Assert.assertFalse(fixture.state.needsFrame())
        Assert.assertTrue(usesEndpoint(true, false, 0.0f, 0.0f))

        fixture.update(0.0f, true)
        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
        Assert.assertTrue(fixture.state.ringTransferVisible())
        Assert.assertTrue(fixture.state.pointVisible())
        Assert.assertTrue(usesEndpoint(true, false, 0.0f, 0.0f))
        Assert.assertFalse(usesEndpoint(true, true, 0.0f, 0.0f))
    }

    private class Fixture {
        @JvmField var now: Long = 1000L
        @JvmField val state: BatteryTransferState = BatteryTransferState(
            BatteryTransferTimeline(BatteryTransferTimeline.Clock { now })
        )

        fun update(control: Float, released: Boolean) {
            state.update(control, 0.0f, control, true, true, true, released, now)
        }
    }

    companion object {
        private fun usesEndpoint(
            controlCenterHost: Boolean,
            statusHostAvailable: Boolean, control: Float, morph: Float
        ): Boolean {
            return SessionPaintPolicy.usesMergedControlCenterEndpoint(
                controlCenterHost, statusHostAvailable, control, morph
            )
        }

        private fun usesLocalNotificationCopy(
            notificationCopy: Boolean,
            rowTransition: Boolean, transferActive: Boolean
        ): Boolean {
            return SessionPaintPolicy.usesLocalNotificationCopy(
                notificationCopy, rowTransition, transferActive
            )
        }
    }
}
