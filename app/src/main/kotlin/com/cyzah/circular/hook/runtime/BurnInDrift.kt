package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.domain.settings.AppearanceScale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class BurnInDrift {
    private var wasMoving = false
    private var slot = Long.MIN_VALUE
    private var phase = 8

    fun update(j: Long, z: Boolean): Boolean {
        val jMax: Long = max(0L, j) / INTERVAL_MS
        if (z || this.wasMoving) {
            this.slot = jMax
            this.wasMoving = z
            return false
        }
        if (jMax == this.slot) {
            return false
        }
        this.slot = jMax
        val length = (jMax % (X.size.toLong())).toInt()
        val z2 = if (X[length] == X[this.phase] && Y[length] == Y[this.phase]) false else true
        this.phase = length
        return z2
    }

    fun x(): Int {
        return X[this.phase]
    }

    fun y(): Int {
        return Y[this.phase]
    }

    fun phase(): Int {
        return this.phase
    }

    companion object {
        const val INTERVAL_MS: Long = 60000
        private val X = intArrayOf(-1, 0, 1, 1, 1, 0, -1, -1, 0)
        private val Y = intArrayOf(-1, -1, -1, 0, 1, 1, 1, 0, 0)
        @JvmStatic fun delay(j: Long): Long {
            return INTERVAL_MS - (max(0L, j) % INTERVAL_MS)
        }

        @JvmStatic fun roomX(f: Float, f2: Float, i: Int): Float {
            if (!java.lang.Float.isFinite(f) || !java.lang.Float.isFinite(f2) || f2 <= 0.0f || f < f2) {
                return 0.0f
            }
            return max(0.0f, (((10.0f - (AppearanceScale.ringWeight(i) * 3.75f)) * f2) / 120.0f) - 0.5f)
        }

        @JvmStatic fun roomY(f: Float, f2: Float, i: Int): Float {
            if (!java.lang.Float.isFinite(f) || !java.lang.Float.isFinite(f2) || f2 <= 0.0f || f < f2) {
                return 0.0f
            }
            val fRingWeight = AppearanceScale.ringWeight(i)
            return max(
                0.0f,
                (((f - f2) / 2.0f) + ((min(
                    2.0f,
                    min(
                        min(AppearanceScale.ringTop(i) - (3.75f * fRingWeight), 2.5f - max(0.0f, fRingWeight - 1.0f)),
                        11.0f - (fRingWeight * 4.9f)
                    )
                ) * f2) / 120.0f)) - 0.5f
            )
        }

        @JvmStatic fun offset(i: Int, f: Float, z: Boolean): Float {
            if (!java.lang.Float.isFinite(f) || f < 1.0f) {
                return 0.0f
            }
            return (max(-1, min(1, i)) * min(if (z) 2 else 1, floor(f.toDouble()).toInt())).toFloat()
        }

        @JvmStatic fun fade(f: Float): Float {
            return 1.0f - Motion.clamp(f)
        }

        @JvmStatic fun shouldSchedule(z: Boolean, z2: Boolean, z3: Boolean, z4: Boolean, f: Float): Boolean {
            return z && z2 && z3 && z4 && f == 0.0f
        }
    }
}
