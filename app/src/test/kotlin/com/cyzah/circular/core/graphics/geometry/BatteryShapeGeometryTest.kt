package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometryTest
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class BatteryShapeGeometryTest {
    @Test
    fun verticalCompressionFollowsTheGestureWithoutPassingTheRingStart() {
        val shape = BatteryShapeGeometry()
        var previousBottom = Float.POSITIVE_INFINITY
        var previousDistance = Float.POSITIVE_INFINITY
        var firstCap = Float.NaN
        for (i in 0..100) {
            val collapse = (BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
                    + (1 - BatteryTransferConfig.HORIZONTAL_COLLAPSE_END) * i / 100)
            shape.update(collapse, 90f, 22f, 100f, 48f, 35f, 55f, 6f, 6f)
            val top = shape.centreY - shape.height / 2
            val bottom = shape.centreY + shape.height / 2
            val cap = top + 3
            if (i == 0) firstCap = cap
            Assert.assertTrue("The top cap must not pass the real ring start", cap <= 55 + .0001f)
            Assert.assertTrue(
                "The cap follows the gesture towards the endpoint",
                abs(55 - cap) <= previousDistance + .0001f
            )
            previousDistance = abs(55 - cap)
            Assert.assertTrue("Only the bottom travels upward", bottom <= previousBottom + .0001f)
            previousBottom = bottom
        }
        Assert.assertEquals(55f, shape.centreY, .0001f)
        Assert.assertEquals(6f, shape.height, .0001f)
        Assert.assertTrue("Do not pin the cap at the endpoint before vertical compression", firstCap < 54)
    }

    @Test
    fun verticalCompressionCanTranslateInEitherDirectionWithoutOvershooting() {
        val shape = BatteryShapeGeometry()
        shape.update(0f, 90f, 22f, 100f, 48f, 35f, 55f, 6f, 6f)
        Assert.assertEquals(22f, shape.centreY, .0001f)
        shape.update(BatteryTransferConfig.HORIZONTAL_COLLAPSE_END, 90f, 22f, 100f, 48f, 35f, 55f, 6f, 6f)
        Assert.assertEquals(48f, shape.height, .0001f)
        Assert.assertTrue(shape.centreY - shape.height / 2 + 3 < 55)
        var distance = Float.POSITIVE_INFINITY
        var bottom = Float.POSITIVE_INFINITY
        for (i in 0..100) {
            val c = (BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
                    + (1 - BatteryTransferConfig.HORIZONTAL_COLLAPSE_END) * i / 100)
            shape.update(c, 90f, 70f, 100f, 48f, 35f, 15f, 6f, 6f)
            val cap = shape.centreY - shape.height / 2 + 3
            Assert.assertTrue(cap >= 15 - .0001f)
            Assert.assertTrue(cap - 15 <= distance + .0001f)
            Assert.assertTrue(shape.centreY + shape.height / 2 <= bottom + .0001f)
            distance = cap - 15
            bottom = shape.centreY + shape.height / 2
        }
    }

    @Test
    fun completeBatteryAtZero() {
        val geometry: BatteryShapeGeometry = geometryAt(0.0f)

        Assert.assertEquals(BatteryShapeGeometry.Stage.COMPLETE, geometry.stage)
        Assert.assertEquals(112.0f, geometry.width, EPSILON)
        Assert.assertEquals(53.1667f, geometry.height, EPSILON)
        Assert.assertEquals(60.0f, geometry.centreX, EPSILON)
        Assert.assertEquals(53.0f, geometry.centreY, EPSILON)
        Assert.assertEquals(1.0f, geometry.batteryContentAlpha, EPSILON)
    }

    @Test
    fun compressedBatteryEndsAreTheTwoHalvesOfTheFinalPoint() {
        val geometry: BatteryShapeGeometry = geometryAt(0.65f)
        Assert.assertEquals(6.0f, geometry.cornerRadius, EPSILON)
        Assert.assertEquals(12.0f, geometry.bodyWidth, EPSILON)
        Assert.assertEquals(0f, geometry.bodyWidth - 2 * geometry.cornerRadius, EPSILON)
        Assert.assertTrue(geometry.height - 2 * geometry.cornerRadius > 41.0f)
        for (progress in floatArrayOf(0.65f, 0.7f, 0.8f, 0.9f, 0.99f, 1f)) {
            Assert.assertEquals(6.0f, geometryAt(progress).cornerRadius, EPSILON)
        }
    }

    @Test
    fun lateContourRoundsContinuouslyIntoTheRingPoint() {
        val before: BatteryShapeGeometry = geometryAt(0.99999f)
        val point: BatteryShapeGeometry = geometryAt(1.0f)
        Assert.assertEquals(point.cornerRadius, before.cornerRadius, 0.001f)
        Assert.assertEquals(point.width, before.width, 0.001f)
        Assert.assertEquals(point.bodyWidth, before.bodyWidth, 0.001f)
        Assert.assertEquals(point.height, before.height, 0.01f)
        for (step in 0..1000) {
            val frame: BatteryShapeGeometry = geometryAt(step / 1000.0f)
            Assert.assertTrue(frame.cornerRadius > 0)
            Assert.assertTrue(frame.cornerRadius <= min(frame.width, frame.height) / 2.0f)
        }
    }

    @Test
    fun rectangleAndPointUseTheActualConfiguredRingStrokeAtEverySize() {
        for (stroke in floatArrayOf(6f, 9f, 16.2f)) {
            for (ringUnit in floatArrayOf(0.25f, 0.6f, 1.0f)) {
                val shapeUnit = 0.8f
                val diameter = BatteryShapeLayout.transferPointDiameterUnits(stroke, ringUnit, shapeUnit)
                val geometry = BatteryShapeGeometry()
                geometry.update(0.65f, 60f, 53f, 112f, 53.1667f, 112f, 89f, diameter, diameter)
                Assert.assertEquals(stroke * ringUnit, geometry.bodyWidth * shapeUnit, EPSILON)
                Assert.assertEquals(stroke * ringUnit / 2, geometry.cornerRadius * shapeUnit, EPSILON)
                geometry.update(1f, 60f, 53f, 112f, 53.1667f, 112f, 89f, diameter, diameter)
                Assert.assertEquals(stroke * ringUnit, geometry.width * shapeUnit, EPSILON)
                Assert.assertEquals(geometry.width, geometry.height, EPSILON)
                Assert.assertEquals(geometry.width / 2, geometry.cornerRadius, EPSILON)
            }
        }
    }

    @Test
    fun collapseAtHorizontalEndIsRoundedLine() {
        val geometry: BatteryShapeGeometry = geometryAt(
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
        )

        Assert.assertEquals(BatteryShapeGeometry.Stage.LINE, geometry.stage)
        Assert.assertEquals(
            BatteryTransferConfig.POINT_DIAMETER_UNITS,
            geometry.width,
            EPSILON
        )
        Assert.assertEquals(53.1667f, geometry.height, EPSILON)
        Assert.assertEquals(0.0f, geometry.batteryContentAlpha, EPSILON)
    }

    @Test
    fun horizontalCompressionKeepsMostOfTheWidthUntilLate() {
        val midpointCollapse =
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END * 0.5f
        val geometry: BatteryShapeGeometry =
            geometryAt(midpointCollapse)

        Assert.assertEquals(87.0f, geometry.width, EPSILON)
    }

    @Test
    fun collapseAtOneIsAnchoredToTheProjectedRightEndpoint() {
        val geometry: BatteryShapeGeometry = geometryAt(1.0f)

        Assert.assertEquals(BatteryShapeGeometry.Stage.POINT, geometry.stage)
        Assert.assertEquals(112.0f, geometry.centreX, EPSILON)
        Assert.assertEquals(89.0f, geometry.centreY, EPSILON)
        Assert.assertEquals(
            BatteryTransferConfig.POINT_DIAMETER_UNITS,
            geometry.width,
            EPSILON
        )
        Assert.assertEquals(geometry.width, geometry.height, EPSILON)
        Assert.assertEquals(geometry.width / 2.0f, geometry.cornerRadius, EPSILON)
        Assert.assertTrue(java.lang.Float.isFinite(geometry.centreX))
        Assert.assertTrue(java.lang.Float.isFinite(geometry.centreY))
    }

    @Test
    fun expansionUsesTheStrictReverseOfCollapse() {
        val shrink: BatteryShapeGeometry = geometryAt(0.31f)
        val reverse: BatteryShapeGeometry = geometryAt(1.0f)

        reverse.update(
            0.31f,
            60.0f,
            53.0f,
            112.0f,
            53.1667f,
            112.0f,
            89.0f,
            BatteryTransferConfig.POINT_DIAMETER_UNITS,
            BatteryTransferConfig.POINT_DIAMETER_UNITS
        )

        Assert.assertEquals(shrink.width, reverse.width, EPSILON)
        Assert.assertEquals(shrink.height, reverse.height, EPSILON)
        Assert.assertEquals(shrink.centreX, reverse.centreX, EPSILON)
        Assert.assertEquals(shrink.centreY, reverse.centreY, EPSILON)
    }

    @Test
    fun horizontalStageApproachesTheRingStartWithoutOvershooting() {
        val collapses = floatArrayOf(0.0f, 0.2f, BatteryTransferConfig.HORIZONTAL_COLLAPSE_END)
        var previousTop = Float.NEGATIVE_INFINITY
        for (collapse in collapses) {
            val geometry = BatteryShapeGeometry()
            geometry.update(
                collapse,
                60.0f,
                53.0f,
                112.0f,
                53.1667f,
                112.0f,
                89.0f,
                BatteryTransferConfig.POINT_DIAMETER_UNITS,
                BatteryTransferConfig.POINT_DIAMETER_UNITS
            )
            val capCentre = (geometry.centreY - geometry.height / 2
                    + BatteryTransferConfig.POINT_DIAMETER_UNITS / 2)
            Assert.assertTrue(capCentre >= previousTop)
            Assert.assertTrue(capCentre <= 89 + EPSILON)
            Assert.assertEquals(53.1667f, geometry.height, EPSILON)
            previousTop = capCentre
        }
    }

    @Test
    fun verticalCompressionKeepsVisualHeightUntilLateInTheStage() {
        val verticalMidpointCollapse =
            (BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
                    + ((1.0f
                    - BatteryTransferConfig.HORIZONTAL_COLLAPSE_END)
                    * 0.5f))
        val geometry: BatteryShapeGeometry =
            geometryAt(verticalMidpointCollapse)

        Assert.assertEquals(42.875f, geometry.height, EPSILON)
    }

    @Test
    fun invalidInputsClampWithoutNonFiniteGeometry() {
        val geometry: BatteryShapeGeometry = geometryAt(Float.NaN)

        Assert.assertEquals(BatteryShapeGeometry.Stage.COMPLETE, geometry.stage)
        Assert.assertTrue(java.lang.Float.isFinite(geometry.width))
        Assert.assertTrue(java.lang.Float.isFinite(geometry.height))
        Assert.assertTrue(java.lang.Float.isFinite(geometry.centreX))
        Assert.assertTrue(java.lang.Float.isFinite(geometry.centreY))
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun geometryAt(collapse: Float): BatteryShapeGeometry {
            val geometry = BatteryShapeGeometry()
            geometry.update(
                collapse,
                60.0f,
                53.0f,
                112.0f,
                53.1667f,
                112.0f,
                89.0f,
                BatteryTransferConfig.POINT_DIAMETER_UNITS,
                BatteryTransferConfig.POINT_DIAMETER_UNITS
            )
            return geometry
        }
    }
}
