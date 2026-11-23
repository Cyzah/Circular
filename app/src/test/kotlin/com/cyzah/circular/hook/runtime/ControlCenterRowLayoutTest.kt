package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.ControlCenterRowLayoutTest
import org.junit.Assert
import org.junit.Test

class ControlCenterRowLayoutTest {
    @Test
    fun nullComponentsKeepTheArgumentValidationContract() {
        val input = ControlCenterRowInput()
        val methods = ControlCenterRowInput::class.java.methods.filter {
            it.parameterTypes.firstOrNull() == ControlCenterRowComponent::class.java
        }
        Assert.assertTrue(methods.isNotEmpty())
        for (method in methods) {
            val arguments = arrayOfNulls<Any>(method.parameterCount)
            for (index in 1 until arguments.size) {
                arguments[index] = if (method.parameterTypes[index] == Boolean::class.javaPrimitiveType) false else 0f
            }
            val failure = try {
                method.invoke(input, *arguments)
                throw AssertionError("Null component was accepted by " + method.name)
            } catch (error: java.lang.reflect.InvocationTargetException) {
                requireNotNull(error.cause)
            }
            Assert.assertEquals(method.name, IllegalArgumentException::class.java, failure.javaClass)
            Assert.assertEquals(method.name, "component", failure.message)
        }
    }


    @Test
    fun threeVisibleComponentsUsePhysicalMarginSums() {
        val layout = ControlCenterRowLayout()

        Assert.assertTrue(layout.solve(threeVisibleInput()))
        Assert.assertEquals(
            300.0f, layout.right(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            200.0f, layout.left(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
        Assert.assertEquals(
            194.0f, layout.right(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            154.0f, layout.left(
                ControlCenterRowComponent.WIFI
            ), EPSILON
        )
        Assert.assertEquals(
            150.0f, layout.right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(
            70.0f, layout.left(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(70.0f, layout.rowLeft(), EPSILON)
        Assert.assertEquals(230.0f, layout.rowWidth(), EPSILON)
    }

    @Test
    fun hidingWifiMergesBatteryAndCellularMargins() {
        val input: ControlCenterRowInput = threeVisibleInput()
        input.setVisible(ControlCenterRowComponent.WIFI, false)
        val layout = ControlCenterRowLayout()

        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(
            196.0f, layout.right(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertEquals(
            116.0f, layout.left(
                ControlCenterRowComponent.CELLULAR
            ), EPSILON
        )
        Assert.assertFalse(layout.visible(ControlCenterRowComponent.WIFI))
    }

    @Test
    fun twoHiddenComponentsLeaveAValidSingleComponentRow() {
        val input: ControlCenterRowInput = threeVisibleInput()
        input.setVisible(ControlCenterRowComponent.WIFI, false)
        input.setVisible(ControlCenterRowComponent.CELLULAR, false)
        val layout = ControlCenterRowLayout()

        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(100.0f, layout.rowWidth(), EPSILON)
        Assert.assertEquals(200.0f, layout.rowLeft(), EPSILON)
    }

    @Test
    fun everySingleHiddenComponentLeavesAValidRow() {
        val components = arrayOf<ControlCenterRowComponent>(
            ControlCenterRowComponent.BATTERY,
            ControlCenterRowComponent.WIFI,
            ControlCenterRowComponent.CELLULAR
        )
        for (component in components) {
            val input: ControlCenterRowInput = threeVisibleInput()
            input.setVisible(component, false)
            Assert.assertTrue(ControlCenterRowLayout().solve(input))
        }
    }

    @Test
    fun everyVisiblePairLeavesAValidRow() {
        Assert.assertTrue(
            solvePair(
                ControlCenterRowComponent.BATTERY,
                ControlCenterRowComponent.WIFI
            )
        )
        Assert.assertTrue(
            solvePair(
                ControlCenterRowComponent.BATTERY,
                ControlCenterRowComponent.CELLULAR
            )
        )
        Assert.assertTrue(
            solvePair(
                ControlCenterRowComponent.WIFI,
                ControlCenterRowComponent.CELLULAR
            )
        )
    }

    @Test
    fun allHiddenComponentsProduceNoCustomRow() {
        val input: ControlCenterRowInput = threeVisibleInput()
        input.setVisible(ControlCenterRowComponent.BATTERY, false)
        input.setVisible(ControlCenterRowComponent.WIFI, false)
        input.setVisible(ControlCenterRowComponent.CELLULAR, false)
        val layout = ControlCenterRowLayout()

        Assert.assertFalse(layout.solve(input))
        Assert.assertEquals(0.0f, layout.rowWidth(), 0.0f)
        assertAllComponentsCleared(layout)
    }

    @Test
    fun resetClearsComponentsAndSetBoundsPreservesComponentData() {
        val input: ControlCenterRowInput = threeVisibleInput()
        val layout = ControlCenterRowLayout()
        input.reset()
        input.setBounds(300.0f, 500.0f)
        Assert.assertFalse(layout.solve(input))

        input.set(
            ControlCenterRowComponent.BATTERY,
            true,
            100.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        input.setBounds(320.0f, 500.0f)
        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(
            320.0f, layout.right(
                ControlCenterRowComponent.BATTERY
            ), EPSILON
        )
    }

    @Test
    fun overflowAndNegativeMarginsAreRejected() {
        var input: ControlCenterRowInput = threeVisibleInput()
        input.setBounds(300.0f, 200.0f)
        val layout = ControlCenterRowLayout()
        Assert.assertFalse(layout.solve(input))

        input = threeVisibleInput()
        input.setMarginLeft(ControlCenterRowComponent.WIFI, -1.0f)
        Assert.assertFalse(layout.solve(input))
    }

    @Test
    fun nonFiniteAndNonPositiveWidthsAreRejected() {
        var input: ControlCenterRowInput = threeVisibleInput()
        val layout = ControlCenterRowLayout()

        input.setContentWidth(
            ControlCenterRowComponent.BATTERY,
            Float.NaN
        )
        Assert.assertFalse(layout.solve(input))
        input.setContentWidth(
            ControlCenterRowComponent.BATTERY,
            Float.POSITIVE_INFINITY
        )
        Assert.assertFalse(layout.solve(input))
        input.setContentWidth(
            ControlCenterRowComponent.BATTERY,
            0.0f
        )
        Assert.assertFalse(layout.solve(input))
        input.setContentWidth(
            ControlCenterRowComponent.BATTERY,
            -1.0f
        )
        Assert.assertFalse(layout.solve(input))

        input = threeVisibleInput()
        input.setMarginRight(
            ControlCenterRowComponent.WIFI,
            Float.NaN
        )
        Assert.assertFalse(layout.solve(input))
        input.setMarginRight(
            ControlCenterRowComponent.WIFI,
            Float.POSITIVE_INFINITY
        )
        Assert.assertFalse(layout.solve(input))

        input = threeVisibleInput()
        input.setBounds(Float.NaN, 500.0f)
        Assert.assertFalse(layout.solve(input))
        input.setBounds(300.0f, Float.POSITIVE_INFINITY)
        Assert.assertFalse(layout.solve(input))
    }

    @Test
    fun invalidSolveClearsPreviousOutputRectangles() {
        val input: ControlCenterRowInput = threeVisibleInput()
        val layout = ControlCenterRowLayout()
        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(230.0f, layout.rowWidth(), EPSILON)

        input.setMarginLeft(ControlCenterRowComponent.WIFI, -1.0f)
        Assert.assertFalse(layout.solve(input))
        Assert.assertEquals(0.0f, layout.rowWidth(), 0.0f)
        Assert.assertEquals(0.0f, layout.rowLeft(), 0.0f)
        assertAllComponentsCleared(layout)
    }

    @Test
    fun ltrAndRtlLabelsShareTheSamePhysicalOrder() {
        val ltr = ControlCenterRowLayout()
        val rtl = ControlCenterRowLayout()
        Assert.assertTrue(ltr.solve(threeVisibleInput()))
        Assert.assertTrue(rtl.solve(threeVisibleInput()))

        Assert.assertEquals(
            ltr.left(ControlCenterRowComponent.CELLULAR),
            rtl.left(ControlCenterRowComponent.CELLULAR),
            EPSILON
        )
        Assert.assertEquals(
            ltr.right(ControlCenterRowComponent.WIFI),
            rtl.right(ControlCenterRowComponent.WIFI),
            EPSILON
        )
        Assert.assertEquals(
            ltr.left(ControlCenterRowComponent.BATTERY),
            rtl.left(ControlCenterRowComponent.BATTERY),
            EPSILON
        )
    }

    @Test
    fun fractionalHostRoundingDoesNotChangeInternalGaps() {
        Assert.assertEquals(
            -0.4f,
            ControlCenterRowLayout.Companion.snapTranslation(300.4f, 300),
            EPSILON
        )

        val input: ControlCenterRowInput = threeVisibleInput()
        input.setMarginRight(ControlCenterRowComponent.WIFI, 5.5f)
        input.setMarginRight(ControlCenterRowComponent.CELLULAR, 6.5f)
        val layout = ControlCenterRowLayout()
        Assert.assertTrue(layout.solve(input))
        Assert.assertEquals(
            5.5f,
            layout.left(ControlCenterRowComponent.BATTERY)
                    - layout.right(ControlCenterRowComponent.WIFI),
            EPSILON
        )
        Assert.assertEquals(
            6.5f,
            layout.left(ControlCenterRowComponent.WIFI)
                    - layout.right(ControlCenterRowComponent.CELLULAR),
            EPSILON
        )
    }

    @Test
    fun negativeRowLeftIsRejectedEvenWhenTheParentHasRoom() {
        val input = ControlCenterRowInput()
        input.reset()
        input.setBounds(88.0f, 1000.0f)
        input.set(
            ControlCenterRowComponent.BATTERY,
            true,
            152.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        input.set(
            ControlCenterRowComponent.WIFI,
            true,
            50.0f,
            0.0f,
            0.0f,
            0.0f,
            20.0f
        )
        input.set(
            ControlCenterRowComponent.CELLULAR,
            true,
            90.0f,
            0.0f,
            0.0f,
            0.0f,
            13.0f
        )
        val layout = ControlCenterRowLayout()

        Assert.assertFalse(layout.solve(input))
    }

    @Test
    fun requiredWidthDoesNotDependOnTheCurrentHostRightEdge() {
        val input = ControlCenterRowInput()
        input.reset()
        input.setBounds(88.0f, 1000.0f)
        input.set(
            ControlCenterRowComponent.BATTERY,
            true,
            152.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        input.set(
            ControlCenterRowComponent.WIFI,
            true,
            50.0f,
            0.0f,
            0.0f,
            0.0f,
            20.0f
        )
        input.set(
            ControlCenterRowComponent.CELLULAR,
            true,
            90.0f,
            0.0f,
            0.0f,
            0.0f,
            13.0f
        )

        Assert.assertEquals(
            325.0f,
            ControlCenterRowLayout.Companion.requiredWidth(input),
            EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.001f

        private fun threeVisibleInput(): ControlCenterRowInput {
            val input = ControlCenterRowInput()
            input.reset()
            input.setBounds(300.0f, 500.0f)
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
            return input
        }

        private fun solvePair(
            first: ControlCenterRowComponent?,
            second: ControlCenterRowComponent?
        ): Boolean {
            val input: ControlCenterRowInput = threeVisibleInput()
            for (component
            in ControlCenterRowComponent.entries) {
                input.setVisible(
                    component,
                    component == first || component == second
                )
            }
            return ControlCenterRowLayout().solve(input)
        }

        private fun assertAllComponentsCleared(
            layout: ControlCenterRowLayout
        ) {
            for (component
            in ControlCenterRowComponent.entries) {
                Assert.assertFalse(layout.visible(component))
                Assert.assertEquals(0.0f, layout.left(component), 0.0f)
                Assert.assertEquals(0.0f, layout.right(component), 0.0f)
                Assert.assertEquals(0.0f, layout.occupiedWidth(component), 0.0f)
            }
        }
    }
}
