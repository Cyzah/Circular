package com.cyzah.circular.hook.runtime

import org.junit.Assert
import org.junit.Test

class DrawCallStackTest {
    @Test
    fun nestedCallsReturnInReverseOrder() {
        val stack = DrawCallStack(4)
        val outer = Any()
        val inner = Any()

        stack.push(outer)
        stack.push(inner)

        Assert.assertSame(inner, stack.current())
        Assert.assertSame(inner, stack.pop())
        Assert.assertSame(outer, stack.pop())
    }

    @Test
    fun underflowIsBenign() {
        val stack = DrawCallStack(2)

        Assert.assertNull(stack.current())
        Assert.assertNull(stack.pop())
        Assert.assertNull(stack.pop())
        Assert.assertEquals(0, stack.depth().toLong())
    }

    @Test
    fun clearDropsAllCalls() {
        val stack = DrawCallStack(2)

        stack.push("call")
        stack.clear()

        Assert.assertEquals(0, stack.depth().toLong())
        Assert.assertNull(stack.current())
    }
}
