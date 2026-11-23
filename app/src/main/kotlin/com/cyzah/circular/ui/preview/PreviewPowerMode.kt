package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState

/** Mutually exclusive simulated power modes; charging remains independent.  */
object PreviewPowerMode {
    @JvmStatic fun setSaver(state: IconState, enabled: Boolean) {
        state.powerSave = enabled
        if (enabled) state.performanceMode = false
    }

    @JvmStatic fun setPerformance(state: IconState, enabled: Boolean) {
        state.performanceMode = enabled
        if (enabled) state.powerSave = false
    }

    @JvmStatic fun normalize(state: IconState) {
        if (state.powerSave) state.performanceMode = false
    }
}
