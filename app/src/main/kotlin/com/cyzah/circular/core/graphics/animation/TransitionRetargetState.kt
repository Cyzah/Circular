package com.cyzah.circular.core.graphics.animation

import com.cyzah.circular.core.graphics.geometry.TransitionGeometry

/**
 * Preserves an already rendered transition position when a native target
 * becomes available or moves while the same transition is still running.
 */
class TransitionRetargetState {
    private val ideal = TransitionGeometry()
    private val lastTarget = TransitionGeometry()
    private val rendered = TransitionGeometry()
    private val correction = FloatArray(6)
    private var correctionAnchor = 0f
    private var hasRendered = false
    private var hasTarget = false
    private var targetWasReady = false

    fun resolve(
        source: TransitionGeometry?,
        target: TransitionGeometry?,
        progress: Float,
        targetReady: Boolean
    ) {
        if (source == null || !source.valid()) {
            reset()
            return
        }
        val value = Motion.clamp(progress)
        if (!targetReady || target == null || !target.valid()) {
            this.targetWasReady = false
            clearCorrectionAtEndpoint(value)
            if (hasTarget) {
                TransitionGeometry.Companion.interpolateSimilarity(
                    source,
                    lastTarget,
                    value,
                    ideal
                )
                applyCorrection(value)
            } else {
                rendered.copyFrom(source)
            }
            hasRendered = true
            return
        }

        val targetChanged = !hasTarget || !this.targetWasReady
        TransitionGeometry.Companion.interpolateSimilarity(
            source,
            target,
            value,
            ideal
        )
        if (targetChanged) {
            setCorrectionFromRendered(ideal)
            correctionAnchor = value
            hasTarget = true
        }
        lastTarget.copyFrom(target)
        this.targetWasReady = true
        clearCorrectionAtEndpoint(value)
        applyCorrection(value)
        hasRendered = true
    }

    fun rendered(): TransitionGeometry {
        return rendered
    }

    fun reset() {
        rendered.set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        lastTarget.set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        ideal.set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        clearCorrection()
        correctionAnchor = 0.0f
        hasRendered = false
        hasTarget = false
        targetWasReady = false
    }

    private fun setCorrectionFromRendered(target: TransitionGeometry) {
        if (!hasRendered) {
            clearCorrection()
            return
        }
        correction[0] = rendered.centreX - target.centreX
        correction[1] = rendered.centreY - target.centreY
        correction[2] = rendered.widthX - target.widthX
        correction[3] = rendered.widthY - target.widthY
        correction[4] = rendered.heightX - target.heightX
        correction[5] = rendered.heightY - target.heightY
    }

    private fun applyCorrection(progress: Float) {
        val weight: Float = retargetWeight(progress, correctionAnchor)
        rendered.set(
            ideal.centreX + (correction[0] * weight),
            ideal.centreY + (correction[1] * weight),
            ideal.widthX + (correction[2] * weight),
            ideal.widthY + (correction[3] * weight),
            ideal.heightX + (correction[4] * weight),
            ideal.heightY + (correction[5] * weight)
        )
    }

    private fun clearCorrection() {
        for (index in correction.indices) {
            correction[index] = 0.0f
        }
    }

    private fun clearCorrectionAtEndpoint(progress: Float) {
        if (progress <= 0.0001f || progress >= 0.9999f) {
            clearCorrection()
        }
    }

    companion object {
        @JvmStatic fun retargetWeight(progress: Float, anchor: Float): Float {
            val value = Motion.clamp(progress)
            val held = Motion.clamp(anchor)
            if (held <= 0.0001f || held >= 0.9999f) {
                return 0.0f
            }
            return if (value <= held)
                value / held
            else
                (1.0f - value) / (1.0f - held)
        }
    }
}
