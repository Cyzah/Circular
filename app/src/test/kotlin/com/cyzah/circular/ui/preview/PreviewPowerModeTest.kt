package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class PreviewPowerModeTest {
    @Test
    fun enablingEitherModeTurnsOffThePreviouslySelectedMode() {
        val state = IconState()
        state.charging = true
        PreviewPowerMode.setPerformance(state, true)
        Assert.assertTrue(state.performanceMode)
        Assert.assertFalse(state.powerSave)
        PreviewPowerMode.setSaver(state, true)
        Assert.assertTrue(state.powerSave)
        Assert.assertFalse(state.performanceMode)
        PreviewPowerMode.setPerformance(state, true)
        Assert.assertFalse(state.powerSave)
        Assert.assertTrue(state.performanceMode)
        Assert.assertTrue(state.charging)
    }

    @Test
    fun disablingAModeNeverEnablesTheOtherOne() {
        val state = IconState()
        PreviewPowerMode.setSaver(state, true)
        PreviewPowerMode.setSaver(state, false)
        Assert.assertFalse(state.powerSave || state.performanceMode)
        PreviewPowerMode.setPerformance(state, true)
        PreviewPowerMode.setPerformance(state, false)
        Assert.assertFalse(state.powerSave || state.performanceMode)
    }

    @Test
    fun restoredConflictingModesKeepSaverWithoutChangingCharging() {
        val state = IconState()
        state.charging = true
        state.performanceMode = state.charging
        state.powerSave = state.performanceMode
        PreviewPowerMode.normalize(state)
        Assert.assertTrue(state.powerSave)
        Assert.assertFalse(state.performanceMode)
        Assert.assertTrue(state.charging)
    }

    @Test
    fun bothSceneFormatsNormalizeOldConflictingPowerModes() {
        for (scene in arrayOf<String>(
            "2;91,1,1,1,1,3,1,4,2,1,1,1,1,0,5G,500,1",
            "91,1,1,1,1,0,3,4,2,1,1,1,1,0,5G,500,1"
        )) {
            val restored = IconState()
            Assert.assertNotNull(PreviewSceneCodec.decode(scene, restored))
            Assert.assertTrue(restored.powerSave)
            Assert.assertFalse(restored.performanceMode)
            Assert.assertTrue(restored.charging)
        }
    }

    @Test
    fun sceneEncodingCannotSaveTwoEnabledPowerModes() {
        val source = IconState()
        source.performanceMode = true
        source.powerSave = source.performanceMode
        val encoded = PreviewSceneCodec.encode(source, 0f, false)
        val fields: Array<String?> = encoded.substring(2).split(",".toRegex()).toTypedArray()
        Assert.assertEquals("1", fields[2])
        Assert.assertEquals("0", fields[3])
        Assert.assertTrue(source.powerSave && source.performanceMode)
    }
}
