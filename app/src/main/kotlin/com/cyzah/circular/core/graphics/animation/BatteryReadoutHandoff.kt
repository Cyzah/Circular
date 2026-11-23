package com.cyzah.circular.core.graphics.animation

/** Closing readout motion reaches its destination before native host handoff.  */
class BatteryReadoutHandoff {
    @JvmField var move: Float = 0f
    @JvmField var sourceAlpha: Float = 0f
    @JvmField var targetAlpha: Float = 0f

    fun update(expansion: Float, sourceVisible: Boolean, targetVisible: Boolean) {
        val fraction = Motion.ease((1.0f - Motion.clamp(expansion)) / 0.95f)
        move = if (sourceVisible && targetVisible) fraction else 1.0f
        sourceAlpha = if (sourceVisible) 1.0f - fraction else 0.0f
        targetAlpha = if (targetVisible) fraction else 0.0f
    }
}
