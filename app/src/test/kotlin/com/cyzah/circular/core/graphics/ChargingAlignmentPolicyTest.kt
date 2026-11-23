package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class ChargingAlignmentPolicyTest {
    @Test
    fun closedChargingOffsetIsCapturedAndHeldDuringExpansion() {
        val policy = ChargingAlignmentPolicy()

        Assert.assertEquals(29, policy.resolve(29, true, false, true).toLong())
        Assert.assertEquals(29, policy.resolve(0, true, false, false).toLong())
        Assert.assertEquals(29, policy.resolve(100, true, false, false).toLong())
        Assert.assertEquals(29, policy.resolve(0, true, false, true).toLong())
    }

    @Test
    fun openPanelTransientsAreNotCapturedAsChargingOffset() {
        val policy = ChargingAlignmentPolicy()

        Assert.assertEquals(0, policy.resolve(1, true, false, false).toLong())
        Assert.assertEquals(0, policy.resolve(100, true, false, false).toLong())
        Assert.assertEquals(29, policy.resolve(29, true, false, true).toLong())
    }

    @Test
    fun dualRowModeKeepsChargingAlignmentDisabled() {
        val policy = ChargingAlignmentPolicy()

        Assert.assertEquals(0, policy.resolve(29, true, true, true).toLong())
        Assert.assertEquals(0, policy.resolve(0, true, false, true).toLong())
    }

    @Test
    fun dischargingRestoresRawAlignmentAndClearsCapture() {
        val policy = ChargingAlignmentPolicy()

        Assert.assertEquals(29, policy.resolve(29, true, false, true).toLong())
        Assert.assertEquals(17, policy.resolve(17, false, false, true).toLong())
        Assert.assertEquals(0, policy.resolve(0, true, false, true).toLong())
        Assert.assertEquals(29, policy.resolve(29, true, false, true).toLong())
    }
}
