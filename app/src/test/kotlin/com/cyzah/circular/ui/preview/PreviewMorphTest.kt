package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.DualStyle
import org.junit.Assert
import org.junit.Test

class PreviewMorphTest {
    @Test
    fun previewUsesTheSameNativeMorphAsDeviceHosts() {
        Assert.assertEquals(
            0.42f,
            PreviewMorph.morphProgress(0.42f),
            0.0001f
        )
        Assert.assertEquals(
            PanelMotionFrame.nativeAlpha(0.42f),
            PreviewMorph.positionProgress(0.42f),
            0.0001f
        )
    }

    @Test
    fun duoWifiRemainsVisibleWhileDisconnectTransitionRuns() {
        Assert.assertTrue(
            PreviewMorph.shouldDrawWifiOverlay(
                DualStyle.DUO,
                false,
                0.45f,
                0.7f
            )
        )
    }

    @Test
    fun duoWifiIsHiddenAfterDisconnectTransitionFinishes() {
        Assert.assertFalse(
            PreviewMorph.shouldDrawWifiOverlay(
                DualStyle.DUO,
                false,
                0.0f,
                0.7f
            )
        )
    }

    @Test
    fun nonDuoStylesDoNotRetainDisconnectedWifiFromDuoProgress() {
        Assert.assertFalse(
            PreviewMorph.shouldDrawWifiOverlay(
                DualStyle.CAPSULE,
                false,
                0.45f,
                0.7f
            )
        )
    }

    @Test
    fun duoWifiOverlayStartsWithLiveExpansion() {
        Assert.assertTrue(
            PreviewMorph.shouldDrawWifiOverlay(
                DualStyle.DUO,
                true,
                1.0f,
                0.7f
            )
        )
        Assert.assertTrue(
            PreviewMorph.shouldDrawWifiOverlay(
                DualStyle.DUO,
                true,
                1.0f,
                0.1f
            )
        )
    }

    @Test
    fun previewBatteryCollapseUsesTheRuntimeHorizontalStage() {
        Assert.assertEquals(
            0.0f,
            PreviewMorph.batteryCollapse(1.0f),
            0.0001f
        )
        Assert.assertEquals(
            0.5731463f,
            PreviewMorph.batteryCollapse(0.208f),
            0.001f
        )
        Assert.assertEquals(
            1.0f,
            PreviewMorph.batteryCollapse(0.0f),
            0.0001f
        )
    }

    @Test
    fun previewBatteryCollapseMatchesTheDeviceAcrossTheCloseTimeline() {
        Assert.assertEquals(0.1130734f, PreviewMorph.batteryCollapse(0.75f), 0.0001f)
        Assert.assertEquals(0.3618348f, PreviewMorph.batteryCollapse(0.5f), 0.0001f)
        Assert.assertEquals(0.0f, PreviewMorph.batteryCollapse(0.96f), 0.0001f)
    }

    @Test
    fun stackedTechnologyRemainsVisibleWithOrWithoutWifi() {
        Assert.assertTrue(PreviewMorph.shouldShowStackedTechnology(true))
        Assert.assertTrue(PreviewMorph.shouldShowStackedTechnology(false))
    }

    @Test
    fun stackedSceneRequiresEnabledSettingsAndReadyNetworkIncludingSingleSim() {
        val scene = IconState()
        scene.battery = 91
        scene.wifiKnown = true
        scene.mobileKnown = true
        scene.simPresent = true
        scene.inService = true
        scene.mobileLevel = 4
        Assert.assertTrue(PreviewMorph.stackedConfigured(true, false, 2, scene))
        Assert.assertFalse(PreviewMorph.stackedConfigured(false, false, 2, scene))
        Assert.assertFalse(PreviewMorph.stackedConfigured(true, true, 2, scene))
        Assert.assertFalse(PreviewMorph.stackedConfigured(true, false, 0, scene))
        scene.airplane = true
        Assert.assertFalse(PreviewMorph.stackedConfigured(true, false, 2, scene))
    }
}
