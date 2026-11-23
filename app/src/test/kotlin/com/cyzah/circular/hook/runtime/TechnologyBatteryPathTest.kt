package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.TechnologyAvoidance
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.ui.preview.PreviewMorph
import org.junit.Assert
import org.junit.Test

class TechnologyBatteryPathTest {
    @Test
    fun widerAndTallerTechnologyAvoidsTheActualShrinkingShapeInBothDirections() {
        for (offset in floatArrayOf(0.0f, 62.0f)) {
            for (targetWidth in floatArrayOf(38.0f, 56.0f, 80.0f)) {
                for (above in booleanArrayOf(true, false)) {
                    val battery = BatteryShapeGeometry()
                    val shifts = FloatArray(1001)
                    for (frame in 0..1000) {
                        val p = frame / 1000.0f
                        battery.update(
                            PreviewMorph.batteryCollapse(p),
                            212.414f, -0.07425f, 85.428f, 40.552f,
                            286.573f + offset, 16.0073f, 5.4f, 5.4f
                        )
                        val size = StackedOverlayLayout.technologyProgress(p)
                        val width = Motion.mix(targetWidth, 28.0f, size)
                        val x = Motion.mix(
                            262.0f + offset - width / 2.0f,
                            63.3f, StackedOverlayLayout.technologyXProgress(p)
                        )
                        val y = Motion.mix(if (above) -8.0f else 24.0f, if (above) -5.0f else 14.0f, size)
                        val height = Motion.mix(28.0f, 16.0f, size)
                        val batteryLeft = battery.centreX - battery.width / 2.0f
                        val batteryRight = battery.centreX + battery.width / 2.0f
                        val batteryTop = battery.centreY - battery.height / 2.0f
                        val batteryBottom = battery.centreY + battery.height / 2.0f
                        val shift = TechnologyAvoidance.baselineShift(
                            x, y - height, x + width, y,
                            batteryLeft, batteryTop, batteryRight, batteryBottom, 3.0f, 28.0f, above
                        )
                        shifts[frame] = shift
                        Assert.assertTrue(x + width <= batteryLeft || x >= batteryRight || y + shift <= batteryTop || y - height + shift >= batteryBottom)
                        Assert.assertEquals(
                            shift, TechnologyAvoidance.baselineShift(
                                x, y - height, x + width, y,
                                batteryLeft, batteryTop, batteryRight, batteryBottom, 3.0f, 28.0f, above
                            ), 0.0f
                        )
                    }
                    Assert.assertEquals(0.0f, shifts[1000], 0.0f)
                }
            }
        }
    }
}
