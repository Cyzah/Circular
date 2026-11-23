package com.cyzah.circular.core.network

import kotlin.math.min

class SecondarySubscriptionBinding<C : Any>(private val platform: Platform<C>, private val scheduler: Scheduler) {
    interface Platform<C : Any> {
        fun subscription(): Int
        fun create(subscription: Int): C
        fun register(connection: C)
        fun refresh(connection: C)
        fun close(connection: C)
        fun unavailable(subscription: Int)
        fun failed(error: Throwable?)
    }

    interface Scheduler {
        fun post(task: Runnable?, delay: Long)
        fun remove(task: Runnable?)
    }

    private var connection: C? = null
    private var subscription = -1
    private var attempt = 0
    private var stopped = false
    private var pending = false
    private val retry: Runnable = object : Runnable {
        override fun run() {
            pending = false
            if (!stopped) refresh()
        }
    }

    fun refresh() {
        stopped = false
        try {
            val target = platform.subscription()
            if (target != subscription) {
                cancelRetry()
                close()
                subscription = target
                attempt = 0
            }
            if (target < 0) {
                cancelRetry()
                platform.unavailable(target)
                return
            }
            if (connection == null) {
                val created = platform.create(target)
                connection = created
                platform.register(created)
            }
            platform.refresh(checkNotNull(connection))
            cancelRetry()
            attempt = 0
        } catch (error: Throwable) {
            close()
            platform.unavailable(subscription)
            platform.failed(error)
            if (!pending && !stopped) {
                pending = true
                val delay = min(60000L, 2000L shl min(5, attempt++))
                scheduler.post(retry, delay)
            }
        }
    }

    fun stop() {
        stopped = true
        cancelRetry()
        close()
        subscription = -1
        attempt = 0
        platform.unavailable(-1)
    }

    fun matches(candidate: C?, target: Int): Boolean {
        return !stopped && connection != null && connection === candidate && subscription == target
    }

    fun connection(): C? {
        return connection
    }

    private fun cancelRetry() {
        scheduler.remove(retry)
        pending = false
    }

    private fun close() {
        val previous = connection
        connection = null
        if (previous == null) return
        try {
            platform.close(previous)
        } catch (error: Throwable) {
            platform.failed(error)
        }
    }
}
