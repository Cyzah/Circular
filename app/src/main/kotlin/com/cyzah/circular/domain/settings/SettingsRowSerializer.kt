package com.cyzah.circular.domain.settings

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.core.graphics.TechnologyText

/** The provider's wire values, including compatibility columns for older clients.  */
object SettingsRowSerializer {
    @JvmStatic fun create(prefs: SettingsValues): Array<Any?>? {
        val row = SettingsSchema.Row(SettingsSchema.DEFINITIONS)
        val ringWeight = ringWeight(prefs)
        val technologyScale = technologyScale(prefs)
        val technologyWeight = technologyWeight(prefs)
        val percentScale = percentScale(prefs)
        val percentWeight = percentWeight(prefs)
        row.put(SettingsSchema.MODE, DisplayPolicy.mode(prefs.getInt(SettingsSchema.MODE, 0)))
        row.put(SettingsSchema.SIZE, DisplayPolicy.size(prefs.getInt(SettingsSchema.SIZE, 24)))
        row.put(SettingsSchema.COLOUR, if (prefs.getBoolean(SettingsSchema.COLOUR, true)) 1 else 0)
        row.put(SettingsSchema.WEIGHT, AppearanceScale.weightToLegacy(ringWeight))
        row.put(SettingsSchema.LOCKSCREEN, if (prefs.getBoolean(SettingsSchema.LOCKSCREEN, true)) 1 else 0)
        row.put(SettingsSchema.AOD, if (prefs.getBoolean(SettingsSchema.AOD, true)) 1 else 0)
        row.put(SettingsSchema.DIAGNOSTIC_REQUEST, prefs.getString(SettingsSchema.DIAGNOSTIC_REQUEST, ""))
        row.put(
            SettingsSchema.LOW_BATTERY_THRESHOLD, DisplayPolicy.lowBatteryThreshold(
                prefs.getInt(
                    SettingsSchema.LOW_BATTERY_THRESHOLD,
                    DisplayPolicy.DEFAULT_LOW_BATTERY_THRESHOLD
                )
            )
        )
        row.put(SettingsSchema.SHOW_PERCENT, if (prefs.getBoolean(SettingsSchema.SHOW_PERCENT, false)) 1 else 0)
        row.put(
            SettingsSchema.WIFI_SCALE, DisplayPolicy.centrePercent(
                prefs.getInt(SettingsSchema.WIFI_SCALE, 100)
            )
        )
        row.put(SettingsSchema.TECHNOLOGY_SCALE, AppearanceScale.sizeToLegacy(technologyScale))
        row.put(SettingsSchema.PERCENT_SCALE, BatteryHeader.toLegacy(percentScale))
        row.put(SettingsSchema.PERCENT_WEIGHT_LINK, 0)
        row.put(SettingsSchema.PERCENT_SCALE_V2, percentScale)
        row.put(
            SettingsSchema.CHARGE_COLOR, StatusColors.opaque(
                prefs.getInt(SettingsSchema.CHARGE_COLOR, StatusColors.CHARGING)
            )
        )
        row.put(
            SettingsSchema.LOW_COLOR, StatusColors.opaque(
                prefs.getInt(SettingsSchema.LOW_COLOR, StatusColors.LOW)
            )
        )
        row.put(
            SettingsSchema.SAVER_COLOR, StatusColors.opaque(
                prefs.getInt(SettingsSchema.SAVER_COLOR, StatusColors.SAVER)
            )
        )
        row.put(
            SettingsSchema.PERFORMANCE_COLOR, StatusColors.opaque(
                prefs.getInt(SettingsSchema.PERFORMANCE_COLOR, StatusColors.PERFORMANCE)
            )
        )
        row.put(SettingsSchema.PERCENT_WEIGHT, BatteryHeader.weightToLegacy(percentWeight))
        row.put(SettingsSchema.SWAP_CONTENTS, if (prefs.getBoolean(SettingsSchema.SWAP_CONTENTS, false)) 1 else 0)
        row.put(SettingsSchema.PERCENT_WEIGHT_V2, percentWeight)
        row.put(SettingsSchema.WIFI_WEIGHT, networkWeight(prefs, SettingsSchema.WIFI_WEIGHT))
        row.put(SettingsSchema.TECHNOLOGY_WEIGHT, AppearanceScale.weightToLegacy(technologyWeight))
        row.put(SettingsSchema.WEIGHT_V2, ringWeight)
        row.put(SettingsSchema.TECHNOLOGY_SCALE_V2, technologyScale)
        row.put(SettingsSchema.TECHNOLOGY_WEIGHT_V2, technologyWeight)
        row.put(
            SettingsSchema.KEEP_MERGED_SHADE,
            if (prefs.getBoolean(SettingsSchema.KEEP_MERGED_SHADE, false)) 1 else 0
        )
        row.put(SettingsSchema.STACKED_SHADE, if (prefs.getBoolean(SettingsSchema.STACKED_SHADE, true)) 1 else 0)
        row.put(SettingsSchema.GROUPED_DOTS, if (prefs.getBoolean(SettingsSchema.GROUPED_DOTS, false)) 1 else 0)
        row.put(SettingsSchema.DUO_DUAL, if (prefs.getBoolean(SettingsSchema.DUO_DUAL, false)) 1 else 0)
        row.put(SettingsSchema.CAPSULE_DUAL, if (prefs.getBoolean(SettingsSchema.CAPSULE_DUAL, false)) 1 else 0)
        row.put(SettingsSchema.TECHNOLOGY_TEXT, technologyText(prefs))
        row.put(SettingsSchema.BATTERY_NUMBER_PLACEMENT, batteryNumberPlacement(prefs).value)
        row.put(SettingsSchema.STYLE_PROFILE_DUO, prefs.getString(SettingsSchema.STYLE_PROFILE_DUO, ""))
        val offsets = contentOffsets(prefs)
        // Older consumers receive whole pixels, truncating toward zero at half-pixel values.
        row.put(SettingsSchema.TOP_CONTENT_OFFSET_X_PX, offsets.topX / 2)
        row.put(SettingsSchema.TOP_CONTENT_OFFSET_Y_PX, offsets.topY / 2)
        row.put(SettingsSchema.CENTER_CONTENT_OFFSET_X_PX, offsets.centerX / 2)
        row.put(SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX, offsets.centerY / 2)
        row.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX, offsets.bottomX / 2)
        row.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX, offsets.bottomY / 2)
        row.put(SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX, offsets.topX)
        row.put(SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX, offsets.topY)
        row.put(SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX, offsets.centerX)
        row.put(SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX, offsets.centerY)
        row.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX, offsets.bottomX)
        row.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX, offsets.bottomY)
        row.put(SettingsSchema.STATUS_FONT_SOURCE, StatusFontSelection.decode(
            prefs.getString(SettingsSchema.STATUS_FONT_SOURCE, null)
        ).encode())
        return row.build()
    }

    @JvmStatic fun contentOffsets(values: SettingsValues): ContentOffsets {
        return ContentOffsets(
            contentOffset(
                values, SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX,
                SettingsSchema.TOP_CONTENT_OFFSET_X_PX
            ),
            contentOffset(
                values, SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX,
                SettingsSchema.TOP_CONTENT_OFFSET_Y_PX
            ),
            contentOffset(
                values, SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX,
                SettingsSchema.CENTER_CONTENT_OFFSET_X_PX
            ),
            contentOffset(
                values, SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX,
                SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX
            ),
            contentOffset(
                values, SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX,
                SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX
            ),
            contentOffset(
                values, SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX,
                SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX
            )
        )
    }

    @JvmStatic fun contentOffset(
        values: SettingsValues, canonicalKey: String?,
        legacyKey: String?
    ): Int {
        if (values.contains(canonicalKey)) {
            return ContentOffsets.clampHalfSteps(values.getInt(canonicalKey, 0))
        }
        return ContentOffsets.legacyPixelToHalfSteps(values.getInt(legacyKey, 0))
    }

    @JvmStatic fun networkWeight(prefs: SettingsValues, key: String?): Int {
        return DisplayPolicy.weight(prefs.getInt(key, prefs.getInt(SettingsSchema.WEIGHT, 100)))
    }

    @JvmStatic fun ringWeight(prefs: SettingsValues): Int {
        return DisplayPolicy.weight(prefs.getInt(SettingsSchema.WEIGHT_V2, 100))
    }

    @JvmStatic fun technologyScale(prefs: SettingsValues): Int {
        return DisplayPolicy.centrePercent(prefs.getInt(SettingsSchema.TECHNOLOGY_SCALE_V2, 100))
    }

    @JvmStatic fun technologyWeight(prefs: SettingsValues): Int {
        return DisplayPolicy.weight(prefs.getInt(SettingsSchema.TECHNOLOGY_WEIGHT_V2, 100))
    }

    @JvmStatic fun percentWeight(prefs: SettingsValues): Int {
        if (prefs.contains(SettingsSchema.PERCENT_WEIGHT_V2)) {
            return BatteryHeader.weight(prefs.getInt(SettingsSchema.PERCENT_WEIGHT_V2, 100))
        }
        if (prefs.contains(SettingsSchema.PERCENT_WEIGHT)) {
            return BatteryHeader.weightFromLegacy(prefs.getInt(SettingsSchema.PERCENT_WEIGHT, 100))
        }
        return 100
    }

    @JvmStatic fun percentScale(prefs: SettingsValues): Int {
        if (prefs.contains(SettingsSchema.PERCENT_SCALE_V2)) {
            return BatteryHeader.scale(prefs.getInt(SettingsSchema.PERCENT_SCALE_V2, 100))
        }
        if (prefs.contains(SettingsSchema.PERCENT_SCALE)) {
            return BatteryHeader.fromLegacy(prefs.getInt(SettingsSchema.PERCENT_SCALE, 100))
        }
        return 100
    }

    @JvmStatic fun technologyText(prefs: SettingsValues): String {
        return TechnologyText.Companion.sanitize(prefs.getString(SettingsSchema.TECHNOLOGY_TEXT, ""))
    }

    @JvmStatic fun batteryNumberPlacement(prefs: SettingsValues): BatteryNumberPlacement {
        return BatteryNumberPlacement.from(
            prefs.getInt(
                SettingsSchema.BATTERY_NUMBER_PLACEMENT, BatteryNumberPlacement.DEFAULT.value
            )
        )
    }
}
