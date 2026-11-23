package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.geometry.ContentPositionGeometryTest
import java.lang.reflect.Method
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class ContentPositionGeometryTest {
    @Test
    fun anOpeningIgnoresContentBeyondItsOppositeHemisphere() {
        for (left in booleanArrayOf(true, false)) {
            Assert.assertEquals(
                0f, ContentPositionGeometry.openingEdge(
                    true, left,
                    60f, 58f, 50f, 50f, 40f, 87.5f, 80f, 111.5f, 6.5f
                ), 0f
            )
            Assert.assertEquals(
                0f, ContentPositionGeometry.openingEdge(
                    false, left,
                    60f, 58f, 50f, 50f, 40f, 0f, 80f, 24f, 6.5f
                ), 0f
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun halfPixelCountsStayHalfAPhysicalPixelAtEveryScale() {
        val convert: Method?
        try {
            convert = ContentPositionGeometry::class.java.getMethod(
                "halfPixelUnits",
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
        } catch (missing: NoSuchMethodException) {
            throw AssertionError("Content offsets need half-pixel conversion", missing)
        }
        for (unit in floatArrayOf(.15f, .5f, .625f, 1.25f, 2f)) {
            val first = convert.invoke(null, 7, unit) as Float
            val second = convert.invoke(null, 8, unit) as Float
            Assert.assertEquals(.5f, (second - first) * unit, .00001f)
            Assert.assertEquals(-first, (convert.invoke(null, -7, unit) as Float?)!!, .00001f)
        }
        Assert.assertEquals(0f, (convert.invoke(null, 1, 0f) as Float?)!!, 0f)
    }

    @Test
    @Throws(Exception::class)
    fun movingAnOpeningLeftShortensItsLeftArcAndExtendsItsRightArc() {
        val edge: Method?
        try {
            edge = ContentPositionGeometry::class.java.getMethod(
                "openingEdge",
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
        } catch (missing: NoSuchMethodException) {
            throw AssertionError("Each opening edge must be measured independently", missing)
        }
        val left = edge.invoke(
            null, true, true, 60f, 58f, 50f, 50f,
            30f, 0f, 70f, 20f, 5f
        ) as Float
        val right = edge.invoke(
            null, true, false, 60f, 58f, 50f, 50f,
            30f, 0f, 70f, 20f, 5f
        ) as Float
        Assert.assertEquals(44.677f, left, .002f)
        Assert.assertEquals(17.708f, right, .002f)
        Assert.assertTrue(left > 30.25f)
        Assert.assertTrue(right < 30.25f)
    }

    @Test
    fun onePixelRemainsOnePixelAtDifferentIconScales() {
        for (unit in floatArrayOf(.15f, .5f, .625f, 1.25f, 2f)) {
            val first = ContentPositionGeometry.units(7, unit)
            val second = ContentPositionGeometry.units(8, unit)
            Assert.assertEquals(1f, (second - first) * unit, .00001f)
            Assert.assertEquals(-first, ContentPositionGeometry.units(-7, unit), .00001f)
        }
    }

    @Test
    fun invalidScaleCannotCreateInfiniteDrawingCoordinates() {
        Assert.assertEquals(0f, ContentPositionGeometry.units(1, 0f), 0f)
        Assert.assertEquals(0f, ContentPositionGeometry.units(1, Float.NaN), 0f)
        Assert.assertEquals(0f, ContentPositionGeometry.units(1, Float.POSITIVE_INFINITY), 0f)
    }

    @Test
    fun openingGrowsWhenTheSameContentMovesSideways() {
        val initial: Float = gap(40f, 0f, 80f, 20f)
        val moved: Float = gap(50f, 0f, 90f, 20f)
        Assert.assertEquals(30.25f, initial, .001f)
        Assert.assertTrue(moved > initial + 10)
        Assert.assertEquals(moved, gap(30f, 0f, 70f, 20f), .001f)
    }

    @Test
    fun movingContentClearOfTheRingReleasesItsOpening() {
        Assert.assertTrue(gap(40f, 0f, 80f, 20f) > 0)
        Assert.assertEquals(0f, gap(40f, -100f, 80f, -80f), 0f)
        Assert.assertEquals(0f, gap(45f, 35f, 75f, 45f), 0f)
    }

    @Test
    fun openingIncludesEndcapClearanceAndVerticalMovement() {
        val narrow = ContentPositionGeometry.halfGap(
            true, 60f, 58f, 50f, 50f,
            40f, 0f, 80f, 20f, 0f
        )
        Assert.assertTrue(gap(40f, 0f, 80f, 20f) > narrow)
        Assert.assertNotEquals(gap(40f, 0f, 80f, 20f), gap(40f, 30f, 80f, 50f), .1f)
        Assert.assertEquals(
            gap(40f, 0f, 80f, 20f),
            ContentPositionGeometry.halfGap(
                false, 60f, 58f, 50f, 50f,
                40f, 96f, 80f, 116f, 5f
            ), .001f
        )
    }

    @Test
    fun offsetInterpolationFollowsTheMovingSlot() {
        Assert.assertEquals(-3f, ContentPositionGeometry.mix(-3f, 9f, 0f), 0f)
        Assert.assertEquals(3f, ContentPositionGeometry.mix(-3f, 9f, .5f), 0f)
        Assert.assertEquals(9f, ContentPositionGeometry.mix(-3f, 9f, 1f), 0f)
    }

    companion object {
        private fun gap(l: Float, t: Float, r: Float, b: Float): Float {
            return ContentPositionGeometry.halfGap(true, 60f, 58f, 50f, 50f, l, t, r, b, 5f)
        }
    }
}
