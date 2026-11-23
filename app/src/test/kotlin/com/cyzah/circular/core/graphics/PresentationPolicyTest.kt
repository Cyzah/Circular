package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class PresentationPolicyTest {
    @Test
    fun configuredMergedIconOwnsItsFirstBatteryFrameWhileNetworksBind() {
        Assert.assertTrue(PresentationPolicy.ownsMerged(true, true, 2, false))
        Assert.assertTrue(PresentationPolicy.ownsMerged(true, true, 2, true))
        Assert.assertFalse(PresentationPolicy.ownsMerged(false, true, 2, true))
        Assert.assertFalse(PresentationPolicy.ownsMerged(true, false, 2, true))
        Assert.assertFalse(PresentationPolicy.ownsMerged(true, true, 0, false))
    }

    @Test
    fun keyguardRemainsOwnedDuringVisibleScreenOffFade() {
        Assert.assertTrue(PresentationPolicy.brightKeyguard(true, true, true))
        Assert.assertTrue(PresentationPolicy.brightKeyguard(false, true, true))
        Assert.assertFalse(PresentationPolicy.brightKeyguard(false, false, true))
        Assert.assertFalse(PresentationPolicy.brightKeyguard(false, true, false))
    }
}
