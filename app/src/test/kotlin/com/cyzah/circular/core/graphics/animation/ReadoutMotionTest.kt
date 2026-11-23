package com.cyzah.circular.core.graphics.animation

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class ReadoutMotionTest {
    @Test
    fun startingFramePreservesTheActualReadoutPositionAndSize() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 0f))
        Assert.assertEquals(20f, motion.centreX, 0f)
        Assert.assertEquals(25f, motion.centreY, 0f)
        Assert.assertEquals(1f, motion.scale, 0f)
        Assert.assertEquals(10f, motion.mapX(10f), 0f)
        Assert.assertEquals(30f, motion.mapY(30f), 0f)
    }

    @Test
    fun intermediateReadoutMovesAndScalesWithTheBattery() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 0.5f))
        Assert.assertEquals(60f, motion.centreX, 0f)
        Assert.assertEquals(37.5f, motion.centreY, 0f)
        Assert.assertEquals(1.5f, motion.scale, 0f)
        Assert.assertEquals(45f, motion.mapX(10f), 0f)
        Assert.assertEquals(30f, motion.mapY(20f), 0f)
    }

    @Test
    fun endFrameAlignsRealGlyphCentresAndHeightDespiteDifferentFontWidths() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 110f, 60f, 1f))
        Assert.assertEquals(95f, motion.mapX(20f), 0f)
        Assert.assertEquals(50f, motion.mapY(25f), 0f)
        Assert.assertEquals(40f, motion.mapY(20f), 0f)
        Assert.assertEquals(60f, motion.mapY(30f), 0f)
        Assert.assertEquals(40f, motion.mapX(30f) - motion.mapX(10f), 0f)
        Assert.assertEquals(2f, motion.scale, 0f)
    }

    @Test
    fun reverseGestureRetracesTheSameReadoutPlacement() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 0.4f))
        val forwardX = motion.mapX(12f)
        val forwardY = motion.mapY(23f)
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 1f))
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 0.4f))
        Assert.assertEquals(forwardX, motion.mapX(12f), 0f)
        Assert.assertEquals(forwardY, motion.mapY(23f), 0f)
    }

    @Test
    fun outOfRangeGestureStopsAtEndpoints() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, -1f))
        Assert.assertEquals(20f, motion.centreX, 0f)
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 2f))
        Assert.assertEquals(100f, motion.centreX, 0f)
    }

    @Test
    fun invalidMetricsClearPreviouslyPreparedMotion() {
        val motion = ReadoutMotion()
        Assert.assertTrue(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, 1f))
        Assert.assertFalse(motion.update(10f, 20f, 30f, 20f, 80f, 40f, 120f, 60f, 0.5f))
        Assert.assertFalse(motion.valid)
        Assert.assertFalse(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 120f, 60f, Float.NaN))
        Assert.assertFalse(motion.update(10f, 20f, 30f, 30f, 80f, 40f, 80f, 60f, 0.5f))
        Assert.assertFalse(motion.update(10f, 20f, 30f, 30f, 80f, 40f, Float.POSITIVE_INFINITY, 60f, 1f))
    }
}
