package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.core.graphics.geometry.TransitionGeometry
import kotlin.math.max

/**
 * Pure geometry state for the Wi-Fi glyph that sits in the battery-ring top
 * opening for swapped LAYERED and GROUPED rows.
 */
class SwappedWifiMotion {
    private val source = TransitionGeometry()
    private val target = TransitionGeometry()
    private val retarget = TransitionRetargetState()

    fun resolveLayout(
        sourceOriginX: Float,
        sourceCentreY: Float,
        sourceScale: Float,
        sourceMinimum: Float,
        targetOriginX: Float,
        targetOriginY: Float,
        targetUnit: Float,
        targetScale: Float,
        anchorY: Float,
        dualOffset: Float,
        glyphWidth: Float,
        glyphHeight: Float,
        progress: Float,
        targetReady: Boolean
    ) {
        resolve(
            sourceOriginX + (sourceMinimum / 2.0f),
            sourceCentreY
                    - ((dualOffset * sourceMinimum) / 120.0f),
            sourceScale,
            targetOriginX + (IconGeometry.centreX() * targetUnit),
            targetOriginY + ((anchorY - dualOffset) * targetUnit),
            targetScale,
            glyphWidth,
            glyphHeight,
            progress,
            targetReady
        )
    }

    fun resolve(
        sourceCentreX: Float,
        sourceCentreY: Float,
        sourceScale: Float,
        targetCentreX: Float,
        targetCentreY: Float,
        targetScale: Float,
        glyphWidth: Float,
        glyphHeight: Float,
        progress: Float,
        targetReady: Boolean
    ) {
        val safeGlyphWidth = max(0.0f, glyphWidth)
        val safeGlyphHeight = max(0.0f, glyphHeight)
        source.setCentred(
            sourceCentreX,
            sourceCentreY,
            safeGlyphWidth * max(0.0f, sourceScale),
            safeGlyphHeight * max(0.0f, sourceScale)
        )
        target.setCentred(
            targetCentreX,
            targetCentreY,
            safeGlyphWidth * max(0.0f, targetScale),
            safeGlyphHeight * max(0.0f, targetScale)
        )
        retarget.resolve(source, target, progress, targetReady)
    }

    fun rendered(): TransitionGeometry {
        return retarget.rendered()
    }

    fun reset() {
        retarget.reset()
    }
}
