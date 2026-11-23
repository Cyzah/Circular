package com.cyzah.circular.hook.runtime

import org.junit.Assert
import org.junit.Test

class NativeIconOwnershipTest {
    @Test
    fun transitionHoldCannotHideIconsWithoutAnAvailableRenderer() {
        for (host in intArrayOf(0, 5, 6)) {
            Assert.assertFalse(
                SessionPaintPolicy.hidesNativeSignalState(
                    host, false, true, true, false, true, false
                )
            )
        }
    }

    @Test
    fun unavailableRendererReleasesEvenStaleMergeOwnership() {
        Assert.assertFalse(
            SessionPaintPolicy.hidesNativeSignalState(
                6, true, true, true, false, true, false
            )
        )
    }

    @Test
    fun activeMergedRendererKeepsRecreatedIconsHiddenDuringDisconnect() {
        for (host in intArrayOf(0, 5, 6)) {
            Assert.assertTrue(
                SessionPaintPolicy.hidesNativeSignalState(
                    host, true, false, true, false, true, true
                )
            )
        }
    }

    @Test
    fun ambientOrUnownedRowsKeepTheirNativeIcons() {
        Assert.assertFalse(
            SessionPaintPolicy.hidesNativeSignalState(
                1, true, true, true, true, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesNativeSignalState(
                6, false, false, false, false, false, true
            )
        )
    }
}
