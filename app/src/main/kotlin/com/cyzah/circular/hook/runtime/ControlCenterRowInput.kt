package com.cyzah.circular.hook.runtime

import java.util.Arrays

class ControlCenterRowInput {
    private val visible = BooleanArray(COMPONENT_COUNT)
    private val contentWidth = FloatArray(COMPONENT_COUNT)
    private val paddingLeft = FloatArray(COMPONENT_COUNT)
    private val paddingRight = FloatArray(COMPONENT_COUNT)
    private val marginLeft = FloatArray(COMPONENT_COUNT)
    private val marginRight = FloatArray(COMPONENT_COUNT)
    private var rowRight = 0f
    private var availableWidth = 0f

    fun reset() {
        Arrays.fill(this.visible, false)
        Arrays.fill(this.contentWidth, 0.0f)
        Arrays.fill(this.paddingLeft, 0.0f)
        Arrays.fill(this.paddingRight, 0.0f)
        Arrays.fill(this.marginLeft, 0.0f)
        Arrays.fill(this.marginRight, 0.0f)
    }

    fun setBounds(rowRight: Float, availableWidth: Float) {
        this.rowRight = rowRight
        this.availableWidth = availableWidth
    }

    fun set(
        component: ControlCenterRowComponent?,
        visible: Boolean,
        contentWidth: Float,
        paddingLeft: Float,
        paddingRight: Float,
        marginLeft: Float,
        marginRight: Float
    ) {
        val index: Int = index(component)
        this.visible[index] = visible
        this.contentWidth[index] = contentWidth
        this.paddingLeft[index] = paddingLeft
        this.paddingRight[index] = paddingRight
        this.marginLeft[index] = marginLeft
        this.marginRight[index] = marginRight
    }

    fun set(
        component: Int,
        visible: Boolean,
        contentWidth: Float,
        paddingLeft: Float,
        paddingRight: Float,
        marginLeft: Float,
        marginRight: Float
    ) {
        set(
            component(component)!!,
            visible,
            contentWidth,
            paddingLeft,
            paddingRight,
            marginLeft,
            marginRight
        )
    }

    fun setVisible(component: ControlCenterRowComponent?, visible: Boolean) {
        this.visible[index(component)] = visible
    }

    fun setVisible(component: Int, visible: Boolean) {
        setVisible(component(component)!!, visible)
    }

    fun setContentWidth(component: ControlCenterRowComponent?, width: Float) {
        this.contentWidth[index(component)] = width
    }

    fun setContentWidth(component: Int, width: Float) {
        setContentWidth(component(component)!!, width)
    }

    fun setMarginLeft(component: ControlCenterRowComponent?, margin: Float) {
        this.marginLeft[index(component)] = margin
    }

    fun setMarginLeft(component: Int, margin: Float) {
        setMarginLeft(component(component)!!, margin)
    }

    fun setMarginRight(component: ControlCenterRowComponent?, margin: Float) {
        this.marginRight[index(component)] = margin
    }

    fun setMarginRight(component: Int, margin: Float) {
        setMarginRight(component(component)!!, margin)
    }

    fun visible(component: ControlCenterRowComponent?): Boolean {
        return this.visible[index(component)]
    }

    fun contentWidth(component: ControlCenterRowComponent?): Float {
        return this.contentWidth[index(component)]
    }

    fun paddingLeft(component: ControlCenterRowComponent?): Float {
        return this.paddingLeft[index(component)]
    }

    fun paddingRight(component: ControlCenterRowComponent?): Float {
        return this.paddingRight[index(component)]
    }

    fun marginLeft(component: ControlCenterRowComponent?): Float {
        return this.marginLeft[index(component)]
    }

    fun marginLeft(component: Int): Float {
        return marginLeft(component(component)!!)
    }

    fun marginRight(component: ControlCenterRowComponent?): Float {
        return this.marginRight[index(component)]
    }

    fun marginRight(component: Int): Float {
        return marginRight(component(component)!!)
    }

    fun rowRight(): Float {
        return this.rowRight
    }

    fun availableWidth(): Float {
        return this.availableWidth
    }

    companion object {
        private val COMPONENTS: Array<ControlCenterRowComponent> = ControlCenterRowComponent.entries.toTypedArray()
        @JvmField val COMPONENT_COUNT: Int = COMPONENTS.size

        @JvmStatic fun component(index: Int): ControlCenterRowComponent {
            require(!(index < 0 || index >= COMPONENTS.size)) { "component" }
            return COMPONENTS[index]
        }

        private fun index(component: ControlCenterRowComponent?): Int {
            return requireNotNull(component) { "component" }.ordinal
        }
    }
}
