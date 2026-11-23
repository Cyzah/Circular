package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.StatusColors
import org.junit.Assert
import org.junit.Test

class BatteryTransferPolicyTest {
    @Test
    fun ringAndBatteryShapeCrossfadeOnTheGestureWithoutWaitingForAClock() {
        Assert.assertEquals(0.0f, BatteryTransferPolicy.gestureShapeAlpha(0.0f), 0.0001f)
        Assert.assertEquals(0.5f, BatteryTransferPolicy.gestureShapeAlpha(0.09f), 0.0001f)
        Assert.assertEquals(1.0f, BatteryTransferPolicy.gestureShapeAlpha(0.18f), 0.0001f)
        Assert.assertEquals(1.0f, BatteryTransferPolicy.gestureShapeAlpha(0.8f), 0.0001f)
    }

    @Test
    fun traceTintEqualsCurrentDisplayPolicyBatteryTint() {
        val expected = DisplayPolicy.batteryTint(
            19,
            true,
            false,
            true,
            true,
            -0x1f1f20,
            15,
            StatusColors.CHARGING,
            StatusColors.LOW,
            StatusColors.SAVER,
            StatusColors.PERFORMANCE
        )

        Assert.assertEquals(
            expected.toLong(),
            BatteryTransferPolicy.traceTint(
                19,
                true,
                false,
                true,
                true,
                -0x1f1f20,
                15,
                StatusColors.CHARGING,
                StatusColors.LOW,
                StatusColors.SAVER,
                StatusColors.PERFORMANCE
            ).toLong()
        )
    }

    @Test
    fun runtimeReadyRejectsOffScreenKeyguardPrivacyAndMissingHosts() {
        Assert.assertTrue(
            BatteryTransferPolicy.runtimeReady(
                true,
                true,
                false,
                false,
                true
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.runtimeReady(
                false,
                true,
                false,
                false,
                true
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.runtimeReady(
                true,
                false,
                false,
                false,
                true
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.runtimeReady(
                true,
                true,
                true,
                false,
                true
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.runtimeReady(
                true,
                true,
                false,
                true,
                true
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.runtimeReady(
                true,
                true,
                false,
                false,
                false
            )
        )
    }

    @Test
    fun ringTransferReplacesOnlyTheRingAndKeepsBatteryDigitsVisible() {
        val mask: Int = BatteryTransferPolicy.ringTransferPaintMask(
            BatteryTransferPolicy.MASK_HIDE_SIGNALS,
            true
        )

        Assert.assertFalse(BatteryTransferPolicy.drawsBatteryRing(mask))
        Assert.assertTrue(BatteryTransferPolicy.drawsBatteryContent(mask))
        Assert.assertFalse((mask and BatteryTransferPolicy.MASK_HIDE_CENTRE) != 0)
        Assert.assertFalse((mask and BatteryTransferPolicy.MASK_HIDE_WIFI) != 0)
        Assert.assertEquals(
            BatteryTransferPolicy.MASK_HIDE_SIGNALS.toLong(),
            BatteryTransferPolicy.ringTransferPaintMask(
                BatteryTransferPolicy.MASK_HIDE_SIGNALS,
                false
            ).toLong()
        )
    }

    @Test
    fun ringTransferKeepsTopOpeningContentVisible() {
        val mask = BatteryTransferPolicy.ringTransferPaintMask(
            0,
            true,
            true
        )

        Assert.assertFalse(BatteryTransferPolicy.drawsBatteryRing(mask))
        Assert.assertTrue(BatteryTransferPolicy.drawsBatteryContent(mask))
        Assert.assertFalse(
            (mask and BatteryTransferPolicy.MASK_HIDE_TECHNOLOGY) != 0
        )
        Assert.assertFalse(
            (mask and BatteryTransferPolicy.MASK_HIDE_WIFI) != 0
        )
        Assert.assertEquals(
            0,
            (mask and BatteryTransferPolicy.MASK_HIDE_CENTRE).toLong()
        )
        Assert.assertEquals(
            0,
            (BatteryTransferPolicy.ringTransferPaintMask(
                0,
                true,
                false
            ) and BatteryTransferPolicy.MASK_HIDE_TECHNOLOGY).toLong()
        )
    }

    @Test
    fun controlCenterReplacementStillHidesOldRingAndDigits() {
        val mask: Int = BatteryTransferPolicy.ringTransferPaintMask(
            BatteryTransferPolicy.MASK_HIDE_BATTERY, true
        )
        Assert.assertFalse(BatteryTransferPolicy.drawsBatteryRing(mask))
        Assert.assertFalse(BatteryTransferPolicy.drawsBatteryContent(mask))
    }

    @Test
    fun leavingRingTransferRestoresTheNormalRingWithoutHidingDigits() {
        val mask: Int = BatteryTransferPolicy.ringTransferPaintMask(0, false)
        Assert.assertTrue(BatteryTransferPolicy.drawsBatteryRing(mask))
        Assert.assertTrue(BatteryTransferPolicy.drawsBatteryContent(mask))
    }

    @Test
    fun reversingTheRingKeepsDigitsVisibleAtFullPanelExpansion() {
        Assert.assertEquals(1.0f, BatteryTransferPolicy.batteryContentAlpha(true, 0.0f), 0.0f)
        Assert.assertEquals(1.0f, BatteryTransferPolicy.batteryContentAlpha(true, 0.25f), 0.0f)
        Assert.assertEquals(0.25f, BatteryTransferPolicy.batteryContentAlpha(false, 0.25f), 0.0f)
        Assert.assertEquals(0.0f, BatteryTransferPolicy.batteryContentAlpha(false, 0.0f), 0.0f)
    }

    @Test
    fun compressedPointAnchorsToTheRightStartNotTheLeftEnd() {
        Assert.assertEquals(
            120.0f,
            BatteryTransferPolicy.batteryAnchorX(120.0f, 80.0f),
            0.0f
        )
        Assert.assertEquals(
            60.0f,
            BatteryTransferPolicy.batteryAnchorY(60.0f, 100.0f),
            0.0f
        )
    }

    @Test
    fun inversePathUsesUnfilledTrackColourWhileRealFillUsesBatteryTint() {
        val plainTint = -0x1f1f20
        val tint = -0xedcbaa

        Assert.assertEquals(plainTint.toLong(), BatteryTransferPolicy.ringTrackTint(plainTint).toLong())
        Assert.assertEquals(plainTint.toLong(), BatteryTransferPolicy.ringTraceTint(plainTint).toLong())
        Assert.assertEquals(tint.toLong(), BatteryTransferPolicy.realFillTint(tint).toLong())
    }

    @Test
    fun closedSettledHoldRequiresZeroControlsAndSettledMorph() {
        Assert.assertTrue(
            BatteryTransferPolicy.closedSettled(
                0.0f,
                0.0f,
                0.0f
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.closedSettled(
                0.01f,
                0.0f,
                0.0f
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.closedSettled(
                0.0f,
                0.01f,
                0.0f
            )
        )
        Assert.assertFalse(
            BatteryTransferPolicy.closedSettled(
                0.0f,
                0.0f,
                0.01f
            )
        )
    }

    @Test
    fun batteryHorizontalStageLastsLongerWithoutChangingSharedMorph() {
        val horizontalEnd = BatteryTransferConfig.BATTERY_HORIZONTAL_CLOSE_PROGRESS

        Assert.assertEquals(
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END,
            BatteryTransferPolicy.stagedBatteryCollapse(horizontalEnd),
            0.0001f
        )
        Assert.assertTrue(
            BatteryTransferPolicy.stagedBatteryCollapse(0.89f)
                    < BatteryTransferConfig.HORIZONTAL_COLLAPSE_END
        )
        Assert.assertEquals(
            BatteryTransferConfig.HORIZONTAL_COLLAPSE_END,
            BatteryTransferPolicy.stagedBatteryCollapse(0.90f),
            0.0001f
        )
        Assert.assertEquals(
            1.0f,
            BatteryTransferPolicy.stagedBatteryCollapse(1.0f),
            0.0001f
        )
    }
}
