package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.platform.OemCompatibility
import org.junit.Assert
import org.junit.Test

class OemCompatibilityPolicyTest {
    @Test
    fun normalizesSupportedLevelsAndRejectsInvalidRanges() {
        Assert.assertEquals(0, OemCompatibility.normalizedLevel(0, 4).toLong())
        Assert.assertEquals(1, OemCompatibility.normalizedLevel(1, 4).toLong())
        Assert.assertEquals(3, OemCompatibility.normalizedLevel(2, 4).toLong())
        Assert.assertEquals(4, OemCompatibility.normalizedLevel(3, 4).toLong())
        Assert.assertEquals(-1, OemCompatibility.normalizedLevel(4, 4).toLong())
        Assert.assertEquals(-1, OemCompatibility.normalizedLevel(0, 1).toLong())
    }

    @Test
    fun statusBarHostIsEligibleOnlyWhenAllForegroundGuardsPass() {
        Assert.assertTrue(
            OemCompatibility.eligible(
                true, true, true, true, true, false, false, true, 0
            )
        )
        Assert.assertFalse(
            OemCompatibility.eligible(
                true, true, true, true, true, true, false, true, 0
            )
        )
        Assert.assertFalse(
            OemCompatibility.eligible(
                true, true, true, true, true, false, false, true, 1
            )
        )
    }

    @Test
    fun keyguardOrPanelHostsRequireUnlockedLockscreenWindow() {
        Assert.assertTrue(
            OemCompatibility.eligible(
                true, true, true, true, true, false, true, true, 1
            )
        )
        Assert.assertFalse(
            OemCompatibility.eligible(
                true, true, true, true, true, false, true, false, 1
            )
        )
        Assert.assertFalse(
            OemCompatibility.eligible(
                true, true, true, true, true, false, true, true, 0
            )
        )
    }
}
