package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.geometry.BatteryTransferGeometryKeyTest
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.hook.runtime.SessionSettings
import org.junit.Assert
import org.junit.Test

class BatteryTransferGeometryKeyTest {
    @Test
    fun enablingOnlySecondarySimRequiresFreshRingGeometry() {
        val state: IconState = state()
        val settings: SessionSettings = SessionSettings.Companion.defaults()
        val key = BatteryTransferGeometryKey()
        key.store(state, settings, 0.0f)

        state.secondarySimPresent = true

        Assert.assertFalse(key.matches(state, settings, 0.0f))
        key.store(state, settings, 0.0f)
        Assert.assertTrue(key.matches(state, settings, 0.0f))
    }

    @Test
    fun disablingOnlySecondarySimRequiresFreshRingGeometry() {
        val state: IconState = state()
        state.secondarySimPresent = true
        val settings: SessionSettings = SessionSettings.Companion.defaults()
        val key = BatteryTransferGeometryKey()
        key.store(state, settings, 0.0f)

        state.secondarySimPresent = false

        Assert.assertFalse(key.matches(state, settings, 0.0f))
        key.store(state, settings, 0.0f)
        Assert.assertTrue(key.matches(state, settings, 0.0f))
    }

    @Test
    fun equivalentFramesReuseTheMeasuredGeometry() {
        val state: IconState = state()
        val settings: SessionSettings = SessionSettings.Companion.defaults()
        val key = BatteryTransferGeometryKey()
        Assert.assertFalse(key.matches(state, settings, 0.0f))
        key.store(state, settings, 0.0f)

        state.mobileTechnology = "5G"
        state.mobileLevel = 4
        state.secondaryLevel = 3
        state.secondaryInService = true

        Assert.assertTrue(key.matches(state, settings, 0.0f))
    }

    @Test
    fun changedAppearanceRequiresFreshRingGeometry() {
        val state: IconState = state()
        val key = BatteryTransferGeometryKey()
        key.store(state, SessionSettings.Companion.defaults(), 0.0f)

        Assert.assertFalse(key.matches(state, SessionSettings.Companion.defaults(), 0.0f))
    }

    @Test
    fun duoWifiMotionRequiresFreshRingGeometry() {
        val state: IconState = state()
        val settings: SessionSettings = SessionSettings.Companion.defaults()
        val key = BatteryTransferGeometryKey()
        key.store(state, settings, 0.0f)

        Assert.assertFalse(key.matches(state, settings, 0.5f))
    }

    companion object {
        private fun state(): IconState {
            val state = IconState()
            state.battery = 74
            state.wifiKnown = true
            state.mobileKnown = true
            state.simPresent = true
            state.inService = true
            state.mobileTechnology = "5G"
            return state
        }
    }
}
