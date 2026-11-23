package com.cyzah.circular.core.diagnostics

import java.util.LinkedHashMap
import kotlin.math.max

class FailureLedger(capacity: Int) {
    private val capacity: Int
    private val entries = LinkedHashMap<String?, String?>()

    init {
        this.capacity = max(1, capacity)
    }

    @Synchronized
    fun record(area: String?, phase: String?, error: Throwable?) {
        try {
            val key = area + ":" + phase
            val type = if (error == null) "unavailable" else error.javaClass.getSimpleName()
            var message = if (error == null || error.message == null)
                ""
            else
                error.message!!.replace('\n', ' ').replace('\r', ' ')
            if (message.length > 120) {
                message = message.substring(0, 120)
            }
            entries.remove(key)
            entries.put(key, type + (if (message.isEmpty()) "" else " " + message))
            while (entries.size > capacity) {
                val iterator: MutableIterator<MutableMap.MutableEntry<String?, String?>?> = entries.entries.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        } catch (ignored: RuntimeException) {
            // Diagnostics must never break the hook path.
        }
    }

    @Synchronized
    fun size(): Int {
        return entries.size
    }

    @Synchronized
    fun diagnostic(): String {
        val builder = StringBuilder()
        for (entry in entries.entries) {
            if (builder.length > 0) {
                builder.append(" | ")
            }
            builder.append(entry.key).append('=').append(entry.value)
        }
        return builder.toString()
    }
}
