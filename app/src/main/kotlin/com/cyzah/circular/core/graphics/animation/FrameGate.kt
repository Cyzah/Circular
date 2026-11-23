package com.cyzah.circular.core.graphics.animation

class FrameGate {
    private var hasToken = false
    private var lastToken: Long = 0

    fun run(token: Long, action: Runnable): Boolean {
        if (hasToken && token == lastToken) {
            return false
        }
        hasToken = true
        lastToken = token
        action.run()
        return true
    }
}
