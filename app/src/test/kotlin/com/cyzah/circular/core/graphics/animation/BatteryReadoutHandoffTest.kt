package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class BatteryReadoutHandoffTest {
    @Test
    fun visibleNumbersHaveOneContinuousCrossfadeBeforeTheRingHandoff() {
        val handoff = BatteryReadoutHandoff()
        handoff.update(1f, true, true)
        Assert.assertEquals(1f, handoff.sourceAlpha, 0f)
        Assert.assertEquals(0f, handoff.targetAlpha, 0f)
        Assert.assertEquals(0f, handoff.move, 0f)
        handoff.update(0.75f, true, true)
        Assert.assertTrue(handoff.move > 0 && handoff.move < 1)
        Assert.assertTrue(handoff.targetAlpha > 0 && handoff.targetAlpha < 1)
        Assert.assertEquals(1f, handoff.sourceAlpha + handoff.targetAlpha, 0.0001f)
        handoff.update(0.002f, true, true)
        Assert.assertEquals(0f, handoff.sourceAlpha, 0f)
        Assert.assertEquals(1f, handoff.targetAlpha, 0f)
        Assert.assertEquals(1f, handoff.move, 0f)
    }

    @Test
    fun hiddenPanelNumberFadesAtTheStatusAnchorBeforeItChangesHost() {
        val handoff = BatteryReadoutHandoff()
        var previous = 0f
        for (step in 100 downTo 0) {
            handoff.update(step / 100.0f, false, true)
            Assert.assertEquals(0f, handoff.sourceAlpha, 0f)
            Assert.assertEquals(1f, handoff.move, 0f)
            Assert.assertTrue(handoff.targetAlpha >= previous)
            Assert.assertTrue(handoff.targetAlpha - previous < 0.03f)
            previous = handoff.targetAlpha
        }
        handoff.update(0.002f, false, true)
        Assert.assertEquals(1f, handoff.targetAlpha, 0f)
    }

    @Test
    fun disabledStatusReadoutDoesNotInventADestinationNumber() {
        val handoff = BatteryReadoutHandoff()
        handoff.update(0.5f, true, false)
        Assert.assertTrue(handoff.sourceAlpha > 0 && handoff.sourceAlpha < 1)
        Assert.assertEquals(0f, handoff.targetAlpha, 0f)
        handoff.update(0f, true, false)
        Assert.assertEquals(0f, handoff.sourceAlpha, 0f)
        handoff.update(0.5f, false, false)
        Assert.assertEquals(0f, handoff.sourceAlpha + handoff.targetAlpha, 0f)
    }

    @Test
    fun reversingACloseGestureRetracesTheSameOpacityAndPosition() {
        val handoff = BatteryReadoutHandoff()
        handoff.update(0.73f, true, true)
        val alpha = handoff.targetAlpha
        val move = handoff.move
        handoff.update(0.3f, true, true)
        handoff.update(0.73f, true, true)
        Assert.assertEquals(alpha, handoff.targetAlpha, 0f)
        Assert.assertEquals(move, handoff.move, 0f)
    }
}
