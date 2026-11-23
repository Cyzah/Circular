package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.animation.BatteryTransferStateTest
import com.cyzah.circular.core.graphics.animation.BatteryTransferStateTest.FakeClock
import org.junit.Assert
import org.junit.Test

class BatteryTransferStateTest {
    @Test
    fun startupWithClosedControlsDoesNotStartAnimation() {
        val fixture = Fixture()

        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        Assert.assertFalse(fixture.state.needsFrame())
        Assert.assertFalse(fixture.state.pointVisible())
    }

    @Test
    fun openingNormalRingImmediatelyUsesGestureBatteryShape() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)

        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, fixture.state.phase())
        Assert.assertTrue(fixture.state.batteryShapeVisible())
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.pointVisible())
        Assert.assertFalse(fixture.state.needsFrame())
        Assert.assertEquals(0.0f, fixture.state.batteryCollapse(1.0f, 1.0f), EPSILON)
        Assert.assertEquals(0.0f, fixture.state.temporaryTraceAlpha(), EPSILON)
    }

    @Test
    fun heldOpeningGestureDoesNotAdvanceWithTime() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.4f, 0.0f, 0.3f, true, true, false, false)
        val collapse = fixture.state.batteryCollapse(0.4f, 0.3f)

        fixture.clock.now += 5000L
        fixture.update(0.4f, 0.0f, 0.3f, true, true, false, false)

        Assert.assertEquals(collapse, fixture.state.batteryCollapse(0.4f, 0.3f), EPSILON)
        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, fixture.state.phase())
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.needsFrame())
    }

    @Test
    fun reversingOpeningGestureImmediatelyReversesBatteryGeometry() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.3f, 0.0f, 0.2f, true, true, false, false)
        val firstCollapse = fixture.state.batteryCollapse(0.3f, 0.2f)
        fixture.update(0.7f, 0.0f, 0.6f, true, true, false, false)
        val expandedCollapse = fixture.state.batteryCollapse(0.7f, 0.6f)
        fixture.update(0.3f, 0.0f, 0.2f, true, true, false, false)

        Assert.assertTrue(fixture.state.batteryShapeVisible())
        Assert.assertTrue(expandedCollapse < firstCollapse)
        Assert.assertEquals(firstCollapse, fixture.state.batteryCollapse(0.3f, 0.2f), EPSILON)
        Assert.assertFalse(fixture.state.needsFrame())
    }

    @Test
    fun cancellingSmallOpeningRestoresNormalRingWithoutAClosingPath() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.05f, 0.0f, 0.0f, true, true, true, false)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)

        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
        Assert.assertEquals(0.74f, fixture.state.fillProgress(74), EPSILON)
        Assert.assertFalse(fixture.state.openingShape())
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.needsFrame())

        fixture.clock.now += 5000L
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        fixture.releaseToPath()
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
    }

    @Test
    fun openingBlendSurvivesHoldingAndReversingThroughTheSolidHandoff() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.5f, 0.0f, 0.5f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.8f, 0.0f, 0.8f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())

        fixture.clock.now += 5000L
        fixture.update(0.8f, 0.0f, 0.8f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.3f, 0.0f, 0.3f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        Assert.assertFalse(fixture.state.needsFrame())
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        fixture.releaseToPath()
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
    }

    @Test
    fun cancellingOpeningKeepsBlendingUntilTheMorphAlsoCloses() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.12f, 0.0f, 0.12f, true, true, false, false)
        fixture.update(0.0f, 0.0f, 0.09f, true, true, false, false)

        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, fixture.state.phase())
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        Assert.assertFalse(fixture.state.needsFrame())
    }

    @Test
    fun completedOpeningBlendUsesTheExistingClosingPathAfterReversing() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.09f, 0.0f, 0.09f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.18f, 0.0f, 0.18f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(1.0f, 0.0f, 1.0f, true, true, true, false)
        Assert.assertFalse(fixture.state.openingShape())
        fixture.update(0.09f, 0.0f, 0.09f, true, true, false, false)
        Assert.assertFalse(fixture.state.openingShape())
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)
        Assert.assertEquals(BatteryTransferState.Phase.WAITING_FOR_HANDOFF, fixture.state.phase())
        fixture.releaseToPath()
        Assert.assertEquals(BatteryTransferState.Phase.PATH_TO_LOWER_LEFT, fixture.state.phase())
    }

    @Test
    fun openingBlendWaitsForTheMorphUsedByThePainter() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.3f, 0.0f, 0.09f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(1.0f, 0.0f, 0.8f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(1.0f, 0.0f, 1.0f, true, true, true, false)
        Assert.assertFalse(fixture.state.openingShape())
    }

    @Test
    fun reopeningDuringRingPathStartsANewOpeningBlend() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += 100L
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.09f, 0.0f, 0.09f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.05f, 0.0f, 0.05f, true, true, false, false)
        Assert.assertTrue(fixture.state.openingShape())
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
    }

    @Test
    fun onlyNonzeroControlGatesPathHandoff() {
        val fixture: Fixture = closedAndWaiting()

        fixture.update(0.001f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, fixture.state.phase())

        fixture.update(0.0f, 0.001f, 0.0f, true, true, true)
        Assert.assertEquals(BatteryTransferState.Phase.PATH_TO_LOWER_LEFT, fixture.state.phase())
        Assert.assertTrue(fixture.state.pathToLowerLeft())
    }

    @Test
    fun unsettledMorphGatesPathHandoff() {
        val fixture: Fixture = closedAndWaiting()
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, false)

        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertFalse(fixture.state.pathToLowerLeft())
    }

    @Test
    fun pointStartsRightAndEndsLeft() {
        val fixture: Fixture = closedAndWaiting()
        Assert.assertFalse(fixture.state.ringTrackVisible())
        fixture.releaseToPath()

        Assert.assertEquals(BatteryTransferState.Phase.PATH_TO_LOWER_LEFT, fixture.state.phase())
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
        Assert.assertFalse(fixture.state.ringTrackVisible())

        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS / 2L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        val halfway = fixture.state.pointPathProgress()
        Assert.assertTrue(halfway > 0.0f && halfway < 1.0f)
        Assert.assertFalse(fixture.state.ringTrackVisible())

        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertTrue(fixture.state.restoringRealFill())
        Assert.assertTrue(fixture.state.ringTrackVisible())
    }

    @Test
    fun ringPathAndRealFillRemainVisibleLongEnoughToRead() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += 400L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )

        fixture.clock.now += 300L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(
            BatteryTransferState.Phase.REAL_BATTERY_RESTORE,
            fixture.state.phase()
        )

        fixture.clock.now += 160L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(
            BatteryTransferState.Phase.REAL_BATTERY_RESTORE,
            fixture.state.phase()
        )

        fixture.clock.now += 200L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(
            BatteryTransferState.Phase.NORMAL_RING,
            fixture.state.phase()
        )
    }

    @Test
    fun batteryReachesPointWhenStackedMotionHandsOff() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true, false)

        Assert.assertEquals(
            1.0f,
            fixture.state.batteryCollapse(0.0f, 0.0f),
            EPSILON
        )
    }

    @Test
    fun batteryDoesNotBecomePointWhileControlCenterStillHasMotion() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)

        fixture.update(0.12f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertTrue(fixture.state.batteryCollapse(0.12f, 0.0f) < 1.0f)
    }

    @Test
    fun batteryBecomesPointOnlyWhenPanelAndMorphBothFinish() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(
            0.0f,
            0.0f,
            0.0f,
            true,
            true,
            true,
            false
        )

        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertEquals(
            1.0f,
            fixture.state.batteryCollapse(0.0f, 0.0f),
            EPSILON
        )
    }

    @Test
    fun batteryStagesUseTheSameMorphAsTheStackedSignalHandoff() {
        val fixture = Fixture()
        val closedMorphAtHorizontalEnd = (1.0f
                - (BatteryTransferConfig.BATTERY_HORIZONTAL_CLOSE_PROGRESS
                * (1.0f - BatteryTransferConfig.CLOSED_MORPH_MAX)))

        fixture.update(
            0.0f,
            0.0f,
            closedMorphAtHorizontalEnd,
            true,
            true,
            true,
            false
        )
        Assert.assertEquals(
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END,
            fixture.state.batteryCollapse(
                0.0f,
                closedMorphAtHorizontalEnd
            ),
            EPSILON
        )
        fixture.update(
            0.0f,
            0.0f,
            BatteryTransferConfig.CLOSED_MORPH_MAX,
            true,
            true,
            true,
            false
        )
        Assert.assertEquals(
            1.0f,
            fixture.state.batteryCollapse(
                0.0f,
                BatteryTransferConfig.CLOSED_MORPH_MAX
            ),
            EPSILON
        )
    }

    @Test
    fun waitingForFingerReleaseKeepsTheFinishedPointAtRingStart() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, false)

        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertEquals(
            1.0f,
            fixture.state.batteryCollapse(0.0f, 0.0f),
            EPSILON
        )
        Assert.assertTrue(fixture.state.pointVisible())
    }

    @Test
    fun fingerReleaseStartsTheRingPathFromTheFinishedPoint() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, false)
        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertEquals(
            1.0f,
            fixture.state.batteryCollapse(0.0f, 0.0f),
            EPSILON
        )

        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
        Assert.assertTrue(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.batteryShapeVisible())
    }

    @Test
    fun reopeningHalfwayCancelsRingAndImmediatelyFollowsGesture() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS / 2L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(0.5f, fixture.state.pointPathProgress(), EPSILON)

        fixture.update(0.4f, 0.2f, 0.65f, true, true, true)

        Assert.assertEquals(BatteryTransferState.Phase.BATTERY_SHAPE, fixture.state.phase())
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.pointVisible())
        Assert.assertTrue(fixture.state.needsFrame())
        Assert.assertEquals(1.0f, fixture.state.reopenAlpha(), EPSILON)
        val collapse = fixture.state.batteryCollapse(0.4f, 0.65f)
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS
        fixture.state.update(0.4f, 0.2f, 0.65f, true, true, true)
        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertEquals(collapse, fixture.state.batteryCollapse(0.4f, 0.65f), EPSILON)
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
    }

    @Test
    fun hostInvalidationJumpsToNormalRealRing() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()

        fixture.state.invalidate()

        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
        Assert.assertFalse(fixture.state.needsFrame())
        Assert.assertEquals(0.37f, fixture.state.fillProgress(37), EPSILON)
    }

    @Test
    fun batteryZeroAndOneHundredHaveCorrectFinalFill() {
        val state: BatteryTransferState = normalState()

        Assert.assertEquals(0.0f, state.fillProgress(0), EPSILON)
        Assert.assertEquals(1.0f, state.fillProgress(100), EPSILON)
        Assert.assertEquals(0.0f, state.temporaryTraceAlpha(), EPSILON)
    }

    @Test
    fun restoreReadsLatestBatteryEveryFrame() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS + 1L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)

        fixture.clock.now += BatteryTransferConfig.RESTORE_DURATION_MS / 4L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        val first = fixture.state.fillProgress(81)
        fixture.clock.now += BatteryTransferConfig.RESTORE_DURATION_MS / 2L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        val second = fixture.state.fillProgress(19)

        Assert.assertTrue(first > 0.0f)
        Assert.assertTrue(second > 0.0f)
        Assert.assertTrue(second < 0.19f)
    }

    @Test
    fun hostSixIsPreferredAndHostFiveIsScreenMirrorFallback() {
        Assert.assertEquals(6, BatteryTransferState.Companion.selectHost(true, true).toLong())
        Assert.assertEquals(5, BatteryTransferState.Companion.selectHost(false, true).toLong())
        Assert.assertEquals(0, BatteryTransferState.Companion.selectHost(false, false).toLong())
        Assert.assertTrue(BatteryTransferState.Companion.mayDrawControlCenter(6, true, true))
        Assert.assertFalse(BatteryTransferState.Companion.mayDrawControlCenter(5, true, true))
        Assert.assertTrue(BatteryTransferState.Companion.mayDrawControlCenter(5, false, true))
    }

    @Test
    fun invalidAnimationContextStaysOnNormalRing() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 0.2f, true, true, false)

        fixture.update(0.0f, 0.0f, 0.0f, true, false, true)

        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())
    }

    @Test
    fun duplicateFrameCallsDoNotRestartTimeline() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS / 3L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        val first = fixture.state.pointPathProgress()
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        val second = fixture.state.pointPathProgress()

        Assert.assertEquals(first, second, EPSILON)
    }

    @Test
    fun closingAgainAfterReopeningStartsANewPathFromLowerRight() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS / 2L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.3f, 0.1f, 0.5f, true, true, true)
        fixture.clock.now += 20L
        fixture.state.update(0.3f, 0.1f, 0.5f, true, true, true)
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)

        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
    }

    @Test
    fun reopeningDuringRealFillImmediatelyReturnsToGestureShape() {
        val fixture: Fixture = closedAndWaiting()
        fixture.releaseToPath()
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.clock.now += BatteryTransferConfig.RESTORE_DURATION_MS / 2L
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(0.37f, fixture.state.fillProgress(74), EPSILON)

        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertEquals(0.0f, fixture.state.batteryCollapse(1.0f, 1.0f), EPSILON)
        Assert.assertFalse(fixture.state.ringTransferVisible())
        Assert.assertFalse(fixture.state.needsFrame())
    }

    @Test
    fun completeBatteryDoesNotScheduleFramesForRemovedChargeBadge() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertFalse(fixture.state.needsFrame())
    }

    @Test
    fun fullyClosedPanelWaitsForFingerReleaseBeforeRingPath() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 0.2f, true, true, false, true)
        fixture.state.update(
            0.0f,
            0.0f,
            0.0f,
            true,
            true,
            true,
            false,
            fixture.clock.now
        )

        Assert.assertEquals(
            BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
            fixture.state.phase()
        )
        Assert.assertTrue(fixture.state.pointVisible())
        Assert.assertTrue(fixture.state.handoffHoldVisible(0.0f, 0.0f, 0.0f))
        Assert.assertFalse(fixture.state.handoffHoldVisible(0.01f, 0.0f, 0.0f))

        fixture.state.update(
            0.0f,
            0.0f,
            0.0f,
            true,
            true,
            true,
            true,
            fixture.clock.now
        )

        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
    }

    @Test
    fun reopeningBeforeReleaseDoesNotQueueARingAnimation() {
        val fixture = Fixture()
        fixture.update(1.0f, 1.0f, 0.2f, true, true, false, true)
        fixture.state.update(
            0.0f,
            0.0f,
            0.0f,
            true,
            true,
            true,
            false,
            fixture.clock.now
        )
        fixture.state.update(
            0.35f,
            0.15f,
            0.45f,
            true,
            true,
            true,
            false,
            fixture.clock.now
        )

        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertFalse(fixture.state.pointVisible())

        fixture.state.update(
            0.0f,
            0.0f,
            0.0f,
            true,
            true,
            true,
            true,
            fixture.clock.now
        )
        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
    }

    @Test
    fun aNewCloseStartsAtLowerRightAfterACompletedCycle() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.clock.now += BatteryTransferConfig.PATH_DURATION_MS
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.clock.now += BatteryTransferConfig.RESTORE_DURATION_MS
        fixture.state.update(0.0f, 0.0f, 0.0f, true, true, true)
        Assert.assertEquals(BatteryTransferState.Phase.NORMAL_RING, fixture.state.phase())

        fixture.update(1.0f, 1.0f, 1.0f, true, true, true)
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)

        Assert.assertEquals(
            BatteryTransferState.Phase.PATH_TO_LOWER_LEFT,
            fixture.state.phase()
        )
        Assert.assertEquals(0.0f, fixture.state.pointPathProgress(), EPSILON)
    }

    @Test
    fun hyperOsStableExpansionBelowOneStillShowsBattery() {
        val fixture = Fixture()
        fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
        fixture.update(0.9905f, 0.0f, 0.9790f, true, true, false)

        Assert.assertEquals(
            BatteryTransferState.Phase.BATTERY_SHAPE,
            fixture.state.phase()
        )
        Assert.assertTrue(fixture.state.batteryShapeVisible())
        Assert.assertFalse(fixture.state.openingShape())
        Assert.assertEquals(
            0.0f,
            fixture.state.batteryCollapse(0.9905f, 0.9790f),
            EPSILON
        )
        fixture.update(0.3f, 0.0f, 0.3f, true, true, false)
        Assert.assertFalse(fixture.state.openingShape())
        fixture.releaseToPath()
        Assert.assertEquals(BatteryTransferState.Phase.PATH_TO_LOWER_LEFT, fixture.state.phase())
    }

    private class FakeClock

        : BatteryTransferTimeline.Clock {
        @JvmField var now: Long = 1000L

        override fun now(): Long {
            return now
        }
    }

    private class Fixture {
        @JvmField val clock: FakeClock = FakeClock()
        @JvmField val state: BatteryTransferState = BatteryTransferState(BatteryTransferTimeline(clock))
        @JvmField val modernState: BatteryTransferState = state

        @JvmOverloads
        fun update(
            control: Float,
            shade: Float,
            morph: Float,
            ready: Boolean,
            animating: Boolean,
            settled: Boolean,
            released: Boolean = true
        ) {
            state.update(
                control,
                shade,
                morph,
                ready,
                animating,
                settled,
                released,
                clock.now()
            )
        }

        fun releaseToPath() {
            state.update(
                0.0f,
                0.0f,
                0.0f,
                true,
                true,
                true,
                true,
                clock.now()
            )
        }
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun closedAndWaiting(): Fixture {
            val fixture = Fixture()
            fixture.update(1.0f, 1.0f, 0.2f, true, true, false)
            fixture.update(0.0f, 0.0f, 0.0f, true, true, false)
            Assert.assertEquals(
                BatteryTransferState.Phase.WAITING_FOR_HANDOFF,
                fixture.state.phase()
            )
            return fixture
        }

        private fun normalState(): BatteryTransferState {
            val fixture = Fixture()
            fixture.update(0.0f, 0.0f, 0.0f, true, true, true)
            return fixture.state
        }
    }
}
