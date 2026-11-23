package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.SignalMotionPolicy
import com.cyzah.circular.core.graphics.geometry.StackedOverlayLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.ceil
import kotlin.math.max

object ControlCenterRowHostPolicy {
    @JvmStatic fun needsWidthBeforeDraw(actualWidth: Int, resolvedRequiredWidth: Int): Boolean {
        return actualWidth > 0 && resolvedRequiredWidth > actualWidth
    }

    @JvmStatic fun needsWidthBeforeDraw(
        actualWidth: Int, resolvedRequiredWidth: Int,
        closedEndpoint: Boolean
    ): Boolean {
        return if (closedEndpoint)
            actualWidth > 0 && resolvedRequiredWidth > 0 && actualWidth != resolvedRequiredWidth
        else
            needsWidthBeforeDraw(actualWidth, resolvedRequiredWidth)
    }

    @JvmStatic fun needsWidthBeforeDraw(
        actualWidth: Int, resolvedRequiredWidth: Int,
        closedEndpoint: Boolean, anchoredClosing: Boolean
    ): Boolean {
        return needsWidthBeforeDraw(actualWidth, resolvedRequiredWidth, closedEndpoint || anchoredClosing)
    }

    @JvmStatic fun reserveProjectedRowWidth(
        expandedWidth: Int, closedWidth: Int,
        progress: Float, placement: BatteryNumberPlacement?
    ): Int {
        val horizontalProgress = StackedOverlayLayout.signalXProgress(progress, placement)
        val signalRemaining = 1.0f - SignalMotionPolicy.moveProgress(
            SignalMotionPolicy.signalProgress(horizontalProgress)
        )
        val textRemaining = StackedOverlayLayout.technologyXProgress(progress)
        // Dots trim before moving, and text can arrive later than the dots.
        // A native neighbour must wait until both have released their space.
        val remaining = max(signalRemaining, textRemaining)
        return ceil(
            Motion.mix(
                max(0, closedWidth).toFloat(), max(0, expandedWidth).toFloat(), remaining
            ).toDouble()
        ).toInt()
    }

    @JvmStatic fun projectedHostWidth(
        rowWidth: Int, sourceLeft: Float, sourceWidth: Float,
        hostLeft: Float, hostWidth: Float
    ): Int {
        if (rowWidth <= 0) {
            return 0
        }
        if (!java.lang.Float.isFinite(sourceLeft) || !java.lang.Float.isFinite(sourceWidth) || !java.lang.Float.isFinite(hostLeft) || !java.lang.Float.isFinite(
                hostWidth
            ) || sourceWidth <= 0.0f || hostWidth <= 0.0f
        ) {
            return rowWidth
        }
        // Measurement must use the same screen coordinates as the copied drawing.
        val extra = (hostLeft + hostWidth) - (sourceLeft + sourceWidth)
        return ceil((rowWidth + max(0.0f, extra)).toDouble()).toInt()
    }

    @JvmStatic fun projectedTransitionWidth(
        expandedWidth: Int, closedWidth: Int,
        sourceLeft: Float, sourceWidth: Float, targetLeft: Float, targetWidth: Float,
        hostLeft: Float, hostWidth: Float, progress: Float, placement: BatteryNumberPlacement?
    ): Int {
        return reserveProjectedRowWidth(
            projectedHostWidth(
                expandedWidth,
                sourceLeft, sourceWidth, hostLeft, hostWidth
            ),
            projectedClosedWidth(closedWidth, targetLeft, targetWidth, hostLeft, hostWidth),
            progress, placement
        )
    }

    @JvmStatic fun projectedClosedWidth(
        closedWidth: Int, targetLeft: Float, targetWidth: Float,
        hostLeft: Float, hostWidth: Float
    ): Int {
        if (closedWidth <= 0) {
            return 0
        }
        if (!java.lang.Float.isFinite(targetLeft) || !java.lang.Float.isFinite(targetWidth) || !java.lang.Float.isFinite(hostLeft) || !java.lang.Float.isFinite(
                hostWidth
            ) || targetWidth <= 0.0f || hostWidth <= 0.0f
        ) {
            return closedWidth
        }
        // Match the real status-bar left boundary, including a negative right-edge
        // offset. Keeping an unnecessary minimum here causes a handoff jump.
        val targetBoundary = targetLeft + targetWidth - closedWidth
        return max(1, ceil((hostLeft + hostWidth - targetBoundary).toDouble()).toInt())
    }

    @JvmStatic fun measurementRight(
        currentRight: Float, targetHostWidth: Int, availableWidth: Float
    ): Float {
        return if (targetHostWidth > 0 && targetHostWidth <= availableWidth)
            targetHostWidth
                .toFloat()
        else
            currentRight
    }

    @JvmStatic fun hostExtraWidthPx(rowWidth: Float, batteryWidth: Float): Int {
        return max(
            0,
            ceil((rowWidth - batteryWidth).toDouble()).toInt()
        )
    }

    @JvmStatic fun hostWidthPx(rowWidth: Float, batteryWidth: Float): Int {
        if (!java.lang.Float.isFinite(rowWidth)
            || !java.lang.Float.isFinite(batteryWidth)
        ) {
            return 0
        }
        val battery = max(
            0,
            ceil(batteryWidth.toDouble()).toInt()
        )
        return battery + hostExtraWidthPx(rowWidth, batteryWidth)
    }

    @JvmStatic fun translateHorizontal(
        sourceX: Float,
        sourceOrigin: Float,
        targetOrigin: Float
    ): Float {
        return sourceX + targetOrigin - sourceOrigin
    }

    @JvmStatic fun screenToLocal(
        screenCoordinate: Float,
        hostScreenOrigin: Float
    ): Float {
        return screenCoordinate - hostScreenOrigin
    }

    @JvmStatic fun centreXAfterTranslation(
        sourceLeft: Float,
        sourceRight: Float,
        sourceOrigin: Float,
        targetOrigin: Float
    ): Float {
        val centre = (sourceLeft + sourceRight) / 2.0f
        return translateHorizontal(
            centre,
            sourceOrigin,
            targetOrigin
        )
    }

    @JvmStatic fun host5CanReuse(
        sourceValid: Boolean,
        targetAvailable: Boolean
    ): Boolean {
        return sourceValid && targetAvailable
    }
}
