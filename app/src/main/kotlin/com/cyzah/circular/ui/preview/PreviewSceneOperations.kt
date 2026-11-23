package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState

object PreviewSceneOperations {
    @JvmStatic fun setDual(state: IconState, dual: Boolean) {
        state.simPresent = true
        state.mobileKnown = true
        state.inService = true
        if (state.mobileLevel < 0) {
            state.mobileLevel = 4
        }
        if (dual) {
            state.secondarySimPresent = true
            state.secondaryKnown = true
            state.secondaryInService = true
            if (state.secondaryLevel < 0) {
                state.secondaryLevel = 4
            }
            return
        }
        state.secondarySimPresent = false
        state.secondaryKnown = false
        state.secondaryInService = false
        state.secondaryLevel = -1
    }

    @JvmStatic fun setCard1NoService(state: IconState, noService: Boolean) {
        state.secondarySimPresent = true
        state.secondaryKnown = true
        if (state.secondaryLevel < 0) {
            state.secondaryLevel = 4
        }
        state.secondaryInService = !noService
    }

    @JvmStatic fun setCard2NoService(state: IconState, noService: Boolean) {
        state.simPresent = true
        state.mobileKnown = true
        state.inService = !noService
    }

    @JvmStatic fun setWifiEnabled(state: IconState, enabled: Boolean) {
        state.wifiEnabled = enabled
        state.wifiConnected = PreviewWifiPolicy.connected(enabled)
    }
}
