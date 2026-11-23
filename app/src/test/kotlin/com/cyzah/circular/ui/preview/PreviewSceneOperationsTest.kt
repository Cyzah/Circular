package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import org.junit.Assert
import org.junit.Test

class PreviewSceneOperationsTest {
    @Test
    fun enablingDualPreservesKnownSignalLevels() {
        val state = IconState()
        state.mobileLevel = 3
        state.secondaryLevel = 2

        PreviewSceneOperations.setDual(state, true)

        Assert.assertTrue(state.dualSim())
        Assert.assertEquals(3, state.mobileLevel.toLong())
        Assert.assertEquals(2, state.secondaryLevel.toLong())
    }

    @Test
    fun disablingDualClearsSecondaryCardOnly() {
        val state = IconState()
        state.secondarySimPresent = true
        state.secondaryKnown = true
        state.secondaryInService = true
        state.secondaryLevel = 4

        PreviewSceneOperations.setDual(state, false)

        Assert.assertFalse(state.secondarySimPresent)
        Assert.assertFalse(state.secondaryKnown)
        Assert.assertFalse(state.secondaryInService)
        Assert.assertEquals(-1, state.secondaryLevel.toLong())
    }

    @Test
    fun cardOneNoServiceKeepsCardPresent() {
        val state = IconState()
        state.secondaryLevel = -1

        PreviewSceneOperations.setCard1NoService(state, true)

        Assert.assertTrue(state.secondarySimPresent)
        Assert.assertTrue(state.secondaryKnown)
        Assert.assertEquals(4, state.secondaryLevel.toLong())
        Assert.assertFalse(state.secondaryInService)
    }

    @Test
    fun wifiUpdateDrivesConnectionState() {
        val state = IconState()

        PreviewSceneOperations.setWifiEnabled(state, true)
        Assert.assertTrue(state.wifiEnabled)
        Assert.assertTrue(state.wifiConnected)

        PreviewSceneOperations.setWifiEnabled(state, false)
        Assert.assertFalse(state.wifiEnabled)
        Assert.assertFalse(state.wifiConnected)
    }
}
