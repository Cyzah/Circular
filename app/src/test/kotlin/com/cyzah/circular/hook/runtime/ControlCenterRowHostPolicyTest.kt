package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.hook.runtime.ControlCenterRowHostPolicyTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowHostPolicyTest {
    @Test
    fun closingStartUsesTheSameScreenOriginAsTheStillExpandedSignals() {
        val reserved = ControlCenterRowHostPolicy.projectedHostWidth(
            230, 770.0f, 230.0f, 832.0f, 230.0f
        )
        val frameWidth = ControlCenterRowHostPolicy.reserveProjectedRowWidth(
            reserved, 60, 0.98f, BatteryNumberPlacement.OUTSIDE
        )
        Assert.assertEquals(292, frameWidth.toLong())
        Assert.assertTrue(1062.0f - frameWidth <= 770.0f)
    }

    @Test
    fun alignmentThatMovesTheNativeNeighbourCannotPublishAnUnderReservedFrame() {
        Assert.assertTrue(ControlCenterRowHostPolicy.needsWidthBeforeDraw(230, 292))
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(292, 292))
        // Extra room is safe. Releasing it belongs to measurement, not pre-draw.
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(292, 260))
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(292, 0))
    }

    @Test
    fun closedNativeNeighbourMustReleaseTheLastExtraPixelsBeforeHandoff() {
        Assert.assertTrue(ControlCenterRowHostPolicy.needsWidthBeforeDraw(90, 60, true))
        Assert.assertTrue(ControlCenterRowHostPolicy.needsWidthBeforeDraw(60, 90, true))
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(60, 60, true))
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(90, 0, true))
        Assert.assertFalse(ControlCenterRowHostPolicy.needsWidthBeforeDraw(90, 60, false))
    }

    @Test
    fun disconnectSolvesAgainstTheShrinkingHostBeforeItsNextLayout() {
        val right = ControlCenterRowHostPolicy.measurementRight(230.0f, 184, 400.0f)
        Assert.assertEquals(184.0f, right, EPSILON)
        val input = ControlCenterRowInput()
        input.setBounds(right, 400.0f)
        input.set(
            ControlCenterRowComponent.BATTERY,
            true, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f
        )
        input.set(
            ControlCenterRowComponent.CELLULAR,
            true, 80.0f, 0.0f, 0.0f, 0.0f, 4.0f
        )
        val layout = ControlCenterRowLayout()
        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(184.0f, layout.right(ControlCenterRowComponent.BATTERY), EPSILON)
        Assert.assertEquals(80.0f, layout.right(ControlCenterRowComponent.CELLULAR), EPSILON)
    }

    @Test
    fun measurementRightAlsoGrowsButDoesNotExceedParentSpace() {
        Assert.assertEquals(
            230.0f,
            ControlCenterRowHostPolicy.measurementRight(184.0f, 230, 400.0f), EPSILON
        )
        Assert.assertEquals(
            184.0f,
            ControlCenterRowHostPolicy.measurementRight(184.0f, 230, 200.0f), EPSILON
        )
        Assert.assertEquals(
            184.0f,
            ControlCenterRowHostPolicy.measurementRight(184.0f, 0, 400.0f), EPSILON
        )
    }

    @Test
    fun hostExtraWidthExcludesTheBatteryAlreadyOwnedByTheHost() {
        Assert.assertEquals(
            70,
            ControlCenterRowHostPolicy.hostExtraWidthPx(
                170.0f,
                100.0f
            ).toLong()
        )
        Assert.assertEquals(
            0,
            ControlCenterRowHostPolicy.hostExtraWidthPx(
                80.0f,
                100.0f
            ).toLong()
        )
    }

    @Test
    fun hostExtraWidthRoundsTheReservationUpOnce() {
        Assert.assertEquals(
            71,
            ControlCenterRowHostPolicy.hostExtraWidthPx(
                170.1f,
                100.0f
            ).toLong()
        )
    }

    @Test
    fun solvedHostWidthContainsTheCompleteBatteryAndNetworkRow() {
        Assert.assertEquals(
            334,
            ControlCenterRowHostPolicy.hostWidthPx(
                333.194f,
                152.419f
            ).toLong()
        )
    }

    @Test
    fun hostFiveTranslatesWithoutChangingOrder() {
        Assert.assertEquals(
            75.0f,
            ControlCenterRowHostPolicy.translateHorizontal(
                65.0f,
                0.0f,
                10.0f
            ),
            EPSILON
        )
    }

    @Test
    fun screenCoordinateConvertsToCurrentHostLocalCoordinate() {
        Assert.assertEquals(
            76.0f,
            ControlCenterRowHostPolicy.screenToLocal(
                1104.0f,
                1028.0f
            ),
            EPSILON
        )
        Assert.assertEquals(
            -37.0f,
            ControlCenterRowHostPolicy.screenToLocal(
                963.0f,
                1000.0f
            ),
            EPSILON
        )
    }

    @Test
    fun commonTranslationPreservesBatteryWifiCellularOrder() {
        val battery = ControlCenterRowHostPolicy.translateHorizontal(
            150.0f, 0.0f, 10.0f
        )
        val wifi = ControlCenterRowHostPolicy.translateHorizontal(
            100.0f, 0.0f, 10.0f
        )
        val cellular = ControlCenterRowHostPolicy.translateHorizontal(
            30.0f, 0.0f, 10.0f
        )

        Assert.assertEquals(160.0f, battery, EPSILON)
        Assert.assertEquals(110.0f, wifi, EPSILON)
        Assert.assertEquals(40.0f, cellular, EPSILON)
        Assert.assertTrue(battery > wifi)
        Assert.assertTrue(wifi > cellular)
    }

    @Test
    fun reuseRequiresValidSourceAndAvailableTarget() {
        Assert.assertTrue(
            ControlCenterRowHostPolicy.host5CanReuse(
                true, true
            )
        )
        Assert.assertFalse(
            ControlCenterRowHostPolicy.host5CanReuse(
                false, true
            )
        )
        Assert.assertFalse(
            ControlCenterRowHostPolicy.host5CanReuse(
                true, false
            )
        )
    }

    @Test
    fun projectedRowReservesSpaceBeforeTheLeftNativeNeighbour() {
        // Panel row occupies screen [770,1000], while the copying host ends at 1062.
        val width = ControlCenterRowHostPolicy.projectedHostWidth(
            230, 770.0f, 230.0f, 832.0f, 230.0f
        )
        Assert.assertEquals(292, width.toLong())
        val speedRight = 1062.0f - width
        Assert.assertTrue(speedRight <= 770.0f)
    }

    @Test
    fun applyingTheReservationCannotIncreaseItOnTheNextMeasure() {
        // Increasing width moves the copying host's left edge, not its right edge.
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.projectedHostWidth(
                230, 770.0f, 230.0f, 770.0f, 292.0f
            ).toLong()
        )
        Assert.assertEquals(
            230, ControlCenterRowHostPolicy.projectedHostWidth(
                230, 770.0f, 230.0f, 770.0f, 230.0f
            ).toLong()
        )
    }

    @Test
    fun wifiReflowChangesTheRowWidthWithoutLosingTheHostOffset() {
        Assert.assertEquals(
            269, ControlCenterRowHostPolicy.projectedHostWidth(
                207, 770.0f, 230.0f, 770.0f, 292.0f
            ).toLong()
        )
        Assert.assertEquals(
            246, ControlCenterRowHostPolicy.projectedHostWidth(
                184, 816.0f, 184.0f, 816.0f, 246.0f
            ).toLong()
        )
    }

    @Test
    fun projectedReservationRoundsOutwardsAndRejectsMissingGeometry() {
        Assert.assertEquals(
            293, ControlCenterRowHostPolicy.projectedHostWidth(
                230, 770.0f, 230.0f, 832.25f, 230.0f
            ).toLong()
        )
        Assert.assertEquals(
            230, ControlCenterRowHostPolicy.projectedHostWidth(
                230, Float.NaN, 230.0f, 832.0f, 230.0f
            ).toLong()
        )
        Assert.assertEquals(
            230, ControlCenterRowHostPolicy.projectedHostWidth(
                230, 770.0f, 230.0f, 0.0f, 0.0f
            ).toLong()
        )
        Assert.assertEquals(
            0, ControlCenterRowHostPolicy.projectedHostWidth(
                0, 770.0f, 230.0f, 832.0f, 230.0f
            ).toLong()
        )
    }

    @Test
    fun projectedReservationMeetsBothNativeSpeedHandoffEndpoints() {
        for (placement in BatteryNumberPlacement.values()) {
            Assert.assertEquals(
                60, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                    292, 60, 0.0f, placement
                ).toLong()
            )
            Assert.assertEquals(
                292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                    292, 60, 1.0f, placement
                ).toLong()
            )
        }
    }

    @Test
    fun projectedReservationKeepsSpaceWhileSignalsAreOnlyTrimming() {
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                292, 60, 0.8f, BatteryNumberPlacement.INSIDE
            ).toLong()
        )
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                292, 60, 0.9f, BatteryNumberPlacement.OUTSIDE
            ).toLong()
        )
    }

    @Test
    fun projectedReservationWaitsForTechnologyAfterTheDotsMove() {
        // At half expansion horizontal movement is held while the battery is wide.
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                292, 60, 0.5f, BatteryNumberPlacement.INSIDE
            ).toLong()
        )
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                292, 60, 0.5f, BatteryNumberPlacement.OUTSIDE
            ).toLong()
        )
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                292, 60, 0.25f, BatteryNumberPlacement.OUTSIDE
            ).toLong()
        )
    }

    @Test
    fun reservationUsesTheSameEasedTextProgressAsTheRenderer() {
        for (placement in BatteryNumberPlacement.values()) {
            Assert.assertEquals(
                139, ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                    292, 60, 0.1f, placement
                ).toLong()
            )
        }
    }

    @Test
    fun movingHostReservesTheProjectedClosedEdgeAsWellAsTheOpenEdge() {
        // Both endpoints end at screen x=1000; the animated copy ends at 1062.
        Assert.assertEquals(
            122, ControlCenterRowHostPolicy.projectedTransitionWidth(
                230, 60, 770f, 230f, 940f, 60f, 832f, 230f, 0.0f,
                BatteryNumberPlacement.INSIDE
            ).toLong()
        )
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.projectedTransitionWidth(
                230, 60, 770f, 230f, 940f, 60f, 832f, 230f, 0.5f,
                BatteryNumberPlacement.INSIDE
            ).toLong()
        )
        Assert.assertEquals(
            292, ControlCenterRowHostPolicy.projectedTransitionWidth(
                230, 60, 770f, 230f, 940f, 60f, 832f, 230f, 0.98f,
                BatteryNumberPlacement.INSIDE
            ).toLong()
        )
    }

    @Test
    fun terminalBoundaryMatchesWhenTheCopyEndsLeftOfTheRealStatusBar() {
        // The real compact host starts at 970. Copy right edge is 1000, so only
        // 30px must be reserved to put its native neighbour at that same edge.
        val width = ControlCenterRowHostPolicy.projectedTransitionWidth(
            230, 60, 770f, 230f, 970f, 60f, 770f, 230f,
            0.0f, BatteryNumberPlacement.INSIDE
        )
        Assert.assertEquals(30, width.toLong())
        Assert.assertEquals(970.0f, 1000.0f - width, EPSILON)
    }

    @Test
    fun closedProjectionDoesNotRequireAnExpandedPanelSnapshot() {
        Assert.assertEquals(
            30, ControlCenterRowHostPolicy.projectedTransitionWidth(
                0, 60, Float.NaN, 0f, 970f, 60f, 770f, 230f,
                0.0f, BatteryNumberPlacement.INSIDE
            ).toLong()
        )
        Assert.assertEquals(
            90, ControlCenterRowHostPolicy.projectedTransitionWidth(
                0, 60, Float.NaN, 0f, 970f, 60f, 830f, 230f,
                0.0f, BatteryNumberPlacement.OUTSIDE
            ).toLong()
        )
    }

    @Test
    fun lastFramesConvergeToTheSameBoundaryForEitherOffsetDirection() {
        for (placement in BatteryNumberPlacement.values()) {
            for (copyRight in floatArrayOf(1000.0f, 1030.0f, 1060.0f)) {
                for (progress in floatArrayOf(0.01f, 0.001f, 0.0001f, 0.0f)) {
                    val width = ControlCenterRowHostPolicy.projectedTransitionWidth(
                        230, 60, 770f, 230f, 970f, 60f, copyRight - 230, 230f,
                        progress, placement
                    )
                    val neighbourRight = copyRight - width
                    Assert.assertTrue(neighbourRight <= 970.0f)
                    Assert.assertTrue(970.0f - neighbourRight <= 1.0f)
                }
            }
        }
    }

    @Test
    fun projectedReservationRemainsContinuousAcrossClosingAndReopening() {
        for (placement in BatteryNumberPlacement.values()) {
            var previous = 60
            for (frame in 1..1000) {
                val width = ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                    292, 60, frame / 1000.0f, placement
                )
                Assert.assertTrue(width >= previous)
                Assert.assertTrue(width - previous <= 2)
                Assert.assertTrue(width <= 292)
                previous = width
            }
            for (frame in 999 downTo 0) {
                val width = ControlCenterRowHostPolicy.reserveProjectedRowWidth(
                    292, 60, frame / 1000.0f, placement
                )
                Assert.assertTrue(width <= previous)
                Assert.assertTrue(previous - width <= 2)
                Assert.assertTrue(width >= 60)
                previous = width
            }
        }
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
