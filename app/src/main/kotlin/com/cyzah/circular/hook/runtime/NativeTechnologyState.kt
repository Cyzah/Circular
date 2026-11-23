package com.cyzah.circular.hook.runtime

import java.util.HashMap
import java.util.HashSet

/** Native reads remain fenced after a platform change until the upstream value changes.  */
class NativeTechnologyState {
    private val samples = HashMap<Int?, Sample?>()
    private var active: HashSet<Int?>? = null

    fun sample(subId: Int, label: String?) {
        if (!accepts(subId)) return
        val text = if (label == null) "" else label
        val previous = samples.get(subId)
        if (previous != null && previous.text == text) {
            // A plain getValue() does not prove an emission occurred. In particular,
            // never undo invalidation by rereading the same value.
            return
        }
        samples.put(subId, Sample(text))
    }

    fun invalidate(subId: Int) {
        val sample = samples.get(subId)
        if (sample != null) sample.valid = false
    }

    /** Binder emission/rebind supplies evidence even when the text is unchanged.  */
    fun emitted(subId: Int, text: String?) {
        if (!accepts(subId)) return
        val value = if (text == null) "" else text
        val previous = samples.get(subId)
        if (previous != null && previous.text == value) {
            previous.valid = true
            return
        }
        samples.put(subId, Sample(value))
    }

    fun label(subId: Int): String? {
        val sample = samples.get(subId)
        return if (sample == null || !sample.valid || sample.text.isEmpty()) null else sample.text
    }

    fun retain(subscriptions: IntArray) {
        active = HashSet<Int?>()
        for (id in subscriptions) active!!.add(id)
        val iterator: MutableIterator<MutableMap.MutableEntry<Int?, Sample?>?> = samples.entries.iterator()
        while (iterator.hasNext()) {
            val subId: Int = iterator.next()!!.key!!
            var present = false
            for (id in subscriptions) if (id == subId) present = true
            if (!present) iterator.remove()
        }
    }

    private fun accepts(id: Int): Boolean {
        return id >= 0 && (active == null || active!!.contains(id))
    }

    override fun toString(): String {
        return samples.toString()
    }

    private class Sample(@JvmField val text: String) {
        @JvmField var valid: Boolean = true
        override fun toString(): String {
            return text + (if (valid) "" else "(invalid)")
        }
    }
}
