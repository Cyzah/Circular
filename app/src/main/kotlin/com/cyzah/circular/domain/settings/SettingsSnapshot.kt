package com.cyzah.circular.domain.settings

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.core.graphics.TechnologyText

class SettingsSnapshot private constructor(
    private val mode: Int,
    private val size: Int,
    private val batterySize: Int,
    private val coloured: Boolean,
    private val weight: Int,
    private val wifiWeight: Int,
    private val technologyWeight: Int,
    private val wifiScale: Int,
    private val technologyScale: Int,
    private val percentScale: Int,
    private val percentWeight: Int,
    private val swapContents: Boolean,
    private val keepMergedShade: Boolean,
    private val stackedShade: Boolean,
    private val groupedDots: Boolean,
    private val capsuleDual: Boolean,
    private val duoDual: Boolean,
    private val chargeColor: Int,
    private val lowColor: Int,
    private val saverColor: Int,
    private val performanceColor: Int,
    private val lowBatteryThreshold: Int,
    private val showPercent: Boolean,
    private val lockscreen: Boolean,
    private val aod: Boolean,
    private val diagnosticRequest: String?,
    private val technologyText: String,
    private val batteryNumberPlacement: BatteryNumberPlacement,
    private val contentOffsets: ContentOffsets,
    private val statusFont: StatusFontSelection
) {
    fun mode(): Int {
        return mode
    }

    /** Effective icon configuration; diagnostic commands have their own delivery channel.  */
    fun sameAppearanceAs(other: SettingsSnapshot?): Boolean {
        return other != null && mode == other.mode && size == other.size && batterySize == other.batterySize && coloured == other.coloured && weight == other.weight && wifiWeight == other.wifiWeight && technologyWeight == other.technologyWeight && wifiScale == other.wifiScale && technologyScale == other.technologyScale && percentScale == other.percentScale && percentWeight == other.percentWeight && swapContents == other.swapContents && keepMergedShade == other.keepMergedShade && stackedShade == other.stackedShade && groupedDots == other.groupedDots && capsuleDual == other.capsuleDual && duoDual == other.duoDual && chargeColor == other.chargeColor && lowColor == other.lowColor && saverColor == other.saverColor && performanceColor == other.performanceColor && lowBatteryThreshold == other.lowBatteryThreshold && showPercent == other.showPercent && lockscreen == other.lockscreen && aod == other.aod && technologyText == other.technologyText
                && batteryNumberPlacement == other.batteryNumberPlacement && contentOffsets.equals(other.contentOffsets)
                && statusFont == other.statusFont
    }

    fun size(): Int {
        return size
    }

    fun batterySize(): Int {
        return batterySize
    }

    fun coloured(): Boolean {
        return coloured
    }

    fun weight(): Int {
        return weight
    }

    fun wifiWeight(): Int {
        return wifiWeight
    }

    fun technologyWeight(): Int {
        return technologyWeight
    }

    fun wifiScale(): Int {
        return wifiScale
    }

    fun technologyScale(): Int {
        return technologyScale
    }

    fun percentScale(): Int {
        return percentScale
    }

    fun percentWeight(): Int {
        return percentWeight
    }

    fun swapContents(): Boolean {
        return swapContents
    }

    fun keepMergedShade(): Boolean {
        return keepMergedShade
    }

    fun stackedShade(): Boolean {
        return stackedShade
    }

    fun groupedDots(): Boolean {
        return groupedDots
    }

    fun capsuleDual(): Boolean {
        return capsuleDual
    }

    fun duoDual(): Boolean {
        return duoDual
    }

    fun dualStyle(): DualStyle {
        return DualStyle.Companion.from(groupedDots, capsuleDual, duoDual)
    }

    fun chargeColor(): Int {
        return chargeColor
    }

    fun lowColor(): Int {
        return lowColor
    }

    fun saverColor(): Int {
        return saverColor
    }

    fun performanceColor(): Int {
        return performanceColor
    }

    fun lowBatteryThreshold(): Int {
        return lowBatteryThreshold
    }

    fun showPercent(): Boolean {
        return showPercent
    }

    fun lockscreen(): Boolean {
        return lockscreen
    }

    fun aod(): Boolean {
        return aod
    }

    fun diagnosticRequest(): String? {
        return diagnosticRequest
    }

    fun technologyText(): String {
        return technologyText
    }

    fun batteryNumberPlacement(): BatteryNumberPlacement {
        return batteryNumberPlacement
    }

    fun contentOffsets(): ContentOffsets {
        return contentOffsets
    }

    fun statusFont(): StatusFontSelection = statusFont

    companion object {
        @JvmStatic fun from(values: SettingsValues): SettingsSnapshot {
            val hasLegacyWeight = values.contains(SettingsSchema.WEIGHT)
            val legacyWeight = if (hasLegacyWeight)
                DisplayPolicy.weight(values.getInt(SettingsSchema.WEIGHT, 100))
            else
                100
            val weight = if (values.contains(SettingsSchema.WEIGHT_V2))
                DisplayPolicy.weight(values.getInt(SettingsSchema.WEIGHT_V2, 100))
            else
                if (hasLegacyWeight)
                    AppearanceScale.weightFromLegacy(legacyWeight)
                else
                    100
            val wifiWeight = DisplayPolicy.weight(
                values.getInt(
                    SettingsSchema.WIFI_WEIGHT,
                    legacyWeight
                )
            )
            val technologyWeight = if (values.contains(SettingsSchema.TECHNOLOGY_WEIGHT_V2))
                DisplayPolicy.weight(
                    values.getInt(
                        SettingsSchema.TECHNOLOGY_WEIGHT_V2,
                        100
                    )
                )
            else
                if (values.contains(SettingsSchema.TECHNOLOGY_WEIGHT))
                    AppearanceScale.weightFromLegacy(
                        values.getInt(
                            SettingsSchema.TECHNOLOGY_WEIGHT,
                            legacyWeight
                        )
                    )
                else
                    if (hasLegacyWeight)
                        AppearanceScale.weightFromLegacy(legacyWeight)
                    else
                        100
            val wifiScale = DisplayPolicy.centrePercent(
                values.getInt(
                    SettingsSchema.WIFI_SCALE,
                    100
                )
            )
            val technologyScale = if (values.contains(SettingsSchema.TECHNOLOGY_SCALE_V2))
                DisplayPolicy.centrePercent(
                    values.getInt(
                        SettingsSchema.TECHNOLOGY_SCALE_V2,
                        100
                    )
                )
            else
                if (values.contains(SettingsSchema.TECHNOLOGY_SCALE))
                    AppearanceScale.sizeFromLegacy(
                        values.getInt(SettingsSchema.TECHNOLOGY_SCALE, 100)
                    )
                else
                    100
            val percentScale = if (values.contains(SettingsSchema.PERCENT_SCALE_V2))
                BatteryHeader.scale(values.getInt(SettingsSchema.PERCENT_SCALE_V2, 100))
            else
                if (values.contains(SettingsSchema.PERCENT_SCALE))
                    BatteryHeader.fromLegacy(
                        values.getInt(
                            SettingsSchema.PERCENT_SCALE,
                            100
                        )
                    )
                else
                    100
            val percentWeight = if (values.contains(SettingsSchema.PERCENT_WEIGHT_V2))
                BatteryHeader.weight(
                    values.getInt(
                        SettingsSchema.PERCENT_WEIGHT_V2,
                        100
                    )
                )
            else
                if (values.contains(SettingsSchema.PERCENT_WEIGHT))
                    BatteryHeader.weightFromLegacy(
                        values.getInt(SettingsSchema.PERCENT_WEIGHT, 100)
                    )
                else
                    BatteryHeader.weightFromLegacy(legacyWeight)

            val grouped = values.getBoolean(SettingsSchema.GROUPED_DOTS, false)
            val capsule = values.getBoolean(SettingsSchema.CAPSULE_DUAL, false)
            val duo = values.getBoolean(SettingsSchema.DUO_DUAL, false)
            val style: DualStyle? = DualStyle.Companion.from(grouped, capsule, duo)
            val activeSize = DisplayPolicy.size(values.getInt(SettingsSchema.SIZE, 24))
            val batterySize = if (style == DualStyle.DUO)
                activeSize
            else
                batterySize(values.getString(SettingsSchema.STYLE_PROFILE_DUO, ""))
            var diagnosticRequest = values.getString(
                SettingsSchema.DIAGNOSTIC_REQUEST,
                ""
            )!!
            if (diagnosticRequest.length > 128) {
                diagnosticRequest = ""
            }

            return SettingsSnapshot(
                DisplayPolicy.mode(values.getInt(SettingsSchema.MODE, 0)),
                activeSize,
                batterySize,
                values.getBoolean(SettingsSchema.COLOUR, true),
                weight,
                wifiWeight,
                technologyWeight,
                wifiScale,
                technologyScale,
                percentScale,
                percentWeight,
                values.getBoolean(SettingsSchema.SWAP_CONTENTS, false),
                values.getBoolean(SettingsSchema.KEEP_MERGED_SHADE, false),
                values.getBoolean(SettingsSchema.STACKED_SHADE, true),
                style == DualStyle.GROUPED,
                style == DualStyle.CAPSULE,
                style == DualStyle.DUO,
                StatusColors.opaque(
                    values.getInt(
                        SettingsSchema.CHARGE_COLOR,
                        StatusColors.CHARGING
                    )
                ),
                StatusColors.opaque(
                    values.getInt(
                        SettingsSchema.LOW_COLOR,
                        StatusColors.LOW
                    )
                ),
                StatusColors.opaque(
                    values.getInt(
                        SettingsSchema.SAVER_COLOR,
                        StatusColors.SAVER
                    )
                ),
                StatusColors.opaque(
                    values.getInt(
                        SettingsSchema.PERFORMANCE_COLOR,
                        StatusColors.PERFORMANCE
                    )
                ),
                DisplayPolicy.lowBatteryThreshold(
                    values.getInt(
                        SettingsSchema.LOW_BATTERY_THRESHOLD,
                        DisplayPolicy.DEFAULT_LOW_BATTERY_THRESHOLD
                    )
                ),
                values.getBoolean(SettingsSchema.SHOW_PERCENT, false),
                values.getBoolean(SettingsSchema.LOCKSCREEN, true),
                values.getBoolean(SettingsSchema.AOD, true),
                diagnosticRequest,
                TechnologyText.Companion.sanitize(
                    values.getString(
                        SettingsSchema.TECHNOLOGY_TEXT,
                        ""
                    )
                ),
                BatteryNumberPlacement.from(
                    values.getInt(
                        SettingsSchema.BATTERY_NUMBER_PLACEMENT,
                        BatteryNumberPlacement.DEFAULT.value
                    )
                ),
                SettingsRowSerializer.contentOffsets(values),
                StatusFontSelection.decode(values.getString(SettingsSchema.STATUS_FONT_SOURCE, null))
            )
        }

        private fun batterySize(duoProfileValue: String?): Int {
            val duoProfile: StyleProfile? = StyleProfile.Companion.decode(duoProfileValue)
            val size = if (duoProfile == null)
                StyleProfile.Companion.defaults(DualStyle.DUO)!!.size
            else
                duoProfile.size
            return SizePolicy.clamp(DualStyle.DUO, size)
        }
    }
}
