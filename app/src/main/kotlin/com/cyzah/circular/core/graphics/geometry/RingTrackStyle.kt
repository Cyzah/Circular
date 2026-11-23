package com.cyzah.circular.core.graphics.geometry

/** The unfinished track keeps the same appearance through every ring phase.  */
object RingTrackStyle {
    @JvmStatic fun tint(capsule: Boolean, battery: Int, plainTint: Int, batteryTint: Int): Int {
        return if (capsule || battery == 0) batteryTint else plainTint
    }

    @JvmStatic fun alpha(capsule: Boolean): Int {
        return if (capsule) 96 else 48
    }

    @JvmStatic fun temporaryAlpha(completeTrackVisible: Boolean, requestedAlpha: Float): Float {
        return if (completeTrackVisible) 0.0f else requestedAlpha
    }
}
