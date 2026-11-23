package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.domain.settings.DisplayPolicy
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class NotificationShadeMotionTest {
    private val transfer = BatteryTransferState(
        BatteryTransferTimeline(object : BatteryTransferTimeline.Clock {
            override fun now(): Long {
                return 0L
            }
        })
    )

    @Test
    fun notificationGesturesKeepMergedContentWithoutStartingBatteryTransfer() {
        transfer.update(0f, 0f, 0f, true, true, true, false, 0L)
        for (shade in floatArrayOf(0.02f, 0.5f, 1f, 0.4f, 0f)) {
            Assert.assertEquals(0f, DisplayPolicy.expansion(0, false, shade, 0f), 0f)
            transfer.update(0f, shade, 0f, true, true, true, false, 10L)
            Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, transfer.phase())
            Assert.assertFalse(transfer.batteryShapeVisible())
            Assert.assertFalse(transfer.needsFrame())
        }
    }

    @Test
    fun switchingFromNotificationToControlCenterStillStartsTheOpeningMorph() {
        transfer.update(0f, 1f, 0f, true, true, true, false, 0L)
        transfer.update(0.3f, 0.7f, 0.3f, true, true, false, false, 10L)
        Assert.assertTrue(transfer.openingShape())
        Assert.assertEquals(Motion.ease(0.3f), DisplayPolicy.expansion(0, false, 0.7f, 0.3f), 0f)
    }

    @Test
    fun notificationExpansionDoesNotExtendTheControlCenterHandoff() {
        transfer.update(0f, 0f, 0f, true, true, true, false, 0L)
        transfer.update(1f, 0f, 1f, true, true, false, false, 10L)
        transfer.update(0f, 1f, 0f, true, true, true, false, 20L)
        Assert.assertEquals(BatteryTransferState.Phase.WAITING_FOR_HANDOFF, transfer.phase())
        Assert.assertTrue(transfer.handoffHoldVisible(0f, 1f, 0f))
        transfer.update(0f, 1f, 0f, true, true, true, true, 30L)
        Assert.assertEquals(BatteryTransferState.Phase.PATH_TO_LOWER_LEFT, transfer.phase())
    }

    @Test
    fun theNativeFakeRowKeepsItsMergedCopyUntilItsOwnVisibilityAnimationFinishes() {
        var copy = SessionPaintPolicy.notificationCopyActive(5, false, 0f, 0f)
        Assert.assertFalse(copy)
        copy = SessionPaintPolicy.notificationCopyActive(5, copy, 0f, 1f)
        Assert.assertTrue(copy)
        copy = SessionPaintPolicy.notificationCopyActive(5, copy, 0f, 0f)
        Assert.assertTrue(SessionPaintPolicy.drawsMergedNotificationCopy(copy, 0f, 0f))
        // A subsequent control-center gesture releases both local drawing and
        // local reservation, retaining the accepted projected CC handoff.
        copy = SessionPaintPolicy.notificationCopyActive(5, copy, 0.4f, 0.6f)
        Assert.assertFalse(copy)
        Assert.assertFalse(SessionPaintPolicy.drawsMergedNotificationCopy(copy, 0f, 0f))
        Assert.assertFalse(SessionPaintPolicy.notificationCopyActive(6, true, 0f, 1f))
    }
}
