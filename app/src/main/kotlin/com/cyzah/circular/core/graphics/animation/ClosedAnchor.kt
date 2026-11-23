package com.cyzah.circular.core.graphics.animation


class ClosedAnchor {
    private var applied = false
    private var originalX = 0f
    private var originalY = 0f
    private var x = 0f
    private var y = 0f

    fun move(f: Float, f2: Float, f3: Float, f4: Float) {
        if (!this.applied || f != this.x) {
            this.originalX = f
        }
        if (!this.applied || f2 != this.y) {
            this.originalY = f2
        }
        this.x = f + f3
        this.y = f2 + f4
        this.applied = true
    }

    fun release(f: Float, f2: Float) {
        var f = f
        var f2 = f2
        if (this.applied && f == this.x) {
            f = this.originalX
        }
        this.x = f
        if (this.applied && f2 == this.y) {
            f2 = this.originalY
        }
        this.y = f2
        this.applied = false
    }

    fun applied(): Boolean {
        return this.applied
    }

    fun x(): Float {
        return this.x
    }

    fun y(): Float {
        return this.y
    }

    fun nativeY(f: Float): Float {
        return if (this.applied && f == this.y) this.originalY else f
    }

    companion object {
        @JvmStatic fun offset(f: Float, f2: Float, f3: Float, f4: Float): Float {
            if (java.lang.Float.isFinite(f) && f >= 0.0f && f <= 1.0f && java.lang.Float.isFinite(f2) && java.lang.Float.isFinite(f3) && java.lang.Float.isFinite(
                    f4
                )
            ) {
                return (f2 + ((f3 - f2) * f)) - f4
            }
            return 0.0f
        }
    }
}
