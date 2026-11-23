package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ControlCenterRowSnapshot {
    private val visible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val left = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val right = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val width = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)

    private val scratchVisible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchLeft = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchRight = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchWidth = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)

    private var valid = false
    private var generation: Long = 0
    private var rowLeft = 0f
    private var rowRight = 0f
    private var rowWidth = 0f
    private var scratchRowLeft = 0f
    private var scratchRowRight = 0f
    private var scratchRowWidth = 0f

    fun copyFrom(
        layout: ControlCenterRowLayout?,
        generation: Long
    ): Boolean {
        clear()
        if (layout == null || !layout.valid()) {
            return false
        }
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            val component: ControlCenterRowComponent =
                ControlCenterRowInput.Companion.component(index)
            this.visible[index] = layout.visible(component)
            this.left[index] = layout.left(component)
            this.right[index] = layout.right(component)
            this.width[index] = layout.occupiedWidth(component)
        }
        this.rowLeft = layout.rowLeft()
        this.rowRight = layout.rowRight()
        this.rowWidth = layout.rowWidth()
        this.generation = generation
        this.valid = true
        return true
    }

    fun copyFrom(source: ControlCenterRowSnapshot?): Boolean {
        if (source == this) {
            return this.valid
        }
        clear()
        if (source == null || !source.valid) {
            return false
        }
        System.arraycopy(
            source.visible,
            0,
            this.visible,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            source.left,
            0,
            this.left,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            source.right,
            0,
            this.right,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            source.width,
            0,
            this.width,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        this.generation = source.generation
        this.rowLeft = source.rowLeft
        this.rowRight = source.rowRight
        this.rowWidth = source.rowWidth
        this.valid = true
        return true
    }

    fun copyFromRects(
        sourceVisible: BooleanArray?,
        sourceLeft: FloatArray?,
        sourceRight: FloatArray?,
        sourceWidth: FloatArray?,
        sourceGeneration: Long
    ): Boolean {
        clear()
        if (sourceVisible == null || sourceLeft == null || sourceRight == null || sourceWidth == null || (sourceVisible.size
                    < ControlCenterRowInput.Companion.COMPONENT_COUNT) || (sourceLeft.size
                    < ControlCenterRowInput.Companion.COMPONENT_COUNT) || (sourceRight.size
                    < ControlCenterRowInput.Companion.COMPONENT_COUNT) || (sourceWidth.size
                    < ControlCenterRowInput.Companion.COMPONENT_COUNT)
        ) {
            return false
        }
        var minimumLeft = Float.POSITIVE_INFINITY
        var maximumRight = Float.NEGATIVE_INFINITY
        var anyVisible = false
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.scratchVisible[index] = sourceVisible[index]
            if (!sourceVisible[index]) {
                this.scratchLeft[index] = 0.0f
                this.scratchRight[index] = 0.0f
                this.scratchWidth[index] = 0.0f
                continue
            }
            val componentLeft = sourceLeft[index]
            val componentRight = sourceRight[index]
            val componentWidth = sourceWidth[index]
            if (!java.lang.Float.isFinite(componentLeft) || !java.lang.Float.isFinite(componentRight) || !java.lang.Float.isFinite(componentWidth) || componentWidth <= 0.0f || componentRight <= componentLeft || abs(
                    (componentRight - componentLeft
                            - componentWidth)
                ) > 0.001f
            ) {
                clear()
                return false
            }
            this.scratchLeft[index] = componentLeft
            this.scratchRight[index] = componentRight
            this.scratchWidth[index] = componentWidth
            minimumLeft = min(minimumLeft, componentLeft)
            maximumRight = max(maximumRight, componentRight)
            anyVisible = true
        }
        if (!anyVisible || !java.lang.Float.isFinite(minimumLeft) || !java.lang.Float.isFinite(maximumRight) || maximumRight <= minimumLeft) {
            clear()
            return false
        }
        publishScratch(sourceGeneration, minimumLeft, maximumRight)
        return true
    }

    fun copyFromRects(
        sourceVisible: BooleanArray?, sourceLeft: FloatArray?, sourceRight: FloatArray?,
        sourceWidth: FloatArray?, sourceGeneration: Long,
        reservedLeft: Float, reservedRight: Float
    ): Boolean {
        if (!java.lang.Float.isFinite(reservedLeft) || !java.lang.Float.isFinite(reservedRight) || reservedRight <= reservedLeft || !copyFromRects(
                sourceVisible, sourceLeft, sourceRight,
                sourceWidth, sourceGeneration
            ) || this.rowLeft < reservedLeft - 0.001f || this.rowRight > reservedRight + 0.001f
        ) {
            clear()
            return false
        }
        this.rowLeft = reservedLeft
        this.rowRight = reservedRight
        this.rowWidth = reservedRight - reservedLeft
        return true
    }

    fun copyTranslatedFrom(
        source: ControlCenterRowSnapshot?,
        translationX: Float
    ): Boolean {
        if (source == null || !source.valid || !java.lang.Float.isFinite(translationX)) {
            clear()
            return false
        }
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.scratchVisible[index] = source.visible[index]
            if (!source.visible[index]) {
                this.scratchLeft[index] = 0.0f
                this.scratchRight[index] = 0.0f
                this.scratchWidth[index] = 0.0f
                continue
            }
            this.scratchLeft[index] =
                source.left[index] + translationX
            this.scratchRight[index] =
                source.right[index] + translationX
            this.scratchWidth[index] = source.width[index]
        }
        publishScratch(
            source.generation,
            source.rowLeft + translationX,
            source.rowRight + translationX
        )
        return true
    }

    fun interpolate(
        source: ControlCenterRowSnapshot?,
        target: ControlCenterRowSnapshot?,
        progress: Float
    ): Boolean {
        if (source == null || target == null || !source.valid || !target.valid || !java.lang.Float.isFinite(progress)) {
            clear()
            return false
        }
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            if (source.visible[index] != target.visible[index]) {
                clear()
                return false
            }
        }

        val amount = Motion.clamp(progress)
        val nextGeneration = target.generation
        this.scratchRowLeft =
            mix(source.rowLeft, target.rowLeft, amount)
        this.scratchRowRight =
            mix(source.rowRight, target.rowRight, amount)
        this.scratchRowWidth =
            mix(source.rowWidth, target.rowWidth, amount)
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.scratchVisible[index] = source.visible[index]
            if (!source.visible[index]) {
                this.scratchLeft[index] = 0.0f
                this.scratchRight[index] = 0.0f
                this.scratchWidth[index] = 0.0f
                continue
            }
            this.scratchLeft[index] =
                mix(source.left[index], target.left[index], amount)
            this.scratchRight[index] =
                mix(source.right[index], target.right[index], amount)
            this.scratchWidth[index] =
                mix(source.width[index], target.width[index], amount)
        }

        System.arraycopy(
            this.scratchVisible,
            0,
            this.visible,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchLeft,
            0,
            this.left,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchRight,
            0,
            this.right,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchWidth,
            0,
            this.width,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        this.rowLeft = this.scratchRowLeft
        this.rowRight = this.scratchRowRight
        this.rowWidth = this.scratchRowWidth
        this.generation = nextGeneration
        this.valid = true
        return true
    }

    fun valid(): Boolean {
        return this.valid
    }

    fun visible(component: ControlCenterRowComponent): Boolean {
        return this.visible[index(component)]
    }

    fun visible(component: Int): Boolean {
        return visible(ControlCenterRowInput.Companion.component(component))
    }

    fun left(component: ControlCenterRowComponent): Float {
        return this.left[index(component)]
    }

    fun left(component: Int): Float {
        return left(ControlCenterRowInput.Companion.component(component))
    }

    fun right(component: ControlCenterRowComponent): Float {
        return this.right[index(component)]
    }

    fun right(component: Int): Float {
        return right(ControlCenterRowInput.Companion.component(component))
    }

    fun width(component: ControlCenterRowComponent): Float {
        return this.width[index(component)]
    }

    fun width(component: Int): Float {
        return width(ControlCenterRowInput.Companion.component(component))
    }

    fun rowLeft(): Float {
        return this.rowLeft
    }

    fun rowRight(): Float {
        return this.rowRight
    }

    fun rowWidth(): Float {
        return this.rowWidth
    }

    fun generation(): Long {
        return this.generation
    }

    fun clear() {
        this.valid = false
        this.generation = 0L
        this.rowLeft = 0.0f
        this.rowRight = 0.0f
        this.rowWidth = 0.0f
        this.scratchRowLeft = 0.0f
        this.scratchRowRight = 0.0f
        this.scratchRowWidth = 0.0f
        for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.visible[index] = false
            this.left[index] = 0.0f
            this.right[index] = 0.0f
            this.width[index] = 0.0f
            this.scratchVisible[index] = false
            this.scratchLeft[index] = 0.0f
            this.scratchRight[index] = 0.0f
            this.scratchWidth[index] = 0.0f
        }
    }

    private fun publishScratch(
        sourceGeneration: Long,
        nextRowLeft: Float,
        nextRowRight: Float
    ) {
        System.arraycopy(
            this.scratchVisible,
            0,
            this.visible,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchLeft,
            0,
            this.left,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchRight,
            0,
            this.right,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        System.arraycopy(
            this.scratchWidth,
            0,
            this.width,
            0,
            ControlCenterRowInput.Companion.COMPONENT_COUNT
        )
        this.rowLeft = nextRowLeft
        this.rowRight = nextRowRight
        this.rowWidth = nextRowRight - nextRowLeft
        this.generation = sourceGeneration
        this.valid = true
    }

    companion object {
        private fun index(component: ControlCenterRowComponent): Int {
            requireNotNull(component) { "component" }
            return component.ordinal
        }

        private fun mix(from: Float, to: Float, amount: Float): Float {
            return from + ((to - from) * amount)
        }
    }
}
