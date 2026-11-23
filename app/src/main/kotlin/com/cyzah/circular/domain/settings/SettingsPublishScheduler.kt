package com.cyzah.circular.domain.settings

/** Main-thread publication pacing. Persistence belongs to the caller and is never deferred. */
class SettingsPublishScheduler(
    private val scheduler: Scheduler,
    private val publish: Runnable,
    private val intervalMillis: Long,
) {
    interface Scheduler {
        fun nowMillis(): Long
        fun post(task: Runnable, delayMillis: Long)
        fun remove(task: Runnable)
    }

    init {
        require(intervalMillis > 0) { "Publication interval must be positive" }
    }

    private var lastPublication = 0L
    private var continuous = false
    private var pending = false
    private var scheduled = false
    private var closed = false
    private val trailing = Runnable {
        scheduled = false
        if (!closed && pending) emit()
    }

    fun changedContinuously() {
        if (closed) return
        val first = !continuous
        continuous = true
        pending = true
        val elapsed = scheduler.nowMillis() - lastPublication
        if (first || elapsed >= intervalMillis) {
            emit()
        } else if (!scheduled) {
            scheduled = true
            scheduler.post(trailing, intervalMillis - maxOf(0L, elapsed))
        }
    }

    fun publishNow() {
        if (closed) return
        continuous = false
        emit()
    }

    private fun emit() {
        cancelPending()
        lastPublication = scheduler.nowMillis()
        publish.run()
    }

    fun flush() {
        continuous = false
        if (!closed && pending) emit()
    }

    fun close() {
        if (closed) return
        closed = true
        continuous = false
        cancelPending()
    }

    private fun cancelPending() {
        if (scheduled) scheduler.remove(trailing)
        scheduled = false
        pending = false
    }
}
