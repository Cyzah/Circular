package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Test

class WifiDisconnectTransitionTest {
    @Test
    fun connectedToDisconnectedRequestsOneNativeHold() {
        val transition =
            WifiDisconnectTransition()

        transition.observe(true, true)
        transition.observe(true, false)

        Assert.assertTrue(transition.consume())
        Assert.assertFalse(transition.consume())
    }

    @Test
    fun startupDisconnectedDoesNotRequestAHold() {
        val transition =
            WifiDisconnectTransition()

        transition.observe(true, false)

        Assert.assertFalse(transition.consume())
    }
}
