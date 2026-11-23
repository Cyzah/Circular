package com.cyzah.circular.hook.runtime
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.domain.settings.DualStyle
import org.junit.Assert
import org.junit.Test
class SessionPaintPolicyTest {
    @Test
    fun closedControlCenterCopyHandsAllContentBackToTheStatusBar() {
        Assert.assertTrue(SessionPaintPolicy.defersClosedControlCenterCopy(true, true, 0.0f, 0.0f, 0.0f))
        Assert.assertFalse(SessionPaintPolicy.defersClosedControlCenterCopy(true, true, 0.01f, 0.0f, 0.01f))
        Assert.assertFalse(SessionPaintPolicy.defersClosedControlCenterCopy(true, false, 0.0f, 0.0f, 0.0f))
        Assert.assertFalse(SessionPaintPolicy.defersClosedControlCenterCopy(false, true, 0.0f, 0.0f, 0.0f))
    }
    @Test
    fun openingRingHostOnlyDrawsItsBatteryContentWhilePanelOwnsNetwork() {
        val mask = SessionPaintPolicy.ringHostContentMask(true)
        Assert.assertFalse(BatteryTransferPolicy.drawsBatteryRing(mask))
        Assert.assertTrue(BatteryTransferPolicy.drawsBatteryContent(mask))
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_CENTRE) != 0)
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_WIFI) != 0)
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_SIGNALS) != 0)
        Assert.assertEquals(0, SessionPaintPolicy.ringHostContentMask(false).toLong())
    }
    @Test
    fun solvedRowCannotDrawWifiFromAnOlderNativePartsList() {
        Assert.assertFalse(SessionPaintPolicy.solvedRowWifiVisible(true, true, false))
        Assert.assertTrue(SessionPaintPolicy.solvedRowWifiVisible(true, true, true))
    }
    @Test
    fun missingSolvedRowKeepsLiveWifiAndDisconnectStillHidesIt() {
        Assert.assertTrue(SessionPaintPolicy.solvedRowWifiVisible(true, false, false))
        Assert.assertFalse(SessionPaintPolicy.solvedRowWifiVisible(false, true, true))
        Assert.assertFalse(SessionPaintPolicy.solvedRowWifiVisible(false, false, false))
    }
    @Test
    fun hostSixUsesSolvedRowGeometryWithoutLegacyAvoidance() {
        Assert.assertTrue(
            SessionPaintPolicy.usesSolvedControlCenterRow(
                6, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesSolvedControlCenterRow(
                5, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesSolvedControlCenterRow(
                6, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesSolvedControlCenterRow(
                6, true, false
            )
        )
    }
    @Test
    fun hostFiveReusesThePrimarySnapshot() {
        Assert.assertTrue(
            SessionPaintPolicy.reusesPrimaryRowSnapshot(
                5, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.reusesPrimaryRowSnapshot(
                6, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.reusesPrimaryRowSnapshot(
                5, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.reusesPrimaryRowSnapshot(
                5, true, false
            )
        )
    }
    @Test
    fun primaryRowTranslationAvoidsDoubleHostShift() {
        Assert.assertEquals(
            -62.0f, SessionPaintPolicy.primaryRowTranslation(
                -62.0f, 0.0f
            ), 0.001f
        )
        Assert.assertEquals(
            -31.0f, SessionPaintPolicy.primaryRowTranslation(
                -62.0f, -31.0f
            ), 0.001f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.primaryRowTranslation(
                -62.0f, -62.0f
            ), 0.001f
        )
    }
    @Test
    fun sharedTargetGenerationIgnoresHostLocalPaintState() {
        Assert.assertEquals(
            0L, SessionPaintPolicy.targetGenerationLocalPaintHash(
                false, 12345L
            )
        )
        Assert.assertEquals(
            12345L, SessionPaintPolicy.targetGenerationLocalPaintHash(
                true, 12345L
            )
        )
    }
    @Test
    fun hostFiveTranslationUsesTheDispatchFrameAlpha() {
        Assert.assertEquals(
            -31.0f, SessionPaintPolicy.rowTransitionTranslation(
                -62.0f, -62.0f, 0.5f
            ), 0.001f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.rowTransitionTranslation(
                -62.0f, -62.0f, 1.0f
            ), 0.001f
        )
    }
    @Test
    fun liveHostOriginCancelsTheOuterCanvasShift() {
        Assert.assertEquals(
            -5.0f, SessionPaintPolicy.rowTransitionTranslation(
                0.0f, 10.0f, 0.5f
            ), 0.001f
        )
        Assert.assertEquals(
            -10.0f, SessionPaintPolicy.rowTransitionTranslation(
                0.0f, 10.0f, 1.0f
            ), 0.001f
        )
        Assert.assertEquals(
            6.0f, SessionPaintPolicy.rowTransitionTranslation(
                -25.0f, -62.0f, 0.5f
            ), 0.001f
        )
    }
    @Test
    fun solvedRowDriftTranslatesTheWholeRowOnce() {
        Assert.assertEquals(
            -29.0f, SessionPaintPolicy.rowTranslationWithDrift(
                -31.0f, 4.0f, 0.5f
            ), 0.001f
        )
    }
    @Test
    fun collapsedTargetRequiresHostZeroAndCurrentGeometry() {
        Assert.assertTrue(
            SessionPaintPolicy.canUseCollapsedRowTarget(
                true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.canUseCollapsedRowTarget(
                false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.canUseCollapsedRowTarget(
                true, false
            )
        )
    }
    @Test
    fun localRowProgressIsZeroAfterReversingAtSixtyPercent() {
        Assert.assertEquals(
            0.6f, SessionPaintPolicy.localRowProgress(
                1.0f, 0.4f, false
            ), 0.001f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.localRowProgress(
                0.4f, 0.4f, true
            ), 0.001f
        )
        Assert.assertEquals(
            0.5f, SessionPaintPolicy.localRowProgress(
                0.4f, 0.7f, true
            ), 0.001f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.localRowProgress(
                0.4f, 1.0f, true
            ), 0.001f
        )
    }
    @Test
    fun firstObservedRowMotionChoosesItsDirectionFromExpansion() {
        Assert.assertTrue(
            SessionPaintPolicy.beginsRowTransitionExpanded(
                0.0f, 0.01f
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.beginsRowTransitionExpanded(
                1.0f, 0.99f
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.beginsRowTransitionExpanded(
                0.0f, 0.0f
            )
        )
    }
    @Test
    fun rowTargetMustMatchTheCurrentAppearanceGeneration() {
        Assert.assertTrue(
            SessionPaintPolicy.transitionTargetCurrent(
                true, 41L, 41L
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.transitionTargetCurrent(
                true, 40L, 41L
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.transitionTargetCurrent(
                false, 41L, 41L
            )
        )
    }
    @Test
    fun activeRowVisibilityMustMatchTheFrozenVisibility() {
        Assert.assertTrue(
            SessionPaintPolicy.rowTransitionVisibilityCurrent(
                true, true, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.rowTransitionVisibilityCurrent(
                true, false, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.rowTransitionVisibilityCurrent(
                true, true, true, false
            )
        )
    }
    @Test
    fun transitionContentFreezesBatteryAndTechnologyUntilSettle() {
        Assert.assertEquals(
            9, SessionPaintPolicy.transitionBattery(
                10, 9, true
            ).toLong()
        )
        Assert.assertEquals(
            10, SessionPaintPolicy.transitionBattery(
                10, 9, false
            ).toLong()
        )
        Assert.assertEquals(
            "5G", SessionPaintPolicy.transitionText(
                "5G+", "5G", true
            )
        )
        Assert.assertEquals(
            "5G+", SessionPaintPolicy.transitionText(
                "5G+", "5G", false
            )
        )
    }
    @Test
    fun solvedBatteryBoundsMapEveryNumberPlacementFromTheirLeftEdge() {
        val hiddenOrigin = SessionPaintPolicy.solvedBatteryOriginX(
            88.0f, 200.0f, -12.0f
        )
        val insideOrigin = SessionPaintPolicy.solvedBatteryOriginX(
            88.0f, 200.0f, -12.0f
        )
        val outsideOrigin = SessionPaintPolicy.solvedBatteryOriginX(
            28.0f, 200.0f, -72.0f
        )
        Assert.assertEquals(
            100.0f, hiddenOrigin, 0.001f
        )
        Assert.assertEquals(
            100.0f, insideOrigin, 0.001f
        )
        Assert.assertEquals(
            100.0f, outsideOrigin, 0.001f
        )
    }
    @Test
    fun controlCenterRowsDeferOnlyAfterQuarterExpansion() {
        Assert.assertTrue(SessionPaintPolicy.defersControlCenterRow(0, 0.26f))
        Assert.assertFalse(SessionPaintPolicy.defersControlCenterRow(0, 0.25f))
        Assert.assertFalse(SessionPaintPolicy.defersControlCenterRow(2, 1.0f))
    }
    @Test
    fun batteryHostWaitsForShapeHandoffEvenAfterControlReachesZero() {
        Assert.assertTrue(
            SessionPaintPolicy.defersBatteryHostDuringTransfer(
                0, 0.0f, true, false
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.defersBatteryHostDuringTransfer(
                1, 0.0f, true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.defersBatteryHostDuringTransfer(
                0, 0.0f, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.defersBatteryHostDuringTransfer(
                0, 0.0f, false, false
            )
        )
    }
    @Test
    fun ambientOrUnmergedHostUsesSimpleIconPath() {
        Assert.assertTrue(SessionPaintPolicy.usesSimpleIcon(true, true))
        Assert.assertTrue(SessionPaintPolicy.usesSimpleIcon(false, false))
        Assert.assertFalse(SessionPaintPolicy.usesSimpleIcon(false, true))
    }
    @Test
    fun capsuleTransferBypassesTheUnmergedSimpleIconFallback() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "usesSimpleIcon", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertFalse(
                (method.invoke(
                    null, false, false, true
                ) as Boolean?)!!
            )
            Assert.assertTrue(
                (method.invoke(
                    null, true, false, true
                ) as Boolean?)!!
            )
            Assert.assertTrue(
                (method.invoke(
                    null, false, false, false
                ) as Boolean?)!!
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing capsule transfer detailed-pipeline bypass", error
            )
        }
    }
    @Test
    fun capsuleOwnsDetailedSignalsOnControlCenterHostsFromTheFirstFrame() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "keepsCapsuleControlCenterSignalOwner", DualStyle::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertTrue(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.CAPSULE, true, false, true, true, true
                ) as Boolean?)!!
            )
            Assert.assertTrue(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.CAPSULE, false, true, true, true, true
                ) as Boolean?)!!
            )
            Assert.assertFalse(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.CAPSULE, false, false, true, true, true
                ) as Boolean?)!!
            )
            Assert.assertFalse(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.DUO, true, true, true, true, true
                ) as Boolean?)!!
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing early capsule control-center ownership", error
            )
        }
    }
    @Test
    fun disabledStackedSignalsReleaseCapsuleNativeOwnership() {
        Assert.assertFalse(
            SessionPaintPolicy.keepsCapsuleControlCenterSignalOwner(
                DualStyle.CAPSULE, true, false, true, true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.keepsCapsuleControlCenterSignalOwner(
                DualStyle.CAPSULE, false, true, true, true, false
            )
        )
    }
    @Test
    fun stackedOverlayKeepsTailFramesAfterActiveFlagClears() {
        Assert.assertTrue(SessionPaintPolicy.drawsStacked(false, 0.003f, true, true))
        Assert.assertFalse(SessionPaintPolicy.drawsStacked(false, 0.002f, true, true))
        Assert.assertFalse(SessionPaintPolicy.drawsStacked(true, 1.0f, false, true))
        Assert.assertFalse(SessionPaintPolicy.drawsStacked(true, 1.0f, true, false))
    }
    @Test
    fun batteryReplacementSuppressesNativeBatteryDraw() {
        Assert.assertFalse(SessionPaintPolicy.drawsNativeBattery(true, 1.0f))
        Assert.assertFalse(SessionPaintPolicy.drawsNativeBattery(true, 0.4f))
        Assert.assertTrue(SessionPaintPolicy.drawsNativeBattery(false, 1.0f))
        Assert.assertFalse(SessionPaintPolicy.drawsNativeBattery(false, 0.0f))
    }
    @Test
    fun batteryReplacementKeepsWifiInTheModulePipeline() {
        Assert.assertTrue(
            SessionPaintPolicy.prefersModuleWifi(
                true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.prefersModuleWifi(
                false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.drawsNativeWifi(
                true, 1.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.drawsNativeWifi(
                false, 1.0f
            )
        )
    }
    @Test
    fun movingModuleWifiSuppressesTheBaseWifiLayer() {
        val mask = SessionPaintPolicy.moduleWifiPaintMask(0, true)
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_WIFI) != 0)
        Assert.assertEquals(0, SessionPaintPolicy.moduleWifiPaintMask(0, false).toLong())
        Assert.assertEquals(
            8, SessionPaintPolicy.moduleWifiPaintMask(8, false).toLong()
        )
    }
    @Test
    fun unownedNativeIconsRemainVisibleWhenPanelHostExists() {
        // An attached panel host does not imply that it paints a replacement.
        // Wi-Fi without a module owner and mobile without stacked drawing
        // both retain their native layer through the same alpha policy.
        Assert.assertTrue(
            SessionPaintPolicy.drawsNativeWifi(
                false, 1.0f
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.drawsNativeWifi(
                true, 1.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.drawsNativeWifi(
                false, 0.5f
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.drawsNativeWifi(
                false, 0.0f
            )
        )
    }
    @Test
    fun controlCenterWifiAlwaysStartsFromItsSolvedRowSlot() {
        Assert.assertTrue(
            SessionPaintPolicy.usesControlCenterWifiSlot(
                true, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.usesControlCenterWifiSlot(
                true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesControlCenterWifiSlot(
                false, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.usesControlCenterWifiSlot(
                false, false
            )
        )
    }
    @Test
    fun controlCenterWifiHasOnlyOnePaintOwner() {
        Assert.assertTrue(SessionPaintPolicy.ownsControlCenterWifi(6, 6))
        Assert.assertFalse(SessionPaintPolicy.ownsControlCenterWifi(5, 6))
        Assert.assertTrue(SessionPaintPolicy.ownsControlCenterWifi(5, 5))
        Assert.assertFalse(SessionPaintPolicy.ownsControlCenterWifi(6, 5))
        Assert.assertTrue(SessionPaintPolicy.ownsControlCenterWifi(0, 6))
    }
    @Test
    fun alignedWifiCopiesKeepTheirFadeWhenPreferredHostChanges() {
        val before = ((if (SessionPaintPolicy.ownsControlCenterWifi(5, 6, true)) 0.99821f else 0f)
                + (if (SessionPaintPolicy.ownsControlCenterWifi(6, 6, true)) 0.00179f else 0f))
        val after = ((if (SessionPaintPolicy.ownsControlCenterWifi(5, 5, true)) 0.999434f else 0f)
                + (if (SessionPaintPolicy.ownsControlCenterWifi(6, 5, true)) 0.000566f else 0f))
        Assert.assertEquals(1f, before, 0.00001f)
        Assert.assertEquals(1f, after, 0.00001f)
        Assert.assertFalse(SessionPaintPolicy.ownsControlCenterWifi(5, 6, false))
    }
    @Test
    fun capsuleDualSignalsMoveLeftToKeepTheWifiGap() {
        Assert.assertEquals(
            13.0f, SessionPaintPolicy.capsuleSignalRowShiftX(
                DualStyle.CAPSULE, true, true, false, 3.25f * ControlCenterAppearance.CAPSULE_WIFI_LEFT_GAP_DP, 1.0f
            ), 0.001f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.capsuleSignalRowShiftX(
                DualStyle.CAPSULE, true, true, false, 6.5f, 0.0f
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.capsuleSignalRowShiftX(
                DualStyle.DUO, true, true, false, 6.5f, 1.0f
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.capsuleSignalRowShiftX(
                DualStyle.CAPSULE, true, false, false, 6.5f, 1.0f
            ), 0.0f
        )
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.capsuleSignalRowShiftX(
                DualStyle.CAPSULE, true, true, true, 6.5f, 1.0f
            ), 0.0f
        )
    }
    @Test
    fun chargingAlsoDisablesMeasuredRowSpeedShift() {
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.stableRowAlignmentShift(62.0f, true), 0.001f
        )
        Assert.assertEquals(
            62.0f, SessionPaintPolicy.stableRowAlignmentShift(62.0f, false), 0.001f
        )
    }
    @Test
    fun mergedControlCenterWidthUsesAStableBaseWidth() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "mergedControlCenterWidth", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                180, (method.invoke(
                    null, 88, 92, 1.0f
                ) as Int).toLong()
            )
            Assert.assertEquals(
                134, (method.invoke(
                    null, 88, 92, 0.5f
                ) as Int).toLong()
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing stable merged-row width", error
            )
        }
    }
    @Test
    fun painterUsesTheStableBaseWidthInsteadOfChargingWidth() {
        Assert.assertEquals(
            88.0f, SessionPaintPolicy.stableControlCenterBaseWidth(
                112.0f, 88.0f
            ), 0.001f
        )
        Assert.assertEquals(
            80.0f, SessionPaintPolicy.stableControlCenterBaseWidth(
                80.0f, 88.0f
            ), 0.001f
        )
    }
    @Test
    fun controlCenterBaseWidthDoesNotJumpAtTheCollapseEndpoint() {
        Assert.assertEquals(
            91.0f, SessionPaintPolicy.controlCenterBaseWidth(
                91.0f, 88.0f, true, 0.0f
            ), 0.001f
        )
        Assert.assertEquals(
            89.5f, SessionPaintPolicy.controlCenterBaseWidth(
                91.0f, 88.0f, true, 0.5f
            ), 0.001f
        )
        Assert.assertEquals(
            88.0f, SessionPaintPolicy.controlCenterBaseWidth(
                91.0f, 88.0f, true, 1.0f
            ), 0.001f
        )
        Assert.assertEquals(
            91.0f, SessionPaintPolicy.controlCenterBaseWidth(
                91.0f, 88.0f, false, 1.0f
            ), 0.001f
        )
    }
    @Test
    fun controlCenterMeasurementConvergesToItsComponentBase() {
        Assert.assertEquals(
            160, SessionPaintPolicy.controlCenterHostBaseWidth(
                160, 88, 0.0f
            ).toLong()
        )
        Assert.assertEquals(
            124, SessionPaintPolicy.controlCenterHostBaseWidth(
                160, 88, 0.5f
            ).toLong()
        )
        Assert.assertEquals(
            88, SessionPaintPolicy.controlCenterHostBaseWidth(
                160, 88, 1.0f
            ).toLong()
        )
    }
    @Test
    fun controlCenterHostsKeepNativeSignalStateHidden() {
        Assert.assertTrue(
            SessionPaintPolicy.hidesNativeSignalState(
                6, false, false, true, false
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.hidesNativeSignalState(
                5, false, true, true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesNativeSignalState(
                0, false, false, true, false
            )
        )
    }
    @Test
    fun rapidPanelSwitchKeepsNativeStatusIconsHidden() {
        Assert.assertTrue(
            SessionPaintPolicy.hidesNativeSignalState(
                0, false, false, true, false, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.hidesNativeSignalState(
                1, false, false, true, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesNativeSignalState(
                0, false, false, true, true, true
            )
        )
    }
    @Test
    fun controlCenterLayoutRestoresEachStyleBaseline() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "controlCenterLayoutSize", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                22, (method.invoke(
                    null, 22, true
                ) as Int).toLong()
            )
            Assert.assertEquals(
                27, (method.invoke(
                    null, 27, true
                ) as Int).toLong()
            )
            Assert.assertEquals(
                27, (method.invoke(
                    null, 27, false
                ) as Int).toLong()
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing fixed control-center layout size", error
            )
        }
    }
    @Test
    fun everyControlCenterWifiEndsAtUnitCanvasScale() {
        Assert.assertEquals(
            0.6f, SessionPaintPolicy.endpointUnit(72.0f, 150.0f), 0.001f
        )
        Assert.assertEquals(
            0.72f, SessionPaintPolicy.controlCenterWifiCanvasScale(
                0.72f, 0.0f
            ), 0.001f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.controlCenterWifiCanvasScale(
                0.72f, 1.0f
            ), 0.001f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.controlCenterWifiCanvasScale(
                0.975333f, 1.0f
            ), 0.001f
        )
    }
    @Test
    fun everyControlCenterWifiStartsAtItsStatusBarScale() {
        Assert.assertEquals(
            0.78f, SessionPaintPolicy.controlCenterWifiStartCanvasScale(
                DualStyle.DUO, 0.6f, 1.30f, 1.10f
            ), 0.001f
        )
        Assert.assertEquals(
            0.66f, SessionPaintPolicy.controlCenterWifiStartCanvasScale(
                DualStyle.CAPSULE, 0.6f, 1.30f, 1.10f
            ), 0.001f
        )
    }
    @Test
    fun capsuleWifiUsesItsOwnSourceGeometry() {
        for (style in DualStyle.entries) {
            if (style == DualStyle.CAPSULE) {
                continue
            }
            Assert.assertEquals(
                120.0f, SessionPaintPolicy.controlCenterWifiSourceX(
                    style, 120.0f, 82.0f
                ), 0.001f
            )
        }
        Assert.assertEquals(
            82.0f, SessionPaintPolicy.controlCenterWifiSourceX(
                DualStyle.CAPSULE, 120.0f, 82.0f
            ), 0.001f
        )
    }
    @Test
    fun styleSpecificSignalTargetsKeepCapsuleAndDuoGeometry() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "controlCenterSignalTargetX", DualStyle::class.java, Float::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertEquals(
                91.0f, (method.invoke(
                    null, DualStyle.CAPSULE, 91.0f, 0, 1, 0.7f, 1.2f
                ) as Float), 0.001f
            )
            Assert.assertEquals(
                91.0f, (method.invoke(
                    null, DualStyle.DUO, 91.0f, 0, 3, 0.7f, 1.2f
                ) as Float), 0.001f
            )
            Assert.assertEquals(
                91.0f, (method.invoke(
                    null, DualStyle.GROUPED, 91.0f, 1, 1, 0.7f, 1.2f
                ) as Float), 0.001f
            )
            Assert.assertEquals(
                IconGeometry.innerDotX(1, 0.7f, 1.2f), (method.invoke(
                    null, DualStyle.LAYERED, 91.0f, 0, 1, 0.7f, 1.2f
                ) as Float), 0.001f
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing capsule-aware signal target geometry", error
            )
        }
    }
    @Test
    fun allDualStylesUseTheSameSignalTargetXGeometry() {
        Assert.assertEquals(
            IconGeometry.innerDotX(1, 0.7f), SessionPaintPolicy.controlCenterSignalTargetX(
                0, 1, 0.7f
            ), 0.001f
        )
        Assert.assertEquals(
            IconGeometry.outerDotX(2), SessionPaintPolicy.controlCenterSignalTargetX(
                1, 2, 0.7f
            ), 0.001f
        )
    }
    @Test
    fun alphaLayersAreUsedOnlyForIntermediateFrames() {
        Assert.assertFalse(SessionPaintPolicy.needsAlphaLayer(0.0f))
        Assert.assertTrue(SessionPaintPolicy.needsAlphaLayer(0.5f))
        Assert.assertFalse(SessionPaintPolicy.needsAlphaLayer(0.999f))
        Assert.assertFalse(SessionPaintPolicy.needsAlphaLayer(1.0f))
        Assert.assertFalse(SessionPaintPolicy.needsAlphaLayer(Float.NaN))
    }
    @Test
    fun nonFiniteAlphaDoesNotDrawAsOpaque() {
        Assert.assertFalse(SessionPaintPolicy.drawsAlpha(Float.NaN))
        Assert.assertFalse(SessionPaintPolicy.drawsAlpha(Float.NEGATIVE_INFINITY))
        Assert.assertFalse(SessionPaintPolicy.drawsAlpha(0.0f))
        Assert.assertTrue(SessionPaintPolicy.drawsAlpha(0.001f))
        Assert.assertTrue(SessionPaintPolicy.drawsAlpha(1.0f))
    }
    @Test
    fun everyControlCenterHostSuppressesNativeBatteryWhileOneOwnsReplacement() {
        Assert.assertTrue(
            SessionPaintPolicy.controlCenterReplacementActive(
                true, true, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.controlCenterReplacementActive(
                false, true, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.controlCenterReplacementActive(
                true, false, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.controlCenterReplacementActive(
                true, true, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.controlCenterReplacementActive(
                true, true, false, false
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.isPreferredBatteryOwner(
                true, true, true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.isPreferredBatteryOwner(
                true, false, true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.isPreferredBatteryOwner(
                true, true, true, true
            )
        )
    }
    @Test
    fun capsuleContentIsRedrawnAboveTheTransferRing() {
        Assert.assertTrue(
            SessionPaintPolicy.redrawsCapsuleContent(
                DualStyle.CAPSULE, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.redrawsCapsuleContent(
                DualStyle.CAPSULE, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.redrawsCapsuleContent(
                DualStyle.DUO, true
            )
        )
    }
    @Test
    fun capsuleContentRedrawHidesBatteryAndSignalPoints() {
        val mask = SessionPaintPolicy.capsuleContentRedrawMask()
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_BATTERY) != 0)
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_SIGNALS) != 0)
        Assert.assertFalse((mask and BatteryTransferPolicy.MASK_HIDE_WIFI) != 0)
        Assert.assertFalse((mask and BatteryTransferPolicy.MASK_HIDE_CENTRE) != 0)
    }
    @Test
    fun capsuleRingTransferKeepsOverlayAsTheOnlySignalOwner() {
        val mask = SessionPaintPolicy.capsuleRingPaintMask(
            IconPainter.Companion.MASK_HIDE_SIGNALS, DualStyle.CAPSULE, true
        )
        Assert.assertTrue((mask and IconPainter.Companion.MASK_HIDE_SIGNALS) != 0)
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.capsuleOverlaySignalAlpha(
                DualStyle.CAPSULE, true
            ), 0.0f
        )
    }
    @Test
    fun capsuleRingTransferKeepsTheOverlayAtTheStaticEndpoint() {
        try {
            val method =
                SessionPaintPolicy::class.java.getDeclaredMethod(
                    "keepsCapsuleRingSignalOwner", DualStyle::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                )
            method.setAccessible(true)
            Assert.assertTrue(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.CAPSULE, true, true
                ) as Boolean?)!!
            )
            Assert.assertFalse(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.CAPSULE, true, false
                ) as Boolean?)!!
            )
            Assert.assertFalse(
                (method.invoke(
                    null, com.cyzah.circular.domain.settings.DualStyle.DUO, true, true
                ) as Boolean?)!!
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing static capsule ring signal owner", error
            )
        }
    }
    @Test
    fun capsuleOverlaySignalsStayOwnedDuringRingTransfer() {
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.capsuleOverlaySignalAlpha(
                DualStyle.CAPSULE, true
            ), 0.0f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.capsuleOverlaySignalAlpha(
                DualStyle.CAPSULE, false
            ), 0.0f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.capsuleOverlaySignalAlpha(
                DualStyle.DUO, true
            ), 0.0f
        )
    }
    @Test
    fun rowDiagnosticContainsEverySolvedRectAndGap() {
        val input = ControlCenterRowInput()
        input.reset()
        input.setBounds(300.0f, 600.0f)
        input.set(
            ControlCenterRowComponent.BATTERY, true, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f
        )
        input.set(
            ControlCenterRowComponent.WIFI, true, 40.0f, 0.0f, 0.0f, 0.0f, 6.0f
        )
        input.set(
            ControlCenterRowComponent.CELLULAR, true, 80.0f, 0.0f, 0.0f, 0.0f, 4.0f
        )
        val layout = ControlCenterRowLayout()
        Assert.assertTrue(layout.solve(input))
        val row = ControlCenterRowSnapshot()
        Assert.assertTrue(row.copyFrom(layout, 42L))
        val diagnostic = SessionPaintPolicy.controlCenterRowDiagnostic(
            6, DualStyle.DUO, row, "none"
        )
        Assert.assertTrue(diagnostic.contains("host=6"))
        Assert.assertTrue(diagnostic.contains("style=DUO"))
        Assert.assertTrue(diagnostic.contains("batteryRect=200.000..300.000"))
        Assert.assertTrue(
            diagnostic.contains(
                "batteryRect=200.000..300.000,width=100.000"
            )
        )
        Assert.assertTrue(diagnostic.contains("wifiRect=154.000..194.000"))
        Assert.assertTrue(
            diagnostic.contains(
                "wifiRect=154.000..194.000,width=40.000"
            )
        )
        Assert.assertTrue(diagnostic.contains("cellularRect=70.000..150.000"))
        Assert.assertTrue(
            diagnostic.contains(
                "cellularRect=70.000..150.000,width=80.000"
            )
        )
        Assert.assertTrue(diagnostic.contains("batteryWifiGap=6.000"))
        Assert.assertTrue(diagnostic.contains("wifiCellularGap=4.000"))
        Assert.assertTrue(diagnostic.contains("outerLeft=0.000"))
        Assert.assertTrue(diagnostic.contains("outerRight=0.000"))
        Assert.assertTrue(diagnostic.contains("rowWidth=230.000"))
        Assert.assertTrue(diagnostic.contains("batteryRightAnchor=300.000"))
        Assert.assertTrue(diagnostic.contains("generation=42"))
        Assert.assertTrue(diagnostic.contains("fallbackReason=none"))
    }
    @Test
    fun invalidControlCenterRowDiagnosticIsExplicit() {
        val diagnostic = SessionPaintPolicy.controlCenterRowDiagnostic(
            6, DualStyle.DUO, ControlCenterRowSnapshot(), "layout-invalid"
        )
        Assert.assertTrue(diagnostic.contains("batteryRect=invalid"))
        Assert.assertTrue(diagnostic.contains("wifiRect=invalid"))
        Assert.assertTrue(diagnostic.contains("cellularRect=invalid"))
        Assert.assertTrue(diagnostic.contains("fallbackReason=layout-invalid"))
    }
    @Test
    fun hostFiveEmitsOnlyItsFallbackDiagnostic() {
        Assert.assertTrue(
            SessionPaintPolicy.logsControlCenterRowDiagnostic(
                6, false
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.logsControlCenterRowDiagnostic(
                5, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.logsControlCenterRowDiagnostic(
                5, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.logsControlCenterRowDiagnostic(
                0, false
            )
        )
    }
    @Test
    fun nativeStatusHoldArmsBeforePanelExpansionWorkRuns() {
        Assert.assertTrue(SessionPaintPolicy.shouldHoldNativeIcons(0.03f, true, true))
        Assert.assertFalse(SessionPaintPolicy.shouldHoldNativeIcons(0.02f, true, true))
        Assert.assertFalse(SessionPaintPolicy.shouldHoldNativeIcons(0.0f, true, true))
        Assert.assertFalse(SessionPaintPolicy.shouldHoldNativeIcons(0.5f, false, true))
        Assert.assertFalse(SessionPaintPolicy.shouldHoldNativeIcons(0.5f, true, false))
    }
    @Test
    fun nativeOwnershipResetsWhenDualRowIsDisabled() {
        Assert.assertTrue(
            SessionPaintPolicy.resetsNativeRowOwnership(
                false, false
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.resetsNativeRowOwnership(
                true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.resetsNativeRowOwnership(
                true, false
            )
        )
    }
    @Test
    fun solvedTrinityStartsAtTheControlCenterBatterySlot() {
        Assert.assertEquals(
            200.0f, SessionPaintPolicy.expandedTrinityCenterX(
                true, 180.0f, 40.0f, 100.0f
            ), 0.001f
        )
        Assert.assertEquals(
            100.0f, SessionPaintPolicy.expandedTrinityCenterX(
                false, 180.0f, 40.0f, 100.0f
            ), 0.001f
        )
    }
    @Test
    fun activeNativeHoldRequestsAnImmediateVisibilitySweep() {
        Assert.assertTrue(SessionPaintPolicy.forceHidesNativeIcons(true))
        Assert.assertFalse(SessionPaintPolicy.forceHidesNativeIcons(false))
    }
    @Test
    fun controlCenterHostsStayOwnedWhileAttachedAcrossRelayout() {
        Assert.assertTrue(
            SessionPaintPolicy.batteryHostPaintAvailable(
                6, false, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.batteryHostPaintAvailable(
                5, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.batteryHostPaintAvailable(
                6, false, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.batteryHostPaintAvailable(
                0, false, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.batteryHostPaintAvailable(
                0, true, false
            )
        )
    }
    @Test
    fun attachedControlCenterHostsKeepOneBatteryPaintOwner() {
        Assert.assertEquals(
            6, SessionPaintPolicy.preferredControlCenterHost(
                true, true
            ).toLong()
        )
        Assert.assertEquals(
            5, SessionPaintPolicy.preferredControlCenterHost(
                false, true
            ).toLong()
        )
        Assert.assertEquals(
            -1, SessionPaintPolicy.preferredControlCenterHost(
                false, false
            ).toLong()
        )
    }
    @Test
    fun batteryPaintPrefersVisibleHostsThenAttachedFallback() {
        Assert.assertEquals(
            6, SessionPaintPolicy.preferredBatteryPaintHost(
                true, true, true, true
            ).toLong()
        )
        Assert.assertEquals(
            5, SessionPaintPolicy.preferredBatteryPaintHost(
                false, true, true, true
            ).toLong()
        )
        Assert.assertEquals(
            6, SessionPaintPolicy.preferredBatteryPaintHost(
                false, false, true, true
            ).toLong()
        )
        Assert.assertEquals(
            5, SessionPaintPolicy.preferredBatteryPaintHost(
                false, false, false, true
            ).toLong()
        )
        Assert.assertEquals(
            -1, SessionPaintPolicy.preferredBatteryPaintHost(
                false, false, false, false
            ).toLong()
        )
    }
    @Test
    fun nativeBatteryReturnsWhenNoControlCenterHostCanPaintIt() {
        Assert.assertTrue(
            SessionPaintPolicy.suppressesNativeBattery(
                true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.suppressesNativeBattery(
                true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.suppressesNativeBattery(
                false, true
            )
        )
    }
    @Test
    fun retainedMergedControlCenterKeepsItsLocalOriginWhenStatusBarMovesOrScales() {
        val localOrigin = 241.0f
        for (host in intArrayOf(5, 6)) {
            for (statusOrigin in floatArrayOf(-120.0f, 76.0f, 500.0f)) {
                for (statusScale in floatArrayOf(0.5f, 1.0f, 1.5f)) {
                    val projectedOrigin = statusOrigin + 60.0f * statusScale
                    Assert.assertEquals(
                        "Retained host " + host + " must not follow the status bar", localOrigin, SessionPaintPolicy.transferTargetOrigin(
                            projectedOrigin, localOrigin, SessionPaintPolicy.usesControlCenterTransferProjection(host, true)
                        ), 0.001f
                    )
                }
            }
        }
    }
    @Test
    fun ordinaryControlCenterTransferStillUsesProjectionAndOtherHostsDoNot() {
        for (host in 0..6) {
            val controlCenter = host == 5 || host == 6
            val projects = SessionPaintPolicy.usesControlCenterTransferProjection(host, false)
            Assert.assertEquals(controlCenter, projects)
            Assert.assertEquals(
                if (controlCenter) 76.0f else 241.0f, SessionPaintPolicy.transferTargetOrigin(76.0f, 241.0f, projects), 0.001f
            )
            if (!controlCenter) {
                Assert.assertFalse(SessionPaintPolicy.usesControlCenterTransferProjection(host, true))
            }
        }
    }
    @Test
    fun projectedRingOriginWinsOverMovingHostLocalFallback() {
        Assert.assertEquals(
            76.0f, SessionPaintPolicy.transferTargetOrigin(
                76.0f, 241.0f, true
            ), 0.001f
        )
        Assert.assertEquals(
            241.0f, SessionPaintPolicy.transferTargetOrigin(
                76.0f, 241.0f, false
            ), 0.001f
        )
        Assert.assertEquals(
            241.0f, SessionPaintPolicy.transferTargetOrigin(
                Float.NaN, 241.0f, true
            ), 0.001f
        )
    }
    @Test
    fun ringTransferHidesSwappedTopContentForFlatStyles() {
        Assert.assertTrue(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.LAYERED, true, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.GROUPED, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.DUO, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.CAPSULE, true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.LAYERED, false, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.hidesTopRingContentDuringTransfer(
                DualStyle.LAYERED, true, false
            )
        )
    }
    @Test
    fun swappedTopWifiStopsDrawingBeforeTheRingTransfer() {
        Assert.assertTrue(SessionPaintPolicy.drawsSwappedTopWifi(false, false))
        Assert.assertFalse(SessionPaintPolicy.drawsSwappedTopWifi(true, false))
        Assert.assertFalse(SessionPaintPolicy.drawsSwappedTopWifi(false, true))
    }
    @Test
    fun swappedTopContentHidesWhenBatteryShapeStartsClosing() {
        Assert.assertFalse(
            SessionPaintPolicy.topContentTransferActive(
                true, false, false, 1.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.topContentTransferActive(
                true, false, false, 0.98f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.topContentTransferActive(
                false, true, false, 0.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.topContentTransferActive(
                false, false, true, 0.0f
            )
        )
    }
    @Test
    fun swappedFlatStylesKeepBaseWifiAndHideOnlyTechnology() {
        Assert.assertTrue(
            SessionPaintPolicy.usesBaseWifiForSwappedFlat(
                DualStyle.LAYERED, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.usesBaseWifiForSwappedFlat(
                DualStyle.GROUPED, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesBaseWifiForSwappedFlat(
                DualStyle.DUO, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesBaseWifiForSwappedFlat(
                DualStyle.LAYERED, false
            )
        )
        Assert.assertEquals(
            (BatteryTransferPolicy.MASK_HIDE_SIGNALS
                    or BatteryTransferPolicy.MASK_HIDE_TECHNOLOGY).toLong(), SessionPaintPolicy.stackedContentPaintMask(true).toLong()
        )
        Assert.assertEquals(
            (BatteryTransferPolicy.MASK_HIDE_SIGNALS
                    or BatteryTransferPolicy.MASK_HIDE_CENTRE).toLong(), SessionPaintPolicy.stackedContentPaintMask(false).toLong()
        )
    }
    @Test
    fun swappedStatusBarWifiTargetUsesTheUserSwapSetting() {
        Assert.assertTrue(
            SessionPaintPolicy.usesSwappedStatusBarWifiTarget(
                DualStyle.LAYERED, true
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.usesSwappedStatusBarWifiTarget(
                DualStyle.GROUPED, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesSwappedStatusBarWifiTarget(
                DualStyle.DUO, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.usesSwappedStatusBarWifiTarget(
                DualStyle.LAYERED, false
            )
        )
    }
    @Test
    fun rowFallbackReasonAndDiagnosticSignatureTrackStateChanges() {
        Assert.assertEquals(
            "primary-unavailable", SessionPaintPolicy.controlCenterRowFallbackReason(
                5, true, false, true, false
            )
        )
        Assert.assertEquals(
            "target-unavailable", SessionPaintPolicy.controlCenterRowFallbackReason(
                5, true, true, false, false
            )
        )
        Assert.assertEquals(
            "stacked-disabled", SessionPaintPolicy.controlCenterRowFallbackReason(
                6, false, true, true, false
            )
        )
        Assert.assertEquals(
            "none", SessionPaintPolicy.controlCenterRowFallbackReason(
                6, true, true, true, true
            )
        )
        val first = SessionPaintPolicy.controlCenterRowDiagnosticSignature(
            6, 42L, DualStyle.DUO, true, true, true, "none"
        )
        val same = SessionPaintPolicy.controlCenterRowDiagnosticSignature(
            6, 42L, DualStyle.DUO, true, true, true, "none"
        )
        val changed = SessionPaintPolicy.controlCenterRowDiagnosticSignature(
            6, 43L, DualStyle.DUO, true, true, true, "none"
        )
        Assert.assertEquals(first, same)
        Assert.assertFalse(first == changed)
    }
    @Test
    fun verticalIconCentreStaysInsideItsClip() {
        Assert.assertEquals(
            12.0f, SessionPaintPolicy.clampIconCentreY(
                5.9f, 12.0f, 0.0f, 150.0f
            ), 0.001f
        )
        Assert.assertEquals(
            138.0f, SessionPaintPolicy.clampIconCentreY(
                145.0f, 12.0f, 0.0f, 150.0f
            ), 0.001f
        )
        Assert.assertEquals(
            70.0f, SessionPaintPolicy.clampIconCentreY(
                70.0f, 12.0f, 0.0f, 150.0f
            ), 0.001f
        )
    }
    @Test
    fun stackedLabelsDoNotFallbackHideInControlCenter() {
        Assert.assertFalse(SessionPaintPolicy.fitsStackedLabels(true))
        Assert.assertTrue(SessionPaintPolicy.fitsStackedLabels(false))
    }
    @Test
    fun controlCenterKeepsStaticSignalEndpointDuringHandoff() {
        Assert.assertTrue(
            SessionPaintPolicy.keepsStaticSignalEndpoint(
                true, true
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.keepsStaticSignalEndpoint(
                true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.keepsStaticSignalEndpoint(
                false, true
            )
        )
    }
    @Test
    fun technologyTextKeepsItsOwnSmoothMotionThroughMerge() {
        Assert.assertTrue(
            SessionPaintPolicy.showsStackedTechnology(
                true, 1.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.showsStackedTechnology(
                true, 0.5f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.showsStackedTechnology(
                true, 0.0f
            )
        )
        Assert.assertTrue(
            SessionPaintPolicy.showsStackedTechnology(
                false, 0.0f
            )
        )
    }
    @Test
    fun wifiConnectedCentreTechnologyFullyHidesAtQuarterPanel() {
        Assert.assertEquals(
            0.0f, SessionPaintPolicy.centreTechnologyAlpha(
                0.25f, false, true
            ), 0.0001f
        )
        Assert.assertEquals(
            0.2593f, SessionPaintPolicy.centreTechnologyAlpha(
                0.50f, false, true
            ), 0.0002f
        )
        Assert.assertEquals(
            0.7407f, SessionPaintPolicy.centreTechnologyAlpha(
                0.75f, false, true
            ), 0.0002f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.centreTechnologyAlpha(
                0.75f, false, false
            ), 0.0001f
        )
        Assert.assertEquals(
            0.75f, SessionPaintPolicy.centreTechnologyAlpha(
                0.75f, true, true
            ), 0.0001f
        )
    }
    @Test
    fun wifiConnectedFadesEveryConvergingTechnologyLabel() {
        Assert.assertEquals(
            0.2593f, SessionPaintPolicy.stackedTechnologyAlpha(
                0.50f, false, false, true
            ), 0.0002f
        )
        Assert.assertEquals(
            0.2593f, SessionPaintPolicy.stackedTechnologyAlpha(
                0.50f, true, false, true
            ), 0.0002f
        )
        Assert.assertEquals(
            0.50f, SessionPaintPolicy.stackedTechnologyAlpha(
                0.50f, false, false, false
            ), 0.0001f
        )
        Assert.assertEquals(
            1.0f, SessionPaintPolicy.stackedTechnologyAlpha(
                0.50f, true, false, false
            ), 0.0001f
        )
    }
    @Test
    fun activeModuleSuppressesNativeStatusRowFallback() {
        Assert.assertTrue(
            SessionPaintPolicy.suppressesNativeRowDraw(
                true, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.suppressesNativeRowDraw(
                false, false
            )
        )
        Assert.assertFalse(
            SessionPaintPolicy.suppressesNativeRowDraw(
                true, true
            )
        )
    }
}
