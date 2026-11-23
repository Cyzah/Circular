package com.cyzah.circular.ui.preview

import kotlin.math.max
import kotlin.math.min

class PreviewStageLayout private constructor(@JvmField val boxSize: Float) {
    companion object {
        private const val ORIGINAL_GAP_RATIO = 0.06f

        /** 先单独缩短 WiFi→信号 1/4，再把两段间距同时缩短 1/3。  */
        private val WIFI_SIGNAL_GAP_RATIO: Float = ORIGINAL_GAP_RATIO * 0.75f * (2.0f / 3.0f)
        private val SIGNAL_BASE_GAP_RATIO: Float = ORIGINAL_GAP_RATIO * (2.0f / 3.0f)

        @JvmStatic fun fit(
            availableWidth: Float,
            preferredBoxSize: Float,
            baseAspect: Float
        ): PreviewStageLayout {
            val safeAspect = if (java.lang.Float.isFinite(baseAspect)) max(0.1f, baseAspect) else 1.0f
            val safeAvailable = if (java.lang.Float.isFinite(availableWidth)) max(0.0f, availableWidth) else 0.0f
            val safePreferred = if (java.lang.Float.isFinite(preferredBoxSize)) max(0.0f, preferredBoxSize) else 0.0f
            val factor: Float = safeAspect + 2.0f + WIFI_SIGNAL_GAP_RATIO + SIGNAL_BASE_GAP_RATIO
            val boxSize = min(safePreferred, safeAvailable / factor)
            return PreviewStageLayout(boxSize)
        }

        @JvmStatic fun alignWifiVisualGap(
            wifiCentre: Float, wifiHalfWidth: Float,
            signalLeft: Float, signalRight: Float, baseLeft: Float
        ): Float {
            val safeWifiHalf = max(0.0f, wifiHalfWidth)
            val wifiGap = signalLeft - (wifiCentre + safeWifiHalf)
            val baseGap = baseLeft - signalRight
            return if (wifiGap > baseGap) wifiCentre + (wifiGap - baseGap) else wifiCentre
        }
    }
}
