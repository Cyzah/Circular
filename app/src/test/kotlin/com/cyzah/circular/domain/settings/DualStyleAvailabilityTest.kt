package com.cyzah.circular.domain.settings

import org.junit.Assert
import org.junit.Test

class DualStyleAvailabilityTest {
    @Test
    fun singleSimAllowsOnlyDefaultRingAndGroupedDots() {
        Assert.assertTrue(DualStyleAvailability.allows(DualStyle.LAYERED, false))
        Assert.assertTrue(DualStyleAvailability.allows(DualStyle.GROUPED, false))
        Assert.assertFalse(DualStyleAvailability.allows(DualStyle.CAPSULE, false))
        Assert.assertFalse(DualStyleAvailability.allows(DualStyle.DUO, false))
    }

    @Test
    fun restoredSingleSimStyleFallsBackToTheDefaultRing() {
        Assert.assertTrue(
            DualStyleAvailability.fallback(DualStyle.CAPSULE, false)
                    == DualStyle.LAYERED
        )
        Assert.assertTrue(
            DualStyleAvailability.fallback(DualStyle.DUO, false)
                    == DualStyle.LAYERED
        )
        Assert.assertTrue(
            DualStyleAvailability.fallback(DualStyle.DUO, true)
                    == DualStyle.DUO
        )
    }

    @Test
    fun singleSimDisablesCapsuleAndDuoChoices() {
        val enabled = DualStyleAvailability.enabledOptions(false)

        Assert.assertTrue(enabled[DualStyle.LAYERED.ordinal])
        Assert.assertTrue(enabled[DualStyle.GROUPED.ordinal])
        Assert.assertFalse(enabled[DualStyle.CAPSULE.ordinal])
        Assert.assertFalse(enabled[DualStyle.DUO.ordinal])
    }
}
