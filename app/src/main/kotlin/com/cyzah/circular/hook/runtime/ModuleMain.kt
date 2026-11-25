package com.cyzah.circular.hook.runtime

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.View
import com.cyzah.circular.BuildConfig
import com.cyzah.circular.Version
import com.cyzah.circular.core.diagnostics.FailureLedger
import com.cyzah.circular.core.diagnostics.MotionTrace
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.PainterConfigurator
import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.StatusFont
import com.cyzah.circular.core.graphics.animation.BatteryTransferProjection
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.FrameGate
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.core.network.NetworkSource
import com.cyzah.circular.core.network.SignalResourcePolicy
import com.cyzah.circular.data.settings.ConfigProvider
import com.cyzah.circular.data.settings.StartupAppearanceCache
import com.cyzah.circular.data.settings.StatusFontFiles
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues
import com.cyzah.circular.domain.settings.StatusColors
import com.cyzah.circular.hook.platform.Compatibility
import com.cyzah.circular.hook.platform.OemStatusAdapter
import com.cyzah.circular.hook.platform.SystemUiHookBootstrap
import com.cyzah.circular.hook.platform.SystemUiSignalHooks
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class ModuleMain : XposedModule() {
    /**
     * 双排块允许向 Wi-Fi 一侧多探出的上限（dp）：原生两张卡所在区域比「条带 + 制式」窄时，
     * 允许占用一点图标间距；超过这个量才退回只画条带。
     */
    @JvmField var apkOk: Boolean = false
    @JvmField var batteryIsland: Field? = null
    @JvmField var batteryLayoutListener: Field? = null
    @JvmField var batteryText: Field? = null
    @JvmField val nativeBatteryTints: WeakHashMap<View?, Int?> = WeakHashMap<View?, Int?>()
    @JvmField var bindingField: Field? = null
    private var burnInPending = false
    @JvmField var bypass: Boolean = false
    @JvmField var charging: Field? = null
    @JvmField var compatibility: Compatibility.Result? = null
    private var configAgain = false
    private var configAttempts = 0
    private var configLoading = false
    @JvmField var configRegistered: Boolean = false
    @JvmField var context: Context? = null
    @JvmField var control: Float = 0f
    @JvmField var panelTrackingActive: Boolean = false
    @JvmField var panelTrackingAvailable: Boolean = false
    @JvmField var displayBlocked: Method? = null
    @JvmField var displaySlot: Method? = null
    @JvmField var displayVisible: Method? = null
    @JvmField var firstLevel: Field? = null
    @JvmField var hideBatteryField: Field? = null
    @JvmField var homeBlock: Field? = null
    @JvmField var iconsField: Field? = null
    private var identityRead = false
    private val saveStartupTopology: Runnable = object : Runnable {
        override fun run() {
            if (context != null && networks != null && networks!!.running()) {
                StartupAppearanceCache.saveTopology(context, state)
            }
        }
    }
    @JvmField var ignoredSlots: Field? = null
    @JvmField var imageResource: Field? = null
    @JvmField var keepMergedShade: Boolean = false
    @JvmField var keyguardType: Class<*>? = null
    @JvmField var lastAodPaint: Long = 0
    @JvmField var lastLockPaint: Long = 0
    private val lastNativeError: Long = 0
    private var lastReport: Long = 0
    @JvmField var layoutEpoch: Long = 0
    @JvmField var layoutFrom: Field? = null
    @JvmField var legacyToAod: Field? = null
    @JvmField var level: Field? = null
    @JvmField var measureChild: Method? = null
    @JvmField var measuredDimension: Method? = null
    @JvmField var mobileSubId: Field? = null
    @JvmField var mode: Int = 0
    @JvmField var networks: NetworkSource? = null
    @JvmField var oem: OemStatusAdapter? = null
    @JvmField var pendingNativePerformance: Boolean? = null
    @JvmField var pendingNativePower: Boolean? = null
    @JvmField var performanceField: Field? = null
    @JvmField var performanceHook: Boolean = false
    @JvmField var fastChargeBadgeStatus: String = "unavailable"
    @JvmField var batteryShapePaintStatus: String = "pending"
    @JvmField var lastBatteryTransferDiagnostic: String = "pending"
    @JvmField val diagnosticTransferProjection: BatteryTransferProjection = BatteryTransferProjection()
    @JvmField var power: PowerManager? = null
    @JvmField var powerSaveField: Field? = null
    @JvmField var privacyField: Field? = null
    @JvmField var setupReady: Boolean = false
    @JvmField var shade: Float = 0f
    private val alignment = AlignmentController(this)

    fun sampleRowAlign(view6: View) {
        this.alignment.sampleRowAlign(view6)
    }

    fun rowAlignHostShift(): Float {
        return this.alignment.rowAlignHostShift()
    }

    fun rowAlignSpeedShift(): Float {
        return this.alignment.rowAlignSpeedShift()
    }

    @JvmField var shouldBeVisible: Method? = null
    @JvmField var showPercent: Boolean = false
    @JvmField var signalSnapshotHook: Boolean = false
    @JvmField var signalTagHook: Boolean = false
    @JvmField var spacingField: Field? = null

    /** 控制中心双排信号  */
    private var stackedShade = true

    /** 双卡信号点第二种样式：下面四个点分两组（左 = 卡一、右 = 卡二）  */
    @JvmField var groupedDots: Boolean = false

    /** 双卡「胶囊」样式（整条胶囊 = 电量环 + 上下各四点 + 左 Wi-Fi/制式 + 中闪电 + 右数字）  */
    @JvmField var capsuleDual: Boolean = false

    /** 双卡 DUO：圆环内水平双排四点，Wi-Fi 连接时两排滑到下方制式缺口。  */
    @JvmField var duoDual: Boolean = false

    /**
     * 胶囊预留宽度专用的「尺子」：只按状态与外观设置量算宽度，不参与绘制。
     * 必须**懒加载** —— IconPainter 的构造函数会碰字体，在 ModuleMain 构造期（模块类加载阶段）
     * 创建会让整个模块类加载失败，SystemUI 侧直接回退成原生图标。
     */
    @JvmField var capsuleRuler: IconPainter? = null
    private var transferRuntime: BatteryTransferRuntime? = null
    @JvmField var started: Boolean = false
    @JvmField var statusState: Field? = null
    @JvmField var storedBatteryIsland: Field? = null
    @JvmField var swapContents: Boolean = false
    @JvmField var toLockScreen: Field? = null
    @JvmField var updateBatteryIsland: Method? = null
    @JvmField var updateChargeAndText: Method? = null
    @JvmField var viewSignalHook: Boolean = false
    @JvmField var visibleState: Method? = null
    @JvmField var vmSignalContext: Field? = null
    @JvmField var vmSignalInteractor: Field? = null
    @JvmField var vmSignalResource: Field? = null
    @JvmField var vmSignalSub: Field? = null
    @JvmField var vmShowName: Method? = null
    @JvmField var vmShowNameValue: Method? = null
    @JvmField val main: Handler = Handler(Looper.getMainLooper())
    @JvmField val nativeStatusIconHold: NativeStatusIconHold = NativeStatusIconHold(this)
    @JvmField val drawingClips: SessionDrawingClips = SessionDrawingClips()
    private var motion: PanelMotionController? = null
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    @JvmField val frameGate: FrameGate = FrameGate()
    @JvmField val frameWork: Runnable = object : Runnable {
        override fun run() {
            this@ModuleMain.refreshClosedAnchors()
            this@ModuleMain.updateBurnIn()
        }
    }
    @JvmField val batteries: MutableMap<View?, IconSession> = IdentityHashMap()
    @JvmField val containers: MutableMap<View?, IconSession?> = IdentityHashMap()
    @JvmField val signals: MutableMap<View?, IconSession?> = IdentityHashMap()
    @JvmField val keyguards: MutableMap<View?, IconSession> = IdentityHashMap()
    @JvmField val visualCopies: MutableMap<View, IconSession> = IdentityHashMap()
    @JvmField val nativeCache: MutableMap<Int, Int> = HashMap()
    @JvmField val installed: ArrayList<HookHandle> = ArrayList<HookHandle>()
    @JvmField val state: IconState = IconState()
    @JvmField val burnIn: BurnInDrift = BurnInDrift()
    private val burnInTick: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@ModuleMain.handleBurnInTick()
        }
    }
    @JvmField val legacyLockTargets: MutableMap<View?, Boolean?> = WeakHashMap()
    @JvmField val deoptimizedListeners: MutableSet<Class<*>?> = HashSet()
    @JvmField val attachOutcomes: MutableMap<Int?, String?> = LinkedHashMap()
    @JvmField val nativeViewCache: MutableMap<Int, Int> = HashMap()
    @JvmField val signalResources: MutableMap<Int?, String?> = HashMap()
    @JvmField var signalPolicy: SignalResourcePolicy = SignalResourcePolicy(null, null)
    @JvmField val boundSignalModels: MutableMap<View?, WeakReference<Any?>?> = WeakHashMap()
    @JvmField val nativeResourceNames: MutableMap<Int?, String?> = HashMap()
    @JvmField val viewResourceNames: MutableMap<Int, String?> = HashMap()
    @JvmField val failures: FailureLedger = FailureLedger(12)
    @JvmField var signalCapture: String = "pending"
    @JvmField var vmCapture: String = "pending"
    @JvmField var signalAdapter: String = "pending"
    @JvmField var size: Int = 24
    @JvmField var weight: Int = 100
    @JvmField var wifiWeight: Int = 100
    @JvmField var technologyWeight: Int = 100
    @JvmField var wifiScale: Int = 100
    @JvmField var technologyScale: Int = 100
    @JvmField var percentScale: Int = 100
    @JvmField var percentWeight: Int = 100
    @JvmField var lowBatteryThreshold: Int = DisplayPolicy.DEFAULT_LOW_BATTERY_THRESHOLD
    @JvmField var coloured: Boolean = true
    @JvmField var lockscreen: Boolean = true
    @JvmField var aod: Boolean = true
    @JvmField var chargeColor: Int = StatusColors.CHARGING
    @JvmField var lowColor: Int = StatusColors.LOW
    @JvmField var saverColor: Int = StatusColors.SAVER
    @JvmField var performanceColor: Int = StatusColors.PERFORMANCE
    private var sessionSettings: SessionSettings = SessionSettings.Companion.defaults()
    private var controlCenterSettings: SessionSettings = SessionSettings.Companion.defaults().controlCenter()
    @Volatile internal var statusFont = StatusFont.DEFAULT
        private set
    @JvmField val lockMotionTrace: MotionTrace = MotionTrace(8)
    @JvmField val realLockTrace: MotionTrace = MotionTrace(16)
    @JvmField var firstLockReturn: String = "尚未捕获收起后锁屏绘制"
    @JvmField var lastLockTransforms: String = "尚未捕获真实锁屏变换"
    private var actualSha = "pending"
    private var systemUiVersion = "unknown"
    @JvmField var setupFailure: String = ""
    private var diagnosticRequest: String? = ""

    /** 制式文本自定义规则串（见 [TechnologyText]）  */
    private var technologyText: String? = ""
    @JvmField var lastLockSnapshot: String = "尚未捕获可见锁屏"
    @JvmField var lastAodSnapshot: String = "尚未捕获可见 AOD"
    private val retryConfig: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@ModuleMain.loadConfig()
        }
    }
    @JvmField val observer: ContentObserver = object : ContentObserver(this.main) {
        // android.database.ContentObserver
        override fun onChange(z: Boolean) {
            this@ModuleMain.loadConfig()
        }
    }

    inner class AnonymousClass25 : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@ModuleMain.changedAll()
        }
    }

    private class CursorSettingsValues(private val cursor: Cursor) : SettingsValues {
        override fun contains(key: String?): Boolean {
            return this.cursor.getColumnIndex(key) >= 0
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val column = this.cursor.getColumnIndex(key)
            return if (column >= 0 && !this.cursor.isNull(column))
                this.cursor.getInt(column)
            else
                fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val column = this.cursor.getColumnIndex(key)
            return if (column >= 0 && !this.cursor.isNull(column))
                this.cursor.getInt(column) != 0
            else
                fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            val column = this.cursor.getColumnIndex(key)
            return if (column >= 0 && !this.cursor.isNull(column))
                this.cursor.getString(column)
            else
                fallback
        }
    }

    fun ensureMotionController() {
        if (this.motion != null) {
            return
        }
        this.motion = PanelMotionController(this, this.main)
    }

    fun advanceMorph() {
        ensureMotionController()
        this.motion!!.advanceMorph()
    }

    fun synchronizeDuoWifiMotion() {
        ensureMotionController()
        this.motion!!.synchronizeDuo()
    }

    fun currentDuoWifiProgress(): Float {
        ensureMotionController()
        return this.motion!!.currentDuoWifiProgress(this.state.showWifi())
    }

    fun morphValue(): Float {
        ensureMotionController()
        return this.motion!!.morphValue()
    }

    fun batteryTransferState(): BatteryTransferState {
        ensureMotionController()
        return this.motion!!.batteryTransferState()
    }

    fun refreshBatteryTransferMotion() {
        ensureMotionController()
        this.motion!!.refreshBatteryTransfer()
    }

    fun markPanelTrackingAvailable() {
        this.panelTrackingAvailable = true
        log(4, TAG, "panelTracking=available")
    }

    fun onPanelTrackingChanged(active: Boolean) {
        this.panelTrackingAvailable = true
        if (this.panelTrackingActive == active) {
            return
        }
        this.panelTrackingActive = active
        log(4, TAG, "panelTracking=" + active)
        refreshBatteryTransferMotion()
    }

    fun panelGestureReleased(): Boolean {
        return !this.panelTrackingAvailable || !this.panelTrackingActive
    }

    fun invalidateBatteryTransfer() {
        if (this.motion != null) {
            this.motion!!.invalidateBatteryTransfer()
        }
    }

    fun recordBatteryTransferDiagnostic() {
        ensureMotionController()
        this.lastBatteryTransferDiagnostic =
            BatteryTransferDiagnostics.build(
                this,
                this.motion!!.batteryTransferState(),
                this.diagnosticTransferProjection
            )
        log(4, TAG, this.lastBatteryTransferDiagnostic)
    }

    fun markBatteryShapeOwner(from: Int) {
        if ("pending" != this.batteryShapePaintStatus) {
            return
        }
        this.batteryShapePaintStatus = "owner-host" + from
        log(4, TAG, "batteryShapePaintStatus=" + this.batteryShapePaintStatus)
    }

    fun markBatteryShapeDraw(from: Int) {
        val status = "drawn-host" + from
        if (status == this.batteryShapePaintStatus) {
            return
        }
        this.batteryShapePaintStatus = status
        log(4, TAG, "batteryShapePaintStatus=" + status)
    }

    fun setFastChargeBadgeStatus(status: String) {
        if (status == this.fastChargeBadgeStatus) {
            return
        }
        this.fastChargeBadgeStatus = status
        log(4, TAG, "fastChargeBadge=" + status)
    }

    fun transferHostAvailable(from: Int): Boolean {
        val session = transferHost(from)
        return session != null && !session.closed && !session.ambient && session.battery.isAttachedToWindow()
                && session.battery.getWindowVisibility() == View.VISIBLE && session.battery.getWidth() > 0 && session.battery.getHeight() > 0 && effectiveViewAlpha(
            session.battery
        ) > 0.001f && (from == 0 || session.battery.isShown())
    }

    fun transferHostAttached(from: Int): Boolean {
        val session = transferHost(from)
        return session != null && !session.closed && session.battery.isAttachedToWindow()
                && session.battery.getWidth() > 0 && session.battery.getHeight() > 0
    }

    fun transferScreenInteractive(): Boolean {
        return this.power == null || this.power!!.isInteractive()
    }

    fun transferKeyguardVisible(): Boolean {
        for (session in this.keyguards.values) {
            if (!session.closed && !session.ambient && session.keyguard != null && session.keyguard.isShown()) {
                return true
            }
        }
        return false
    }

    fun transferPrivacyBlocked(): Boolean {
        for (session in this.batteries.values) {
            if ((session.from == 0 || session.from == 5 || session.from == 6)
                && ("START_SHOW_PRIVACY" == session.privacy
                        || "COMPLETE_SHOW_PRIVACY" == session.privacy)
            ) {
                return true
            }
        }
        return false
    }

    fun transferHost(from: Int): IconSession? {
        for (session in this.batteries.values) {
            if (session.from == from) {
                return session
            }
        }
        return null
    }

    fun preferredControlCenterHost(): IconSession? {
        return transferHost(
            SessionPaintPolicy.preferredControlCenterHost(
                transferHostAvailable(6),
                transferHostAvailable(5)
            )
        )
    }

    fun preferredAttachedControlCenterHost(): IconSession? {
        return transferHost(
            SessionPaintPolicy.preferredControlCenterHost(
                transferHostAttached(6), transferHostAttached(5)
            )
        )
    }

    fun preferredBatteryPaintHost(): IconSession? {
        return transferHost(
            SessionPaintPolicy.preferredBatteryPaintHost(
                transferHostAvailable(6),
                transferHostAvailable(5),
                transferHostAttached(6),
                transferHostAttached(5)
            )
        )
    }

    fun sessionSettings(): SessionSettings {
        return this.sessionSettings
    }

    fun controlCenterSettings(): SessionSettings {
        return this.controlCenterSettings
    }

    fun handleBurnInTick() {
        this.burnInPending = false
        updateBurnIn()
    }

    private var hookBootstrap: SystemUiHookBootstrap? = null

    private fun hookBootstrap(): SystemUiHookBootstrap {
        if (this.hookBootstrap == null) {
            this.hookBootstrap = SystemUiHookBootstrap(this)
        }
        return this.hookBootstrap!!
    }

    override fun onPackageReady(packageReadyParam: PackageReadyParam) {
        hookBootstrap().install(packageReadyParam)
    }

    fun ensureContext(context: Context) {
        hookBootstrap().ensureContext(context)
    }

    fun rollback(installedCount: Int) {
        hookBootstrap().rollback(installedCount)
    }

    @Throws(Exception::class)
    fun deoptimizeListener(listenerClass: Class<*>) {
        hookBootstrap().deoptimizeListener(listenerClass)
    }

    @Throws(Exception::class)
    fun hookAlignment(classLoader: ClassLoader?) {
        this.alignment.hookAlignment(classLoader)
    }

    @Throws(Throwable::class)
    fun handleAlignmentProgress(chain: XposedInterface.Chain): Any? {
        return this.alignment.handleAlignmentProgress(chain)
    }

    fun applyAlignment(): Int? {
        return this.alignment.applyAlignment()
    }

    fun synchronizeAlignment(session: IconSession) {
        this.alignment.synchronizeAlignment(session)
    }

    fun globalEdge(view: View, start: Boolean): Float {
        return this.alignment.globalEdge(view, start)
    }

    fun slotEdge(session: IconSession, start: Boolean): Float {
        return this.alignment.slotEdge(session, start)
    }

    fun releaseClosedAnchors() {
        this.alignment.releaseClosedAnchors()
    }

    fun refreshClosedAnchors() {
        this.alignment.refreshClosedAnchors()
    }

    fun disableAlignment(error: Throwable) {
        this.alignment.disableAlignment(error)
    }

    fun alignmentSyncing(): Boolean {
        return this.alignment.isSynchronizing
    }

    fun captureAlignmentController(callback: Any?) {
        this.alignment.captureController(callback)
    }

    fun alignmentController(): Any? {
        return this.alignment.controller()
    }

    fun restoreAlignment(controller: Any?, offset: Int) {
        this.alignment.restoreOffset(controller, offset)
    }

    fun resetAnchorSample() {
        this.alignment.resetAnchorSample()
    }

    private var signalHooks: SystemUiSignalHooks? = null

    fun signalHooks(): SystemUiSignalHooks {
        if (this.signalHooks == null) {
            this.signalHooks = SystemUiSignalHooks(this)
        }
        return this.signalHooks!!
    }

    @Throws(Exception::class)
    fun hookKeyguard(classLoader: ClassLoader?, cls: Class<*>) {
        signalHooks().hookKeyguard(classLoader, cls)
    }

    @Throws(IllegalAccessException::class)
    fun isBrightKeyguard(view: View?, view2: View): Boolean {
        return signalHooks().isBrightKeyguard(view, view2)
    }

    fun screenInteractive(view: View?): Boolean {
        return signalHooks().screenInteractive(view)
    }

    fun keyguardStatusBarState(view: View?): Boolean {
        return signalHooks().keyguardStatusBarState(view)
    }

    fun setupSignalResources(classLoader: ClassLoader?) {
        signalHooks().setupSignalResources(classLoader)
    }

    fun refreshNativeTechnology() {
        signalHooks().refreshNativeTechnology()
    }

    fun refreshNativeTechnologyForExpansion() {
        signalHooks().refreshNativeTechnologyForExpansion(this.control, this.shade)
    }

    @Throws(Exception::class)
    fun hookNativeSignal(classLoader: ClassLoader?) {
        signalHooks().hookNativeSignal(classLoader)
    }

    @Throws(Exception::class)
    fun hookSignalSnapshot(classLoader: ClassLoader?) {
        signalHooks().hookSignalSnapshot(classLoader)
    }

    @Throws(Exception::class)
    fun hookViewSignal() {
        signalHooks().hookViewSignal()
    }

    fun owner(obj: Any?): IconSession? {
        return signalHooks().owner(obj)
    }

    fun detach(view: View?) {
        this.alignment.onHostDetached(view)
        signalHooks().detach(view)
    }

    fun scanSignal(view: View, i: Int) {
        signalHooks().scanSignal(view, i)
    }

    fun has(capability: Compatibility.Capability?): Boolean {
        return this.compatibility != null && this.compatibility!!.isAvailable(capability)
    }

    private fun checkCompatibility(): Boolean {
        return this.setupReady && (if (this.oem == null)
            has(Compatibility.Capability.CORE)
        else
            this.oem!!.installed)
    }

    private fun readApkIdentity() {
        if (!this.identityRead) {
            try {
                val packageInfo = this.context!!.getPackageManager().getPackageInfo("com.android.systemui", 0)
                this.systemUiVersion = packageInfo.versionName + " (" + packageInfo.getLongVersionCode() + ")"
                val messageDigest = MessageDigest.getInstance("SHA-256")
                FileInputStream(this.context!!.getApplicationInfo().sourceDir).use { fileInputStream ->
                    val bArr = ByteArray(65536)
                    while (true) {
                        val read = fileInputStream.read(bArr)
                        if (read == -1) {
                            break
                        }
                        messageDigest.update(bArr, 0, read)
                    }
                    val sb = StringBuilder()
                    for (b in messageDigest.digest()) {
                        sb.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255))
                    }
                    this.actualSha = sb.toString()
                    this.identityRead = true
                }
            } catch (e: Exception) {
                this.actualSha = "unavailable"
                log(5, TAG, "APK identity unavailable; runtime contract still checked", e)
            }
        }
    }

    fun restoreStartupAppearance() {
        if (this.context == null || !checkCompatibility() || this.sessionSettings.compatible()) return
        val cached = StartupAppearanceCache.restore(this.context)
        if (cached == null) return
        statusFont = StatusFont.resolve(cached.statusFont()) { null }
        applySettings(cached, true)
        if (StartupAppearanceCache.restoreTopology(this.context, this.state)) {
            for (session in this.batteries.values) session.changed()
        }
        log(
            4, TAG, ("startupAppearanceRestored style=" + this.sessionSettings.dualStyle()
                    + " dual=" + this.state.dualSim())
        )
    }

    fun loadConfig() {
        if (this.context == null) {
            return
        }
        if (this.configLoading) {
            this.configAgain = true
            return
        }
        this.configLoading = true
        if (!this.configRegistered) {
            try {
                this.context!!.getContentResolver()
                    .registerContentObserver(ConfigProvider.Companion.URI, false, this.observer)
                this.configRegistered = true
            } catch (th: Throwable) {
                this.failures.record("config", "observer-register", th)
            }
        }
        this.io.execute(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                try {
                    this@ModuleMain.loadConfigFromProvider()
                } catch (th: Throwable) {
                    this@ModuleMain.configLoading = false
                    HookFailureGuard.attempt(
                        this@ModuleMain.failures,
                        "config",
                        "load",
                        object : HookFailureGuard.Action {
                            @Throws(Throwable::class)
                            override fun run() {
                                throw th
                            }
                        })
                }
            }
        })
    }
    @Throws(Throwable::class)
    fun loadConfigFromProvider() {
        var compatibility = false
        var snapshot: SettingsSnapshot? = null
        try {
            compatibility = checkCompatibility()
            val query = this.context!!.getContentResolver().query(ConfigProvider.Companion.URI, null, null, null, null)
            if (query != null) {
                try {
                    if (query.moveToFirst()) {
                        val values: SettingsValues = CursorSettingsValues(query)
                        snapshot = SettingsSnapshot.Companion.from(values)
                        StartupAppearanceCache.save(this.context, values, compatibility)
                    }
                } finally {
                    query.close()
                }
            }
        } catch (th: Throwable) {
            if (this.configAttempts == 0) {
                log(5, TAG, "Settings temporarily unavailable; will retry", th)
            }
        }
        val fSnapshot = snapshot
        val fCompatibility = compatibility
        val font = fSnapshot?.statusFont()?.let { selection ->
            val current = statusFont
            if (current.selection == selection) current
            else StatusFont.resolve(selection) { StatusFontFiles.load(this.context!!, it) }
        }
        this.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                if (font != null) statusFont = font
                this@ModuleMain.applySettings(fSnapshot, fCompatibility)
            }
        })
        // Collect the diagnostic digest after queuing the settings result.
        if (BuildConfig.DIAGNOSTIC_REPORTS) {
            readApkIdentity()
        }
    }

    fun applySettings(snapshot: SettingsSnapshot?, compatibility: Boolean) {
        this.configLoading = false
        this.main.removeCallbacks(this.retryConfig)
        if (snapshot != null) {
            this.sessionSettings = SessionSettings.Companion.from(snapshot, compatibility)
            this.controlCenterSettings =
                this.sessionSettings.controlCenterAppearance()
            this.mode = snapshot.mode()
            this.size = snapshot.size()
            this.weight = snapshot.weight()
            this.wifiWeight = snapshot.wifiWeight()
            this.technologyWeight = snapshot.technologyWeight()
            this.wifiScale = snapshot.wifiScale()
            this.technologyScale = snapshot.technologyScale()
            this.percentScale = snapshot.percentScale()
            this.percentWeight = snapshot.percentWeight()
            this.swapContents = snapshot.swapContents()
            this.keepMergedShade = snapshot.keepMergedShade()
            this.stackedShade = snapshot.stackedShade()
            this.groupedDots = snapshot.groupedDots()
            this.capsuleDual = snapshot.capsuleDual()
            this.duoDual = snapshot.duoDual()
            if (SessionPaintPolicy.resetsNativeRowOwnership(
                    this.stackedShade, this.keepMergedShade
                )
            ) {
                this.nativeStatusIconHold.reset()
                ensureMotionController()
                this.motion!!.snapMorph(PanelMotionFrame.morph(this.control))
            }
            synchronizeDuoWifiMotion()
            this.chargeColor = snapshot.chargeColor()
            this.lowColor = snapshot.lowColor()
            this.saverColor = snapshot.saverColor()
            this.performanceColor = snapshot.performanceColor()
            this.lowBatteryThreshold = snapshot.lowBatteryThreshold()
            this.coloured = snapshot.coloured()
            this.lockscreen = snapshot.lockscreen()
            this.aod = snapshot.aod()
            this.showPercent = snapshot.showPercent()
            this.apkOk = compatibility
            this.configAttempts = 0
            this.diagnosticRequest = snapshot.diagnosticRequest()
            this.technologyText = snapshot.technologyText()
            if (this.networks != null) {
                this.networks!!.setTechnologyText(this.technologyText)
            }
            if (this.mode <= 0 || !this.apkOk || (this.batteries.isEmpty() && (this.oem == null || !this.oem!!.hasHosts()))) {
                this.networks!!.stop()
            } else {
                this.networks!!.start()
            }
            for (session in this.batteries.values) {
                session.failedUntil = 0L
                session.changed()
                session.requestLayout()
            }
            if (this.oem != null) {
                this.oem!!.update()
            }
            report(true)
        } else {
            scheduleConfigRetry()
        }
        if (this.configAgain) {
            this.configAgain = false
            loadConfig()
        } else if (snapshot != null && !this.configRegistered) {
            this.main.postDelayed(this.retryConfig, 5000L)
        }
    }

    private fun scheduleConfigRetry() {
        val attempts = this.configAttempts
        this.configAttempts = attempts + 1
        this.main.postDelayed(
            this.retryConfig,
            min(BurnInDrift.Companion.INTERVAL_MS, (2000 shl min(5, attempts)).toLong())
        )
    }

    fun changedAll() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            this.main.post(AnonymousClass25())
            return
        }
        this.nativeStatusIconHold.networkChanged()
        this.main.removeCallbacks(this.saveStartupTopology)
        this.main.postDelayed(this.saveStartupTopology, 200L)
        synchronizeDuoWifiMotion()
        refreshBatteryTransferMotion()
        val it = this.batteries.values.iterator()
        while (it.hasNext()) {
            it.next().changed()
        }
        if (this.oem != null) {
            this.oem!!.update()
        }
        report(false)
    }

    fun updateBurnIn() {
        val z: Boolean
        val it = this.batteries.values.iterator()
        while (true) {
            if (!it.hasNext()) {
                z = false
                break
            }
            val next = it.next()
            if (next.draw
                && !next.closed && next.battery.isShown()
                && next.battery.getWindowVisibility() == View.VISIBLE && !next.ambient && next.progress > 0.0f && next.progress < 1.0f
            ) {
                z = true
                break
            }
        }
        if (this.burnIn.update(SystemClock.elapsedRealtime(), z)) {
            for (session in this.batteries.values) {
                if (session.draw
                    && !session.closed && session.battery.isShown()
                    && session.battery.getWindowVisibility() == View.VISIBLE
                ) {
                    session.battery.invalidate()
                }
            }
        }
        scheduleBurnIn()
    }

    fun scheduleBurnIn() {
        val z: Boolean
        val it = this.batteries.values.iterator()
        while (true) {
            if (!it.hasNext()) {
                z = false
                break
            }
            val next = it.next()
            val display = next.battery.getDisplay()
            if (BurnInDrift.Companion.shouldSchedule(
                    next.battery.isAttachedToWindow(),
                    next.battery.isShown()
                            && next.battery.getWindowVisibility() == View.VISIBLE,
                    display != null && display.getState() == Display.STATE_ON,
                    next.draw && !next.closed && !next.ambient,
                    next.progress
                )
            ) {
                z = true
                break
            }
        }
        val z2 = this.burnInPending
        if (z) {
            if (!z2) {
                this.burnInPending = true
                this.main.postDelayed(this.burnInTick, BurnInDrift.Companion.delay(SystemClock.elapsedRealtime()))
                return
            }
            return
        }
        if (z2) {
            this.main.removeCallbacks(this.burnInTick)
            this.burnInPending = false
        }
    }

    fun report(z: Boolean) {
        if (!BuildConfig.DIAGNOSTIC_REPORTS || this.context == null) {
            return
        }
        val jElapsedRealtime = SystemClock.elapsedRealtime()
        if (z || jElapsedRealtime - this.lastReport >= BurnInDrift.Companion.INTERVAL_MS) {
            this.lastReport = jElapsedRealtime
            if (this.oem != null) {
                reportOem()
                return
            }
            val strClassification =
                if (this.compatibility == null) "PENDING" else this.compatibility!!.classification(this.actualSha)
            val sbAppend =
                StringBuilder(if (!this.apkOk) "当前系统未适配" else if (strClassification == "KNOWN_APK") "已识别已验证的系统文件" else "基础接口检查通过，实机兼容性待确认").append(
                    "\n版本 "
                ).append(
                    Version.NAME
                ).append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            sbAppend.append("\nSystemUI ").append(this.systemUiVersion).append("\nprofile=")
                .append(this.compatibility!!.profile()).append(" compatibility=").append(strClassification)
            sbAppend.append("\ncore=").append(this.setupReady)
                .append(" motion=").append(has(Compatibility.Capability.MOTION))
                .append(" nativeHook=").append(
                    has(
                        Compatibility.Capability.NATIVE_SIGNAL
                    )
                )
            sbAppend.append("\nhookOrder=")
                .append(
                    if (this.hookBootstrap == null)
                        "pending"
                    else
                        this.hookBootstrap!!.hookOrderSummary()
                )
            sbAppend.append(" keyguardHook=").append(has(Compatibility.Capability.KEYGUARD))
            sbAppend.append(" keyguardEvents=")
                .append(has(Compatibility.Capability.KEYGUARD_EVENTS))
                .append(" aodEvents=")
                .append(has(Compatibility.Capability.AOD_EVENTS))
            sbAppend.append(" closedAnchorHook=")
                .append(
                    has(Compatibility.Capability.CLOSED_ANCHOR)
                            && has(Compatibility.Capability.MOTION_ALIGNMENT)
                            && has(Compatibility.Capability.KEYGUARD)
                )
                .append(" copies=").append(this.visualCopies.size)
            sbAppend.append(" privacyMotion=")
                .append(has(Compatibility.Capability.PRIVACY))
            sbAppend.append(" alignmentHook=")
                .append(
                    has(Compatibility.Capability.MOTION)
                            && has(Compatibility.Capability.MOTION_ALIGNMENT)
                )
            sbAppend.append(" handoffHook=")
                .append(has(Compatibility.Capability.HANDOFF))
                .append(" viewSignalHook=").append(this.viewSignalHook)
                .append(" chargingIslandHook=")
                .append(has(Compatibility.Capability.CHARGING_ISLAND))
            sbAppend.append(" handoffHints=")
                .append(has(Compatibility.Capability.HANDOFF_HINTS))
                .append(" powerHook=")
                .append(has(Compatibility.Capability.BATTERY_POWER))
                .append(" performanceHook=").append(this.performanceHook)
            if (this.compatibility != null) {
                sbAppend.append("\n接口诊断：").append(this.compatibility!!.details())
            }
            sbAppend.append("\nfailures=").append(this.failures.diagnostic())
            sbAppend.append("\nattachRoles=").append(this.attachOutcomes)
            sbAppend.append("\nwifiGlyph=hyperos-outline tintAlpha=system")
            sbAppend.append("\nsize=").append(this.size).append("dp wifiScale=").append(this.wifiScale)
                .append("% technologyScale=").append(this.technologyScale).append('%')
            sbAppend.append(" percentScale=").append(this.percentScale).append("% percentBase=130% percentWeight=")
                .append(this.percentWeight)
                .append("% percentWeightBase=130% technologyWeightBase=144% appearanceBase=1.4.2x120% technologyFollowsIcon=false technologyTypefaceWeight=")
                .append(
                    AppearanceScale.technologyTextWeight(this.technologyWeight)
                ).append(" swapContents=").append(this.swapContents)
            sbAppend.append("\nchargeColor=").append(StatusColors.hex(this.chargeColor)).append(" lowColor=").append(
                StatusColors.hex(this.lowColor)
            ).append(" saverColor=").append(StatusColors.hex(this.saverColor)).append(" performanceColor=").append(
                StatusColors.hex(this.performanceColor)
            )
            sbAppend.append("\nburnIn=true phase=").append(this.burnIn.phase()).append(" drift=")
                .append(this.burnIn.x()).append(',').append(this.burnIn.y()).append(" tickPending=")
                .append(this.burnInPending).append(" interval=60s aodWake=false")
            sbAppend.append("\nsignalAdapter=").append(this.signalAdapter).append(" snapshotHook=")
                .append(this.signalSnapshotHook).append(" tagHook=").append(this.signalTagHook)
                .append(" imageResource=").append(this.imageResource != null)
            sbAppend.append("\nsignalView=").append(this.signalCapture).append(" signalVM=").append(this.vmCapture)
            sbAppend.append("\nweight=").append(this.weight).append("% wifiWeight=").append(this.wifiWeight)
                .append("% technologyWeight=").append(this.technologyWeight).append("% lockscreen=")
                .append(this.lockscreen).append(" fullAod=").append(this.aod)
            sbAppend.append(" lowBatteryBelow=").append(this.lowBatteryThreshold).append("% colour=")
                .append(this.coloured)
            sbAppend.append(" showPercent=").append(this.showPercent)
            sbAppend.append("\nkeepMergedShade=").append(this.keepMergedShade)
            sbAppend.append("\nstackedShade=").append(this.stackedShade).append(" stackedGap=")
                .append(StackedSignals.Companion.DEFAULT_GAP).append(" stackedReady=").append(this.state.stackedReady())
                .append(" stackedBars=").append(this.state.card1Bars()).append('/').append(this.state.card2Bars())
                .append(" stackedSlots=").append(this.state.mobileSlot).append('/').append(this.state.secondarySlot)
                .append(" stackedSubIds=").append(this.state.subId).append('/').append(this.state.secondarySubId)
            sbAppend.append("\nrawShade=").append(this.shade).append(" rawControl=").append(this.control)
            sbAppend.append("\nbatteryTransfer=")
                .append(
                    BatteryTransferDiagnostics.build(
                        this,
                        batteryTransferState(),
                        this.diagnosticTransferProjection
                    )
                )
            sbAppend.append("\nbatteryShapePaint=")
                .append(this.batteryShapePaintStatus)
            sbAppend.append("\nsha256=").append(this.actualSha).append("\nmode=").append(this.mode).append(" hosts=")
                .append(this.batteries.size)
            sbAppend.append(" healthy=").append(this.networks != null && this.networks!!.healthy)
                .append(" nativeSignal=").append(this.state.nativeMobile).append(" rawLevel=")
                .append(this.state.rawMobileLevel).append(" sub=").append(this.state.subId).append('\n')
                .append(this.state.description())
            if (this.networks != null) {
                sbAppend.append('\n').append(this.networks!!.diagnostic())
                sbAppend.append('\n').append(this.networks!!.technologyDiagnostic())
            }
            for (session in this.batteries.values) {
                sbAppend.append("\nhost=").append(session.from).append(" draw=").append(session.draw).append(" merge=")
                    .append(session.merge).append(" ambient=").append(session.ambient).append(" progress=")
                    .append(session.progress).append(" stacked=").append(session.stackedActive).append(" stackedAlpha=")
                    .append(session.stackedAlpha).append(" stackedArea=").append(session.stackedLeft).append(',')
                    .append(session.stackedRight).append(" width=").append(session.battery.getWidth())
                    .append(" height=").append(session.battery.getHeight()).append(" requestedPx=")
                    .append(session.dp(this.size)).append(" parentLimit=").append(session.parentWidthLimit).append('x')
                    .append(session.parentHeightLimit).append(" sizeAdjusted=").append(session.sizeAdjusted)
                    .append(" paintDrift=").append(session.driftX).append(',').append(session.driftY).append(" slots=")
                    .append(session.networkWidth).append(" reason=").append(session.reason).append(" state=")
                    .append(session.cachedStatus).append(" batteryVis=").append(session.battery.getVisibility())
                    .append(" rootShown=").append(session.rootShown).append(" keyguardPrepared=")
                    .append(session.keyguardHandoff.retained()).append(" tint=#").append(
                        Integer.toHexString(session.lastTint)
                    ).append(session.islandSummary())
            }
            for (sessionProbe in this.batteries.values) {
                if (sessionProbe.keyguard != null) {
                    sbAppend.append("\nkeyguardProbe=").append(sessionProbe.keyguardProbe())
                }
            }
            sbAppend.append("\n最近可见锁屏：").append(this.lastLockSnapshot).append("\n最近锁屏绘制：")
                .append(stamp(this.lastLockPaint))
            sbAppend.append("\n最近可见 AOD：").append(this.lastAodSnapshot).append("\n最近 AOD 绘制：")
                .append(stamp(this.lastAodPaint))
            sbAppend.append("\nlockMotion=").append(this.alignment.motionGate()).append("\n最近锁屏轨迹对齐：")
                .append(this.alignment.lastClosedAnchor()).append("\n最近非零位置校正：")
                .append(this.alignment.lastClosedCorrection())
            sbAppend.append("\nlockGeometry=compact keyguardReflowHook=")
                .append(has(Compatibility.Capability.HANDOFF))
                .append("\n最近收起后锁屏首次绘制：").append(this.firstLockReturn)
            sbAppend.append("\n真实锁屏变换：").append(this.lastLockTransforms)
            sbAppend.append("\n最近真实锁屏帧（h=1）：\n").append(this.realLockTrace.text())
            sbAppend.append("\n最近锁屏手势帧（p=0..1000）：\n").append(this.lockMotionTrace.text())
            if (!this.setupFailure.isEmpty()) {
                sbAppend.append("\nsetupFailure=").append(this.setupFailure)
            }
            val strSubstring = if (sbAppend.length > 7800) sbAppend.substring(0, 7800) else sbAppend.toString()
            val str = this.diagnosticRequest
            this.io.execute(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@ModuleMain.publishDiagnosticReport(strSubstring, str)
                }
            })
        }
    }

    private fun publishDiagnosticReport(str: String?, str2: String?) {
        try {
            val bundle = Bundle()
            bundle.putString("text", str)
            bundle.putString("request_token", str2)
            this.context!!.getContentResolver().call(ConfigProvider.Companion.URI, "report", null as String?, bundle)
        } catch (th: Throwable) {
            this.failures.record("report", "module-copy", th)
        }
    }

    fun oemState(): IconState {
        return this.state
    }

    /**
     * 双排信号某一排的制式文本。
     * 
     * @param z true = 第一排（卡槽 0 / 卡一），false = 第二排（卡槽 1 / 卡二）
     */
    fun stackedLabel(iconState: IconState, z: Boolean): String? {
        if (this.networks == null) {
            return null
        }
        val i =
            if (z) (if (iconState.mobileIsSlot0()) iconState.subId else iconState.secondarySubId) else (if (iconState.mobileIsSlot0()) iconState.secondarySubId else iconState.subId)
        if (i < 0 || !(if (z) iconState.card1InService() else iconState.card2InService())) {
            return null
        }
        return StackedSignals.Companion.clip(this.networks!!.technology(i))
    }

    /** 单卡时那一排的制式文本（就是唯一那张卡）  */
    fun stackedLabel(iconState: IconState): String? {
        if (this.networks == null) {
            return null
        }
        if (iconState.subId < 0 || !iconState.inService) {
            return null
        }
        return StackedSignals.Companion.clip(this.networks!!.technology(iconState.subId))
    }

    /** 双排信号是否真的生效：开关打开、且没有勾「下拉后保持三合一」（两者互斥）  */
    fun stackedOn(): Boolean {
        return this.stackedShade && !this.keepMergedShade
    }

    fun oemKeepMergedShade(): Boolean {
        return this.keepMergedShade
    }

    fun oemContext(context: Context) {
        ensureContext(context)
        loadConfig()
    }

    fun oemEnabled(): Boolean {
        return this.mode == 2 && this.apkOk
    }

    fun oemReady(): Boolean {
        return this.state.readyForMerge() && this.networks != null && this.networks!!.uiReady()
    }

    fun oemLockscreen(): Boolean {
        return this.lockscreen
    }

    fun oemColoured(): Boolean {
        return this.coloured
    }

    fun oemSize(): Int {
        return this.size
    }

    fun oemWeight(): Int {
        return this.weight
    }

    fun oemHostsChanged() {
        if (this.networks != null) {
            if (oemEnabled() && this.oem != null && this.oem!!.hasHosts()) {
                this.networks!!.start()
            } else {
                this.networks!!.stop()
            }
        }
    }

    fun oemSignal(i: Int, i2: Int) {
        if (this.networks != null) {
            this.networks!!.setNativeSignal(i, i2)
        }
    }

    /**
     * 胶囊模式需要给图标视图预留的宽度（120 坐标系单位）。
     * 用「尺子」按当前状态与外观设置实时量算 —— 胶囊宽度随内容（Wi-Fi / 制式 / 位数）变，
     * 但左边那个图标的间距保持固定，右边缘也仍然贴着官方电池的最右侧。
     */
    fun capsuleReserveUnits(): Float {
        return capsuleRuler(this.sessionSettings)!!
            .capsuleReserveWidth(this.state)
    }

    /** 懒加载并配置好「尺子」：ModuleMain 构造期不能 new IconPainter（会让整个模块类加载失败）  */
    fun capsuleRuler(settings: SessionSettings): IconPainter {
        val ruler = this.capsuleRuler ?: IconPainter().also { this.capsuleRuler = it }
        PainterConfigurator.apply(
            ruler,
            settings.appearance(),
            currentDuoWifiProgress()
        )
        ruler.setStatusFont(statusFont)
        ruler.setContentPixelUnit(contentPixelUnit(settings))
        return ruler
    }

    fun contentPixelUnit(settings: SessionSettings): Float {
        var pixels = Math.round(
            settings.size() * (if (this.context == null)
                1.0f
            else
                this.context!!.getResources().getDisplayMetrics().density)
        ).toFloat()
        val source = transferHost(0)
        if (source != null && source.battery.getHeight() > 0) pixels = min(pixels, source.battery.getHeight().toFloat())
        return max(.001f, pixels / 120.0f)
    }

    fun transferRingGeometry(iconState: IconState): RingGeometry {
        if (this.transferRuntime == null) {
            this.transferRuntime = BatteryTransferRuntime(this)
        }
        return this.transferRuntime!!.geometry(iconState)
    }

    fun oemPainter(iconPainter: IconPainter) {
        PainterConfigurator.apply(
            iconPainter,
            this.sessionSettings.appearance(),
            currentDuoWifiProgress()
        )
        iconPainter.setStatusFont(statusFont)
        iconPainter.setContentPixelUnit(contentPixelUnit(this.sessionSettings))
    }

    private fun reportOem() {
        val str =
            (if (this.apkOk) "适配接口检查通过，实机效果待确认" else "适配未启用，保留系统原生显示") + "\n版本 " + Version.NAME + " · " + Build.MANUFACTURER + " " + Build.MODEL + "\nSystemUI " + this.systemUiVersion + "\n" + this.oem!!.diagnostic() + "\n本适配支持状态栏、亮屏锁屏；控制中心独立副本和 AOD 使用原生显示。\nsize=" + this.size + "dp weight=" + this.weight + "% wifiScale=" + this.wifiScale + "% wifiWeight=" + this.wifiWeight + "% technologyScale=" + this.technologyScale + "% technologyWeight=" + this.technologyWeight + "%\npercentScale=" + this.percentScale + "% percentWeight=" + this.percentWeight + "% swapContents=" + this.swapContents + "\nmode=" + this.mode + " healthy=" + (this.networks != null && this.networks!!.healthy) + " nativeSignal=" + this.state.nativeMobile + " sub=" + this.state.subId + "\n" + this.state.description() + "\n" + (if (this.networks == null) "networks=pending" else this.networks!!.diagnostic()) + "\nsha256=" + this.actualSha
        val str2 = this.diagnosticRequest
        this.io.execute(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                this@ModuleMain.publishOemDiagnosticReport(str, str2)
            }
        })
    }

    private fun publishOemDiagnosticReport(str: String?, str2: String?) {
        try {
            val bundle = Bundle()
            bundle.putString("text", str)
            bundle.putString("request_token", str2)
            this.context!!.getContentResolver().call(ConfigProvider.Companion.URI, "report", null as String?, bundle)
        } catch (th: Throwable) {
            this.failures.record("report", "oem-copy", th)
        }
    }

    companion object {
        const val BATTERY: String = "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
        const val CONTAINER: String = "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
        const val FAKE_STATUS: String = "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
        const val MOBILE: String = "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        const val MODERN: String = "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
        const val PANEL_STATUS: String = "com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBarIcon"
        const val TAG: String = "Circular"
        const val WIFI: String = "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
        private fun effectiveViewAlpha(view: View?): Float {
            var alpha = 1.0f
            var current = view
            while (current != null && alpha > 0.001f) {
                alpha *= Motion.clamp(current.getAlpha())
                current = if (current.getParent() is View)
                    current.getParent() as View?
                else
                    null
            }
            return alpha
        }

        @JvmStatic @Throws(Exception::class)
        fun method(cls: Class<*>, str: String, vararg clsArr: Class<*>?): Method {
            val declaredMethod = cls.getDeclaredMethod(str, *clsArr)
            declaredMethod.setAccessible(true)
            return declaredMethod
        }

        @JvmStatic @Throws(Exception::class)
        fun field(cls: Class<*>, str: String): Field {
            val declaredField = cls.getDeclaredField(str)
            declaredField.setAccessible(true)
            return declaredField
        }

        @JvmStatic @Throws(Exception::class)
        fun cls(classLoader: ClassLoader?, str: String?): Class<*> {
            return Class.forName(str, false, classLoader)
        }

        @JvmStatic fun stamp(j: Long): String {
            return if (j == 0L) "尚未绘制" else SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(Date(j))
        }
    }
}
