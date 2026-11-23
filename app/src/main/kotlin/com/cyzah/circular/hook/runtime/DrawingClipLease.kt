package com.cyzah.circular.hook.runtime

import java.util.ArrayList
import java.util.IdentityHashMap

/** Restorable drawing bounds shared by animation owners.  */
class DrawingClipLease<T>(private val platform: Platform<T?>) {
    interface Platform<T> {
        fun flags(node: T?): Int
        fun flags(node: T?, flags: Int)
    }

    private class Saved(@JvmField val flags: Int) {
        @JvmField var owners: Int = 0
    }

    private val saved: MutableMap<T?, Saved?> = IdentityHashMap<T?, Saved?>()
    private val owners: MutableMap<Any?, MutableList<T?>?> = IdentityHashMap<Any?, MutableList<T?>?>()

    fun update(owner: Any?, nodes: MutableList<T?>) {
        var held = this.owners.get(owner)
        if (held == null) {
            held = ArrayList<T?>()
            this.owners.put(owner, held)
        }
        for (index in held.indices.reversed()) {
            val node = held.get(index)
            if (!containsIdentity<T?>(nodes, node)) {
                drop(node)
                held.removeAt(index)
            }
        }
        for (node in nodes) {
            if (containsIdentity<T?>(held, node)) {
                continue
            }
            var entry = this.saved.get(node)
            if (entry == null) {
                entry = Saved(this.platform.flags(node))
                this.saved.put(node, entry)
                if (entry.flags != 0) {
                    this.platform.flags(node, 0)
                }
            }
            entry.owners++
            held.add(node)
        }
        if (held.isEmpty()) {
            this.owners.remove(owner)
        }
    }

    fun release(owner: Any?) {
        val held = this.owners.remove(owner)
        if (held != null) {
            for (node in held) {
                drop(node)
            }
        }
    }

    private fun drop(node: T?) {
        val entry = this.saved.get(node)
        if (entry != null && --entry.owners == 0) {
            this.saved.remove(node)
            if (entry.flags != 0) {
                this.platform.flags(node, entry.flags)
            }
        }
    }

    companion object {
        private fun <T> containsIdentity(nodes: MutableList<T?>, target: T?): Boolean {
            for (node in nodes) {
                if (node === target) {
                    return true
                }
            }
            return false
        }
    }
}
