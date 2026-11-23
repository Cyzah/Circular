package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.AppearanceSettings
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.ContentOffsets
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues

/** Hook compatibility and visibility wrap the shared drawing appearance.  */
class SessionSettings private constructor(
    private val snapshot: SettingsSnapshot,
    private val compatible: Boolean,
    private val appearance: AppearanceSettings
) {
    fun controlCenter(): SessionSettings {
        return SessionSettings(snapshot, compatible, appearance.controlCenter())
    }

    fun controlCenterAppearance(): SessionSettings {
        return if (keepMergedShade()) this else controlCenter()
    }

    fun appearance(): AppearanceSettings {
        return appearance
    }

    fun lockscreen(): Boolean {
        return snapshot.lockscreen()
    }

    fun aod(): Boolean {
        return snapshot.aod()
    }

    fun compatible(): Boolean {
        return compatible
    }

    fun mode(): Int {
        return appearance.mode()
    }

    fun size(): Int {
        return appearance.size()
    }

    fun batterySize(): Int {
        return appearance.batterySize()
    }

    fun weight(): Int {
        return appearance.weight()
    }

    fun wifiWeight(): Int {
        return appearance.wifiWeight()
    }

    fun technologyWeight(): Int {
        return appearance.technologyWeight()
    }

    fun wifiScale(): Int {
        return appearance.wifiScale()
    }

    fun technologyScale(): Int {
        return appearance.technologyScale()
    }

    fun percentScale(): Int {
        return appearance.percentScale()
    }

    fun percentWeight(): Int {
        return appearance.percentWeight()
    }

    fun swapContents(): Boolean {
        return appearance.swapContents()
    }

    fun keepMergedShade(): Boolean {
        return appearance.keepMergedShade()
    }

    fun stackedShade(): Boolean {
        return appearance.stackedShade()
    }

    fun groupedDots(): Boolean {
        return appearance.groupedDots()
    }

    fun capsuleDual(): Boolean {
        return appearance.capsuleDual()
    }

    fun duoDual(): Boolean {
        return appearance.duoDual()
    }

    fun chargeColor(): Int {
        return appearance.chargeColor()
    }

    fun lowColor(): Int {
        return appearance.lowColor()
    }

    fun saverColor(): Int {
        return appearance.saverColor()
    }

    fun performanceColor(): Int {
        return appearance.performanceColor()
    }

    fun lowBatteryThreshold(): Int {
        return appearance.lowBatteryThreshold()
    }

    fun showPercent(): Boolean {
        return appearance.showPercent()
    }

    fun coloured(): Boolean {
        return appearance.coloured()
    }

    fun dualStyle(): DualStyle {
        return appearance.dualStyle()
    }

    fun batteryNumberPlacement(): BatteryNumberPlacement {
        return appearance.batteryNumberPlacement()
    }

    fun contentOffsets(): ContentOffsets {
        return appearance.contentOffsets()
    }

    private class EmptySettingsValues : SettingsValues {
        override fun contains(key: String?): Boolean {
            return false
        }

        override fun getInt(key: String?, fallback: Int): Int {
            return fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            return fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            return fallback
        }
    }

    companion object {
        @JvmStatic fun defaults(): SessionSettings {
            return from(SettingsSnapshot.Companion.from(EmptySettingsValues()), false)
        }

        @JvmStatic fun from(snapshot: SettingsSnapshot, compatible: Boolean): SessionSettings {
            return SessionSettings(snapshot, compatible, AppearanceSettings.Companion.from(snapshot))
        }
    }
}
