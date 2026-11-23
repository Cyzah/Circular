package com.cyzah.circular.core.diagnostics

import java.util.ArrayList
import java.util.Collections
import kotlin.math.max

/**
 * Diagnostic guard for the verified SystemUI Hook installation order.
 */
class HookOrderRecorder {
    enum class Stage {
        COMPATIBILITY,
        CORE_VIEWS,
        POWER,
        PERFORMANCE,
        PRIVACY,
        MOTION,
        ALIGNMENT,
        NATIVE_SIGNAL,
        SIGNAL_RESOURCES,
        SIGNAL_SNAPSHOT,
        VIEW_SIGNAL,
        HANDOFF,
        CHARGING_ISLAND,
        KEYGUARD,
        HOME_PRIVACY,
        DEOPTIMIZE,
        COMPLETE
    }

    private var last: Stage? = null
    private var firstViolation: Stage? = null
    private var reached = 0
    private var violations = 0
    private var durationMs: Long = 0
    private val history = ArrayList<Stage?>()

    @JvmOverloads
    fun reached(stage: Stage?, elapsedMs: Long = 0L) {
        if (stage == null) {
            return
        }
        history.add(stage)
        durationMs += max(0L, elapsedMs)
        if (this.last != null && stage.ordinal < this.last!!.ordinal) {
            this.violations++
            if (this.firstViolation == null) {
                this.firstViolation = stage
            }
            return
        }
        this.last = stage
        this.reached++
    }

    fun recordDuration(elapsedMs: Long) {
        durationMs += max(0L, elapsedMs)
    }

    fun last(): Stage? {
        return this.last
    }

    fun violations(): Int {
        return this.violations
    }

    fun firstViolation(): Stage? {
        return this.firstViolation
    }

    fun durationMs(): Long {
        return this.durationMs
    }

    fun history(): MutableList<Stage?> {
        return Collections.unmodifiableList<Stage?>(ArrayList<Stage?>(history))
    }

    fun valid(): Boolean {
        return this.violations == 0
    }

    fun summary(): String {
        return ("stages=" + this.reached
                + " last=" + (if (this.last == null) "pending" else this.last!!.name)
                + " durationMs=" + this.durationMs
                + " violations=" + this.violations
                + (if (this.firstViolation == null)
            ""
        else
            " first=" + this.firstViolation!!.name))
    }
}
