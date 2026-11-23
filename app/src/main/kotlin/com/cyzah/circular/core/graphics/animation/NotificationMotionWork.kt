package com.cyzah.circular.core.graphics.animation

/** Pure notification drags leave icon geometry and radio state event-driven.  */
object NotificationMotionWork {
    @JvmStatic fun update(
        controlCallback: Boolean, control: Float, previousShade: Float,
        nextShade: Float, transferActive: Boolean
    ): Boolean {
        if (controlCallback || control > BatteryTransferConfig.HANDOFF_MAX || transferActive) return true
        val previous = Motion.clamp(previousShade)
        val next = Motion.clamp(nextShade)
        return (previous > BatteryTransferConfig.HANDOFF_MAX) != (next > BatteryTransferConfig.HANDOFF_MAX) || (previous > 0.02f) != (next > 0.02f) || (previous == 1.0f) != (next == 1.0f)
    }
}
