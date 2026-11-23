package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.diagnostics.FailureLedger
import org.junit.Assert
import org.junit.Test

class HookFailureGuardTest {
    @Test
    fun failedActionIsRecordedAndContained() {
        val ledger = FailureLedger(4)

        val completed = HookFailureGuard.attempt(
            ledger,
            "network",
            "read",
            HookFailureGuard.Action {
                throw IllegalStateException("denied")
            })

        Assert.assertFalse(completed)
        Assert.assertTrue(
            ledger.diagnostic().contains(
                "network:read=IllegalStateException denied"
            )
        )
    }

    @Test
    fun successfulActionReturnsTrue() {
        val ledger = FailureLedger(4)

        val completed = HookFailureGuard.attempt(
            ledger,
            "hook",
            "install",
            HookFailureGuard.Action {})

        Assert.assertTrue(completed)
        Assert.assertTrue(ledger.diagnostic().isEmpty())
    }

    @Test
    fun interruptionPreservesThreadInterruptStatus() {
        val ledger = FailureLedger(4)

        HookFailureGuard.attempt(
            ledger,
            "config",
            "wait",
            HookFailureGuard.Action {
                throw InterruptedException("stopped")
            })

        Assert.assertTrue(Thread.currentThread().isInterrupted())
        Thread.interrupted()
    }
}
