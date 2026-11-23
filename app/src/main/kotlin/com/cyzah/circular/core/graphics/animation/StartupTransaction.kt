package com.cyzah.circular.core.graphics.animation

class StartupTransaction(private val rollbackAction: Runnable) {
    private var active = false

    fun begin() {
        active = true
    }

    fun commit() {
        active = false
    }

    fun active(): Boolean {
        return active
    }

    fun rollback(): Boolean {
        if (!active) {
            return false
        }
        active = false
        rollbackAction.run()
        return true
    }
}
