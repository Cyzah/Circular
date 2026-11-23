package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.DisplayPolicyTest
import com.cyzah.circular.hook.runtime.SessionSettings
import org.junit.Assert
import org.junit.Test

class DisplayPolicyTest {
    @Test
    fun chargingWinsOverEveryOtherActiveStatusIncludingLowBattery() {
        for (flags in 0..3) {
            Assert.assertEquals(
                -0xedcbaa, DisplayPolicy.batteryTint(
                    1, true,
                    (flags and 1) != 0, (flags and 2) != 0, true, PLAIN, 20,
                    0x00123456, -0x340000, -0x5600, -0xff7734
                ).toLong()
            )
        }
    }

    @Test
    fun disabledStatusColoursKeepTheNativeTintEvenWhileCharging() {
        Assert.assertEquals(
            PLAIN.toLong(), DisplayPolicy.batteryTint(
                1, true, true, true, false,
                PLAIN, 20, 1, 2, 3, 4
            ).toLong()
        )
    }

    @Test
    fun freshSettingsUseTheRequestedStatusColoursAndTwentyPercentBoundary() {
        val settings: SessionSettings = SessionSettings.Companion.defaults()
        Assert.assertEquals(-0xff6000, tint(settings, 80, true, true, true).toLong())
        Assert.assertEquals(-0xd9da, tint(settings, 19, false, false, false).toLong())
        Assert.assertEquals(PLAIN.toLong(), tint(settings, 20, false, false, false).toLong())
        Assert.assertEquals(-0x5b00, tint(settings, 10, false, true, false).toLong())
        Assert.assertEquals(-0xd07f13, tint(settings, 10, false, false, true).toLong())
    }

    companion object {
        private const val PLAIN = -0xf0f10

        private fun tint(
            settings: SessionSettings, battery: Int, charging: Boolean,
            saver: Boolean, performance: Boolean
        ): Int {
            return DisplayPolicy.batteryTint(
                battery, charging, saver, performance, true,
                PLAIN, settings.lowBatteryThreshold(), settings.chargeColor(), settings.lowColor(),
                settings.saverColor(), settings.performanceColor()
            )
        }
    }
}
