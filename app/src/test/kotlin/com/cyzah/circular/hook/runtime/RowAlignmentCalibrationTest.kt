package com.cyzah.circular.hook.runtime

import org.junit.Assert
import org.junit.Test

class RowAlignmentCalibrationTest {
    @Test
    fun zeroShiftLocksAfterTwoStableSamples() {
        val calibration = RowAlignmentCalibration()
        Assert.assertFalse(calibration.sample(100f, 100f))
        Assert.assertTrue(calibration.sample(100f, 100f))
        Assert.assertTrue(calibration.isCalibrated)
        calibration.sample(200f, 140f)
        calibration.sample(200f, 140f)
        Assert.assertEquals(0.0f, calibration.hostShift(), 0.0f)
    }

    @Test
    fun acceptedShiftRemainsFixedWhenCompensationMovesHosts() {
        val calibration = RowAlignmentCalibration()
        Assert.assertFalse(calibration.sample(100f, 162f))
        Assert.assertTrue(calibration.sample(101f, 163f))
        Assert.assertEquals(-62.0f, calibration.hostShift(), 0.0f)
        Assert.assertEquals(62.0f, calibration.speedShift(), 0.0f)
        calibration.sample(163f, 163f)
        calibration.sample(163f, 163f)
        Assert.assertEquals(-62.0f, calibration.hostShift(), 0.0f)
    }

    @Test
    fun movingOrImplausibleSamplesCannotLock() {
        val calibration = RowAlignmentCalibration()
        Assert.assertFalse(calibration.sample(100f, 162f))
        Assert.assertFalse(calibration.sample(110f, 172f))
        Assert.assertFalse(calibration.sample(300f, 100f))
        Assert.assertFalse(calibration.sample(300f, 100f))
        Assert.assertFalse(calibration.isCalibrated)
        Assert.assertFalse(calibration.sample(200f, 100f))
        Assert.assertTrue(calibration.sample(200f, 100f))
    }

    @Test
    fun resetRequiresFreshStableSamplesAndClearsBothShifts() {
        val calibration = RowAlignmentCalibration()
        calibration.sample(100f, 162f)
        calibration.sample(100f, 162f)
        calibration.reset()
        Assert.assertFalse(calibration.isCalibrated)
        Assert.assertEquals(0.0f, calibration.hostShift(), 0.0f)
        Assert.assertEquals(0.0f, calibration.speedShift(), 0.0f)
        Assert.assertFalse(calibration.sample(100f, 162f))
        Assert.assertTrue(calibration.sample(100f, 162f))
    }

    @Test
    fun invalidCoordinatesBreakTheStableSequence() {
        val calibration = RowAlignmentCalibration()
        calibration.sample(100f, 162f)
        Assert.assertFalse(calibration.sample(Float.NaN, 162f))
        Assert.assertFalse(calibration.sample(100f, 162f))
        Assert.assertTrue(calibration.sample(100f, 162f))
    }
}
