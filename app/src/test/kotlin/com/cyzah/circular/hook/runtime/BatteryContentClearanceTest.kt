package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.ui.preview.PreviewMorph
import org.junit.Assert
import org.junit.Test

/** Measured-size fixtures, with the panel and status-bar endpoints in one canvas.  */
class BatteryContentClearanceTest {
    @Test
    fun technologyWaitsOutsideTheShrinkingBatteryUntilThereIsRoom() {
        for (placement in BatteryNumberPlacement.values()) {
            for (statusOffset in floatArrayOf(0.0f, 62.0f)) {
                val ring = RingGeometry()
                ring.setCircular(DualStyle.GROUPED, 10f, 8f, 110f, 108f, 18f, 55f, 9f)
                val ringLeft = 226.0f + statusOffset
                val battery = BatteryShapeGeometry()
                for (frame in 0..1000) {
                    val expansion = 1.0f - frame / 1000.0f
                    battery.update(
                        PreviewMorph.batteryCollapse(expansion),
                        212.414f, -0.07425f, 85.428f, 40.552f,
                        ringLeft + ring.startX * 0.6f, -36.0f + ring.startY * 0.6f,
                        5.4f, 5.4f
                    )
                    val textX = StackedOverlayLayout.technologyXProgress(expansion)
                    val textSize = StackedOverlayLayout.technologyProgress(expansion)
                    // 28px expanded label, 38px merged label; normal font measurement
                    // scales linearly with the same text size used by drawLabels.
                    val textWidth = Motion.mix(38.0f, 28.0f, textSize)
                    val labelLeft = Motion.mix(
                        ringLeft + 36.0f - textWidth / 2.0f,
                        63.3f, textX
                    )
                    val clearance = (battery.centreX - battery.width / 2.0f - (labelLeft + textWidth))
                    Assert.assertTrue(
                        "Text reaches the battery at " + expansion + "/" + placement,
                        clearance >= 2.0f
                    )
                }
            }
        }
    }
}
