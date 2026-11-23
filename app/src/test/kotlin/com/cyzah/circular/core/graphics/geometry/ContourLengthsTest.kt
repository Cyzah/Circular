package com.cyzah.circular.core.graphics.geometry

import org.junit.Assert
import org.junit.Test

class ContourLengthsTest {
    @Test
    fun contentCutoutsKeepLengthsAndEndpointForMoreThanFourContours() {
        val lengths = ContourLengths()
        for (length in floatArrayOf(10f, 20f, 30f, 40f, 50f, 60f)) {
            lengths.add(length)
        }

        Assert.assertEquals(6, lengths.count().toLong())
        Assert.assertEquals(210.0f, lengths.total(), 0.001f)
        Assert.assertEquals(150.0f, lengths.start(5), 0.001f)
        Assert.assertEquals(5, lengths.contourAt(0.9f).toLong())
        Assert.assertEquals(39.0f, lengths.localDistance(5, 0.9f), 0.001f)
        Assert.assertEquals(5, lengths.contourAt(1.0f).toLong())
        Assert.assertEquals(60.0f, lengths.localDistance(5, 1.0f), 0.001f)
    }

    @Test
    fun resetClearsAllContoursAfterTheLengthBufferGrows() {
        val lengths = ContourLengths()
        for (length in 1..12) {
            lengths.add(length.toFloat())
        }
        Assert.assertEquals(12, lengths.count().toLong())
        Assert.assertEquals(78.0f, lengths.total(), 0.001f)

        lengths.reset()
        Assert.assertEquals(0, lengths.count().toLong())
        Assert.assertEquals(-1, lengths.contourAt(1.0f).toLong())
        lengths.add(7.0f)

        Assert.assertEquals(1, lengths.count().toLong())
        Assert.assertEquals(7.0f, lengths.total(), 0.001f)
        Assert.assertEquals(0, lengths.contourAt(1.0f).toLong())
        Assert.assertEquals(7.0f, lengths.localDistance(0, 1.0f), 0.001f)
    }

    @Test
    fun progressContinuesAcrossADisconnectedPathContour() {
        try {
            val type = Class.forName(
                "com.cyzah.circular.core.graphics.geometry.ContourLengths"
            )
            val lengths: Any = type.getDeclaredConstructor().newInstance()
            type.getDeclaredMethod("reset").invoke(lengths)
            val add =
                type.getDeclaredMethod("add", Float::class.javaPrimitiveType)
            add.invoke(lengths, 100.0f)
            add.invoke(lengths, 50.0f)

            Assert.assertEquals(
                150.0f,
                (type.getDeclaredMethod("total")
                    .invoke(lengths) as Float),
                0.001f
            )
            Assert.assertEquals(
                0,
                (type.getDeclaredMethod(
                    "contourAt",
                    Float::class.javaPrimitiveType
                ).invoke(lengths, 0.5f) as Int).toLong()
            )
            Assert.assertEquals(
                75.0f,
                (type.getDeclaredMethod(
                    "localDistance",
                    Int::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                ).invoke(lengths, 0, 0.5f) as Float),
                0.001f
            )
            Assert.assertEquals(
                1,
                (type.getDeclaredMethod(
                    "contourAt",
                    Float::class.javaPrimitiveType
                ).invoke(lengths, 0.8f) as Int).toLong()
            )
            Assert.assertEquals(
                20.0f,
                (type.getDeclaredMethod(
                    "localDistance",
                    Int::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                ).invoke(lengths, 1, 0.8f) as Float),
                0.001f
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing multi-contour progress mapping",
                error
            )
        }
    }
}
