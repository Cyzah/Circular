package com.cyzah.circular.core.graphics.animation

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable

class FrameGateTest {
    @Test
    fun sameFrameRunsOnce() {
        val gate = FrameGate()
        val runs = AtomicInteger()

        Assert.assertTrue(gate.run(10L, Runnable { runs.incrementAndGet() }))
        Assert.assertFalse(gate.run(10L, Runnable { runs.incrementAndGet() }))
        Assert.assertEquals(1, runs.get().toLong())
    }

    @Test
    fun nextFrameRunsAgain() {
        val gate = FrameGate()
        val runs = AtomicInteger()

        Assert.assertTrue(gate.run(10L, Runnable { runs.incrementAndGet() }))
        Assert.assertTrue(gate.run(11L, Runnable { runs.incrementAndGet() }))
        Assert.assertEquals(2, runs.get().toLong())
    }

    @Test
    fun exceptionDoesNotPoisonNextFrame() {
        val gate = FrameGate()
        Assert.assertThrows<IllegalStateException?>(
            IllegalStateException::class.java,
            ThrowingRunnable {
                gate.run(10L, Runnable {
                    throw IllegalStateException()
                })
            })
        Assert.assertTrue(gate.run(11L, Runnable {}))
    }
}
