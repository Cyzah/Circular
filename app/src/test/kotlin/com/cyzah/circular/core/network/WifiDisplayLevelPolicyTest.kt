package com.cyzah.circular.core.network

import com.cyzah.circular.core.network.WifiDisplayLevelPolicyTest
import org.junit.Assert
import org.junit.Test

class WifiDisplayLevelPolicyTest {
    @Test
    fun missingRssiKeepsConnectedIconsMergedWhileMeasurementIsRetried() {
        val policy = WifiDisplayLevelPolicy()
        val state: IconState = readyConnectedState()
        state.wifiLevel = policy.resolve(1000L, true, true, 3)
        Assert.assertTrue(state.readyForMerge())

        state.wifiLevel = policy.resolve(1050L, true, true, -1)

        Assert.assertEquals(3, state.wifiLevel.toLong())
        Assert.assertTrue(state.showWifi())
        Assert.assertTrue(state.readyForMerge())
        Assert.assertTrue(policy.needsMeasurement())
    }

    @Test
    fun prolongedMeasurementFailureStopsDisplayingOldStrength() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, 3)
        policy.resolve(1500L, true, true, -1)
        policy.resolve(4500L, true, true, -1)

        Assert.assertEquals(0, policy.resolve(10000L, true, true, -1).toLong())
        Assert.assertTrue(policy.needsMeasurement())
    }

    @Test
    fun connectedNetworkWithoutAnyValidStrengthKeepsMergeAndRequestsMeasurement() {
        val policy = WifiDisplayLevelPolicy()
        val state: IconState = readyConnectedState()

        state.wifiLevel = policy.resolve(1000L, true, true, -1)

        Assert.assertEquals(0, state.wifiLevel.toLong())
        Assert.assertTrue(state.showWifi())
        Assert.assertTrue(state.readyForMerge())
        Assert.assertTrue(policy.needsMeasurement())
    }

    @Test
    fun explicitDisconnectRemovesWifiImmediatelyWithoutRequestingStrength() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, 3)
        val state: IconState = readyConnectedState()
        state.wifiConnected = false

        state.wifiLevel = policy.resolve(1010L, true, false, -1)

        Assert.assertEquals(-1, state.wifiLevel.toLong())
        Assert.assertFalse(state.showWifi())
        Assert.assertTrue(state.readyForMerge())
        Assert.assertFalse(policy.needsMeasurement())
    }

    @Test
    fun reconnectDoesNotReuseStrengthFromDisconnectedNetwork() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, 3)
        policy.resolve(1100L, true, false, -1)

        Assert.assertEquals(0, policy.resolve(1200L, true, true, -1).toLong())
        Assert.assertTrue(policy.needsMeasurement())
    }

    @Test
    fun disablingWifiClearsStrengthEvenWhenOldConnectionSamplePersists() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, 3)

        Assert.assertEquals(-1, policy.resolve(1100L, false, true, 3).toLong())
        Assert.assertFalse(policy.needsMeasurement())
        Assert.assertEquals(0, policy.resolve(1200L, true, true, -1).toLong())
    }

    @Test
    fun validMeasurementReplacesTemporaryStrengthAndEndsRecovery() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, -1)
        Assert.assertTrue(policy.needsMeasurement())

        Assert.assertEquals(2, policy.resolve(2000L, true, true, 2).toLong())
        Assert.assertFalse(policy.needsMeasurement())
    }

    @Test
    fun sourceRestartClearsStrengthFromPreviousBinding() {
        val policy = WifiDisplayLevelPolicy()
        policy.resolve(1000L, true, true, 3)

        policy.reset()

        Assert.assertFalse(policy.needsMeasurement())
        Assert.assertEquals(0, policy.resolve(1100L, true, true, -1).toLong())
    }

    companion object {
        private fun readyConnectedState(): IconState {
            val state = IconState()
            state.battery = 80
            state.wifiKnown = true
            state.wifiEnabled = true
            state.wifiConnected = true
            state.mobileKnown = true
            state.simPresent = true
            state.inService = true
            state.mobileLevel = 3
            return state
        }
    }
}
