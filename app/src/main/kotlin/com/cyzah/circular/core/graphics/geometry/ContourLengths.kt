package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.max
import kotlin.math.min

/**
 * Allocation-free mapping from a global path progress to disconnected contours.
 */
class ContourLengths {
    private var lengths = FloatArray(4)
    private var count = 0
    private var total = 0f

    fun reset() {
        this.count = 0
        this.total = 0.0f
    }

    fun add(length: Float) {
        if (!java.lang.Float.isFinite(length)
            || length <= 0.0f
        ) {
            return
        }
        if (this.count == this.lengths.size) {
            this.lengths = this.lengths.copyOf(this.lengths.size * 2)
        }
        this.lengths[this.count++] = length
        this.total += length
    }

    fun total(): Float {
        return this.total
    }

    fun count(): Int {
        return this.count
    }

    fun start(contour: Int): Float {
        var start = 0.0f
        var index = 0
        while (index < contour && index < this.count) {
            start += this.lengths[index]
            index++
        }
        return start
    }

    fun length(contour: Int): Float {
        return if (contour >= 0 && contour < this.count)
            this.lengths[contour]
        else
            0.0f
    }

    fun contourAt(progress: Float): Int {
        if (this.count == 0 || this.total <= 0.0f) {
            return -1
        }
        val distance = Motion.clamp(progress) * this.total
        var start = 0.0f
        for (index in 0..<this.count) {
            val end = start + this.lengths[index]
            if (distance <= end) {
                return index
            }
            start = end
        }
        return this.count - 1
    }

    fun localDistance(contour: Int, progress: Float): Float {
        if (contour < 0 || contour >= this.count || this.total <= 0.0f) {
            return 0.0f
        }
        val distance = (Motion.clamp(progress) * this.total
                - start(contour))
        return max(
            0.0f,
            min(this.lengths[contour], distance)
        )
    }
}
