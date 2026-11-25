package com.cyzah.circular.domain.settings

import org.junit.Assert
import org.junit.Test

class DualStyleAvailabilityTest {
    @Test
    fun singleSimAllowsOnlyDefaultRingAndSingleCapsule() {
        Assert.assertTrue(DualStyleAvailability.allows(DualStyle.LAYERED, false))
        Assert.assertFalse(DualStyleAvailability.allows(DualStyle.GROUPED, false))
        Assert.assertTrue(DualStyleAvailability.allows(DualStyle.CAPSULE, false))
        Assert.assertFalse(DualStyleAvailability.allows(DualStyle.DUO, false))
    }

    @Test
    fun restoredSingleSimStyleFallsBackToTheDefaultRing() {
        Assert.assertTrue(
            DualStyleAvailability.fallback(DualStyle.CAPSULE, false)
                    == DualStyle.CAPSULE
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
    fun singleSimDisablesGroupedAndDuoChoices() {
        val enabled = DualStyleAvailability.enabledOptions(false)

        Assert.assertTrue(enabled[DualStyle.LAYERED.ordinal])
        Assert.assertFalse(enabled[DualStyle.GROUPED.ordinal])
        Assert.assertTrue(enabled[DualStyle.CAPSULE.ordinal])
        Assert.assertFalse(enabled[DualStyle.DUO.ordinal])
    }
}
