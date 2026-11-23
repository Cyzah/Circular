package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.diagnostics.FailureLedger

object HookFailureGuard {
    @JvmStatic fun attempt(
        ledger: FailureLedger?,
        area: String?,
        phase: String?,
        action: Action
    ): Boolean {
        try {
            action.run()
            return true
        } catch (error: Throwable) {
            if (error is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            if (ledger != null) {
                ledger.record(area, phase, error)
            }
            return false
        }
    }

    fun interface Action {
        @Throws(Throwable::class)
        fun run()
    }
}
