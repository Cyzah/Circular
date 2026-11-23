package com.cyzah.circular.core.graphics

import com.cyzah.circular.domain.settings.AppearanceSettings

/** Shared appearance mapping; each host applies its layout and motion afterwards.  */
object PainterConfigurator {
    @JvmStatic fun apply(
        painter: IconPainter,
        settings: AppearanceSettings,
        duoWifiProgress: Float
    ) {
        painter.setWeightPercent(settings.weight())
        painter.setContentOffsets(settings.contentOffsets())
        painter.setNetworkWeights(
            settings.wifiWeight(),
            settings.technologyWeight()
        )
        painter.setLowBatteryThreshold(settings.lowBatteryThreshold())
        painter.setShowPercent(settings.showPercent())
        painter.setBatteryText(settings.percentScale())
        painter.setBatteryWeightPercent(settings.percentWeight())
        painter.setSwapContents(settings.swapContents())
        painter.setGroupedDots(settings.groupedDots())
        painter.setCapsuleDual(settings.capsuleDual())
        painter.setDuoDual(settings.duoDual())
        painter.setDuoWifiProgress(duoWifiProgress)
        painter.setStatusColors(
            settings.chargeColor(),
            settings.lowColor(),
            settings.saverColor(),
            settings.performanceColor()
        )
        painter.setCentreSizes(
            settings.wifiScale(),
            settings.technologyScale()
        )
    }
}
