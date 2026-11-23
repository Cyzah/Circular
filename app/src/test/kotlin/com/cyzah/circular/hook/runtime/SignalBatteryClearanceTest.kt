package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.DuoLayout
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.hook.runtime.SignalBatteryClearanceTest
import com.cyzah.circular.ui.preview.PreviewMorph
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

/**
 * A fixed, LTR, density-3 row with a 72px closed ring and an 81px battery frame.
 * These fixtures cover representative projected endpoints, not every OEM host
 * transform, ring weight, font measurement, or capsule layout.
 */
class SignalBatteryClearanceTest {
    @Test
    fun layeredSignalsClearTheShrinkingBatteryAtBothProjectedEndpoints() {
        assertClearance(DualStyle.LAYERED, 0.0f)
    }

    @Test
    fun groupedSignalsClearTheShrinkingBatteryAtBothProjectedEndpoints() {
        assertClearance(DualStyle.GROUPED, 0.0f)
    }

    @Test
    fun duoSignalsWithoutWifiClearBothProjectedEndpoints() {
        assertClearance(DualStyle.DUO, 0.0f)
    }

    @Test
    fun duoSignalsWithWifiClearBothProjectedEndpoints() {
        assertClearance(DualStyle.DUO, 1.0f)
    }

    companion object {
        private const val DENSITY = 3.0f
        private const val RING_UNIT = 0.6f
        private const val SIGNAL_WEIGHT = 1.2f
        private val RING_TOP = -36.0f
        private const val FULL_BATTERY_X = 212.414f
        private val FULL_BATTERY_Y = -0.07425f
        private const val FULL_BATTERY_WIDTH = 85.428f

        private fun assertClearance(style: DualStyle?, wifiProgress: Float) {
            val ring = RingGeometry()
            ring.setCircular(
                style, 10.0f, 8.0f, 110.0f, 108.0f,
                18.0f, 55.0f, 9.0f
            )
            Assert.assertTrue(ring.valid())
            val battery = BatteryShapeGeometry()
            val target = StackedSignals.Target()
            for (endpointShift in floatArrayOf(0.0f, 62.0f)) {
                val ringLeft = 226.0f + endpointShift
                val pointDiameter: Float = ring.strokeWidth * RING_UNIT
                for (placement in BatteryNumberPlacement.values()) {
                    val previousDuoX = FloatArray(StackedSignals.Companion.ELEMENTS)
                    val hasPreviousDuoX = BooleanArray(StackedSignals.Companion.ELEMENTS)
                    for (frame in 0..5000) {
                        val expansion = frame / 5000.0f
                        val signalY = SignalMotionPolicy.signalProgress(expansion)
                        val signalX = SignalMotionPolicy.signalProgress(
                            StackedOverlayLayout.signalXProgress(expansion, placement)
                        )
                        battery.update(
                            PreviewMorph.batteryCollapse(expansion),
                            FULL_BATTERY_X, FULL_BATTERY_Y,
                            FULL_BATTERY_WIDTH,
                            FULL_BATTERY_WIDTH * 94.2f / 198.44f,
                            ringLeft + ring.startX * RING_UNIT,
                            RING_TOP + ring.startY * RING_UNIT,
                            pointDiameter, pointDiameter
                        )
                        fillTarget(target, style, wifiProgress, ringLeft, signalX, signalY)
                        assertElementsClear(
                            battery, target, signalX, signalY,
                            style, wifiProgress, endpointShift, placement, expansion,
                            previousDuoX, hasPreviousDuoX
                        )
                    }
                }
            }
        }

        private fun fillTarget(
            target: StackedSignals.Target, style: DualStyle?, wifiProgress: Float,
            ringLeft: Float, signalX: Float, signalY: Float
        ) {
            for (element in 0..7) {
                val row = element / 4
                val slot = element % 4
                val x: Float
                val y: Float
                if (style == DualStyle.DUO) {
                    x = DuoLayout.dotX(slot, SIGNAL_WEIGHT)
                    y = DuoLayout.dotY(row, wifiProgress, SIGNAL_WEIGHT)
                } else if (style == DualStyle.GROUPED) {
                    val dot = row * 2 + (if (slot < 2) 0 else 1)
                    x = IconGeometry.groupedDotX(dot, signalX, SIGNAL_WEIGHT)
                    y = IconGeometry.groupedDotY(dot, SIGNAL_WEIGHT)
                } else if (row == 0) {
                    val dot = min(slot, 2)
                    x = IconGeometry.innerDotX(dot, signalX, SIGNAL_WEIGHT)
                    y = IconGeometry.innerDotY(dot, signalY, SIGNAL_WEIGHT)
                } else {
                    x = IconGeometry.outerDotX(slot, SIGNAL_WEIGHT)
                    y = IconGeometry.outerDotY(slot, SIGNAL_WEIGHT)
                }
                target.x[element] = ringLeft + x * RING_UNIT
                target.y[element] = RING_TOP + y * RING_UNIT
                target.radius[element] = IconGeometry.dotRadius(100) * RING_UNIT
            }
            SignalMotionPolicy.updateRowAnchors(target, 2)
        }

        private fun assertElementsClear(
            battery: BatteryShapeGeometry, target: StackedSignals.Target,
            signalX: Float, signalY: Float, style: DualStyle?, wifiProgress: Float,
            endpointShift: Float, placement: BatteryNumberPlacement?, expansion: Float,
            previousDuoX: FloatArray, hasPreviousDuoX: BooleanArray
        ) {
            val xTrim = SignalMotionPolicy.trimProgress(signalX)
            val yTrim = SignalMotionPolicy.trimProgress(signalY)
            val xMove = SignalMotionPolicy.moveProgress(signalX)
            val yMove = SignalMotionPolicy.moveProgress(signalY)
            val xSpread = SignalMotionPolicy.rowProgress(signalX)
            val ySpread = SignalMotionPolicy.barProgress(signalY)
            val rowTravel = SignalMotionPolicy.rowProgress(signalY)
            val batteryLeft = battery.centreX - battery.width / 2.0f
            val batteryRight = battery.centreX + battery.width / 2.0f
            val batteryTop = battery.centreY - battery.height / 2.0f
            val batteryBottom = battery.centreY + battery.height / 2.0f
            // Source cellular bounds start at x=0; barLeft=-6.5 at density=3.
            // This gives centres 14.725/27.725/40.725/53.725 and row anchor x=34.225.
            for (element in 0..7) {
                val row = element / 4
                val slot = element % 4
                val sourceHeightDp = if (row == 0) 3.25f + slot * 1.625f else 3.25f
                val sourceY = if (row == 0)
                    (1.625f - sourceHeightDp / 2.0f) * DENSITY
                else
                    14.625f
                val localTrim = SignalMotionPolicy.dotTrim(slot, yTrim)
                var x = SignalMotionPolicy.coordinate(
                    14.725f + slot * 13.0f, 34.225f,
                    target.rowAnchorX[row], target.x[element],
                    SignalMotionPolicy.dotTrim(slot, xTrim), xMove, xSpread
                )
                val y = SignalMotionPolicy.coordinate(
                    sourceY, if (row == 0) -7.3125f else 14.625f,
                    target.rowAnchorY[row], target.y[element],
                    localTrim, yMove, ySpread
                )
                val scale = SignalMotionPolicy.dotScale(slot, localTrim, rowTravel)
                val diameter = target.radius[element] * 2.0f
                val width = Motion.mix(2.816667f * DENSITY, diameter, yTrim) * scale
                val height = Motion.mix(sourceHeightDp * DENSITY, diameter, yTrim) * scale
                if (width <= 0.0f || height <= 0.0f) {
                    hasPreviousDuoX[element] = false
                    continue
                }
                x = SignalMotionPolicy.clearBatteryX(
                    x, y, width / 2, height / 2,
                    batteryLeft, batteryTop, batteryRight, batteryBottom,
                    target.x[element] <= battery.centreX,
                    style == DualStyle.DUO
                )
                if (style == DualStyle.DUO && hasPreviousDuoX[element]) {
                    Assert.assertTrue(
                        ("DUO signal moved abruptly: wifi=" + wifiProgress
                                + " shift=" + endpointShift + " placement=" + placement
                                + " expansion=" + expansion + " element=" + element
                                + " previousX=" + previousDuoX[element] + " x=" + x),
                        abs(x - previousDuoX[element]) < 1.0f
                    )
                }
                previousDuoX[element] = x
                hasPreviousDuoX[element] = true
                // Disjoint bounding boxes imply disjoint rounded glyphs, including
                // their full radii. Testing only the row anchor would miss collisions.
                val separated =
                    x + width / 2.0f <= batteryLeft || x - width / 2.0f >= batteryRight || y + height / 2.0f <= batteryTop || y - height / 2.0f >= batteryBottom
                if (!separated) {
                    throw AssertionError(
                        ("Signal intersects battery: style=" + style
                                + " wifi=" + wifiProgress + " shift=" + endpointShift
                                + " placement=" + placement + " expansion=" + expansion
                                + " element=" + element)
                    )
                }
            }
        }
    }
}
