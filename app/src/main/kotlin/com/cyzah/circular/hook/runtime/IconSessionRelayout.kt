package com.cyzah.circular.hook.runtime

import android.os.SystemClock

/** Main-thread adapter for native visibility changes and their final relayout.  */
class IconSessionRelayout(private val owner: IconSession) {
    private val scheduler: RelayoutScheduler

    init {
        this.scheduler = RelayoutScheduler(object : RelayoutScheduler.Host {
            override fun now(): Long {
                return SystemClock.uptimeMillis()
            }

            override fun post(callback: Runnable?, delayMillis: Long) {
                owner.module.main.postDelayed(callback!!, delayMillis)
            }

            override fun perform() {
                if (!owner.closed) {
                    owner.changed()
                    owner.requestLayout()
                }
            }
        })
    }

    fun request() {
        if (this.owner.closed) {
            return
        }
        var signature = 17L
        try {
            for (index in 0..<this.owner.icons.getChildCount()) {
                val child = this.owner.icons.getChildAt(index)
                val name = child.javaClass.getName()
                if (ModuleMain.Companion.WIFI == name || ModuleMain.Companion.MOBILE == name) {
                    signature = signature * 31L + System.identityHashCode(child)
                    signature = signature * 31L + (if (this.owner.wanted(child)) 1L else 0L)
                }
            }
        } catch (error: Exception) {
            // Allow one fallback layout on entry to an unreadable state. Repeated
            // failures and our own visibleState calls must not create a loop.
            signature = Long.MIN_VALUE
        }
        this.scheduler.request(signature)
    }
}
