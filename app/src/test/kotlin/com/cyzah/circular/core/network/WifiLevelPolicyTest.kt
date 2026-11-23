package com.cyzah.circular.core.network

import org.junit.Assert
import org.junit.Test

class WifiLevelPolicyTest {
    private fun calculator(callback: (Boolean, Int) -> Int): WifiLevelPolicy.Calculator {
        return object : WifiLevelPolicy.Calculator {
            override fun calculate(fiveGhz: Boolean, rssi: Int): Int = callback(fiveGhz, rssi)
        }
    }

    @Test
    fun usesFirstAvailableCalculatorAndClampsToFourBars() {
        val policy = WifiLevelPolicy(
            calculator { fiveGhz: Boolean, rssi: Int -> 9 },
            calculator { fiveGhz: Boolean, rssi: Int -> 2 },
            calculator { fiveGhz: Boolean, rssi: Int -> 1 })

        Assert.assertEquals(3, policy.calculate(5180, -55).toLong())
        Assert.assertEquals("systemui-tracker", policy.source())
    }

    @Test
    fun fallsBackAfterCalculatorFailure() {
        val policy = WifiLevelPolicy(
            calculator { fiveGhz: Boolean, rssi: Int ->
                throw IllegalStateException("unavailable")
            },
            calculator { fiveGhz: Boolean, rssi: Int -> 2 },
            calculator { fiveGhz: Boolean, rssi: Int -> 1 })

        Assert.assertEquals(2, policy.calculate(2412, -60).toLong())
        Assert.assertEquals("miui-rssi", policy.source())
    }

    @Test
    fun invalidRssiDoesNotInvokeCalculators() {
        val policy = WifiLevelPolicy(
            calculator { fiveGhz: Boolean, rssi: Int ->
                throw AssertionError("must not run")
            },
            null,
            null
        )

        Assert.assertEquals(-1, policy.calculate(2412, 10).toLong())
        Assert.assertEquals("unavailable", policy.source())
    }
}
