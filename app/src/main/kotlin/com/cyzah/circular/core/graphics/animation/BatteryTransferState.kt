package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.geometry.RingTrackStyle
import kotlin.math.max

/**
 * Gesture-synchronized battery shape with a timed ring restore after closing.
 */
class BatteryTransferState(private val timeline: BatteryTransferTimeline) {
    enum class Phase {
        NORMAL_RING,
        BATTERY_SHAPE,
        WAITING_FOR_HANDOFF,
        PATH_TO_LOWER_LEFT,
        REAL_BATTERY_RESTORE
    }

    private val reopening = ReopeningRingSnapshot()
    private var phase = Phase.NORMAL_RING
    private var initialized = false
    private var observedOpen = false
    private var openingBlend = false
    private var pointProgress = 0f
    private var synchronizedExpansion = 1.0f
    private var restoreProgress = 1.0f
    private var stageStart: Long = 0

    fun update(
        control: Float,
        shade: Float,
        morph: Float,
        ready: Boolean,
        animationReady: Boolean,
        morphSettled: Boolean,
        now: Long
    ) {
        update(
            control,
            shade,
            morph,
            ready,
            animationReady,
            morphSettled,
            true,
            now
        )
    }

    @JvmOverloads
    fun update(
        control: Float,
        shade: Float,
        morph: Float,
        ready: Boolean,
        animationReady: Boolean,
        morphSettled: Boolean,
        gestureReleased: Boolean = true,
        now: Long = this.timeline.now()
    ) {
        val safeControl = Motion.clamp(control)
        val safeMorph = Motion.clamp(morph)
        this.synchronizedExpansion = max(
            safeMorph,
            Motion.ease(safeControl)
        )
        // Opening notifications never transfers ownership of the battery ring.
        val open = safeControl > BatteryTransferConfig.HANDOFF_MAX
        if (!this.initialized) {
            this.initialized = true
            this.observedOpen = open
            this.phase = if (open) Phase.BATTERY_SHAPE else Phase.NORMAL_RING
            this.restoreProgress = 1.0f
            return
        }
        if (!ready || !animationReady) {
            invalidate()
            return
        }
        this.reopening.sample(now)
        if (open && ringTransferVisible()) {
            // Preserve the last displayed ring, rather than completing the
            // interrupted trace/fill as soon as a new opening gesture begins.
            this.reopening.capture(
                ringTrackVisible(), ringTraceProgress(),
                RingTrackStyle.temporaryAlpha(ringTrackVisible(), temporaryTraceAlpha()),
                if (this.phase == Phase.REAL_BATTERY_RESTORE) this.restoreProgress else 0.0f,
                pointVisible(), pointPathProgress(), now
            )
        }
        if (!open || BatteryTransferPolicy.openSettled(safeControl, safeMorph)) {
            this.reopening.clear()
        }
        if (open && (this.phase == Phase.NORMAL_RING || ringTransferVisible())) {
            this.openingBlend = true
        }
        // Keep the opening path reversible through the outline and fill stages.
        // Hand off only at the settled full battery, including HyperOS panels
        // whose stable expansion ends slightly below 1.0.
        if (BatteryTransferPolicy.openSettled(safeControl, safeMorph)) {
            this.openingBlend = false
        }
        if (open) {
            this.observedOpen = true
            this.phase = Phase.BATTERY_SHAPE
            this.pointProgress = 0.0f
            this.restoreProgress = 1.0f
            this.stageStart = 0L
            return
        }

        if (!this.observedOpen) {
            advance(now)
            return
        }
        if (this.openingBlend) {
            if (BatteryTransferPolicy.closedSettled(safeControl, 0.0f, safeMorph)) {
                this.openingBlend = false
                this.observedOpen = false
                this.phase = Phase.NORMAL_RING
            }
            return
        }
        if (!morphSettled || !BatteryTransferPolicy.closedSettled(
                safeControl,
                0.0f,
                safeMorph
            ) || !gestureReleased
        ) {
            this.phase = Phase.WAITING_FOR_HANDOFF
            return
        }

        this.observedOpen = false
        beginPathForward(now)
        advance(now)
    }

    fun invalidate() {
        this.reopening.clear()
        this.initialized = true
        this.observedOpen = false
        this.openingBlend = false
        this.phase = Phase.NORMAL_RING
        this.pointProgress = 0.0f
        this.synchronizedExpansion = 1.0f
        this.restoreProgress = 1.0f
        this.stageStart = 0L
    }

    fun phase(): Phase {
        return this.phase
    }

    fun openingShape(): Boolean {
        return this.phase == Phase.BATTERY_SHAPE && this.openingBlend
    }

    fun reopeningSnapshot(): ReopeningRingSnapshot {
        return reopening
    }

    fun reopenAlpha(): Float {
        return reopening.alpha()
    }

    fun openingAlpha(): Float {
        return 1.0f - reopening.alpha()
    }

    fun pointPathProgress(): Float {
        return this.pointProgress
    }

    fun fillProgress(battery: Int): Float {
        val ratio = Motion.clamp(battery / 100.0f)
        if (this.phase == Phase.REAL_BATTERY_RESTORE) {
            return ratio * Motion.clamp(this.restoreProgress)
        }
        if (this.phase == Phase.WAITING_FOR_HANDOFF
            || this.phase == Phase.PATH_TO_LOWER_LEFT
        ) {
            return 0.0f
        }
        return ratio
    }

    fun temporaryTraceAlpha(): Float {
        if (this.phase == Phase.PATH_TO_LOWER_LEFT) {
            return 1.0f
        }
        if (this.phase == Phase.REAL_BATTERY_RESTORE) {
            return 1.0f - Motion.clamp(this.restoreProgress)
        }
        return 0.0f
    }

    fun ringTraceProgress(): Float {
        if (this.phase == Phase.NORMAL_RING) {
            return 0.0f
        }
        if (this.phase == Phase.REAL_BATTERY_RESTORE) {
            return 1.0f
        }
        return this.pointProgress
    }

    fun pointVisible(): Boolean {
        return this.phase == Phase.WAITING_FOR_HANDOFF
                || this.phase == Phase.PATH_TO_LOWER_LEFT
    }

    fun handoffHoldVisible(control: Float, shade: Float, morph: Float): Boolean {
        return this.phase == Phase.WAITING_FOR_HANDOFF
                && BatteryTransferPolicy.closedSettled(control, 0.0f, morph)
    }

    fun pathToLowerLeft(): Boolean {
        return this.phase == Phase.PATH_TO_LOWER_LEFT
    }

    fun restoringRealFill(): Boolean {
        return this.phase == Phase.REAL_BATTERY_RESTORE
    }

    fun batteryShapeVisible(): Boolean {
        return this.phase == Phase.BATTERY_SHAPE
                || this.phase == Phase.WAITING_FOR_HANDOFF
    }

    fun ringTransferVisible(): Boolean {
        return this.phase == Phase.PATH_TO_LOWER_LEFT
                || this.phase == Phase.REAL_BATTERY_RESTORE
    }

    fun ringTrackVisible(): Boolean {
        return this.phase == Phase.REAL_BATTERY_RESTORE
    }

    fun needsFrame(): Boolean {
        return this.reopening.alpha() > 0.0f || this.phase == Phase.PATH_TO_LOWER_LEFT || this.phase == Phase.REAL_BATTERY_RESTORE
    }

    fun batteryCollapse(control: Float, morph: Float): Float {
        if (this.phase == Phase.BATTERY_SHAPE
            && BatteryTransferPolicy.openSettled(control, morph)
        ) {
            return 0.0f
        }
        val denominator = (1.0f
                - BatteryTransferConfig.CLOSED_MORPH_MAX)
        return BatteryTransferPolicy.stagedBatteryCollapse(
            (1.0f - this.synchronizedExpansion) / denominator
        )
    }

    private fun advance(now: Long) {
        when (this.phase) {
            Phase.PATH_TO_LOWER_LEFT -> advancePathForward(now)
            Phase.REAL_BATTERY_RESTORE -> advanceRestoreForward(now)
            else -> {}
        }
    }

    private fun beginPathForward(now: Long) {
        this.phase = Phase.PATH_TO_LOWER_LEFT
        this.stageStart = now
        this.pointProgress = 0.0f
        this.restoreProgress = 0.0f
    }

    private fun beginRestoreForward(now: Long) {
        this.phase = Phase.REAL_BATTERY_RESTORE
        this.stageStart = now
        this.restoreProgress = 0.0f
    }

    private fun advancePathForward(now: Long) {
        this.pointProgress = this.timeline.progress(
            this.stageStart, BatteryTransferConfig.PATH_DURATION_MS
        )
        if (this.timeline.complete(this.stageStart, BatteryTransferConfig.PATH_DURATION_MS)) {
            this.pointProgress = 1.0f
            beginRestoreForward(now)
        }
    }

    private fun advanceRestoreForward(now: Long) {
        this.restoreProgress = this.timeline.progress(
            this.stageStart, BatteryTransferConfig.RESTORE_DURATION_MS
        )
        if (this.timeline.complete(this.stageStart, BatteryTransferConfig.RESTORE_DURATION_MS)) {
            this.restoreProgress = 1.0f
            this.phase = Phase.NORMAL_RING
        }
    }

    companion object {
        @JvmStatic fun selectHost(host6: Boolean, host5: Boolean): Int {
            if (host6) {
                return 6
            }
            return if (host5) 5 else 0
        }

        @JvmStatic fun mayDrawControlCenter(
            host: Int,
            host6: Boolean,
            host5: Boolean
        ): Boolean {
            return if (host == 6) host6 else host == 5 && host5 && !host6
        }
    }
}
