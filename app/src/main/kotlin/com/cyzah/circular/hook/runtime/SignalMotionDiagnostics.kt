package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy

object SignalMotionDiagnostics {
    @JvmStatic fun maybeLog(
        owner: IconSession,
        morph: Float,
        xProgress: Float
    ) {
        if (owner.from != 5 && owner.from != 6) {
            return
        }
        val bucket = stageBucket(morph)
        if (owner.signalStageDiagnosticBucket == bucket) {
            return
        }
        owner.signalStageDiagnosticBucket = bucket
        val signal = SignalMotionPolicy.signalProgress(morph)
        owner.module.log(
            6,
            ModuleMain.Companion.TAG,
            ("signalMotion host=" + owner.from
                    + " style="
                    + owner.module.controlCenterSettings().dualStyle()
                    + " morph=" + morph
                    + " xProgress=" + xProgress
                    + " signal=" + signal
                    + " trim=" + SignalMotionPolicy.trimProgress(signal)
                    + " move=" + SignalMotionPolicy.moveProgress(signal)
                    + " row=" + SignalMotionPolicy.rowProgress(signal)
                    + " bars=" + SignalMotionPolicy.barProgress(signal)
                    + " labels=true")
        )
    }

    private fun stageBucket(morph: Float): Int {
        val value = Motion.clamp(morph)
        if (value >= 0.70f) {
            return 0
        }
        if (value >= 0.43f) {
            return 1
        }
        if (value >= 0.28f) {
            return 2
        }
        if (value > 0.0f) {
            return 3
        }
        return 4
    }
}
