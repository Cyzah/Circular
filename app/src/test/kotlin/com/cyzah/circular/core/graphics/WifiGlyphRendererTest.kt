package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class WifiGlyphRendererTest {
    @Test
    fun litArcsUseFullOpacityAndUnlitArcsUseTrackOpacity() {
        Assert.assertEquals(255, WifiGlyphRenderer.Companion.arcAlpha(2, 1).toLong())
        Assert.assertEquals(102, WifiGlyphRenderer.Companion.arcAlpha(2, 2).toLong())
    }

    @Test
    fun zeroBarsLeavesEveryArcOnTheTrack() {
        Assert.assertEquals(102, WifiGlyphRenderer.Companion.arcAlpha(0, 0).toLong())
        Assert.assertEquals(102, WifiGlyphRenderer.Companion.arcAlpha(-1, 2).toLong())
    }
}
