package com.cyzah.circular.data.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import com.cyzah.circular.BuildConfig
import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.core.graphics.TechnologyText
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.InstallDefaults
import com.cyzah.circular.domain.settings.SettingsRowSerializer
import com.cyzah.circular.domain.settings.SettingsSchema

class ConfigProvider : ContentProvider() {
    // android.content.ContentProvider
    override fun onCreate(): Boolean {
        return true
    }

    private fun checkCaller() {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) {
            return
        }
        try {
            if (callingUid == getContext()!!.getPackageManager().getApplicationInfo("com.android.systemui", 0).uid) {
                return
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        throw SecurityException("System UI only")
    }

    // android.content.ContentProvider
    override fun query(
        uri: Uri,
        strArr: Array<String?>?,
        str: String?,
        strArr2: Array<String?>?,
        str2: String?
    ): Cursor {
        checkCaller()
        require(URI == uri) { "Unknown URI" }
        val sharedPreferencesPrefs: SharedPreferences = Companion.prefs(getContext()!!)
        val matrixCursor = MatrixCursor(SettingsSchema.CURSOR_COLUMNS)
        matrixCursor.addRow(SettingsCursorRow.create(sharedPreferencesPrefs))
        return matrixCursor
    }

    // android.content.ContentProvider
    override fun call(str: String, str2: String?, bundle: Bundle?): Bundle? {
        checkCaller()
        if ("get" == str) {
            // 字符串型设置（如制式文本规则）走方法调用，避免改动 MatrixCursor 的列定义
            val bundle2 = Bundle()
            bundle2.putString("value", Companion.value(getContext()!!, str2))
            return bundle2
        }
        require(BuildConfig.DIAGNOSTIC_REPORTS && str == "report") { "Unknown method" }
        val string = if (bundle == null) null else bundle.getString("text")
        if (string != null && string.length <= 8192) {
            val string2 = bundle!!.getString("request_token", "")
            Companion.prefs(getContext()!!).edit().putString("report", string)
                .putLong("report_time", System.currentTimeMillis())
                .putString("report_token", if (string2.length <= 128) string2 else "").apply()
        }
        return Bundle.EMPTY
    }

    // android.content.ContentProvider
    override fun getType(uri: Uri): String {
        checkCaller()
        return "vnd.android.cursor.item/vnd.com.cyzah.circular.settings"
    }

    // android.content.ContentProvider
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        throw SecurityException("Read only")
    }

    // android.content.ContentProvider
    override fun update(uri: Uri, contentValues: ContentValues?, str: String?, strArr: Array<String?>?): Int {
        throw SecurityException("Read only")
    }

    // android.content.ContentProvider
    override fun delete(uri: Uri, str: String?, strArr: Array<String?>?): Int {
        throw SecurityException("Read only")
    }

    companion object {
        @JvmField val URI: Uri = Uri.parse("content://${BuildConfig.APPLICATION_ID}.config/settings")

        @JvmStatic @Synchronized
        fun prefs(context: Context): SharedPreferences {
            val sharedPreferences: SharedPreferences
            sharedPreferences = context.createDeviceProtectedStorageContext().getSharedPreferences("settings", 0)
            InstallDefaults.applyIfEmpty(sharedPreferences)
            if (!sharedPreferences.contains(SettingsSchema.PERCENT_WEIGHT_V2)) {
                sharedPreferences.edit().putInt(
                    SettingsSchema.PERCENT_WEIGHT_V2,
                    if (sharedPreferences.contains(SettingsSchema.PERCENT_WEIGHT))
                        BatteryHeader.weightFromLegacy(
                            sharedPreferences.getInt(
                                SettingsSchema.PERCENT_WEIGHT,
                                100
                            )
                        )
                    else
                        100
                ).apply()
            }
            if (!sharedPreferences.contains(SettingsSchema.WIFI_WEIGHT) || !sharedPreferences.contains(SettingsSchema.WEIGHT_V2) || !sharedPreferences.contains(
                    SettingsSchema.TECHNOLOGY_SCALE_V2
                ) || !sharedPreferences.contains(SettingsSchema.TECHNOLOGY_WEIGHT_V2)
            ) {
                val mapMigration = AppearanceScale.migration(sharedPreferences.getAll())
                val editorEdit = sharedPreferences.edit()
                for (entry in mapMigration.entries) {
                    editorEdit.putInt(entry.key, entry.value!!)
                }
                editorEdit.apply()
            }
            return sharedPreferences
        }

        @JvmStatic fun networkWeight(sharedPreferences: SharedPreferences, str: String?): Int {
            return SettingsRowSerializer.networkWeight(SettingsCursorRow.values(sharedPreferences), str)
        }

        @JvmStatic fun ringWeight(sharedPreferences: SharedPreferences): Int {
            return SettingsRowSerializer.ringWeight(SettingsCursorRow.values(sharedPreferences))
        }

        @JvmStatic fun technologyScale(sharedPreferences: SharedPreferences): Int {
            return SettingsRowSerializer.technologyScale(SettingsCursorRow.values(sharedPreferences))
        }

        @JvmStatic fun technologyWeight(sharedPreferences: SharedPreferences): Int {
            return SettingsRowSerializer.technologyWeight(SettingsCursorRow.values(sharedPreferences))
        }

        @JvmStatic fun percentWeight(sharedPreferences: SharedPreferences): Int {
            return SettingsRowSerializer.percentWeight(SettingsCursorRow.values(sharedPreferences))
        }

        @JvmStatic fun percentScale(sharedPreferences: SharedPreferences): Int {
            return SettingsRowSerializer.percentScale(SettingsCursorRow.values(sharedPreferences))
        }

        /** 制式文本自定义规则串（见 [TechnologyText]）  */
        @JvmStatic fun technologyText(sharedPreferences: SharedPreferences): String {
            return SettingsRowSerializer.technologyText(SettingsCursorRow.values(sharedPreferences))
        }

        @JvmStatic fun batteryNumberPlacement(
            sharedPreferences: SharedPreferences
        ): BatteryNumberPlacement {
            return SettingsRowSerializer.batteryNumberPlacement(SettingsCursorRow.values(sharedPreferences))
        }

        /** `call("get", 键)` 取值  */
        @JvmStatic fun value(context: Context, str: String?): String? {
            val sharedPreferencesPrefs: SharedPreferences = prefs(context)
            if (SettingsSchema.TECHNOLOGY_TEXT == str) {
                return technologyText(sharedPreferencesPrefs)
            }
            // 布尔型设置（开关）：返回 "1" / "0"
            if (SettingsSchema.CAPSULE_DUAL == str) {
                return if (sharedPreferencesPrefs.getBoolean(SettingsSchema.CAPSULE_DUAL, false))
                    "1"
                else
                    "0"
            }
            return if (str == null) "" else TechnologyText.Companion.sanitize(sharedPreferencesPrefs.getString(str, ""))
        }
    }
}
