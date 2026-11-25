package com.cyzah.circular.ui.state

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.edit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cyzah.circular.Version
import com.cyzah.circular.core.graphics.BatteryHeader
import com.cyzah.circular.core.graphics.TechnologyText
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.data.settings.ConfigProvider
import com.cyzah.circular.data.settings.SharedPreferencesSettingsStore
import com.cyzah.circular.data.settings.SettingsMutation
import com.cyzah.circular.domain.settings.*
import com.cyzah.circular.framework.device.DualSimCapability
import com.cyzah.circular.framework.device.ScopeRestarter
import com.cyzah.circular.ui.preview.*
import com.cyzah.circular.ui.theme.AppThemeMode
import com.cyzah.circular.ui.theme.AppThemeSettings
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** Persistence and background work live outside the composable tree. */
open class SettingsState(context: Context, restored: Bundle?) {
    protected val app = context.applicationContext
    val prefs: SharedPreferences = ConfigProvider.prefs(app)
    private val settingsStore = SharedPreferencesSettingsStore(prefs)
    private val styles = StyleSettings(settingsStore)
    protected val handler = Handler(Looper.getMainLooper())
    private val publisher = SettingsPublishScheduler(object : SettingsPublishScheduler.Scheduler {
        override fun nowMillis() = SystemClock.uptimeMillis()
        override fun post(task: Runnable, delayMillis: Long) { handler.postDelayed(task, delayMillis) }
        override fun remove(task: Runnable) { handler.removeCallbacks(task) }
    }, Runnable { app.contentResolver.notifyChange(ConfigProvider.URI, null) }, 50L)
    protected var disposed = false
        private set
    protected var resumed = false
        private set
    private var recoverySyncPending = true
    var previewActive by mutableStateOf(false)
        private set
    var revision by mutableIntStateOf(0)
        private set
    var sceneRevision by mutableIntStateOf(0)
        private set
    private var currentPage by mutableIntStateOf(restored?.getInt("page", 0)?.coerceIn(0, 3) ?: 0)
    var page: Int
        get() = currentPage
        set(value) {
            if (value != currentPage) {
                flushSettings()
                currentPage = value.coerceIn(0, 3)
            }
        }
    private val themePrefs = app.getSharedPreferences(AppThemeSettings.PREFERENCES, Context.MODE_PRIVATE)
    var themeSettings by mutableStateOf(AppThemeSettings.read(themePrefs, prefs))
        private set
    private var systemDark by mutableStateOf(
        app.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES)
    val darkTheme: Boolean get() = themeSettings.isDark(systemDark)
    var dualSimAvailable by mutableStateOf(DualSimCapability.available(app))
        private set
    var launcherHidden by mutableStateOf(readLauncherHidden())
        private set
    val scene = IconState()
    val runtime = PreviewRuntimeState(prefs.getBoolean("preview_dark", darkTheme), 0f, 0f)
    var expansion by mutableFloatStateOf(0f)
        private set
    var expansionDragging by mutableStateOf(false)
        private set
    var scenesExpanded by mutableStateOf(false)
        private set
    var restartBusy by mutableStateOf(ScopeRestarter.BUSY.get())
        private set
    var info by mutableStateOf<Pair<String, String>?>(null)
    private val installStamp = NoticePolicy.stamp(runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime
    }.getOrDefault(0L), Version.CODE)
    var showNotice by mutableStateOf(restored?.getBoolean("notice_visible") ?: NoticePolicy.shouldShow(
        true, prefs.getBoolean("ai_modification_notice_dismissed", false),
        prefs.getString("ai_modification_notice_install_stamp", ""), installStamp))
        private set

    private val refreshWork = object : Runnable {
        override fun run() {
            if (!resumed || disposed) return
            restartBusy = ScopeRestarter.BUSY.get()
            if (restartBusy) handler.postDelayed(this, 500L)
        }
    }
    private var observedSceneState = prefs.getString("scene_state", "")
    private val observation = SettingsObservation(settingsStore,
        object : SettingsObservation.Scheduler {
            override fun post(task: Runnable) { handler.post(task) }
            override fun remove(task: Runnable) { handler.removeCallbacks(task) }
        }, object : SettingsObservation.Listener {
            override fun onAppearanceChanged(snapshot: SettingsSnapshot) {
                if (!disposed) revision++
            }
            override fun onSceneChanged() {
                if (disposed) return
                var changed = false
                val encoded = prefs.getString("scene_state", "")
                if (encoded != observedSceneState) {
                    val incoming = IconState()
                    PreviewSceneCodec.decode(encoded, incoming)?.let {
                        scene.copyFrom(incoming)
                        scenesExpanded = it.expanded
                        changed = true
                    }
                    observedSceneState = encoded
                }
                if (prefs.getBoolean("preview_dark", runtime.dark()) != runtime.dark()) {
                    runtime.toggleDark()
                    changed = true
                }
                if (changed) sceneRevision++
            }
            override fun onReportChanged() = this@SettingsState.onReportChanged()
        })

    init {
        val mode = prefs.getInt("mode", 0)
        if (mode != DisplayPolicy.mode(mode)) write(SettingsMutation.integer("mode", mode))
        if (styles.initialize(dualSimAvailable)) notifySettings()
        if (bool("keep_merged_shade", false) && bool("stacked_shade", true)) write(SettingsMutation.bool("stacked_shade", false))
        val encoded = restored?.getString("scene_state") ?: prefs.getString("scene_state", "")
        val decoded = PreviewSceneCodec.decode(encoded, scene)
        if (decoded != null) {
            // Expansion is a held gesture preview, so never restore a half-open icon.
            expansion = 0f
            scenesExpanded = decoded.expanded
        } else {
            scene.wifiKnown = true
            scene.wifiEnabled = true
            scene.wifiConnected = true
            scene.wifiLevel = 3
            scene.battery = 91
            scene.mobileLevel = 4
            scene.mobileTechnology = "5G"
            scene.mobileSlot = 1
            scene.secondarySlot = 0
            PreviewSceneOperations.setDual(scene, dualSimAvailable && style() != DualStyle.LAYERED)
        }
        runtime.setExpansion(if (page == 0) expansion else 0f)
        runtime.setDuoWifiProgress(if (scene.showWifi()) 1f else 0f)
        observation.attach()
    }

    protected open fun onReportChanged() = Unit

    fun bool(key: String, default: Boolean) = prefs.getBoolean(key, default)
    fun int(key: String, default: Int) = prefs.getInt(key, default)
    fun style(): DualStyle = styles.selected()
    fun notifySettings() = publisher.publishNow()
    fun flushSettings() = publisher.flush()
    protected fun write(change: SettingsMutation, continuous: Boolean = false) {
        change.apply(settingsStore) {
            if (change.affectsAppearance()) observation.refreshAppearanceNow()
            if (continuous) publisher.changedContinuously() else publisher.publishNow()
        }
    }
    fun setBoolean(key: String, value: Boolean) = write(SettingsMutation.bool(key, value))
    fun setInt(key: String, value: Int) = write(SettingsMutation.integer(key,
        if (key == SettingsSchema.SIZE) SizePolicy.clamp(style(), value) else value))
    fun setIntContinuously(key: String, value: Int) = write(SettingsMutation.integer(key,
        if (key == SettingsSchema.SIZE) SizePolicy.clamp(style(), value) else value), continuous = true)
    fun setTechnologyText(value: String) = write(SettingsMutation.text("technology_text", value))
    fun selectStyle(style: DualStyle) {
        dualSimAvailable = DualSimCapability.available(app)
        if (styles.select(style, dualSimAvailable)) {
            notifySettings()
            observation.refreshAppearanceNow()
            editScene { PreviewSceneOperations.setDual(it, dualSimAvailable) }
        }
    }
    fun toggleTheme() {
        setThemeMode(themeSettings.oppositeMode(systemDark))
    }
    fun updateSystemDark(value: Boolean) { systemDark = value }
    private fun updateTheme(value: AppThemeSettings) {
        if (themeSettings == value) return
        themeSettings = value
        value.save(themePrefs)
    }
    fun setThemeMode(value: AppThemeMode) = updateTheme(themeSettings.copy(mode = value))
    fun setThemeAmoled(value: Boolean) = updateTheme(themeSettings.copy(amoled = value))
    fun setThemeCustomColor(value: Boolean) = updateTheme(themeSettings.copy(customColor = value))
    fun setThemeDynamicColor(value: Boolean) = updateTheme(themeSettings.copy(
        customColor = themeSettings.customColor || value, dynamicColor = value))
    fun setThemeKeyColor(value: Int) = updateTheme(themeSettings.copy(
        customColor = true, dynamicColor = false, keyColor = value or 0xFF000000.toInt()))
    fun setThemePalette(value: ThemePaletteStyle) = updateTheme(themeSettings.copy(paletteStyle = value))
    fun setThemeColorSpec(value: ThemeColorSpec) = updateTheme(themeSettings.copy(colorSpec = value))
    fun previewDarkChanged() {
        prefs.edit { putBoolean("preview_dark", runtime.dark()) }
        sceneRevision++
    }
    fun editScene(edit: (IconState) -> Unit) {
        edit(scene)
        PreviewPowerMode.normalize(scene)
        persistScene()
        sceneRevision++
    }
    fun updateExpansion(value: Float) {
        expansionDragging = true
        expansion = value.coerceIn(0f, 1f)
        sceneRevision++
    }
    fun finishExpansionPreview() {
        if (!expansionDragging && expansion == 0f) return
        expansionDragging = false
        expansion = 0f
        sceneRevision++
    }
    fun toggleScenes() {
        scenesExpanded = !scenesExpanded
        persistScene()
    }
    private fun persistScene() {
        val encoded = PreviewSceneCodec.encode(scene, 0f, scenesExpanded)
        observedSceneState = encoded
        if (encoded != prefs.getString("scene_state", "")) prefs.edit { putString("scene_state", encoded) }
    }
    fun changeLauncherHidden(hidden: Boolean) {
        try {
            app.packageManager.setComponentEnabledSetting(ComponentName(app, app.packageName + ".Launcher"),
                if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
            launcherHidden = hidden
        } catch (error: RuntimeException) {
            info = "修改失败" to "请重新打开设置后重试。${error.message.orEmpty()}"
        }
    }
    private fun readLauncherHidden() = app.packageManager.getComponentEnabledSetting(
        ComponentName(app, app.packageName + ".Launcher")) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun restartSystemUi() {
        flushSettings()
        if (!ScopeRestarter.BUSY.compareAndSet(false, true)) return
        restartBusy = true
        Thread({
            val message = try {
                ScopeRestarter.restart()
            } catch (error: Throwable) {
                "重启失败：${error.message ?: error.javaClass.simpleName}"
            } finally {
                ScopeRestarter.BUSY.set(false)
            }
            handler.post {
                if (!disposed) {
                    restartBusy = false
                    if (message.isNotEmpty()) info = "重启系统界面" to message
                }
            }
        }, "circluar-restart").start()
        refreshWork.run()
    }
    fun dismissNotice(neverAgain: Boolean) {
        if (neverAgain) prefs.edit {
            putBoolean("ai_modification_notice_dismissed", true)
            putString("ai_modification_notice_install_stamp", installStamp)
        }
        showNotice = false
    }
    open fun resume() {
        resumed = true
        previewActive = true
        dualSimAvailable = DualSimCapability.available(app)
        if (styles.ensureAvailable(dualSimAvailable)) observation.refreshAppearanceNow()
        launcherHidden = readLauncherHidden()
        handler.removeCallbacks(refreshWork)
        refreshWork.run()
        // Reconcile persisted values after process loss; a prior force-stop cannot flush a callback.
        if (recoverySyncPending) {
            recoverySyncPending = false
            publisher.publishNow()
        }
    }
    fun pause() { flushSettings(); recoverySyncPending = true }
    open fun stop() {
        flushSettings()
        recoverySyncPending = true
        finishExpansionPreview()
        resumed = false
        previewActive = false
        handler.removeCallbacks(refreshWork)
    }
    open fun save(out: Bundle) {
        out.putInt("page", page)
        out.putString("scene_state", PreviewSceneCodec.encode(scene, 0f, scenesExpanded))
        out.putBoolean("notice_visible", showNotice)
    }
    fun dispose() {
        disposed = true
        stop()
        observation.detach()
        publisher.close()
        handler.removeCallbacksAndMessages(null)
        runtime.resetBatteryTransfer()
    }
    @SuppressLint("MissingPermission")
    fun technologies(): List<String> {
        val selected = scene.mobileTechnology
        val available = try {
            val radio = (app.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.supportedRadioAccessFamily ?: 0L
            if (radio == 0L) NetworkTechnologyOptions.all() else {
                fun has(vararg types: Long) = types.any { radio and it != 0L }
                NetworkTechnologyOptions.supported(
                    has(TelephonyManager.NETWORK_TYPE_BITMASK_GSM, TelephonyManager.NETWORK_TYPE_BITMASK_GPRS, TelephonyManager.NETWORK_TYPE_BITMASK_EDGE, TelephonyManager.NETWORK_TYPE_BITMASK_CDMA, TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT),
                    has(TelephonyManager.NETWORK_TYPE_BITMASK_UMTS, TelephonyManager.NETWORK_TYPE_BITMASK_HSPA, TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP, TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0, TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A, TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B),
                    has(TelephonyManager.NETWORK_TYPE_BITMASK_LTE, TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA),
                    has(TelephonyManager.NETWORK_TYPE_BITMASK_NR))
            }
        } catch (_: Exception) { NetworkTechnologyOptions.all() }
        return NetworkTechnologyOptions.withSelected(available, selected).toList()
    }
}


