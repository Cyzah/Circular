package com.cyzah.circular.hook.runtime

import android.os.SystemClock
import kotlin.math.ceil
import kotlin.math.max

/**
 * Per-session owner for stable row layout and transition snapshots.
 */
class ControlCenterRowCoordinator(measurementSource: MeasurementSource) {
    interface MeasurementSource {
        fun measure(output: ControlCenterRowInput): Boolean
    }

    private class SessionMeasurementSource
        (private val owner: IconSession) : MeasurementSource {
        private val measurer = ControlCenterRowMeasurer()

        override fun measure(output: ControlCenterRowInput): Boolean {
            val right = this.owner.battery.getWidth().toFloat()
            val available = if (this.owner.parentWidthLimit > 0)
                this.owner.parentWidthLimit
                    .toFloat()
            else
                right
            if (!this.measurer.measure(
                    this.owner,
                    output,
                    right,
                    available
                )
            ) {
                return false
            }
            val required: Float =
                ControlCenterRowLayout.Companion.requiredWidth(output)
            val batteryWidth =
                (output.contentWidth(
                    ControlCenterRowComponent.BATTERY
                )
                        + output.paddingLeft(
                    ControlCenterRowComponent.BATTERY
                )
                        + output.paddingRight(
                    ControlCenterRowComponent.BATTERY
                ))
            val hostWidth = ControlCenterRowHostPolicy.hostWidthPx(
                required,
                batteryWidth
            )
            val measurementRight = ControlCenterRowHostPolicy.measurementRight(
                right, hostWidth, available
            )
            if (measurementRight == right) {
                return java.lang.Float.isFinite(required)
            }
            return this.measurer.measure(
                this.owner,
                output,
                measurementRight,
                available
            )
        }
    }

    private val measurementSource: MeasurementSource
    private var owner: IconSession? = null
    private val reflow = ControlCenterRowReflow()
    private val reflowDraw = ControlCenterRowSnapshot()
    private val transitionSource = ControlCenterRowSnapshot()
    private val transitionTarget = ControlCenterRowSnapshot()
    private var transitionHostWidth = 0
    private val reflowFrame: Runnable = object : Runnable {
        override fun run() {
            if (owner == null || owner!!.closed) {
                reflow.clear()
                return
            }
            advanceReflow(SystemClock.uptimeMillis())
            for (session in owner!!.module.batteries.values) {
                if (!session.closed && (session.from == 5 || session.from == 6)) {
                    session.requestLayout()
                    session.battery.invalidate()
                }
            }
            scheduleReflow()
        }
    }
    private val input = ControlCenterRowInput()
    private val layout = ControlCenterRowLayout()

    // Expanded measurement geometry must survive a compact animation endpoint.
    private val stable = ControlCenterRowSnapshot()
    private val settled = ControlCenterRowSnapshot()
    private val transition = ControlCenterRowTransitionState()
    private var lastDiagnosticSignature = Long.MIN_VALUE

    constructor(owner: IconSession) : this(SessionMeasurementSource(owner)) {
        this.owner = owner
    }

    init {
        requireNotNull(measurementSource) { "measurementSource" }
        this.measurementSource = measurementSource
    }

    @JvmOverloads
    fun updateStable(
        generation: Long,
        now: Long = if (owner == null) 0L else SystemClock.uptimeMillis(),
        animate: Boolean = owner != null && owner!!.module.control >= 0.999f && owner!!.module.stackedOn() && owner!!.merge && !owner!!.ambient
    ): Boolean {
        if (this.reflow.active() && !animate) {
            advanceReflow(now)
            return this.stable.valid()
        }
        if (this.stable.valid()
            && this.stable.generation() == generation
        ) {
            return true
        }
        if (!this.measurementSource.measure(this.input) || !this.layout.solve(this.input) || !this.stable.copyFrom(
                this.layout,
                generation
            )
        ) {
            this.stable.clear()
            this.settled.clear()
            this.transition.clear()
            this.reflow.clear()
            this.transitionHostWidth = 0
            return false
        }
        this.settled.clear()
        this.transition.clear()
        this.transitionHostWidth = 0
        this.reflow.update(this.stable, now, animate)
        advanceReflow(now)
        scheduleReflow()
        return true
    }

    @JvmOverloads
    fun beginTransition(
        target: ControlCenterRowSnapshot?,
        generation: Long,
        hostRight: Float = if (this.owner == null) Float.NaN else this.owner!!.battery.getWidth()
            .toFloat()
    ): Boolean {
        var target = target
        if (target == null || !target.valid() || target.generation() != generation) {
            this.transition.clear()
            this.settled.clear()
            this.transitionHostWidth = 0
            return false
        }
        var source = currentSnapshot()
        var hostWidth = measuredHostWidthPx()
        if (java.lang.Float.isFinite(hostRight)) {
            hostWidth = max(hostWidth, ceil(hostRight.toDouble()).toInt())
            if (target.generation() == this.stable.generation()
                && this.transitionTarget.copyTranslatedFrom(
                    target, hostRight - target.rowRight()
                )
            ) {
                target = this.transitionTarget
            }
        }
        if (usesMeasuredGeometry() && java.lang.Float.isFinite(hostRight)
            && this.transitionSource.copyTranslatedFrom(
                source, hostRight - source.rowRight()
            )
        ) {
            source = this.transitionSource
        }
        if (!source.valid()
            || !this.transition.begin(source, target)
        ) {
            this.transition.clear()
            this.settled.clear()
            this.transitionHostWidth = 0
            return false
        }
        this.transitionHostWidth = hostWidth
        this.reflow.clear()
        scheduleReflow()
        return true
    }

    fun sampleTransition(
        progress: Float,
        output: ControlCenterRowSnapshot?
    ): Boolean {
        return this.transition.sample(progress, output)
    }

    fun finishTransition() {
        val wasActive = this.transition.active()
        if (!wasActive) {
            this.transition.clear()
            return
        }
        this.transition.finish()
        if (!this.settled.copyFrom(this.transition.current())) {
            this.settled.clear()
        }
        this.transition.clear()
    }

    fun restoreMeasuredLayout() {
        this.settled.clear()
        this.transition.clear()
        this.transitionHostWidth = 0
    }

    fun measuredHostWidthPx(): Int {
        return if (this.stable.valid()
            && this.stable.visible(ControlCenterRowComponent.BATTERY)
        ) max(
            this.transitionHostWidth, ceil(
                (if (this.reflow.active())
                    this.reflow.snapshot().rowWidth()
                else
                    this.stable.rowWidth()).toDouble()
            ).toInt()
        ) else
            0
    }

    fun advanceReflow(now: Long) {
        this.reflow.sample(now)
        this.reflowDraw.copyTranslatedFrom(this.reflow.snapshot(), this.stable.rowRight())
    }

    private fun scheduleReflow() {
        if (this.owner == null) {
            return
        }
        this.owner!!.battery.removeCallbacks(this.reflowFrame)
        if (!this.owner!!.closed && this.reflow.active()) {
            this.owner!!.battery.postOnAnimation(this.reflowFrame)
        }
    }

    fun invalidate() {
        this.input.reset()
        this.input.setBounds(0.0f, 0.0f)
        this.layout.solve(this.input)
        this.stable.clear()
        this.settled.clear()
        this.transition.clear()
        this.reflow.clear()
        this.reflowDraw.clear()
        this.transitionHostWidth = 0
        scheduleReflow()
    }

    fun transitioning(): Boolean {
        return this.transition.active()
    }

    fun usesMeasuredGeometry(): Boolean {
        return !this.transition.active() && !this.settled.valid()
    }

    fun claimDiagnostic(signature: Long): Boolean {
        if (signature == this.lastDiagnosticSignature) {
            return false
        }
        this.lastDiagnosticSignature = signature
        return true
    }

    fun stableSnapshot(): ControlCenterRowSnapshot {
        return this.stable
    }

    fun currentSnapshot(): ControlCenterRowSnapshot {
        return if (this.transition.active())
            this.transition.current()
        else
            if (this.settled.valid())
                this.settled
            else
                if (this.reflow.active()) this.reflowDraw else this.stable
    }
}
