package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.abs

class ControlCenterRowReflow {
    private val frame = ControlCenterRowSnapshot()
    private val source = ControlCenterRowSnapshot()
    private val target = ControlCenterRowSnapshot()
    private val next = ControlCenterRowSnapshot()
    private val visible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val left = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val right = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val width = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private var startedAt: Long = 0
    private var running = false

    fun update(measured: ControlCenterRowSnapshot?, now: Long, animate: Boolean) {
        if (measured == null || !measured.valid() || !this.next.copyTranslatedFrom(measured, -measured.rowRight())) {
            clear()
            return
        }
        sample(now)
        if (!animate || !this.frame.valid()) {
            this.target.copyFrom(this.next)
            this.frame.copyFrom(this.next)
            this.running = false
            return
        }
        if (sameGeometry(this.target, this.next)) {
            this.target.copyFrom(this.next)
            if (!this.running) {
                this.frame.copyFrom(this.target)
            }
            return
        }
        this.source.copyFrom(this.frame)
        this.target.copyFrom(this.next)
        this.startedAt = now
        this.running = true
        sample(now)
    }

    fun sample(now: Long) {
        if (!this.running) {
            return
        }
        val raw = Motion.clamp((now - this.startedAt) / DURATION_MS.toFloat())
        if (raw >= 1.0f) {
            this.frame.copyFrom(this.target)
            this.running = false
            return
        }
        val amount = Motion.ease(raw)
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            // Remove disconnected content immediately. Newly connected content
            // appears only after its space is free, so it cannot cover a neighbour.
            this.visible[index] = this.source.visible(index) && this.target.visible(index)
            this.left[index] = Motion.mix(this.source.left(index), this.target.left(index), amount)
            this.right[index] = Motion.mix(this.source.right(index), this.target.right(index), amount)
            this.width[index] = this.right[index] - this.left[index]
        }
        val rowWidth = Motion.mix(this.source.rowWidth(), this.target.rowWidth(), amount)
        if (!this.frame.copyFromRects(
                this.visible, this.left, this.right, this.width,
                this.target.generation(), -rowWidth, 0.0f
            )
        ) {
            clear()
        }
    }

    fun snapshot(): ControlCenterRowSnapshot {
        return this.frame
    }

    fun active(): Boolean {
        return this.running
    }

    fun clear() {
        this.frame.clear()
        this.source.clear()
        this.target.clear()
        this.next.clear()
        this.running = false
    }

    companion object {
        const val DURATION_MS: Long = 220L
        private fun sameGeometry(
            first: ControlCenterRowSnapshot,
            second: ControlCenterRowSnapshot
        ): Boolean {
            if (!first.valid() || !second.valid() || abs(first.rowWidth() - second.rowWidth()) > 0.001f) {
                return false
            }
            for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
                if (first.visible(index) != second.visible(index)
                    || (first.visible(index)
                            && (abs(first.left(index) - second.left(index)) > 0.001f
                            || abs(first.right(index) - second.right(index)) > 0.001f))
                ) {
                    return false
                }
            }
            return true
        }
    }
}
