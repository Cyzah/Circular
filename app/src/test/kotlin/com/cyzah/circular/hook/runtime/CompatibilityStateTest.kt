package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.platform.Compatibility
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable

class CompatibilityStateTest {
    @Test
    fun unavailableCapabilityDoesNotDisableCore() {
        val result = Compatibility.Result()
        result.markUnavailable(
            Compatibility.Capability.BATTERY_POWER,
            "install", "mPerformanceMode missing"
        )

        Assert.assertTrue(result.isAvailable(Compatibility.Capability.CORE))
        Assert.assertFalse(result.isAvailable(Compatibility.Capability.BATTERY_POWER))
        Assert.assertEquals(
            "UNAVAILABLE",
            result.state(Compatibility.Capability.BATTERY_POWER)!!.status()!!.name
        )
    }

    @Test
    fun failedCoreMarksProjectUnsupported() {
        val result = Compatibility.Result()
        result.markFailed(Compatibility.Capability.CORE, "probe", "missing binding")

        Assert.assertFalse(result.isAvailable(Compatibility.Capability.CORE))
        Assert.assertTrue(result.details().contains("missing binding"))
    }

    @Test
    fun unknownContractGroupFailsFast() {
        Assert.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            ThrowingRunnable { Compatibility.Capability.Companion.fromGroup("typo") })
    }
}
