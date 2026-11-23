package com.cyzah.circular.hook.runtime


class ControlCenterRowLayout {
    private val visible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val left = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val right = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val occupiedWidth = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)

    private val scratchLeft = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchRight = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchOccupiedWidth = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val scratchVisible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)

    private var valid = false
    private var rowLeft = 0f
    private var rowRight = 0f
    private var rowWidth = 0f

    fun solve(input: ControlCenterRowInput?): Boolean {
        clearOutput()
        if (!validate(input)) {
            return false
        }

        for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            val id: ControlCenterRowComponent =
                ControlCenterRowInput.Companion.component(component)
            scratchVisible[component] = input!!.visible(id)
            scratchOccupiedWidth[component] =
                (input.contentWidth(id)
                        + input.paddingLeft(id)
                        + input.paddingRight(id))
        }

        var previousVisible = -1
        var visibleCount = 0
        var lastLeft = 0.0f
        var lastVisible = -1
        for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            if (!scratchVisible[component]) {
                continue
            }
            val id: ControlCenterRowComponent =
                ControlCenterRowInput.Companion.component(component)
            val componentRight: Float
            if (previousVisible < 0) {
                componentRight =
                    input!!.rowRight() - input.marginRight(id)
            } else {
                val previous: ControlCenterRowComponent =
                    ControlCenterRowInput.Companion.component(previousVisible)
                componentRight =
                    (scratchLeft[previousVisible]
                            - input!!.marginLeft(previous)
                            - input.marginRight(id))
            }
            val componentLeft =
                componentRight - scratchOccupiedWidth[component]
            if (!java.lang.Float.isFinite(componentRight) || !java.lang.Float.isFinite(componentLeft) || (previousVisible >= 0
                        && (componentRight
                        > scratchLeft[previousVisible] + EPSILON))
            ) {
                clearOutput()
                return false
            }
            scratchRight[component] = componentRight
            scratchLeft[component] = componentLeft
            previousVisible = component
            lastVisible = component
            lastLeft = componentLeft
            visibleCount++
        }

        if (visibleCount <= 0 || lastVisible < 0) {
            return false
        }
        val last: ControlCenterRowComponent =
            ControlCenterRowInput.Companion.component(lastVisible)
        val solvedLeft = lastLeft - input!!.marginLeft(last)
        val solvedWidth = input.rowRight() - solvedLeft
        if (!java.lang.Float.isFinite(solvedLeft) || !java.lang.Float.isFinite(solvedWidth) || solvedLeft < -EPSILON || solvedWidth <= 0.0f || solvedWidth > input.availableWidth() + EPSILON) {
            clearOutput()
            return false
        }

        this.rowLeft = solvedLeft
        this.rowRight = input.rowRight()
        this.rowWidth = solvedWidth
        for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.visible[component] = scratchVisible[component]
            this.left[component] = scratchLeft[component]
            this.right[component] = scratchRight[component]
            this.occupiedWidth[component] =
                scratchOccupiedWidth[component]
        }
        this.valid = true
        return true
    }

    fun valid(): Boolean {
        return this.valid
    }

    fun visible(component: ControlCenterRowComponent): Boolean {
        return this.visible[component.ordinal]
    }

    fun visible(component: Int): Boolean {
        return visible(ControlCenterRowInput.Companion.component(component))
    }

    fun left(component: ControlCenterRowComponent): Float {
        return this.left[component.ordinal]
    }

    fun left(component: Int): Float {
        return left(ControlCenterRowInput.Companion.component(component))
    }

    fun right(component: ControlCenterRowComponent): Float {
        return this.right[component.ordinal]
    }

    fun right(component: Int): Float {
        return right(ControlCenterRowInput.Companion.component(component))
    }

    fun occupiedWidth(component: ControlCenterRowComponent): Float {
        return this.occupiedWidth[component.ordinal]
    }

    fun occupiedWidth(component: Int): Float {
        return occupiedWidth(ControlCenterRowInput.Companion.component(component))
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

    private fun clearOutput() {
        this.valid = false
        this.rowLeft = 0.0f
        this.rowRight = 0.0f
        this.rowWidth = 0.0f
        for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
            this.visible[component] = false
            this.left[component] = 0.0f
            this.right[component] = 0.0f
            this.occupiedWidth[component] = 0.0f
            this.scratchVisible[component] = false
            this.scratchLeft[component] = 0.0f
            this.scratchRight[component] = 0.0f
            this.scratchOccupiedWidth[component] = 0.0f
        }
    }

    companion object {
        private const val EPSILON = 0.001f

        @JvmStatic fun requiredWidth(input: ControlCenterRowInput?): Float {
            if (input == null) {
                return Float.NaN
            }
            var width = 0.0f
            var previous = -1
            for (component in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
                val id: ControlCenterRowComponent =
                    ControlCenterRowInput.Companion.component(component)
                if (!input.visible(id)) {
                    continue
                }
                val content = input.contentWidth(id)
                val paddingLeft = input.paddingLeft(id)
                val paddingRight = input.paddingRight(id)
                val marginLeft = input.marginLeft(id)
                val marginRight = input.marginRight(id)
                if (!java.lang.Float.isFinite(content) || !java.lang.Float.isFinite(paddingLeft) || !java.lang.Float.isFinite(paddingRight) || !java.lang.Float.isFinite(
                        marginLeft
                    ) || !java.lang.Float.isFinite(marginRight) || content <= 0.0f || paddingLeft < 0.0f || paddingRight < 0.0f || marginLeft < 0.0f || marginRight < 0.0f
                ) {
                    return Float.NaN
                }
                if (previous < 0) {
                    width += marginRight
                } else {
                    val previousId: ControlCenterRowComponent =
                        ControlCenterRowInput.Companion.component(previous)
                    width += input.marginLeft(previousId)
                    width += marginRight
                }
                width += content + paddingLeft + paddingRight
                previous = component
            }
            if (previous < 0) {
                return Float.NaN
            }
            return width + input.marginLeft(
                ControlCenterRowInput.Companion.component(previous)
            )
        }

        @JvmStatic fun snapTranslation(floatRowRight: Float, snappedRowRight: Int): Float {
            if (!java.lang.Float.isFinite(floatRowRight)) {
                return Float.NaN
            }
            return snappedRowRight - floatRowRight
        }

        private fun validate(input: ControlCenterRowInput?): Boolean {
            if (input == null || !java.lang.Float.isFinite(input.rowRight()) || !java.lang.Float.isFinite(input.availableWidth()) || input.availableWidth() < 0.0f) {
                return false
            }
            for (index in 0..<ControlCenterRowInput.Companion.COMPONENT_COUNT) {
                val component: ControlCenterRowComponent =
                    ControlCenterRowInput.Companion.component(index)
                val contentWidth = input.contentWidth(component)
                val paddingLeft = input.paddingLeft(component)
                val paddingRight = input.paddingRight(component)
                val marginLeft = input.marginLeft(component)
                val marginRight = input.marginRight(component)
                if (!java.lang.Float.isFinite(contentWidth) || !java.lang.Float.isFinite(paddingLeft) || !java.lang.Float.isFinite(paddingRight) || !java.lang.Float.isFinite(
                        marginLeft
                    ) || !java.lang.Float.isFinite(marginRight) || contentWidth < 0.0f || paddingLeft < 0.0f || paddingRight < 0.0f || marginLeft < 0.0f || marginRight < 0.0f || (input.visible(
                        component
                    )
                            && contentWidth <= 0.0f)
                ) {
                    return false
                }
            }
            return true
        }
    }
}
