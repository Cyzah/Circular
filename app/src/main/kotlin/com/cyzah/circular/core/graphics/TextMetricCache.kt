package com.cyzah.circular.core.graphics

class TextMetricCache {
    private var valid = false
    private var text = ""
    private var scale = -1
    private var weight = -1
    private var typeface: Any? = null
    private var width = 0f
    private var textSize = 0f
    private var top = 0
    private var bottom = 0

    fun matches(
        text: String?,
        scale: Int,
        weight: Int,
        typeface: Any?
    ): Boolean {
        return this.valid
                && this.scale == scale && this.weight == weight && this.typeface === typeface && this.text == text
    }

    fun store(
        text: String,
        scale: Int,
        weight: Int,
        typeface: Any?,
        width: Float,
        textSize: Float,
        top: Int,
        bottom: Int
    ) {
        this.valid = true
        this.text = text
        this.scale = scale
        this.weight = weight
        this.typeface = typeface
        this.width = width
        this.textSize = textSize
        this.top = top
        this.bottom = bottom
    }

    fun width(): Float {
        return this.width
    }

    fun textSize(): Float {
        return this.textSize
    }

    fun top(): Int {
        return this.top
    }

    fun bottom(): Int {
        return this.bottom
    }
}
