package com.cyzah.circular.core.graphics.geometry

import org.junit.Assert
import org.junit.Test

class IconGeometryTest {
    @Test
    fun outerDotsStayCentered() {
        Assert.assertEquals(
            60.0f,
            (IconGeometry.outerDotX(0, 1.2f)
                    + IconGeometry.outerDotX(3, 1.2f)) / 2.0f,
            0.001f
        )
    }

    @Test
    fun collapsedInnerDotsStayCentered() {
        val averageX = ((IconGeometry.innerDotX(0, 0.0f, 1.2f)
                + IconGeometry.innerDotX(1, 0.0f, 1.2f)
                + IconGeometry.innerDotX(2, 0.0f, 1.2f))) / 3.0f
        Assert.assertEquals(60.0f, averageX, 0.001f)
    }

    @Test
    fun technologyBaseOnlySplitsShortSuffixes() {
        Assert.assertEquals("5G", IconGeometry.technologyBase("5GA"))
        Assert.assertEquals("5G", IconGeometry.technologyBase("5G+"))
        Assert.assertEquals("5G", IconGeometry.technologyBase("5GSA"))
    }
}
