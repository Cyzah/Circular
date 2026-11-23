package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.ControlCenterRowSnapshotTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowSnapshotTest {
    @Test
    fun midpointInterpolationUsesHalfwayRects() {
        val source =
            ControlCenterRowSnapshot()
        val target =
            ControlCenterRowSnapshot()
        val result =
            ControlCenterRowSnapshot()
        Assert.assertTrue(source.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(target.copyFrom(layout(120.0f, true), 2L))

        Assert.assertTrue(result.interpolate(source, target, 0.5f))
        Assert.assertEquals(
            110.0f,
            result.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(
            300.0f,
            result.right(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(60.0f, result.rowLeft(), EPSILON)
        Assert.assertEquals(300.0f, result.rowRight(), EPSILON)
        Assert.assertEquals(240.0f, result.rowWidth(), EPSILON)
        Assert.assertEquals(2L, result.generation())
    }

    @Test
    fun interpolationCanUseItsSourceAsDestination() {
        val source =
            ControlCenterRowSnapshot()
        val target =
            ControlCenterRowSnapshot()
        Assert.assertTrue(source.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(target.copyFrom(layout(120.0f, true), 2L))

        Assert.assertTrue(source.interpolate(source, target, 0.5f))
        Assert.assertEquals(
            110.0f,
            source.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(2L, source.generation())
    }

    @Test
    fun interpolationCanUseItsTargetAsDestination() {
        val source =
            ControlCenterRowSnapshot()
        val target =
            ControlCenterRowSnapshot()
        Assert.assertTrue(source.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(target.copyFrom(layout(120.0f, true), 2L))

        Assert.assertTrue(target.interpolate(source, target, 0.5f))
        Assert.assertEquals(
            110.0f,
            target.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(
            100.0f,
            source.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertEquals(2L, target.generation())
    }

    @Test
    fun interpolationClampsProgressToItsEndpoints() {
        val source =
            ControlCenterRowSnapshot()
        val target =
            ControlCenterRowSnapshot()
        val result =
            ControlCenterRowSnapshot()
        Assert.assertTrue(source.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(target.copyFrom(layout(120.0f, true), 2L))

        Assert.assertTrue(result.interpolate(source, target, -0.5f))
        Assert.assertEquals(
            100.0f,
            result.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
        Assert.assertTrue(result.interpolate(source, target, 1.5f))
        Assert.assertEquals(
            120.0f,
            result.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
    }

    @Test
    fun visibilityMismatchRequiresFallback() {
        val source =
            ControlCenterRowSnapshot()
        val target =
            ControlCenterRowSnapshot()
        val result =
            ControlCenterRowSnapshot()
        Assert.assertTrue(source.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(target.copyFrom(layout(100.0f, false), 2L))

        Assert.assertFalse(result.interpolate(source, target, 0.5f))
        Assert.assertFalse(result.valid())
    }

    @Test
    fun currentSnapshotCanReverseWithoutRestart() {
        val expanded =
            ControlCenterRowSnapshot()
        val collapsed =
            ControlCenterRowSnapshot()
        val current =
            ControlCenterRowSnapshot()
        val reversed =
            ControlCenterRowSnapshot()
        Assert.assertTrue(expanded.copyFrom(layout(100.0f, true), 1L))
        Assert.assertTrue(collapsed.copyFrom(layout(120.0f, true), 2L))
        Assert.assertTrue(current.interpolate(expanded, collapsed, 0.6f))
        Assert.assertTrue(reversed.interpolate(current, expanded, 0.5f))
        Assert.assertEquals(
            106.0f,
            reversed.width(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun layout(
            batteryWidth: Float,
            wifiVisible: Boolean
        ): ControlCenterRowLayout {
            val input = ControlCenterRowInput()
            input.reset()
            input.setBounds(300.0f, 600.0f)
            input.set(
                ControlCenterRowComponent.BATTERY,
                true,
                batteryWidth,
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
            input.set(
                ControlCenterRowComponent.CELLULAR,
                true,
                80.0f,
                0.0f,
                0.0f,
                0.0f,
                4.0f
            )
            val output = ControlCenterRowLayout()
            Assert.assertTrue(output.solve(input))
            return output
        }
    }
}
