package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class DuoSignalRendererTest {
    @Test
    fun rowIsAvailableOnlyWhenCardIsKnownAndInService() {
        Assert.assertTrue(DuoSignalRenderer.Companion.available(true, true, true, false))
        Assert.assertFalse(DuoSignalRenderer.Companion.available(false, true, true, false))
        Assert.assertFalse(DuoSignalRenderer.Companion.available(true, false, true, false))
        Assert.assertFalse(DuoSignalRenderer.Companion.available(true, true, false, false))
        Assert.assertFalse(DuoSignalRenderer.Companion.available(true, true, true, true))
    }

    @Test
    fun cardRowStateUsesTheSelectedCardWithoutTemporaryArrays() {
        Assert.assertTrue(DuoSignalRenderer.Companion.rowAvailable(0, true, false))
        Assert.assertFalse(DuoSignalRenderer.Companion.rowAvailable(1, true, false))
        Assert.assertEquals(2, DuoSignalRenderer.Companion.rowLevel(0, 2, 4).toLong())
        Assert.assertEquals(4, DuoSignalRenderer.Companion.rowLevel(1, 2, 4).toLong())
    }
}
