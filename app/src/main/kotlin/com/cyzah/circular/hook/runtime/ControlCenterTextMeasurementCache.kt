package com.cyzah.circular.hook.runtime


/**
 * Single-entry text-metric cache with a complete invalidation key.
 */
class ControlCenterTextMeasurementCache {
    private var valid = false
    private var text = ""
    private var textSizeBits = 0
    private var fontScaleBits = 0
    private var locale = ""
    private var fontWeight = 0
    private var typeface: Any? = null
    private var typefaceGeneration: Long = 0
    private var width = 0f
    private var textSize = 0f
    private var textTop = 0
    private var textBottom = 0

    fun matches(
        text: String?,
        textSize: Float,
        fontScale: Float,
        locale: String?,
        fontWeight: Int,
        typeface: Any?,
        typefaceGeneration: Long
    ): Boolean {
        return this.valid
                && this.text == text.orEmpty()
                && this.textSizeBits == java.lang.Float.floatToIntBits(textSize) && this.fontScaleBits == java.lang.Float.floatToIntBits(
            fontScale
        ) && this.locale == locale.orEmpty()
                && this.fontWeight == fontWeight && this.typeface === typeface && this.typefaceGeneration == typefaceGeneration
    }

    @JvmOverloads
    fun store(
        text: String?,
        textSize: Float,
        fontScale: Float,
        locale: String?,
        fontWeight: Int,
        typeface: Any?,
        typefaceGeneration: Long,
        width: Float,
        measuredTextSize: Float = textSize,
        textTop: Int = Math.round(-textSize),
        textBottom: Int = Math.round(textSize)
    ) {
        this.valid = true
        this.text = if (text == null) "" else text
        this.textSizeBits = java.lang.Float.floatToIntBits(textSize)
        this.fontScaleBits = java.lang.Float.floatToIntBits(fontScale)
        this.locale = if (locale == null) "" else locale
        this.fontWeight = fontWeight
        this.typeface = typeface
        this.typefaceGeneration = typefaceGeneration
        this.width = width
        this.textSize = measuredTextSize
        this.textTop = textTop
        this.textBottom = textBottom
    }

    fun width(): Float {
        return this.width
    }

    fun textSize(): Float {
        return this.textSize
    }

    fun textTop(): Int {
        return this.textTop
    }

    fun textBottom(): Int {
        return this.textBottom
    }
}
