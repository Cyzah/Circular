package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class NotificationMotionWorkTest {
    @Test
    fun aPureNotificationDragDoesNotRescanNetworksOrRedrawEveryHost() {
        Assert.assertFalse(NotificationMotionWork.update(false, 0f, 0.3f, 0.4f, false))
        Assert.assertFalse(NotificationMotionWork.update(false, 0f, 0.9f, 0.5f, false))
        Assert.assertFalse(NotificationMotionWork.update(false, 0f, 0f, 0f, false))
    }

    @Test
    fun lifecycleBoundariesAndControlCenterTransfersStillUpdate() {
        Assert.assertTrue(NotificationMotionWork.update(false, 0f, 0f, 0.01f, false))
        Assert.assertTrue(NotificationMotionWork.update(false, 0f, 0.01f, 0.04f, false))
        Assert.assertTrue(NotificationMotionWork.update(false, 0f, 0.01f, 0f, false))
        Assert.assertTrue(NotificationMotionWork.update(false, 0f, 0.4f, 0.5f, true))
        Assert.assertTrue(NotificationMotionWork.update(false, 0.3f, 0.4f, 0.5f, false))
        Assert.assertTrue(NotificationMotionWork.update(true, 0f, 0f, 0f, false))
    }
}
