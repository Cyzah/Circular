package com.cyzah.circular.core.graphics

import com.cyzah.circular.core.graphics.RendererContentPositionTest
import org.junit.Assert
import org.junit.Test

/** Numeric contracts used by both painted positions and their animation endpoints.  */
class RendererContentPositionTest {
    @Test
    fun duoContentCarriesItsOwnSlotOffsetAcrossTheWifiTransition() {
        Assert.assertEquals(-12.0f, duo(-12f, 20f, 0f), 0.0001f)
        Assert.assertEquals(-4.0f, duo(-12f, 20f, 0.25f), 0.0001f)
        Assert.assertEquals(20.0f, duo(-12f, 20f, 1f), 0.0001f)
        Assert.assertEquals(0.0f, duo(0f, 0f, 0.4f), 0.0f)
    }

    @Test
    fun capsuleOffsetKeepsItsPhysicalDistanceWhenTheBodyShrinks() {
        Assert.assertEquals(12.0f, local(6f, 0.5f), 0.0001f)
        Assert.assertEquals(-4.0f, local(-3f, 0.75f), 0.0001f)
        Assert.assertEquals(0.0f, local(0f, 0.5f), 0.0f)
    }

    @Test
    fun capsuleSignalGapFollowsHorizontalAndVerticalMotion() {
        Assert.assertEquals(30.0f, gap(0f, 0f), 0.0f)
        Assert.assertEquals(30.0f, gap(15f, 0f), 0.0001f)
        Assert.assertEquals(28.0f, gap(0f, 6f), 0.0001f)
        Assert.assertEquals(0.0f, gap(0f, 11f), 0.0f)
        Assert.assertEquals(30.0f, gap(100f, 0f), 0.0f)
    }

    companion object {
        private fun duo(from: Float, to: Float, progress: Float): Float {
            return call(DuoStyleRenderer::class.java, "contentOffset", from, to, progress)
        }

        private fun local(offset: Float, scale: Float): Float {
            return call(CapsuleStyleRenderer::class.java, "localOffset", offset, scale)
        }

        private fun gap(x: Float, y: Float): Float {
            return call(
                CapsuleStyleRenderer::class.java, "adaptedSignalHalfGap",
                30f, 60f, 20f, 5f, 3f, x, y, 2f
            )
        }

        private fun call(type: Class<*>, name: String, vararg arguments: Float): Float {
            try {
                val parameters = arrayOfNulls<Class<*>>(arguments.size)
                val values = arrayOfNulls<Any>(arguments.size)
                for (index in arguments.indices) {
                    parameters[index] = Float::class.javaPrimitiveType
                    values[index] = arguments[index]
                }
                val method = type.getDeclaredMethod(name, *parameters)
                method.setAccessible(true)
                return (method.invoke(null, *values) as Float)
            } catch (error: ReflectiveOperationException) {
                throw AssertionError("Missing renderer content-position contract: " + name, error)
            }
        }
    }
}
