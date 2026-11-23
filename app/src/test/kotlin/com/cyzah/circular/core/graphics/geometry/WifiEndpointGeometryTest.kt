package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.geometry.WifiEndpointGeometryTest
import com.cyzah.circular.domain.settings.DualStyle
import org.junit.Assert
import org.junit.Test

class WifiEndpointGeometryTest {
    @Test
    fun duoTargetMatchesTheStaticGlyphAfterPivotScaling() {
        val centre = WifiEndpointGeometry.centreY(
            DualStyle.DUO, true, false, true,
            57.0f, 20.0f, 57.175f, 1.4f
        )

        Assert.assertEquals(55.245f, centre, EPSILON)
    }

    @Test
    fun groupedWithoutNumberUsesItsStaticWifiHeight() {
        val centre = WifiEndpointGeometry.centreY(
            DualStyle.GROUPED, true, false, false,
            57.0f, 20.0f, 57.175f, 1.0f
        )

        Assert.assertEquals(58.175f, centre, EPSILON)
    }

    @Test
    fun swappedWifiIsAlreadyCentredAndOnlyAppliesTheDualOffset() {
        Assert.assertEquals(
            18.0f, WifiEndpointGeometry.centreY(
                DualStyle.LAYERED, true, true, true,
                57.0f, 20.0f, 57.175f, 1.4f
            ), EPSILON
        )
        Assert.assertEquals(
            20.0f, WifiEndpointGeometry.centreY(
                DualStyle.GROUPED, false, true, true,
                57.0f, 20.0f, 57.175f, 1.4f
            ), EPSILON
        )
    }

    @Test
    fun collapsedWifiUsesTheUserWeightBeforeReturningToTheStaticOwner() {
        Assert.assertEquals(160, WifiEndpointGeometry.weight(160, 110, 0.0f).toLong())
        Assert.assertEquals(135, WifiEndpointGeometry.weight(160, 110, 0.5f).toLong())
        Assert.assertEquals(110, WifiEndpointGeometry.weight(160, 110, 1.0f).toLong())
    }

    @Test
    fun duoConnectionFadeSurvivesTheOwnerHandoff() {
        Assert.assertEquals(0.25f, WifiEndpointGeometry.alpha(0.25f, 0.0f), EPSILON)
        Assert.assertEquals(0.625f, WifiEndpointGeometry.alpha(0.25f, 0.5f), EPSILON)
        Assert.assertEquals(1.0f, WifiEndpointGeometry.alpha(0.25f, 1.0f), EPSILON)
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
