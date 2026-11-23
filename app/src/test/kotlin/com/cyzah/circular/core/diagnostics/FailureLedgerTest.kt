package com.cyzah.circular.core.diagnostics

import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Test

class FailureLedgerTest {
    @Test
    fun repeatedAreaPhaseUpdatesOneEntry() {
        val ledger = FailureLedger(3)

        ledger.record("network", "receiver", IllegalStateException("first"))
        ledger.record("network", "receiver", IllegalArgumentException("second"))

        Assert.assertEquals(1, ledger.size().toLong())
        Assert.assertTrue(ledger.diagnostic().contains("second"))
    }

    @Test
    fun ledgerIsBoundedAndDropsOldestEntry() {
        val ledger = FailureLedger(2)

        ledger.record("a", "phase", RuntimeException("old"))
        ledger.record("b", "phase", RuntimeException("middle"))
        ledger.record("c", "phase", RuntimeException("new"))

        Assert.assertEquals(2, ledger.size().toLong())
        Assert.assertFalse(ledger.diagnostic().contains("old"))
        Assert.assertTrue(ledger.diagnostic().contains("new"))
    }

    @Test
    @Throws(Exception::class)
    fun concurrentWritersAndReadersStayConsistent() {
        val ledger = FailureLedger(8)
        val start = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        val threads: MutableList<Thread> = ArrayList<Thread>()

        for (writer in 0..3) {
            val writerId = writer
            threads.add(Thread(Runnable {
                try {
                    start.await()
                    for (index in 0..499) {
                        ledger.record(
                            "writer-" + writerId,
                            "phase-" + index,
                            RuntimeException("failure")
                        )
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                }
            }))
        }
        for (reader in 0..1) {
            threads.add(Thread(Runnable {
                try {
                    start.await()
                    for (index in 0..499) {
                        ledger.diagnostic()
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                }
            }))
        }

        for (thread in threads) {
            thread.start()
        }
        start.countDown()
        for (thread in threads) {
            thread.join()
        }

        Assert.assertTrue(failure.get() == null)
        Assert.assertTrue(ledger.size() <= 8)
    }
}
