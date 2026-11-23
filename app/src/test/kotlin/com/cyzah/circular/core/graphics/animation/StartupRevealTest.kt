package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class StartupRevealTest {
    @Test
    fun aNewHostDoesNotEmitAnOpaqueWhiteFirstFrame() {
        val reveal = StartupReveal()
        Assert.assertEquals(0.0f, reveal.alpha(1000, true), 0.0f)
        Assert.assertEquals(0.5f, reveal.alpha(1080, true), 0.0001f)
        Assert.assertEquals(1.0f, reveal.alpha(1160, true), 0.0f)
        Assert.assertEquals(1.0f, reveal.alpha(5000, true), 0.0f)
    }

    @Test
    fun nativeTintStartsTheRevealWithoutReleasingModuleOwnership() {
        val reveal = StartupReveal()
        Assert.assertEquals(0.0f, reveal.alpha(1000, false), 0.0f)
        Assert.assertEquals(0.0f, reveal.alpha(5000, true), 0.0f)
        Assert.assertEquals(1.0f, reveal.alpha(5160, true), 0.0f)
    }
}
