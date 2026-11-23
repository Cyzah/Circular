package com.cyzah.circular.data.settings

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.SettingsValues
import java.util.LinkedHashMap

/** Typed, versioned startup records, independent of Android storage.  */
object StartupAppearanceCodec {
    private const val SCHEMA_VERSION = 1
    private const val SCHEMA = "cache_schema"
    private const val BUILD = "cache_build"
    private const val COMPATIBLE = "cache_compatible"
    private const val PRIMARY_PRESENT = "primary_present"
    private const val SECONDARY_PRESENT = "secondary_present"
    private const val PRIMARY_SLOT = "primary_slot"
    private const val SECONDARY_SLOT = "secondary_slot"

    @JvmStatic fun appearance(values: SettingsValues?, version: Int, compatible: Boolean): MutableMap<String?, Any?> {
        if (!compatible || values == null || !values.contains(SettingsSchema.MODE)) {
            return mutableMapOf<String?, Any?>()
        }
        val record = header(version)
        record.put(COMPATIBLE, true)
        for (definition in SettingsSchema.DEFINITIONS) {
            val key = definition.cursorColumn()
            if (key == null || SettingsSchema.DIAGNOSTIC_REQUEST == key
                || !values.contains(key)
            ) {
                continue
            }
            when (definition.kind()) {
                SettingsSchema.Kind.INTEGER -> record.put(key, values.getInt(key, 0))
                SettingsSchema.Kind.BOOLEAN -> record.put(key, values.getBoolean(key, false))
                SettingsSchema.Kind.STRING -> record.put(key, values.getString(key, ""))
            }
        }
        return record
    }

    @JvmStatic fun restoreAppearance(record: MutableMap<String?, *>?, version: Int): SettingsValues? {
        if (!validHeader(
                record,
                version
            ) || (true != record!!.get(COMPATIBLE)) || (record.get(SettingsSchema.MODE) !is Int)
        ) {
            return null
        }
        val values: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        for (definition in SettingsSchema.DEFINITIONS) {
            val key = definition.cursorColumn()
            if (key == null || SettingsSchema.DIAGNOSTIC_REQUEST == key
                || !record.containsKey(key)
            ) {
                continue
            }
            val value: Any? = record.get(key)
            val valid = if (definition.kind() == SettingsSchema.Kind.INTEGER)
                value is Int
            else
                if (definition.kind() == SettingsSchema.Kind.BOOLEAN)
                    value is Boolean
                else
                    value is String
            if (!valid) {
                return null
            }
            values.put(key, value)
        }
        return RecordedValues(values)
    }

    @JvmStatic fun topology(state: IconState?, version: Int): MutableMap<String?, Any?> {
        if (state == null || !state.mobileKnown || !validTopology(
                state.simPresent, state.secondarySimPresent,
                state.mobileSlot, state.secondarySlot
            )
        ) {
            return mutableMapOf<String?, Any?>()
        }
        val record = header(version)
        record.put(PRIMARY_PRESENT, state.simPresent)
        record.put(SECONDARY_PRESENT, state.secondarySimPresent)
        record.put(PRIMARY_SLOT, if (state.simPresent) state.mobileSlot else -1)
        record.put(SECONDARY_SLOT, if (state.secondarySimPresent) state.secondarySlot else -1)
        return record
    }

    @JvmStatic fun restoreTopology(record: MutableMap<String?, *>?, version: Int, state: IconState?): Boolean {
        if (state == null || state.mobileKnown || state.secondaryKnown
            || !validHeader(record, version) || (record!!.get(PRIMARY_PRESENT) !is Boolean) || (record.get(
                SECONDARY_PRESENT
            ) !is Boolean) || (record.get(PRIMARY_SLOT) !is Int) || (record.get(SECONDARY_SLOT) !is Int)
        ) {
            return false
        }
        val primaryPresent = record.get(PRIMARY_PRESENT) as Boolean
        val secondaryPresent = record.get(SECONDARY_PRESENT) as Boolean
        val primarySlot = record.get(PRIMARY_SLOT) as Int
        val secondarySlot = record.get(SECONDARY_SLOT) as Int
        if (!validTopology(primaryPresent, secondaryPresent, primarySlot, secondarySlot)) {
            return false
        }
        state.simPresent = primaryPresent
        state.secondarySimPresent = secondaryPresent
        state.mobileSlot = if (primaryPresent) primarySlot else -1
        state.secondarySlot = if (secondaryPresent) secondarySlot else -1
        return true
    }

    private fun header(version: Int): MutableMap<String?, Any?> {
        val record: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        record.put(SCHEMA, SCHEMA_VERSION)
        record.put(BUILD, version)
        return record
    }

    private fun validHeader(record: MutableMap<String?, *>?, version: Int): Boolean {
        return record != null && SCHEMA_VERSION == record.get(SCHEMA)
                && version == record.get(BUILD)
    }

    private fun validTopology(
        primaryPresent: Boolean, secondaryPresent: Boolean,
        primarySlot: Int, secondarySlot: Int
    ): Boolean {
        return (!primaryPresent || primarySlot >= 0)
                && (!secondaryPresent || secondarySlot >= 0)
                && (!secondaryPresent || primaryPresent)
                && (!primaryPresent || !secondaryPresent || primarySlot != secondarySlot)
    }

    private class RecordedValues(private val values: MutableMap<String?, Any?>) : SettingsValues {
        override fun contains(key: String?): Boolean {
            return this.values.containsKey(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val value = this.values.get(key)
            return if (value is Int) value else fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val value = this.values.get(key)
            return if (value is Boolean) value else fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            val value = this.values.get(key)
            return if (value is String) value else fallback
        }
    }
}
