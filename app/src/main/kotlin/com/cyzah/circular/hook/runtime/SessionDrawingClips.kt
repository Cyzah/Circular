package com.cyzah.circular.hook.runtime

import android.view.View
import android.view.ViewGroup
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import java.util.ArrayList

/** Allows the transfer to reach the status-bar endpoint outside a panel child.  */
class SessionDrawingClips {
    private val path = ArrayList<ViewGroup?>()
    private val clips = DrawingClipLease<ViewGroup?>(
        object : DrawingClipLease.Platform<ViewGroup?> {
            override fun flags(view: ViewGroup?): Int {
                val node = view!!
                return ((if (node.getClipChildren()) 1 else 0)
                        or (if (node.getClipToPadding()) 2 else 0)
                        or (if (node.getClipToOutline()) 4 else 0))
            }

            override fun flags(view: ViewGroup?, flags: Int) {
                val node = view!!
                node.setClipChildren((flags and 1) != 0)
                node.setClipToPadding((flags and 2) != 0)
                node.setClipToOutline((flags and 4) != 0)
            }
        })

    fun update(owner: IconSession) {
        val state = owner.module.batteryTransferState()
        val expansion = owner.module.control
        val moving = expansion > BatteryTransferConfig.HANDOFF_MAX
                && expansion < 0.999f
        if (owner.closed || !owner.draw || !owner.merge || owner.ambient
            || (owner.from != 0 && owner.from != 5 && owner.from != 6)
            || owner.module.mode != 2 || !owner.module.stackedOn() || !owner.module.transferHostAttached(0) || !owner.module.transferScreenInteractive() || owner.module.transferKeyguardVisible() || owner.module.transferPrivacyBlocked()
            || (!moving && !state.needsFrame() && !state.ringTransferVisible() && !state.handoffHoldVisible(
                owner.module.control, 0.0f,
                owner.module.morphValue()
            ))
        ) {
            release(owner)
            return
        }
        val ringHost = owner.module.transferHost(0)
        val boundary: View? = if (ringHost == null) null else commonAncestor(owner.battery, ringHost.battery)
        this.path.clear()
        // The remaining native icons are individually packed during transfer.
        // Their translated children can extend outside the original icon container.
        this.path.add(owner.icons)
        var current: View? = owner.battery
        while (current != null) {
            if (current is ViewGroup) {
                this.path.add(current)
            }
            if (current === boundary) {
                break
            }
            val parent = current.getParent()
            current = if (parent is View) parent as View else null
        }
        this.clips.update(owner, this.path)
        this.path.clear()
    }

    fun release(owner: IconSession?) {
        this.clips.release(owner)
    }

    companion object {
        private fun commonAncestor(first: View, second: View): View? {
            var current = first.getParent()
            while (current is View) {
                var target = second.getParent()
                while (target is View) {
                    if (target === current) {
                        return current as View
                    }
                    target = target.getParent()
                }
                current = current.getParent()
            }
            return null
        }
    }
}
