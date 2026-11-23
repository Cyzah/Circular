package com.cyzah.circular.core.graphics

/** Keeps presentation ownership independent of transient network readiness.  */
object PresentationPolicy {
    @JvmStatic fun ownsMerged(eligible: Boolean, batteryKnown: Boolean, mode: Int, networkReady: Boolean): Boolean {
        // Unknown signal levels can be drawn as unknown; they must not return
        // ownership to the stock battery while native callbacks are binding.
        return eligible && batteryKnown && mode == 2
    }

    @JvmStatic fun brightKeyguard(interactive: Boolean, displayOn: Boolean, keyguardState: Boolean): Boolean {
        return (interactive || displayOn) && keyguardState
    }
}
