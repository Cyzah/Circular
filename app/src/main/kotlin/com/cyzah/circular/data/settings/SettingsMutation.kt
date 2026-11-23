package com.cyzah.circular.data.settings

import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.core.graphics.TechnologyText
import com.cyzah.circular.domain.settings.ContentOffsets
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.StatusColors
import com.cyzah.circular.domain.settings.StyleSettings
import java.util.LinkedHashMap

/** One complete typed preference transaction, including its compatibility aliases.  */
class SettingsMutation private constructor(key: String?, value: Any?) {
    private val values: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()

    init {
        values.put(key, value)
    }

    fun apply(store: StyleSettings.Store, changed: Runnable): Boolean {
        var different = false
        for (entry in values.entries) {
            if (!matches(store, entry.key, entry.value)) {
                different = true
                break
            }
        }
        if (!different) return false
        val editor = store.edit()
        for (entry in values.entries) {
            val value = entry.value
            if (value is Int) editor.putInt(entry.key, value)
            else if (value is Boolean) editor.putBoolean(entry.key, value)
            else editor.putString(entry.key, value as String?)
        }
        editor.apply()
        changed.run()
        return true
    }

    fun affectsAppearance(): Boolean {
        for (key in values.keys) {
            if (SettingsSchema.affectsAppearance(key)) return true
        }
        return false
    }

    companion object {
        @JvmStatic fun integer(key: String, value: Int): SettingsMutation {
            when (key) {
                SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.TOP_CONTENT_OFFSET_X_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.TOP_CONTENT_OFFSET_Y_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.CENTER_CONTENT_OFFSET_X_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX -> return contentOffset(
                    key,
                    SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX,
                    ContentOffsets.clampHalfSteps(value)
                )

                SettingsSchema.TOP_CONTENT_OFFSET_X_PX -> return contentOffset(
                    SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                SettingsSchema.TOP_CONTENT_OFFSET_Y_PX -> return contentOffset(
                    SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                SettingsSchema.CENTER_CONTENT_OFFSET_X_PX -> return contentOffset(
                    SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX -> return contentOffset(
                    SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX -> return contentOffset(
                    SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX -> return contentOffset(
                    SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX,
                    key,
                    ContentOffsets.legacyPixelToHalfSteps(value)
                )

                else -> {}
            }
            var normalized = value
            when (key) {
                SettingsSchema.MODE -> normalized = DisplayPolicy.mode(value)
                SettingsSchema.SIZE -> normalized = DisplayPolicy.size(value)
                SettingsSchema.WEIGHT_V2, SettingsSchema.WIFI_WEIGHT, SettingsSchema.TECHNOLOGY_WEIGHT_V2 -> normalized =
                    DisplayPolicy.weight(value)

                SettingsSchema.WIFI_SCALE, SettingsSchema.TECHNOLOGY_SCALE_V2 -> normalized =
                    DisplayPolicy.centrePercent(value)

                SettingsSchema.PERCENT_SCALE_V2 -> normalized = BatteryHeader.scale(value)
                SettingsSchema.PERCENT_WEIGHT_V2 -> normalized = BatteryHeader.weight(value)
                SettingsSchema.LOW_BATTERY_THRESHOLD -> normalized = DisplayPolicy.lowBatteryThreshold(value)
                SettingsSchema.CHARGE_COLOR, SettingsSchema.LOW_COLOR, SettingsSchema.SAVER_COLOR, SettingsSchema.PERFORMANCE_COLOR -> normalized =
                    StatusColors.opaque(value)

                else -> {}
            }
            val mutation = SettingsMutation(key, normalized)
            when (key) {
                SettingsSchema.WEIGHT_V2 -> mutation.values.put(
                    SettingsSchema.WEIGHT,
                    AppearanceScale.weightToLegacy(normalized)
                )

                SettingsSchema.TECHNOLOGY_SCALE_V2 -> mutation.values.put(
                    SettingsSchema.TECHNOLOGY_SCALE,
                    AppearanceScale.sizeToLegacy(normalized)
                )

                SettingsSchema.TECHNOLOGY_WEIGHT_V2 -> mutation.values.put(
                    SettingsSchema.TECHNOLOGY_WEIGHT,
                    AppearanceScale.weightToLegacy(normalized)
                )

                SettingsSchema.PERCENT_WEIGHT_V2 -> mutation.values.put(
                    SettingsSchema.PERCENT_WEIGHT,
                    BatteryHeader.weightToLegacy(normalized)
                )

                else -> {}
            }
            return mutation
        }

        private fun contentOffset(canonicalKey: String?, legacyKey: String?, halfSteps: Int): SettingsMutation {
            val mutation = SettingsMutation(canonicalKey, halfSteps)
            // Signed integer division keeps the compatibility pixel alias truncated toward zero.
            mutation.values.put(legacyKey, halfSteps / 2)
            return mutation
        }

        @JvmStatic fun bool(key: String?, value: Boolean): SettingsMutation {
            val mutation = SettingsMutation(key, value)
            if (value && SettingsSchema.KEEP_MERGED_SHADE == key) {
                mutation.values.put(SettingsSchema.STACKED_SHADE, false)
            } else if (value && SettingsSchema.STACKED_SHADE == key) {
                mutation.values.put(SettingsSchema.KEEP_MERGED_SHADE, false)
            }
            return mutation
        }

        @JvmStatic fun text(key: String?, value: String?): SettingsMutation {
            return SettingsMutation(
                key, if (SettingsSchema.TECHNOLOGY_TEXT == key)
                    TechnologyText.Companion.sanitize(value)
                else
                    value
            )
        }

        private fun matches(store: StyleSettings.Store, key: String?, value: Any?): Boolean {
            if (!store.contains(key)) return false
            try {
                if (value is Int) return value == store.getInt(key, 0)
                if (value is Boolean) return value == store.getBoolean(key, false)
                return value == store.getString(key, null)
            } catch (malformed: ClassCastException) {
                return false
            }
        }
    }
}
