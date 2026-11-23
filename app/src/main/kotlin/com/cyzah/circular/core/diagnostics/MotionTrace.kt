package com.cyzah.circular.core.diagnostics

import java.util.Arrays
import kotlin.math.min

class MotionTrace @JvmOverloads constructor(i: Int = 20) {
    private var count = 0
    private val frames: Array<String?>
    private var next = 0

    init {
        require(!(i < 1 || i > 32)) { "capacity" }
        this.frames = arrayOfNulls<String>(i)
    }

    fun clear() {
        Arrays.fill(this.frames, null as Any?)
        this.count = 0
        this.next = 0
    }

    fun add(str: String) {
        if (this.count <= 0 || str != this.frames[((this.next + this.frames.size) - 1) % this.frames.size]) {
            this.frames[this.next] = str
            this.next = (this.next + 1) % this.frames.size
            this.count = min(this.count + 1, this.frames.size)
        }
    }

    fun size(): Int {
        return this.count
    }

    fun text(): String {
        val sb = StringBuilder()
        for (i in 0..<this.count) {
            if (i > 0) {
                sb.append('\n')
            }
            sb.append(this.frames[(((this.next - this.count) + i) + this.frames.size) % this.frames.size])
        }
        return sb.toString()
    }
}
