package com.cyzah.circular.hook.platform

class ObserverRegistration(private val platform: Platform) {
    interface Platform {
        fun register(index: Int)
        fun unregister()
        fun failed(phase: String?, error: Throwable?)
    }

    private var owned = false
    private var complete = false

    fun start() {
        if (complete) return
        if (owned) {
            stop()
            if (owned) return
        }
        try {
            for (index in 0..4) {
                // Also cover a platform call that registers and then throws.
                owned = true
                platform.register(index)
            }
            complete = true
        } catch (error: Throwable) {
            platform.failed("data-observer-register", error)
            stop()
        }
    }

    fun stop() {
        complete = false
        if (!owned) return
        try {
            platform.unregister()
            owned = false
        } catch (error: Throwable) {
            // Keep the owner reachable so stop/start can try cleanup again.
            platform.failed("data-observer-unregister", error)
        }
    }

    fun ownsRegistration(): Boolean {
        return owned
    }
}
