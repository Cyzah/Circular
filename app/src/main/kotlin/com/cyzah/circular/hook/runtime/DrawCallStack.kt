package com.cyzah.circular.hook.runtime

import java.util.Arrays
import kotlin.math.max

class DrawCallStack(capacity: Int) {
    private var calls: Array<Any?>
    private var depth = 0

    init {
        calls = arrayOfNulls<Any>(max(2, capacity))
    }

    fun push(call: Any?) {
        if (call == null) {
            return
        }
        if (depth == calls.size) {
            calls = calls.copyOf<Any?>(calls.size * 2)
        }
        calls[depth++] = call
    }

    fun current(): Any? {
        return if (depth == 0) null else calls[depth - 1]
    }

    fun pop(): Any? {
        if (depth == 0) {
            return null
        }
        val index = --depth
        val call = calls[index]
        calls[index] = null
        return call
    }

    fun clear() {
        Arrays.fill(calls, 0, depth, null)
        depth = 0
    }

    fun depth(): Int {
        return depth
    }
}
