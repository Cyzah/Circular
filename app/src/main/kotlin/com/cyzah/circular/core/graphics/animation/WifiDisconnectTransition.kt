package com.cyzah.circular.core.graphics.animation

/**
 * Detects the connected-to-disconnected edge that rebuilds native Wi-Fi views.
 */
class WifiDisconnectTransition {
    private var known = false
    private var connected = false
    private var pending = false

    fun observe(nowKnown: Boolean, nowConnected: Boolean) {
        if (!nowKnown) {
            return
        }
        if (this.known && this.connected && !nowConnected) {
            this.pending = true
        }
        this.known = true
        this.connected = nowConnected
    }

    fun consume(): Boolean {
        val result = this.pending
        this.pending = false
        return result
    }

    fun reset() {
        this.known = false
        this.connected = false
        this.pending = false
    }
}
