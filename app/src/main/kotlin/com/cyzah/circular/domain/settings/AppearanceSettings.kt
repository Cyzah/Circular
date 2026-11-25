package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.StyleProfile

/**
 * Shared immutable drawing appearance; parsing and defaults remain in SettingsSnapshot.
 */
class AppearanceSettings private constructor(
    private val snapshot: SettingsSnapshot,
    private val controlCenterProfile: StyleProfile?
) {
    fun controlCenter(): AppearanceSettings {
        val profile = ControlCenterAppearance.profile(
            snapshot.dualStyle(), snapshot.size(), snapshot.swapContents()
        )
        return AppearanceSettings(
            snapshot,
            profile
        )
    }

    fun controlCenterAppearance(): AppearanceSettings {
        return if (keepMergedShade()) this else controlCenter()
    }

    fun mode(): Int {
        return snapshot.mode()
    }

    fun size(): Int {
        return if (controlCenterProfile == null)
            snapshot.size()
        else
            controlCenterProfile.size
    }

    fun batterySize(): Int {
        return if (controlCenterProfile == null)
            snapshot.batterySize()
        else
            ControlCenterAppearance.BATTERY_SIZE
    }

    fun weight(): Int {
        return if (controlCenterProfile == null)
            snapshot.weight()
        else
            controlCenterProfile.weight
    }

    fun wifiWeight(): Int {
        return if (controlCenterProfile == null)
            snapshot.wifiWeight()
        else
            controlCenterProfile.wifiWeight
    }

    fun technologyWeight(): Int {
        return if (controlCenterProfile == null)
            snapshot.technologyWeight()
        else
            controlCenterProfile.technologyWeight
    }

    fun wifiScale(): Int {
        return if (controlCenterProfile == null)
            snapshot.wifiScale()
        else
            controlCenterProfile.wifiScale
    }

    fun technologyScale(): Int {
        return if (controlCenterProfile == null)
            snapshot.technologyScale()
        else
            controlCenterProfile.technologyScale
    }

    fun percentScale(): Int {
        return if (controlCenterProfile == null)
            snapshot.percentScale()
        else
            controlCenterProfile.percentScale
    }

    fun percentWeight(): Int {
        return if (controlCenterProfile == null)
            snapshot.percentWeight()
        else
            controlCenterProfile.percentWeight
    }

    fun swapContents(): Boolean {
        return if (controlCenterProfile == null)
            snapshot.swapContents()
        else
            controlCenterProfile.swapContents
    }

    fun keepMergedShade(): Boolean {
        return snapshot.keepMergedShade()
    }

    fun stackedShade(): Boolean {
        return snapshot.stackedShade()
    }

    fun groupedDots(): Boolean {
        return snapshot.groupedDots()
    }

    fun capsuleDual(): Boolean {
        return snapshot.capsuleDual()
    }

    fun duoDual(): Boolean {
        return snapshot.duoDual()
    }

    fun chargeColor(): Int {
        return snapshot.chargeColor()
    }

    fun lowColor(): Int {
        return snapshot.lowColor()
    }

    fun saverColor(): Int {
        return snapshot.saverColor()
    }

    fun performanceColor(): Int {
        return snapshot.performanceColor()
    }

    fun lowBatteryThreshold(): Int {
        return snapshot.lowBatteryThreshold()
    }

    fun showPercent(): Boolean {
        return snapshot.showPercent()
    }

    fun contentOffsets(): ContentOffsets {
        // Expanded native slots retain their solved geometry independently of merged offsets.
        return if (controlCenterProfile == null) snapshot.contentOffsets() else ContentOffsets.DEFAULT
    }

    fun coloured(): Boolean {
        return snapshot.coloured()
    }

    fun dualStyle(): DualStyle {
        return snapshot.dualStyle()
    }

    fun batteryNumberPlacement(): BatteryNumberPlacement {
        return snapshot.batteryNumberPlacement()
    }

    companion object {
        @JvmStatic fun from(snapshot: SettingsSnapshot): AppearanceSettings {
            return AppearanceSettings(snapshot, null)
        }
    }
}
