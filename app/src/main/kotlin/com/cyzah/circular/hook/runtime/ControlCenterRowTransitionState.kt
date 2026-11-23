package com.cyzah.circular.hook.runtime

/**
 * Owns source/current/target snapshots for one row transition.
 */
class ControlCenterRowTransitionState {
    private val source = ControlCenterRowSnapshot()
    private val target = ControlCenterRowSnapshot()
    private val current = ControlCenterRowSnapshot()
    private var active = false

    fun begin(
        newSource: ControlCenterRowSnapshot?,
        newTarget: ControlCenterRowSnapshot?
    ): Boolean {
        if (!this.source.copyFrom(newSource) || !this.target.copyFrom(newTarget) || visibilityDiffers()) {
            clear()
            return false
        }
        if (!this.current.copyFrom(this.source)) {
            clear()
            return false
        }
        this.active = true
        return true
    }

    fun sample(
        progress: Float,
        output: ControlCenterRowSnapshot?
    ): Boolean {
        if (!this.active || output == null || !this.current.interpolate(
                this.source,
                this.target,
                progress
            )
        ) {
            return false
        }
        return output.copyFrom(this.current)
    }

    fun finish() {
        if (this.active) {
            this.current.copyFrom(this.target)
        }
        this.active = false
    }

    fun clear() {
        this.active = false
        this.source.clear()
        this.target.clear()
        this.current.clear()
    }

    fun active(): Boolean {
        return this.active && this.current.valid()
    }

    fun current(): ControlCenterRowSnapshot {
        return this.current
    }

    private fun visibilityDiffers(): Boolean {
        for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            val id: ControlCenterRowComponent =
                ControlCenterRowInput.Companion.component(component)
            if (this.source.visible(id) != this.target.visible(id)) {
                return true
            }
        }
        return false
    }
}
