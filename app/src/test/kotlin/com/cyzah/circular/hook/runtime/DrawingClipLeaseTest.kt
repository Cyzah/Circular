package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.DrawingClipLeaseTest
import java.util.Arrays
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class DrawingClipLeaseTest {
    private class Node(@JvmField var flags: Int) {
        @JvmField var writes: Int = 0
    }

    @Test
    fun projectedRingCanCrossHostBoundsUntilTransferEnds() {
        val lease: DrawingClipLease<Node?> = lease()
        val parent = Node(7)
        val owner = Any()
        lease.update(owner, mutableListOf<Node?>(parent))
        // A 288px projected endpoint cannot survive a 230px child clip.
        val paintedRight = if ((parent.flags and 1) != 0) min(288.0f, 230.0f) else 288.0f
        Assert.assertEquals(288.0f, paintedRight, 0.0f)
        Assert.assertEquals(0, parent.flags.toLong())
        lease.release(owner)
        Assert.assertEquals(7, parent.flags.toLong())
    }

    @Test
    fun sharedAncestorStaysOpenUntilBothHostsReleaseIt() {
        val lease: DrawingClipLease<Node?> = lease()
        val shared = Node(3)
        val first = Any()
        val second = Any()
        lease.update(first, mutableListOf<Node?>(shared))
        lease.update(second, mutableListOf<Node?>(shared))
        lease.release(first)
        Assert.assertEquals(0, shared.flags.toLong())
        lease.release(second)
        Assert.assertEquals(3, shared.flags.toLong())
    }

    @Test
    fun repeatedFramesDoNotOverwriteTheOriginalFlagsOrRepeatMutations() {
        val lease: DrawingClipLease<Node?> = lease()
        val child = Node(1)
        val parent = Node(4)
        val owner = Any()
        lease.update(owner, Arrays.asList<Node?>(child, parent))
        lease.update(owner, Arrays.asList<Node?>(child, parent))
        Assert.assertEquals(1, child.writes.toLong())
        Assert.assertEquals(1, parent.writes.toLong())
        lease.release(owner)
        Assert.assertEquals(1, child.flags.toLong())
        Assert.assertEquals(4, parent.flags.toLong())
    }

    @Test
    fun reparentingAndDetachingRestoreOnlyReleasedNodes() {
        val lease: DrawingClipLease<Node?> = lease()
        val oldParent = Node(3)
        val newParent = Node(1)
        val owner = Any()
        lease.update(owner, mutableListOf<Node?>(oldParent))
        lease.update(owner, mutableListOf<Node?>(newParent))
        Assert.assertEquals(3, oldParent.flags.toLong())
        Assert.assertEquals(0, newParent.flags.toLong())
        lease.update(owner, mutableListOf<Node?>())
        Assert.assertEquals(1, newParent.flags.toLong())
        lease.release(owner)
        Assert.assertEquals(2, newParent.writes.toLong())
    }

    companion object {
        private fun lease(): DrawingClipLease<Node?> {
            return DrawingClipLease<Node?>(object : DrawingClipLease.Platform<Node?> {
                override fun flags(node: Node?): Int {
                    return node!!.flags
                }

                override fun flags(node: Node?, flags: Int) {
                    node!!.flags = flags
                    node.writes++
                }
            })
        }
    }
}
