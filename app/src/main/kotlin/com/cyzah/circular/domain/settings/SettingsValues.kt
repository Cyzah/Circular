package com.cyzah.circular.domain.settings

interface SettingsValues {
    fun contains(key: String?): Boolean

    fun getInt(key: String?, fallback: Int): Int

    fun getBoolean(key: String?, fallback: Boolean): Boolean

    fun getString(key: String?, fallback: String?): String?
}
