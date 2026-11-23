package com.cyzah.circular.domain.settings

/** Immutable half-physical-pixel steps for the merged icon's three content slots. */
class ContentOffsets(topX: Int, topY: Int, centerX: Int, centerY: Int, bottomX: Int, bottomY: Int) {
    @JvmField val topX: Int = clampHalfSteps(topX)
    @JvmField val topY: Int = clampHalfSteps(topY)
    @JvmField val centerX: Int = clampHalfSteps(centerX)
    @JvmField val centerY: Int = clampHalfSteps(centerY)
    @JvmField val bottomX: Int = clampHalfSteps(bottomX)
    @JvmField val bottomY: Int = clampHalfSteps(bottomY)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContentOffsets) return false
        return topX == other.topX && topY == other.topY && centerX == other.centerX &&
            centerY == other.centerY && bottomX == other.bottomX && bottomY == other.bottomY
    }

    override fun hashCode(): Int {
        var result = topX
        result = 31 * result + topY
        result = 31 * result + centerX
        result = 31 * result + centerY
        result = 31 * result + bottomX
        return 31 * result + bottomY
    }

    companion object {
        const val MIN_HALF_STEPS: Int = -48
        const val MAX_HALF_STEPS: Int = 48
        @JvmField val DEFAULT: ContentOffsets = ContentOffsets(0, 0, 0, 0, 0, 0)

        @JvmStatic fun clampHalfSteps(value: Int): Int = value.coerceIn(MIN_HALF_STEPS, MAX_HALF_STEPS)

        @JvmStatic fun legacyPixelToHalfSteps(pixels: Int): Int =
            pixels.coerceIn(MIN_HALF_STEPS / 2, MAX_HALF_STEPS / 2) * 2

        @JvmStatic fun fromLegacyPixels(
            topX: Int, topY: Int, centerX: Int, centerY: Int, bottomX: Int, bottomY: Int,
        ): ContentOffsets = ContentOffsets(
            legacyPixelToHalfSteps(topX), legacyPixelToHalfSteps(topY),
            legacyPixelToHalfSteps(centerX), legacyPixelToHalfSteps(centerY),
            legacyPixelToHalfSteps(bottomX), legacyPixelToHalfSteps(bottomY),
        )
    }
}
