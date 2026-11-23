package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.geometry.RingGeometryTest
import com.cyzah.circular.domain.settings.DualStyle
import kotlin.math.max
import kotlin.math.sqrt
import org.junit.Assert
import org.junit.Test

class RingGeometryTest {
    @Test
    fun heavyStrokeEndpointsUseTheActualCanvasOvalRadii() {
        val geometry = RingGeometry()
        geometry.setCircular(DualStyle.LAYERED, 10f, 8.6f, 110f, 108f, 25f, 60f, 16.2f)
        Assert.assertEquals(103.30127f, geometry.startX, .0001f)
        Assert.assertEquals(83.15f, geometry.startY, .0001f)
        Assert.assertEquals(16.69873f, geometry.endX, .0001f)
        Assert.assertEquals(83.15f, geometry.endY, .0001f)
    }

    @Test
    @Throws(Exception::class)
    fun lowerOpeningCanMoveLeftWithoutMakingTheOppositeArcShorter() {
        val baseline = RingGeometry()
        baseline.setCircular(DualStyle.DUO, 10f, 8f, 110f, 108f, 20f, 40f, 7.5f)
        val moved = RingGeometry()
        setCircularOpenings(moved, 20f, 20f, 55f, 25f)
        Assert.assertEquals(baseline.length, moved.length, EPSILON)
        Assert.assertTrue(moved.startX < baseline.startX)
        Assert.assertTrue(moved.endX < baseline.endX)
        Assert.assertTrue(moved.rawContourBoundary() > baseline.rawContourBoundary())
        Assert.assertEquals(81.13091f, moved.startX, EPSILON)
        Assert.assertEquals(19.04240f, moved.endX, EPSILON)
        Assert.assertEquals(moved.startX, moved.pointX(0f), EPSILON)
        Assert.assertEquals(moved.endX, moved.pointX(moved.length), EPSILON)
    }

    @Test
    @Throws(Exception::class)
    fun signedTopEdgesCanPutTheWholeOpeningOnOneSideOfItsAxis() {
        val geometry = RingGeometry()
        setCircularOpenings(geometry, -10f, 50f, 40f, 40f)
        val boundary = geometry.rawContourBoundary()
        Assert.assertTrue(geometry.pointX(boundary) > geometry.centreX)
        Assert.assertTrue(geometry.pointX(boundary + .001f) > geometry.centreX)
        Assert.assertEquals(98.30222f, geometry.pointX(boundary), EPSILON)
        Assert.assertEquals(68.6824f, geometry.pointX(boundary + .001f), EPSILON)
        Assert.assertTrue(!geometry.continuousContour())
    }

    @Test
    @Throws(Exception::class)
    fun legacySetterRestoresSymmetryAfterAnAsymmetricOpening() {
        val geometry = RingGeometry()
        setCircularOpenings(geometry, 20f, 20f, 55f, 25f)
        val previous = geometry.revision
        geometry.setCircular(DualStyle.DUO, 10f, 8f, 110f, 108f, 20f, 40f, 7.5f)
        Assert.assertTrue(geometry.revision > previous)
        Assert.assertEquals(92.13938f, geometry.startX, EPSILON)
        Assert.assertEquals(27.86062f, geometry.endX, EPSILON)
        Assert.assertEquals(geometry.length / 2, geometry.rawContourBoundary(), EPSILON)
    }

    @Test
    @Throws(Exception::class)
    fun capsuleOpeningEndpointsMoveTogetherAndSurviveTransferCopy() {
        val source = RingGeometry()
        try {
            RingGeometry::class.java.getMethod(
                "setCapsuleOpenings",
                Float::class.javaPrimitiveType,
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
                .invoke(source, 240f, 104f, 96f, 132f, 74f, 142f, 7.5f, 0f, 0f, 1f)
        } catch (missing: NoSuchMethodException) {
            throw AssertionError("Capsule openings need independent real endpoints", missing)
        }
        Assert.assertEquals(142f, source.startX, EPSILON)
        Assert.assertEquals(74f, source.endX, EPSILON)
        Assert.assertEquals(494.72565f, source.length, EPSILON)
        val transfer = RingGeometry()
        transfer.copyForTransferFrom(source)
        Assert.assertEquals(source.length, transfer.length, EPSILON)
        for (i in 0..100) {
            Assert.assertEquals(
                source.pointX(source.length * i / 100),
                transfer.pointX(transfer.length * i / 100),
                EPSILON
            )
            Assert.assertEquals(
                source.pointY(source.length * i / 100),
                transfer.pointY(transfer.length * i / 100),
                EPSILON
            )
        }
    }

    @Test
    fun layeredPathStartsRightAndEndsLeftWithFiniteLength() {
        assertCircularPath(
            DualStyle.LAYERED,
            RingGapPolicy.topHalfAngle(true, false, 48.0f, 0.0f),
            RingGapPolicy.lowerHalfAngle(DualStyle.LAYERED, 1.2f)
        )
    }

    @Test
    fun groupedPathUsesItsActualLowerGap() {
        assertCircularPath(
            DualStyle.GROUPED,
            RingGapPolicy.topHalfAngle(true, false, 48.0f, 0.0f),
            RingGapPolicy.lowerHalfAngle(DualStyle.GROUPED, 1.2f)
        )
    }

    @Test
    fun duoPathUsesDynamicArcGaps() {
        val top = DuoLayout.gapHalfAngle(48.0f, 1.2f)
        val bottom = DuoLayout.gapHalfAngle(68.0f, 1.2f)
        assertCircularPath(DualStyle.DUO, top, bottom)
    }

    @Test
    fun capsulePathUsesExactStraightSegmentsAndSemicircles() {
        val geometry = RingGeometry()
        geometry.setCapsule(
            240.0f,
            104.0f,
            18.0f,
            34.0f,
            7.072f
        )

        assertFinitePath(geometry)
        Assert.assertTrue(geometry.startX > geometry.endX)
        Assert.assertEquals(geometry.startX, geometry.pointX(0.0f), EPSILON)
        Assert.assertEquals(geometry.startY, geometry.pointY(0.0f), EPSILON)
        Assert.assertEquals(geometry.endX, geometry.pointX(geometry.length), EPSILON)
        Assert.assertEquals(geometry.endY, geometry.pointY(geometry.length), EPSILON)
        Assert.assertTrue(geometry.length > straightDistance(geometry))
    }

    @Test
    fun transformedCapsulePointsUseTheActualRenderedOriginAndScale() {
        val geometry = RingGeometry()
        geometry.setCapsule(
            240.0f,
            104.0f,
            18.0f,
            34.0f,
            3.536f,
            100.0f,
            20.0f,
            0.5f
        )

        Assert.assertEquals(177.0f, geometry.startX, EPSILON)
        Assert.assertEquals(72.0f, geometry.startY, EPSILON)
        Assert.assertEquals(geometry.startX, geometry.pointX(0.0f), EPSILON)
        Assert.assertEquals(geometry.startY, geometry.pointY(0.0f), EPSILON)
        Assert.assertEquals(geometry.endX, geometry.pointX(geometry.length), EPSILON)
        Assert.assertEquals(geometry.endY, geometry.pointY(geometry.length), EPSILON)
    }

    @Test
    fun transferRingClosesTopOpeningAndStartsAtLowerRight() {
        val source = RingGeometry()
        source.setCircular(
            DualStyle.DUO,
            10.0f,
            8.0f,
            110.0f,
            108.0f,
            18.0f,
            12.0f,
            7.5f
        )
        val transfer = RingGeometry()
        transfer.copyClosedTopFrom(source)

        Assert.assertEquals(0.0f, transfer.topHalfAngle, EPSILON)
        Assert.assertTrue(transfer.continuousContour())
        Assert.assertTrue(transfer.startX > transfer.endX)
        Assert.assertTrue(transfer.pointY(transfer.length / 2.0f) < transfer.centreY)
        Assert.assertEquals(transfer.startX, transfer.pointX(0.0f), EPSILON)
        Assert.assertEquals(transfer.endX, transfer.pointX(transfer.length), EPSILON)
    }

    @Test
    fun transferCopyKeepsTheActualTopAndBottomGaps() {
        val source = RingGeometry()
        source.setCapsule(
            240.0f,
            104.0f,
            18.0f,
            34.0f,
            7.072f,
            100.0f,
            20.0f,
            0.5f
        )
        try {
            val method =
                RingGeometry::class.java.getDeclaredMethod(
                    "copyFrom",
                    RingGeometry::class.java
                )
            method.setAccessible(true)
            val transfer = RingGeometry()
            method.invoke(transfer, source)

            Assert.assertEquals(18.0f, transfer.halfTop, EPSILON)
            Assert.assertEquals(34.0f, transfer.halfBottom, EPSILON)
            Assert.assertEquals(source.startX, transfer.startX, EPSILON)
            Assert.assertEquals(source.endY, transfer.endY, EPSILON)
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing geometry copy that preserves the signal gap",
                error
            )
        }
    }

    @Test
    fun everyStyleTransferPreservesTheActualTopGap() {
        try {
            val method =
                RingGeometry::class.java.getDeclaredMethod(
                    "copyForTransferFrom",
                    RingGeometry::class.java
                )
            method.setAccessible(true)

            val capsule = RingGeometry()
            capsule.setCapsule(
                240.0f,
                104.0f,
                18.0f,
                34.0f,
                7.072f
            )
            val capsuleTransfer = RingGeometry()
            method.invoke(capsuleTransfer, capsule)
            Assert.assertEquals(18.0f, capsuleTransfer.halfTop, EPSILON)

            for (style in arrayOf<DualStyle>(
                DualStyle.LAYERED,
                DualStyle.GROUPED
            )) {
                val circular = RingGeometry()
                circular.setCircular(
                    style,
                    10.0f,
                    8.0f,
                    110.0f,
                    108.0f,
                    cardStyleTopHalfAngle(style),
                    12.0f,
                    7.5f
                )
                val circularTransfer = RingGeometry()
                method.invoke(circularTransfer, circular)
                Assert.assertEquals(
                    circular.topHalfAngle,
                    circularTransfer.topHalfAngle,
                    EPSILON
                )
                Assert.assertEquals(12.0f, circularTransfer.bottomHalfAngle, EPSILON)
                Assert.assertEquals(circular.length, circularTransfer.length, EPSILON)
                Assert.assertEquals(circular.startX, circularTransfer.startX, EPSILON)
                Assert.assertEquals(circular.endX, circularTransfer.endX, EPSILON)
            }

            val duo = RingGeometry()
            duo.setCircular(
                DualStyle.DUO,
                10.0f,
                8.0f,
                110.0f,
                108.0f,
                18.0f,
                12.0f,
                7.5f
            )
            val duoTransfer = RingGeometry()
            method.invoke(duoTransfer, duo)
            Assert.assertEquals(18.0f, duoTransfer.topHalfAngle, EPSILON)
            Assert.assertEquals(12.0f, duoTransfer.bottomHalfAngle, EPSILON)
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing style-specific transfer geometry copy",
                error
            )
        }
    }

    @Test
    fun duoTransferTraceAndMovingPointAvoidTheTopNumberInBothDirections() {
        val source = RingGeometry()
        source.setCircular(
            DualStyle.DUO, 10.0f, 8.0f, 110.0f, 108.0f,
            30.0f, 45.0f, 7.5f
        )
        val transfer = RingGeometry()
        transfer.copyForTransferFrom(source)

        // Digit bounds centred at DUO's top readout (60,14), including its height.
        assertPathAvoidsNumber(transfer, 45.0f, 4.0f, 75.0f, 24.0f)
        Assert.assertEquals(183.25957f, transfer.length, EPSILON)
        Assert.assertTrue(transfer.pointX(transfer.length * 0.4999f) > 84.9f)
        Assert.assertTrue(transfer.pointX(transfer.length * 0.5001f) < 35.1f)
    }

    @Test
    fun duoTransferFollowsTheGapWhenTheNumberMovesBelowOrIsHidden() {
        val source = RingGeometry()
        val transfer = RingGeometry()
        source.setCircular(
            DualStyle.DUO, 10.0f, 8.0f, 110.0f, 108.0f,
            30.0f, 45.0f, 7.5f
        )
        transfer.copyForTransferFrom(source)
        source.setCircular(
            DualStyle.DUO, 10.0f, 8.0f, 110.0f, 108.0f,
            0.0f, 30.0f, 7.5f
        )
        transfer.copyForTransferFrom(source)
        Assert.assertTrue(transfer.continuousContour())
        assertPathAvoidsNumber(transfer, 45.0f, 92.0f, 75.0f, 112.0f)

        source.setCircular(
            DualStyle.DUO, 10.0f, 8.0f, 110.0f, 108.0f,
            0.0f, 45.0f, 7.5f
        )
        transfer.copyForTransferFrom(source)
        Assert.assertEquals(0.0f, transfer.topHalfAngle, EPSILON)
        Assert.assertEquals(45.0f, transfer.bottomHalfAngle, EPSILON)
    }

    companion object {
        private const val EPSILON = 0.01f

        @Throws(Exception::class)
        private fun setCircularOpenings(
            geometry: RingGeometry?,
            topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float
        ) {
            try {
                RingGeometry::class.java.getMethod(
                    "setCircularOpenings",
                    DualStyle::class.java,
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
                    .invoke(
                        geometry, DualStyle.DUO, 10f, 8f, 110f, 108f,
                        topLeft, topRight, bottomLeft, bottomRight, 7.5f
                    )
            } catch (missing: NoSuchMethodException) {
                throw AssertionError("Circular openings need four independent signed edges", missing)
            }
        }

        private fun assertPathAvoidsNumber(
            path: RingGeometry, left: Float, top: Float, right: Float, bottom: Float
        ) {
            for (direction in intArrayOf(1, -1)) {
                for (index in 0..400) {
                    val progress = (if (direction > 0) index else 400 - index) / 400.0f
                    val x = path.pointX(path.length * progress)
                    val y = path.pointY(path.length * progress)
                    val dx = max(0.0f, max(left - x, x - right))
                    val dy = max(0.0f, max(top - y, y - bottom))
                    Assert.assertTrue(
                        "ring stroke or moving point intersects the number at " + progress,
                        dx * dx + dy * dy > 3.75f * 3.75f
                    )
                }
            }
        }

        private fun cardStyleTopHalfAngle(style: DualStyle?): Float {
            return RingGapPolicy.topHalfAngle(
                true,
                true,
                48.0f,
                if (style == DualStyle.LAYERED) 30.0f else 34.0f
            )
        }

        private fun assertCircularPath(
            style: DualStyle?,
            topHalfAngle: Float,
            bottomHalfAngle: Float
        ) {
            val geometry = RingGeometry()
            geometry.setCircular(
                style,
                10.0f,
                8.0f,
                110.0f,
                108.0f,
                topHalfAngle,
                bottomHalfAngle,
                7.5f
            )

            assertFinitePath(geometry)
            Assert.assertTrue(geometry.startX > geometry.endX)
            Assert.assertTrue(geometry.length > straightDistance(geometry))
            Assert.assertEquals(geometry.startX, geometry.pointX(0.0f), EPSILON)
            Assert.assertEquals(geometry.startY, geometry.pointY(0.0f), EPSILON)
            Assert.assertEquals(geometry.endX, geometry.pointX(geometry.length), EPSILON)
            Assert.assertEquals(geometry.endY, geometry.pointY(geometry.length), EPSILON)
            Assert.assertEquals(1.0f, tangentLength(geometry, geometry.length * 0.37f), EPSILON)
        }

        private fun assertFinitePath(geometry: RingGeometry) {
            Assert.assertTrue(geometry.length > 0.0f)
            Assert.assertTrue(java.lang.Float.isFinite(geometry.length))
            Assert.assertTrue(java.lang.Float.isFinite(geometry.startX))
            Assert.assertTrue(java.lang.Float.isFinite(geometry.startY))
            Assert.assertTrue(java.lang.Float.isFinite(geometry.endX))
            Assert.assertTrue(java.lang.Float.isFinite(geometry.endY))
        }

        private fun straightDistance(geometry: RingGeometry): Float {
            val dx = geometry.startX - geometry.endX
            val dy = geometry.startY - geometry.endY
            return sqrt(((dx * dx) + (dy * dy)).toDouble()).toFloat()
        }

        private fun tangentLength(geometry: RingGeometry, distance: Float): Float {
            val tx = geometry.tangentX(distance)
            val ty = geometry.tangentY(distance)
            return sqrt(((tx * tx) + (ty * ty)).toDouble()).toFloat()
        }
    }
}
