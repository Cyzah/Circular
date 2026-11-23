package com.cyzah.circular.core.network

import org.junit.Assert
import org.junit.Test

class NetworkReadinessHoldTest {
    @Test
    fun transientFailureKeepsLastReadyState() {
        val hold = NetworkReadinessHold()

        hold.observe(1000L, true, true)

        Assert.assertTrue(hold.ready(1042L, false))
    }

    @Test
    fun sustainedFailureFallsBackAfterGracePeriod() {
        val hold = NetworkReadinessHold()

        hold.observe(1000L, true, true)

        Assert.assertTrue(hold.ready(6000L, false))
        Assert.assertFalse(hold.ready(6001L, false))
    }

    @Test
    fun unavailableSourceNeverLatchesReady() {
        val hold = NetworkReadinessHold()

        hold.observe(1000L, false, false)

        Assert.assertFalse(hold.ready(1010L, false))
    }
}
