package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.NativeIconVisibilityPolicyTest
import org.junit.Assert
import org.junit.Test

class NativeIconVisibilityPolicyTest {
    @Test
    fun followsSiblingFadeWithoutMultiplyingSharedParentAgain() {
        val shared: Node = node(null, 0.2f)
        Assert.assertEquals(0.25f, alpha(node(shared, 1.0f), node(shared, 0.25f)), 0.0001f)
    }

    @Test
    fun accountsForBothUniqueBranchesBeforeTheirCommonAncestor() {
        val shared: Node = node(null, 0.2f)
        val battery: Node = node(node(shared, 0.5f), 0.8f)
        val icons: Node = node(node(shared, 0.4f), 0.25f)
        Assert.assertEquals(0.25f, alpha(battery, icons), 0.0001f)
    }

    @Test
    fun sharedVisibilityIsAlreadyAppliedByAndroid() {
        val shared: Node = node(null, 0.0f)
        shared.visible = false
        Assert.assertEquals(1.0f, alpha(node(shared, 1.0f), node(shared, 1.0f)), 0.0f)
    }

    @Test
    fun invisibleIconBranchHidesTheWholeReplacement() {
        val shared: Node = node(null, 1.0f)
        val iconParent: Node = node(shared, 1.0f)
        iconParent.visible = false
        Assert.assertEquals(0.0f, alpha(node(shared, 1.0f), node(iconParent, 1.0f)), 0.0f)
    }

    @Test
    fun transparentIconBranchHidesTheWholeReplacement() {
        val shared: Node = node(null, 1.0f)
        Assert.assertEquals(0.0f, alpha(node(shared, 1.0f), node(shared, 0.0f)), 0.0f)
    }

    @Test
    fun hiddenBatteryCannotBeRecoveredWithAnAdditionalCanvasLayer() {
        val shared: Node = node(null, 1.0f)
        val battery: Node = node(shared, 0.0f)
        val icons: Node = node(shared, 1.0f)
        Assert.assertEquals(0.0f, alpha(battery, icons), 0.0f)
        battery.alpha = 1.0f
        battery.visible = false
        Assert.assertEquals(0.0f, alpha(battery, icons), 0.0f)
    }

    @Test
    fun brighterReferenceNeverRequestsGainAboveOne() {
        Assert.assertEquals(1.0f, NativeIconVisibilityPolicy.gain(0.25f, 0.75f), 0.0f)
        Assert.assertEquals(0.5f, NativeIconVisibilityPolicy.gain(0.8f, 0.4f), 0.0f)
    }

    @Test
    fun unrelatedTreesDoNotSuppressTheReplacement() {
        Assert.assertEquals(1.0f, alpha(node(null, 0.4f), node(null, 0.0f)), 0.0f)
    }

    @Test
    fun missingOrIdenticalSourcesNeedNoAdditionalOpacity() {
        val view: Node = node(null, 0.4f)
        Assert.assertEquals(1.0f, alpha(null, view), 0.0f)
        Assert.assertEquals(1.0f, alpha(view, null), 0.0f)
        Assert.assertEquals(1.0f, alpha(view, view), 0.0f)
    }

    @Test
    fun reparentingIsSampledOnTheNextFrame() {
        val shared: Node = node(null, 1.0f)
        val battery: Node = node(shared, 1.0f)
        val icons: Node = node(shared, 0.3f)
        Assert.assertEquals(0.3f, alpha(battery, icons), 0.0f)
        icons.parent = node(null, 1.0f)
        Assert.assertEquals(1.0f, alpha(battery, icons), 0.0f)
    }

    @Test
    fun malformedOrUnavailableAlphaPreservesNativeRendering() {
        val shared: Node = node(null, 1.0f)
        val battery: Node = node(shared, 1.0f)
        val icons: Node = node(shared, Float.NaN)
        Assert.assertEquals(1.0f, alpha(battery, icons), 0.0f)
        icons.alpha = Float.POSITIVE_INFINITY
        Assert.assertEquals(1.0f, alpha(battery, icons), 0.0f)
        icons.failed = true
        Assert.assertEquals(1.0f, alpha(battery, icons), 0.0f)
    }

    @Test
    fun cyclesAndUnusuallyDeepTreesFallBackWithoutHanging() {
        val battery: Node = node(null, 1.0f)
        battery.parent = battery
        Assert.assertEquals(1.0f, alpha(battery, node(null, 0.0f)), 0.0f)
        val shared: Node = node(null, 1.0f)
        var deep = shared
        for (index in 0..39) deep = node(deep, 1.0f)
        Assert.assertEquals(1.0f, alpha(deep, node(shared, 0.0f)), 0.0f)
    }

    private class Node {
        @JvmField var parent: Node? = null
        @JvmField var alpha: Float = 0f
        @JvmField var visible: Boolean = true
        @JvmField var failed: Boolean = false
    }

    companion object {
        private val NODES: NativeIconVisibilityPolicy.Nodes<Node?> = object : NativeIconVisibilityPolicy.Nodes<Node?> {
            override fun parent(node: Node?): Node? {
                return node!!.parent
            }

            override fun alpha(node: Node?): Float {
                check(!node!!.failed) { "unavailable native view" }
                return node.alpha
            }

            override fun visible(node: Node?): Boolean {
                return node!!.visible
            }
        }

        private fun alpha(battery: Node?, icons: Node?): Float {
            return NativeIconVisibilityPolicy.alpha<Node?>(battery, icons, NODES)
        }

        private fun node(parent: Node?, alpha: Float): Node {
            val node = Node()
            node.parent = parent
            node.alpha = alpha
            return node
        }
    }
}
