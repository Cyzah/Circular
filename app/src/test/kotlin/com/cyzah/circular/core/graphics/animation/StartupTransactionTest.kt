package com.cyzah.circular.core.graphics.animation

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert
import org.junit.Test

class StartupTransactionTest {
    @Test
    fun failureRunsRollbackExactlyOnce() {
        val rollbacks = AtomicInteger()
        val transaction =
            StartupTransaction(Runnable { rollbacks.incrementAndGet() })

        transaction.begin()

        Assert.assertTrue(transaction.rollback())
        Assert.assertFalse(transaction.rollback())
        Assert.assertEquals(1, rollbacks.get().toLong())
    }

    @Test
    fun committedStartupDoesNotRollback() {
        val rollbacks = AtomicInteger()
        val transaction =
            StartupTransaction(Runnable { rollbacks.incrementAndGet() })

        transaction.begin()
        transaction.commit()

        Assert.assertFalse(transaction.rollback())
        Assert.assertEquals(0, rollbacks.get().toLong())
    }
}
