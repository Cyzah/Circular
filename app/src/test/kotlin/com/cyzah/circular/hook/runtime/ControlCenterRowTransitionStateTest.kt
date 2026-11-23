package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.ControlCenterRowTransitionStateTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowTransitionStateTest {
    @Test
    fun sampleProducesMidpointAndUpdatesCurrent() {
        val state =
            ControlCenterRowTransitionState()
        val source: ControlCenterRowSnapshot = snapshot(100.0f, true, 1L)
        val target: ControlCenterRowSnapshot = snapshot(120.0f, true, 2L)
        val output =
            ControlCenterRowSnapshot()

        Assert.assertTrue(state.begin(source, target))
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertEquals(
            110.0f,
            output.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(
            110.0f,
            state.current().width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        state.finish()
        Assert.assertFalse(state.active())
        Assert.assertEquals(2L, state.current().generation())
    }

    @Test
    fun reverseCanStartFromCurrentInterpolatedSnapshot() {
        val state =
            ControlCenterRowTransitionState()
        val expanded: ControlCenterRowSnapshot = snapshot(100.0f, true, 1L)
        val collapsed: ControlCenterRowSnapshot = snapshot(120.0f, true, 2L)
        val output =
            ControlCenterRowSnapshot()
        Assert.assertTrue(state.begin(expanded, collapsed))
        Assert.assertTrue(state.sample(0.6f, output))

        val current: ControlCenterRowSnapshot = copyOf(state.current())
        Assert.assertTrue(state.begin(current, expanded))
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertEquals(
            106.0f,
            output.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
    }

    @Test
    fun localProgressOneReachesTheNewTargetExactly() {
        val state =
            ControlCenterRowTransitionState()
        val start: ControlCenterRowSnapshot = snapshot(110.0f, true, 4L)
        val expanded: ControlCenterRowSnapshot = snapshot(100.0f, true, 5L)
        val output =
            ControlCenterRowSnapshot()
        Assert.assertTrue(state.begin(start, expanded))
        Assert.assertTrue(state.sample(1.0f, output))
        Assert.assertEquals(
            100.0f,
            output.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(5L, output.generation())
    }

    @Test
    fun threeConsecutiveReversalsDoNotPolluteSnapshots() {
        val state =
            ControlCenterRowTransitionState()
        val expanded: ControlCenterRowSnapshot = snapshot(100.0f, true, 1L)
        val collapsed: ControlCenterRowSnapshot = snapshot(120.0f, true, 2L)
        val output =
            ControlCenterRowSnapshot()
        Assert.assertTrue(state.begin(expanded, collapsed))
        Assert.assertTrue(state.sample(0.6f, output))
        Assert.assertTrue(state.begin(copyOf(state.current()), expanded))
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertTrue(state.begin(copyOf(state.current()), collapsed))
        Assert.assertTrue(state.sample(0.25f, output))
        Assert.assertTrue(state.begin(copyOf(state.current()), expanded))
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertEquals(
            104.75f,
            output.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
    }

    @Test
    fun visibilityMismatchRejectsTransition() {
        val state =
            ControlCenterRowTransitionState()
        val visible: ControlCenterRowSnapshot = snapshot(100.0f, true, 1L)
        val hidden: ControlCenterRowSnapshot = snapshot(100.0f, false, 2L)

        Assert.assertFalse(state.begin(visible, hidden))
    }

    @Test
    fun everyComponentInterpolatesFromExpandedToStatusBarTarget() {
        val state =
            ControlCenterRowTransitionState()
        val expanded: ControlCenterRowSnapshot = literalSnapshot(
            100.0f, 220.0f, 120.0f,
            40.0f, 90.0f,
            0.0f, 40.0f,
            1L
        )
        val collapsed: ControlCenterRowSnapshot = literalSnapshot(
            180.0f, 260.0f, 80.0f,
            110.0f, 150.0f,
            70.0f, 110.0f,
            2L
        )
        val output =
            ControlCenterRowSnapshot()

        Assert.assertTrue(state.begin(expanded, collapsed))
        Assert.assertTrue(state.sample(0.0f, output))
        assertSnapshot(expanded, output)
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertEquals(
            140.0f, output.left(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            240.0f, output.right(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            100.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            75.0f, output.left(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            120.0f, output.right(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            35.0f, output.left(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(
            75.0f, output.right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertTrue(state.sample(1.0f, output))
        assertSnapshot(collapsed, output)
    }

    @Test
    fun reversalStartsAtTheCurrentLiteralRectangle() {
        val state =
            ControlCenterRowTransitionState()
        val collapsed: ControlCenterRowSnapshot = literalSnapshot(
            180.0f, 260.0f, 80.0f,
            110.0f, 150.0f,
            70.0f, 110.0f,
            1L
        )
        val expanded: ControlCenterRowSnapshot = literalSnapshot(
            100.0f, 220.0f, 120.0f,
            40.0f, 90.0f,
            0.0f, 40.0f,
            2L
        )
        val output =
            ControlCenterRowSnapshot()

        Assert.assertTrue(state.begin(expanded, collapsed))
        Assert.assertTrue(state.sample(0.6f, output))
        val current: ControlCenterRowSnapshot = copyOf(state.current())
        Assert.assertTrue(state.begin(current, expanded))
        Assert.assertTrue(state.sample(0.0f, output))
        assertSnapshot(current, output)
        Assert.assertTrue(state.sample(0.5f, output))
        Assert.assertEquals(
            124.0f, output.left(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            232.0f, output.right(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            108.0f, output.width(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            61.0f, output.left(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            108.0f, output.right(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            21.0f, output.left(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(
            61.0f, output.right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertTrue(state.sample(1.0f, output))
        assertSnapshot(expanded, output)
    }

    @Test
    fun translatedCopyMovesEveryEdgeWithoutChangingOrder() {
        val source: ControlCenterRowSnapshot = literalSnapshot(
            100.0f, 220.0f, 120.0f,
            40.0f, 90.0f,
            0.0f, 40.0f,
            3L
        )
        val translated =
            ControlCenterRowSnapshot()

        Assert.assertTrue(translated.copyTranslatedFrom(source, 12.0f))

        Assert.assertEquals(
            112.0f, translated.left(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            232.0f, translated.right(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            52.0f, translated.left(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            102.0f, translated.right(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            12.0f, translated.left(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(
            52.0f, translated.right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun snapshot(
            width: Float,
            wifiVisible: Boolean,
            generation: Long
        ): ControlCenterRowSnapshot {
            val input = ControlCenterRowInput()
            input.reset()
            input.setBounds(300.0f, 600.0f)
            input.set(
                ControlCenterRowComponent.BATTERY,
                true,
                width,
                0.0f,
                0.0f,
                0.0f,
                0.0f
            )
            input.set(
                ControlCenterRowComponent.WIFI,
                wifiVisible,
                40.0f,
                0.0f,
                0.0f,
                0.0f,
                6.0f
            )
            val layout = ControlCenterRowLayout()
            Assert.assertTrue(layout.solve(input))
            val snapshot =
                ControlCenterRowSnapshot()
            Assert.assertTrue(snapshot.copyFrom(layout, generation))
            return snapshot
        }

        private fun copyOf(
            source: ControlCenterRowSnapshot?
        ): ControlCenterRowSnapshot {
            val copy =
                ControlCenterRowSnapshot()
            Assert.assertTrue(copy.copyFrom(source))
            return copy
        }

        private fun literalSnapshot(
            batteryLeft: Float,
            batteryRight: Float,
            batteryWidth: Float,
            wifiLeft: Float,
            wifiRight: Float,
            cellularLeft: Float,
            cellularRight: Float,
            generation: Long
        ): ControlCenterRowSnapshot {
            val visible = booleanArrayOf(true, true, true)
            val left = floatArrayOf(
                batteryLeft,
                wifiLeft,
                cellularLeft
            )
            val right = floatArrayOf(
                batteryRight,
                wifiRight,
                cellularRight
            )
            val width = floatArrayOf(
                batteryWidth,
                wifiRight - wifiLeft,
                cellularRight - cellularLeft
            )
            val snapshot =
                ControlCenterRowSnapshot()
            Assert.assertTrue(
                snapshot.copyFromRects(
                    visible,
                    left,
                    right,
                    width,
                    generation
                )
            )
            return snapshot
        }

        private fun assertSnapshot(
            expected: ControlCenterRowSnapshot,
            actual: ControlCenterRowSnapshot
        ) {
            for (component
            in ControlCenterRowComponent.entries) {
                Assert.assertEquals(
                    expected.visible(component),
                    actual.visible(component)
                )
                if (!expected.visible(component)) {
                    continue
                }
                Assert.assertEquals(
                    expected.left(component),
                    actual.left(component), EPSILON
                )
                Assert.assertEquals(
                    expected.right(component),
                    actual.right(component), EPSILON
                )
                Assert.assertEquals(
                    expected.width(component),
                    actual.width(component), EPSILON
                )
            }
        }
    }
}
