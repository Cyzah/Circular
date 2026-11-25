package com.cyzah.circular.domain.settings

import java.util.ArrayList
import java.util.LinkedHashMap

object SettingsSchema {
    const val MODE: String = "mode"
    const val SIZE: String = "size"
    const val COLOUR: String = "colour"
    const val WEIGHT: String = "weight"
    const val WEIGHT_V2: String = "weight_v2"
    const val WIFI_WEIGHT: String = "wifi_weight"
    const val TECHNOLOGY_WEIGHT: String = "technology_weight"
    const val TECHNOLOGY_WEIGHT_V2: String = "technology_weight_v2"
    const val WIFI_SCALE: String = "wifi_scale"
    const val TECHNOLOGY_SCALE: String = "technology_scale"
    const val TECHNOLOGY_SCALE_V2: String = "technology_scale_v2"
    const val PERCENT_SCALE: String = "percent_scale"
    const val PERCENT_SCALE_V2: String = "percent_scale_v2"
    const val PERCENT_WEIGHT: String = "percent_weight"
    const val PERCENT_WEIGHT_V2: String = "percent_weight_v2"
    const val PERCENT_WEIGHT_LINK: String = "percent_weight_link"
    const val SWAP_CONTENTS: String = "swap_contents"
    const val KEEP_MERGED_SHADE: String = "keep_merged_shade"
    const val STACKED_SHADE: String = "stacked_shade"
    const val GROUPED_DOTS: String = "dual_dots_grouped"
    const val CAPSULE_DUAL: String = "capsule_dual"
    const val DUO_DUAL: String = "duo_dual"
    const val CHARGE_COLOR: String = "charge_color"
    const val LOW_COLOR: String = "low_color"
    const val SAVER_COLOR: String = "saver_color"
    const val PERFORMANCE_COLOR: String = "performance_color"
    const val LOW_BATTERY_THRESHOLD: String = "low_battery_threshold"
    const val SHOW_PERCENT: String = "show_percent"
    const val LOCKSCREEN: String = "lockscreen"
    const val AOD: String = "aod"
    const val DIAGNOSTIC_REQUEST: String = "diagnostic_request"
    const val TECHNOLOGY_TEXT: String = "technology_text"
    const val STATUS_FONT_SOURCE: String = "status_font_source"
    const val BATTERY_NUMBER_PLACEMENT: String = "battery_number_placement"
    const val STYLE_PROFILE_DUO: String = "style_profile_duo"
    const val TOP_CONTENT_OFFSET_X_PX: String = "top_content_offset_x_px"
    const val TOP_CONTENT_OFFSET_Y_PX: String = "top_content_offset_y_px"
    const val CENTER_CONTENT_OFFSET_X_PX: String = "center_content_offset_x_px"
    const val CENTER_CONTENT_OFFSET_Y_PX: String = "center_content_offset_y_px"
    const val BOTTOM_CONTENT_OFFSET_X_PX: String = "bottom_content_offset_x_px"
    const val BOTTOM_CONTENT_OFFSET_Y_PX: String = "bottom_content_offset_y_px"
    const val TOP_CONTENT_OFFSET_X_HALF_PX: String = "top_content_offset_x_half_px"
    const val TOP_CONTENT_OFFSET_Y_HALF_PX: String = "top_content_offset_y_half_px"
    const val CENTER_CONTENT_OFFSET_X_HALF_PX: String = "center_content_offset_x_half_px"
    const val CENTER_CONTENT_OFFSET_Y_HALF_PX: String = "center_content_offset_y_half_px"
    const val BOTTOM_CONTENT_OFFSET_X_HALF_PX: String = "bottom_content_offset_x_half_px"
    const val BOTTOM_CONTENT_OFFSET_Y_HALF_PX: String = "bottom_content_offset_y_half_px"

    @JvmField val DEFINITIONS: List<Definition>
    @JvmField val CURSOR_COLUMNS: Array<String>

    init {
        val definitions = ArrayList<Definition>()
        add(definitions, MODE, MODE, Kind.INTEGER)
        add(definitions, SIZE, SIZE, Kind.INTEGER)
        add(definitions, COLOUR, COLOUR, Kind.BOOLEAN)
        add(definitions, WEIGHT, WEIGHT, Kind.INTEGER)
        add(definitions, LOCKSCREEN, LOCKSCREEN, Kind.BOOLEAN)
        add(definitions, AOD, AOD, Kind.BOOLEAN)
        add(definitions, DIAGNOSTIC_REQUEST, DIAGNOSTIC_REQUEST, Kind.STRING)
        add(definitions, LOW_BATTERY_THRESHOLD, LOW_BATTERY_THRESHOLD, Kind.INTEGER)
        add(definitions, SHOW_PERCENT, SHOW_PERCENT, Kind.BOOLEAN)
        add(definitions, WIFI_SCALE, WIFI_SCALE, Kind.INTEGER)
        add(definitions, TECHNOLOGY_SCALE, TECHNOLOGY_SCALE, Kind.INTEGER)
        add(definitions, PERCENT_SCALE, PERCENT_SCALE, Kind.INTEGER)
        add(definitions, null, PERCENT_WEIGHT_LINK, Kind.INTEGER)
        add(definitions, PERCENT_SCALE_V2, PERCENT_SCALE_V2, Kind.INTEGER)
        add(definitions, CHARGE_COLOR, CHARGE_COLOR, Kind.INTEGER)
        add(definitions, LOW_COLOR, LOW_COLOR, Kind.INTEGER)
        add(definitions, SAVER_COLOR, SAVER_COLOR, Kind.INTEGER)
        add(definitions, PERFORMANCE_COLOR, PERFORMANCE_COLOR, Kind.INTEGER)
        add(definitions, PERCENT_WEIGHT, PERCENT_WEIGHT, Kind.INTEGER)
        add(definitions, SWAP_CONTENTS, SWAP_CONTENTS, Kind.BOOLEAN)
        add(definitions, PERCENT_WEIGHT_V2, PERCENT_WEIGHT_V2, Kind.INTEGER)
        add(definitions, WIFI_WEIGHT, WIFI_WEIGHT, Kind.INTEGER)
        add(definitions, TECHNOLOGY_WEIGHT, TECHNOLOGY_WEIGHT, Kind.INTEGER)
        add(definitions, WEIGHT_V2, WEIGHT_V2, Kind.INTEGER)
        add(definitions, TECHNOLOGY_SCALE_V2, TECHNOLOGY_SCALE_V2, Kind.INTEGER)
        add(definitions, TECHNOLOGY_WEIGHT_V2, TECHNOLOGY_WEIGHT_V2, Kind.INTEGER)
        add(definitions, KEEP_MERGED_SHADE, KEEP_MERGED_SHADE, Kind.BOOLEAN)
        add(definitions, STACKED_SHADE, STACKED_SHADE, Kind.BOOLEAN)
        add(definitions, GROUPED_DOTS, GROUPED_DOTS, Kind.BOOLEAN)
        add(definitions, DUO_DUAL, DUO_DUAL, Kind.BOOLEAN)
        add(definitions, CAPSULE_DUAL, CAPSULE_DUAL, Kind.BOOLEAN)
        add(definitions, TECHNOLOGY_TEXT, TECHNOLOGY_TEXT, Kind.STRING)
        add(definitions, BATTERY_NUMBER_PLACEMENT,
                BATTERY_NUMBER_PLACEMENT, Kind.INTEGER)
        add(definitions, STYLE_PROFILE_DUO, STYLE_PROFILE_DUO, Kind.STRING)
        add(definitions, TOP_CONTENT_OFFSET_X_PX, TOP_CONTENT_OFFSET_X_PX, Kind.INTEGER)
        add(definitions, TOP_CONTENT_OFFSET_Y_PX, TOP_CONTENT_OFFSET_Y_PX, Kind.INTEGER)
        add(definitions, CENTER_CONTENT_OFFSET_X_PX, CENTER_CONTENT_OFFSET_X_PX, Kind.INTEGER)
        add(definitions, CENTER_CONTENT_OFFSET_Y_PX, CENTER_CONTENT_OFFSET_Y_PX, Kind.INTEGER)
        add(definitions, BOTTOM_CONTENT_OFFSET_X_PX, BOTTOM_CONTENT_OFFSET_X_PX, Kind.INTEGER)
        add(definitions, BOTTOM_CONTENT_OFFSET_Y_PX, BOTTOM_CONTENT_OFFSET_Y_PX, Kind.INTEGER)
        add(definitions, TOP_CONTENT_OFFSET_X_HALF_PX, TOP_CONTENT_OFFSET_X_HALF_PX, Kind.INTEGER)
        add(definitions, TOP_CONTENT_OFFSET_Y_HALF_PX, TOP_CONTENT_OFFSET_Y_HALF_PX, Kind.INTEGER)
        add(definitions, CENTER_CONTENT_OFFSET_X_HALF_PX, CENTER_CONTENT_OFFSET_X_HALF_PX, Kind.INTEGER)
        add(definitions, CENTER_CONTENT_OFFSET_Y_HALF_PX, CENTER_CONTENT_OFFSET_Y_HALF_PX, Kind.INTEGER)
        add(definitions, BOTTOM_CONTENT_OFFSET_X_HALF_PX, BOTTOM_CONTENT_OFFSET_X_HALF_PX, Kind.INTEGER)
        add(definitions, BOTTOM_CONTENT_OFFSET_Y_HALF_PX, BOTTOM_CONTENT_OFFSET_Y_HALF_PX, Kind.INTEGER)
        add(definitions, STATUS_FONT_SOURCE, STATUS_FONT_SOURCE, Kind.STRING)
        DEFINITIONS = java.util.Collections.unmodifiableList(definitions)
        CURSOR_COLUMNS = cursorColumns(DEFINITIONS)
    }

    @JvmStatic fun isSceneKey(key: String?): Boolean {
        return "scene_state" == key || "preview_dark" == key
    }

    @JvmStatic fun isReportKey(key: String?): Boolean {
        return "report" == key || "report_time" == key || "report_token" == key
    }

    /** Exclude only known nonappearance events; future and unclassified keys stay observable.  */
    @JvmStatic fun affectsAppearance(key: String?): Boolean {
        return key == null || !(isSceneKey(key) || isReportKey(key)
                || DIAGNOSTIC_REQUEST == key
                || "ai_modification_notice_dismissed" == key
                || "ai_modification_notice_install_stamp" == key
                || "light_theme" == key)
    }

    private fun add(
        definitions: MutableList<Definition>,
        preferenceKey: String?,
        cursorColumn: String?,
        kind: Kind
    ) {
        definitions.add(Definition(preferenceKey, cursorColumn, kind))
    }

    private fun cursorColumns(definitions: List<Definition>): Array<String> {
        val columns = ArrayList<String>()
        for (definition in definitions) {
            if (definition.cursorColumn() != null) {
                columns.add(requireNotNull(definition.cursorColumn()))
            }
        }
        return columns.toTypedArray()
    }

    enum class Kind {
        INTEGER,
        BOOLEAN,
        STRING
    }

    class Definition(private val preferenceKey: String?, private val cursorColumn: String?, private val kind: Kind) {
        fun preferenceKey(): String? {
            return preferenceKey
        }

        fun cursorColumn(): String? {
            return cursorColumn
        }

        fun kind(): Kind {
            return kind
        }
    }

    /** Binds values by name while preserving the schema's public cursor order.  */
    class Row(definitions: List<Definition>) {
        private val positions: MutableMap<String?, Int> = LinkedHashMap<String?, Int>()
        private val values: Array<Any?>
        private val assigned: BooleanArray

        init {
            for (definition in definitions) {
                val column = definition.cursorColumn()
                if (column != null) {
                    require(!positions.containsKey(column)) { "Duplicate settings column: " + column }
                    positions.put(column, positions.size)
                }
            }
            values = arrayOfNulls<Any>(positions.size)
            assigned = BooleanArray(positions.size)
        }

        fun put(column: String?, value: Any?) {
            val position = requireNotNull(positions[column]) { "Unknown settings column: " + column }
            val index = position
            require(!assigned[index]) { "Duplicate settings assignment: " + column }
            values[index] = value
            assigned[index] = true
        }

        fun build(): Array<Any?> {
            for (entry in positions.entries) {
                check(assigned[entry.value]) { "Missing settings assignment: " + entry.key }
            }
            return values
        }
    }
}
