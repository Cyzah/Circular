package com.cyzah.circular.core.network

import org.junit.Assert
import org.junit.Test

class IconStateTest {
    @Test
    fun copyFromSnapshotsEverySignalTopologyField() {
        val source = IconState()
        source.airplane = true
        source.charging = true
        source.inService = true
        source.mobileKnown = true
        source.nativeMobile = true
        source.performanceMode = true
        source.powerSave = true
        source.simPresent = true
        source.wifiConnected = true
        source.wifiEnabled = true
        source.wifiKnown = true
        source.dataEnabled = false
        source.secondaryKnown = true
        source.secondarySimPresent = true
        source.secondaryInService = true
        source.secondaryLevel = 3
        source.secondaryRawLevel = 4
        source.secondarySubId = 22
        source.mobileSlot = 1
        source.secondarySlot = 0
        source.battery = 9
        source.wifiLevel = 2
        source.mobileLevel = 3
        source.subId = 11
        source.rawMobileLevel = 4
        source.mobileTechnology = "5G"
        val copy = IconState()

        Assert.assertTrue(copy.copyFrom(source))
        source.airplane = false
        source.secondarySimPresent = false
        source.battery = 10
        source.mobileTechnology = "5G+"

        Assert.assertTrue(copy.airplane)
        Assert.assertTrue(copy.secondarySimPresent)
        Assert.assertTrue(copy.dualSim())
        Assert.assertEquals(9, copy.battery.toLong())
        Assert.assertEquals("5G", copy.mobileTechnology)
        Assert.assertFalse(copy.dataEnabled)
        Assert.assertTrue(copy.copyFrom(copy))
    }
}
