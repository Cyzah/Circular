package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.DualStyle
import org.junit.Assert
import org.junit.Test

class PreviewRuntimeStateTest {
    @Test
    fun togglesPreviewBackgroundAndForegroundTogether() {
        val state = PreviewRuntimeState(false, 0.0f, 0.0f)
        Assert.assertEquals(-0x41000000, state.iconTint().toLong())
        Assert.assertEquals(-0x1000000, state.captionTint().toLong())

        Assert.assertTrue(state.toggleDark())
        Assert.assertTrue(state.dark())
        Assert.assertEquals(-0x19000001, state.iconTint().toLong())
        Assert.assertEquals(-0x1, state.captionTint().toLong())
        Assert.assertFalse(state.toggleDark())
        Assert.assertFalse(state.dark())
        Assert.assertEquals(-0x41000000, state.iconTint().toLong())
        Assert.assertEquals(-0x1000000, state.captionTint().toLong())
    }

    @Test
    fun animationUpdatesDoNotOverridePreviewForeground() {
        for (dark in booleanArrayOf(false, true)) {
            val state = PreviewRuntimeState(dark, 0.0f, 0.0f)
            val icon = state.iconTint()
            val caption = state.captionTint()
            state.setExpansion(0.8f)
            state.updateMotion(true, DualStyle.LAYERED, IconState(), 100L)
            Assert.assertEquals(icon.toLong(), state.iconTint().toLong())
            Assert.assertEquals(caption.toLong(), state.captionTint().toLong())
        }
    }

    @Test
    fun clampsExpansionAndDuoProgress() {
        val state = PreviewRuntimeState(true, -1.0f, 2.0f)

        Assert.assertEquals(0.0f, state.expansion(), 0.0001f)
        Assert.assertEquals(1.0f, state.duoWifiProgress(), 0.0001f)
        state.setExpansion(0.45f)
        state.setDuoWifiProgress(0.3f)
        Assert.assertEquals(0.45f, state.expansion(), 0.0001f)
        Assert.assertEquals(0.3f, state.duoWifiProgress(), 0.0001f)
    }

    @Test
    fun previewBatteryUsesTheSharedExpansionWithoutExtraLag() {
        val state = PreviewRuntimeState(true, 0.6f, 0.0f)

        Assert.assertEquals(
            PreviewMorph.batteryCollapse(state.expansion()),
            state.batteryCollapse(),
            0.0001f
        )
    }

    @Test
    fun duoWifiUsesTheDeviceCurveAndRespondsToStyleAndSimChanges() {
        val runtime = PreviewRuntimeState(false, 0.0f, 0.0f)
        val scene = IconState()
        scene.simPresent = true
        scene.secondarySimPresent = true
        scene.wifiKnown = true
        runtime.updateMotion(false, DualStyle.DUO, scene, 0L)
        scene.wifiEnabled = true
        scene.wifiConnected = true
        runtime.updateMotion(false, DualStyle.DUO, scene, 100L)
        runtime.updateMotion(false, DualStyle.DUO, scene, 165L)
        Assert.assertEquals(0.15625f, runtime.duoWifiProgress(), 0.0001f)
        runtime.updateMotion(false, DualStyle.DUO, scene, 360L)
        Assert.assertEquals(1.0f, runtime.duoWifiProgress(), 0.0001f)
        scene.secondarySimPresent = false
        runtime.updateMotion(false, DualStyle.DUO, scene, 400L)
        runtime.updateMotion(false, DualStyle.DUO, scene, 530L)
        Assert.assertEquals(0.5f, runtime.duoWifiProgress(), 0.0001f)
        runtime.updateMotion(false, DualStyle.LAYERED, scene, 660L)
        Assert.assertEquals(0.0f, runtime.duoWifiProgress(), 0.0001f)
    }
}
