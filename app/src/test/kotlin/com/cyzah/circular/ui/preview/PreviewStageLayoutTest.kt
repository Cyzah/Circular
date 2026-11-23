package com.cyzah.circular.ui.preview

import com.cyzah.circular.hook.runtime.ControlCenterRowComponent
import com.cyzah.circular.hook.runtime.ControlCenterRowHostPolicy
import com.cyzah.circular.hook.runtime.ControlCenterRowInput
import com.cyzah.circular.hook.runtime.ControlCenterRowLayout
import com.cyzah.circular.ui.preview.PreviewStageLayoutTest
import org.junit.Assert
import org.junit.Test

class PreviewStageLayoutTest {
    @Test
    fun solvedRowCentresTranslateIntoPreviewStageCoordinates() {
        val input = ControlCenterRowInput()
        input.reset()
        input.setBounds(300.0f, 600.0f)
        input.set(
            ControlCenterRowComponent.BATTERY,
            true,
            100.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        input.set(
            ControlCenterRowComponent.WIFI,
            true,
            40.0f,
            0.0f,
            0.0f,
            0.0f,
            6.0f
        )
        input.set(
            ControlCenterRowComponent.CELLULAR,
            true,
            80.0f,
            0.0f,
            0.0f,
            0.0f,
            4.0f
        )
        val layout = ControlCenterRowLayout()
        Assert.assertTrue(layout.solve(input))

        val battery = ControlCenterRowHostPolicy.centreXAfterTranslation(
            layout.left(ControlCenterRowComponent.BATTERY),
            layout.right(ControlCenterRowComponent.BATTERY),
            300.0f,
            400.0f
        )
        val wifi = ControlCenterRowHostPolicy.centreXAfterTranslation(
            layout.left(ControlCenterRowComponent.WIFI),
            layout.right(ControlCenterRowComponent.WIFI),
            300.0f,
            400.0f
        )
        val cellular = ControlCenterRowHostPolicy.centreXAfterTranslation(
            layout.left(ControlCenterRowComponent.CELLULAR),
            layout.right(ControlCenterRowComponent.CELLULAR),
            300.0f,
            400.0f
        )

        Assert.assertEquals(350.0f, battery, EPSILON)
        Assert.assertEquals(274.0f, wifi, EPSILON)
        Assert.assertEquals(210.0f, cellular, EPSILON)
        Assert.assertTrue(battery > wifi)
        Assert.assertTrue(wifi > cellular)
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
