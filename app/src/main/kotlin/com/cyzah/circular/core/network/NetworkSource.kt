package com.cyzah.circular.core.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import android.telephony.TelephonyManager
import android.util.Log
import com.cyzah.circular.core.diagnostics.FailureLedger
import com.cyzah.circular.core.graphics.TechnologyText
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.StartupTransaction
import com.cyzah.circular.core.graphics.animation.WifiDisconnectTransition
import com.cyzah.circular.core.network.WifiLevelPolicy
import com.cyzah.circular.data.settings.StartupAppearanceCache
import com.cyzah.circular.hook.platform.ObserverRegistration
import com.cyzah.circular.hook.runtime.BurnInDrift
import com.cyzah.circular.hook.runtime.NativeTechnologyState
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.HashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Runs inside SystemUI's privileged process. Optional privileged reads keep their existing
 * capability checks and throwable fallbacks.
 */
@SuppressLint("MissingPermission")
class NetworkSource
    (private val context: Context, private val state: IconState, private val changed: Runnable) :
    WifiNetworkMonitor.Host, MobileNetworkMonitor.Host {
    private var cell: MobileNetworkMonitor.Cell? = null
    private var connectivity: ConnectivityManager? = null
    private var lastError: Long = 0
    private var nativePowerSave: Boolean? = null
    private var wifiMonitor: WifiNetworkMonitor? = null
    @JvmField val performance: PerformanceModeSource
    private var phone: TelephonyManager? = null
    private var radio: MobileNetworkMonitor.Radio? = null
    private var radioRegistered = false
    private var receiverRegistered = false
    private val secondaryBinding: SecondarySubscriptionBinding<SecondaryConnection>
    private val dataRegistration: ObserverRegistration
    private var nativeTechnologyRefresh: Runnable? = null
    private var requested = false
    private var retryAttempt = 0
    private var retryPending = false
    private var running = false
    private var subsRegistered = false
    private var subscriptions: SubscriptionManager? = null
    private var wifi: WifiManager? = null
    private var wifiFrequency = 0
    private val main = Handler(Looper.getMainLooper())
    private val mobileMonitor = MobileNetworkMonitor(this)
    private var nativeServiceAuthoritative = true
    private var powerSource = "pending"
    private val technology = MobileTechnology()
    private var technologySource = "pending"

    /** 系统原生状态栏正在显示的制式文本（subId → 文本）  */
    private val nativeTechnologies = NativeTechnologyState()
    private var technologyText: TechnologyText = TechnologyText.Companion.parse("")
    private var technologyTextSpec = ""
    private var technologyTextSource = "pending"
    @JvmField var healthy: Boolean = true
    private val uiReadiness = NetworkReadinessHold()
    private val wifiDisconnect = WifiDisconnectTransition()
    private var rawLevel = -1
    private val wifiRecovery = WifiRecovery()
    private val wifiDisplayLevel = WifiDisplayLevelPolicy()
    private var wifiMeasuredLevel = -1
    private var wifiSource = "pending"
    private var wifiRssi = -127
    @JvmField var levelProbe: String? = "pending"
    private val failures = FailureLedger(12)
    private val nativeLevels = HashMap<Int?, Int?>()
    private val viewLevels = HashMap<Int?, Int?>()
    private val viewResources = HashMap<Int?, String?>()
    private var mobileSource = "platform"
    private val retry: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@NetworkSource.retryNetworkBinding()
        }
    }
    private val wifiRetry: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@NetworkSource.retryWifi()
        }
    }
    private val dataObserver: ContentObserver = object : ContentObserver(this.main) {
        // android.database.ContentObserver
        override fun onChange(z: Boolean) {
            if (this@NetworkSource.running) {
                this@NetworkSource.refreshDataEnabled()
            }
        }
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        // android.content.BroadcastReceiver
        override fun onReceive(context: Context?, intent: Intent) {
            if (this@NetworkSource.running) {
                try {
                    this@NetworkSource.refreshPowerSave()
                    val zEquals = "android.intent.action.USER_SWITCHED" == intent.getAction()
                    val networkSource = this@NetworkSource
                    if (zEquals) {
                        networkSource.performance.userChanged()
                        this@NetworkSource.changed.run()
                        return
                    }
                    networkSource.performance.refresh()
                    val zEquals2 = "android.os.action.POWER_SAVE_MODE_CHANGED" == intent.getAction()
                    val networkSource2 = this@NetworkSource
                    if (zEquals2) {
                        networkSource2.changed.run()
                        return
                    }
                    networkSource2.state.airplane = Settings.Global.getInt(
                        this@NetworkSource.context.getContentResolver(),
                        "airplane_mode_on",
                        0
                    ) != 0
                    this@NetworkSource.refreshWifi(true)
                    this@NetworkSource.refreshDataEnabled()
                    if ("android.intent.action.AIRPLANE_MODE" == intent.getAction() || DEFAULT_DATA_CHANGED == intent.getAction()) {
                        this@NetworkSource.bindPhone()
                        this@NetworkSource.bindSecondary()
                    }
                    this@NetworkSource.refreshTechnology()
                    this@NetworkSource.recovered()
                    this@NetworkSource.changed.run()
                } catch (th: Throwable) {
                    this@NetworkSource.error("Network broadcast", th)
                }
            }
        }
    }
    private val subsListener: OnSubscriptionsChangedListener = object : OnSubscriptionsChangedListener() {
        // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
        override fun onSubscriptionsChanged() {
            if (this@NetworkSource.running) {
                this@NetworkSource.bindPhone()
                this@NetworkSource.bindSecondary()
            }
        }
    }
    private val wifiLevels: WifiLevelPolicy

    fun preservePlatformService() {
        this.nativeServiceAuthoritative = false
    }

    /**
     * 读取移动数据开关状态（用于「只有开启数据网络时才显示制式」）。
     * 优先使用 TelephonyManager.isDataEnabled()，失败则退回 Settings.Global 的 mobile_data / mobile_data<subId> 键。
    </subId> */
    fun refreshDataEnabled() {
        var z = true
        try {
            if (this.phone != null) {
                z = this.phone!!.isDataEnabled()
            } else {
                z = readMobileDataSetting(this.state.subId)
            }
        } catch (th: Throwable) {
            try {
                z = readMobileDataSetting(this.state.subId)
            } catch (th2: Throwable) {
                z = true
            }
        }
        if (this.state.dataEnabled != z) {
            this.state.dataEnabled = z
            if (this.running) {
                this.changed.run()
            }
        }
    }

    private fun readMobileDataSetting(i: Int): Boolean {
        var iSlotIndex = -1
        try {
            if (this.subscriptions != null && i >= 0) {
                iSlotIndex = SubscriptionManager.getSlotIndex(i)
            }
        } catch (th: Throwable) {
            this.failures.record("settings", "subscription-slot", th)
        }
        // 小米双卡：按卡槽写入 mobile_data<slot>；AOSP：按订阅写入 mobile_data<subId>；最后回退到 mobile_data
        if (iSlotIndex >= 0) {
            val i2 = Settings.Global.getInt(this.context.getContentResolver(), "mobile_data" + iSlotIndex, -1)
            if (i2 >= 0) {
                return i2 != 0
            }
        }
        if (i >= 0) {
            val i3 = Settings.Global.getInt(this.context.getContentResolver(), "mobile_data" + i, -1)
            if (i3 >= 0) {
                return i3 != 0
            }
        }
        return Settings.Global.getInt(this.context.getContentResolver(), "mobile_data", 1) != 0
    }

    fun setNativePowerSave(z: Boolean) {
        this.nativePowerSave = z
        this.powerSource = "native-battery"
        if (this.state.powerSave != z) {
            this.state.powerSave = z
            if (this.running) {
                this.changed.run()
            }
        }
    }

    fun seedNativePowerSave(z: Boolean) {
        if (this.nativePowerSave == null) {
            setNativePowerSave(z)
        }
    }

    fun refreshPowerSave() {
        if (this.nativePowerSave != null) {
            this.state.powerSave = this.nativePowerSave!!
            return
        }
        try {
            val powerManager = this.context.getSystemService<PowerManager?>(PowerManager::class.java)
            if (powerManager != null) {
                this.state.powerSave = powerManager.isPowerSaveMode()
                this.powerSource = "power-manager"
            }
        } catch (e: RuntimeException) {
            this.powerSource = "unavailable"
        }
    }

    fun recordViewResource(i: Int, str: String?) {
        if (this.viewResources.size > 8) {
            this.viewResources.clear()
        }
        this.viewResources.put(i, str)
    }

    fun setViewSignal(i: Int, i2: Int) {
        if (i < 0) {
            return
        }
        if (this.viewLevels.size > 8) {
            this.viewLevels.clear()
        }
        val map = this.viewLevels
        if (i2 == -1) {
            map.remove(i)
        } else {
            map.put(i, i2)
        }
        if (!this.running || this.state.subId != i) {
            return
        }
        applyLevel()
        recovered()
        this.changed.run()
    }

    fun retryNetworkBinding() {
        this.retryPending = false
        if (this.requested) {
            // The retained monitor owns its finite cleanup schedule, including exhaustion.
            if (awaitingWifiCleanup()) return
            stopInternal()
            if (!awaitingWifiCleanup()) startInternal()
        }
    }

    private fun awaitingWifiCleanup(): Boolean {
        return this.wifiMonitor != null && this.wifiMonitor!!.ownsRegistration()
                && !this.wifiMonitor!!.isStarted
    }

    override fun cleanupComplete() {
        if (!this.running) scheduleBindingRetry()
    }

    fun retryWifi() {
        this.wifiRecovery.fired()
        if (!this.running || !this.requested) {
            return
        }
        try {
            refreshWifi(true)
            recovered()
            this.changed.run()
        } catch (error: Throwable) {
            error("Wi-Fi recovery", error)
        }
    }

    private fun updateWifiRetry() {
        if (!this.running || !this.requested || this.state.wifiKnown && !this.wifiDisplayLevel.needsMeasurement()) {
            this.main.removeCallbacks(this.wifiRetry)
            this.wifiRecovery.reset()
        } else {
            val jSchedule = this.wifiRecovery.schedule()
            if (jSchedule >= 0) {
                this.main.postDelayed(this.wifiRetry, jSchedule)
            }
        }
    }

    fun diagnostic(): String {
        return "wifiSource=" + this.wifiSource + " wifiRssi=" + this.wifiRssi + " wifiFrequency=" + this.wifiFrequency + " wifiMeasuredLevel=" + this.wifiMeasuredLevel + " wifiMeasurementPending=" + this.wifiDisplayLevel.needsMeasurement() + " wifiLevelSource=" + this.wifiLevels.source() + " trackerLevel=" + this.wifiLevels.hasTracker() + " miuiLevel=" + this.wifiLevels.hasMiui() + " wifiSamples=" + (if (this.wifiMonitor == null) 0 else this.wifiMonitor!!.sampleCount()) + " wifiRetry=" + this.wifiRecovery.pending() + " wifiAttempts=" + this.wifiRecovery.attempts() + " listenerRetry=" + this.retryPending + " mobileSource=" + this.mobileSource + " viewResource=" + this.viewResources.get(
            this.state.subId
        ) + " technology=" + this.state.mobileTechnology + " rat=" + this.technology.networkType() + " ratOverride=" + this.technology.overrideType() + " technologySource=" + this.technologySource + " radioCallback=" + this.radioRegistered + " dataEnabled=" + this.state.dataEnabled + " secondarySub=" + this.state.secondarySubId + " secondarySim=" + this.state.secondarySimPresent + " secondaryKnown=" + this.state.secondaryKnown + " secondaryInService=" + this.state.secondaryInService + " secondaryLevel=" + this.state.secondaryLevel + " levelProbe=" + this.levelProbe + " powerSave=" + this.state.powerSave + " powerSource=" + this.powerSource + " failures=" + this.failures.diagnostic() + " " + this.performance.diagnostic()
    }

    fun applyTechnology() {
        this.state.mobileTechnology =
            if (this.state.simPresent && this.state.inService && !this.state.airplane) resolveTechnology() else ""
    }

    /** 制式文本诊断：原生文本 + 自定义规则  */
    fun technologyDiagnostic(): String {
        return "nativeTechnology=" + this.nativeTechnologies + " technologyTextSource=" + this.technologyTextSource + " technologyText=" + this.technologyTextSpec + " mobileSlot=" + this.state.mobileSlot + " secondarySlot=" + this.state.secondarySlot
    }

    /**
     * 制式文本：优先用系统自己算出来的字（SystemUI 的 MiuiMobileIconVMImpl.showName，
     * 与原生状态栏显示的完全一致），取不到再退回模块自己的映射表，最后套用用户自定义规则。
     */
    private fun resolveTechnology(): String {
        val str = this.nativeTechnologies.label(this.state.subId)
        if (str != null && !str.isEmpty()) {
            this.technologyTextSource = "native"
            return checkNotNull(this.technologyText.apply(str))
        }
        this.technologyTextSource = "module"
        return checkNotNull(this.technologyText.apply(this.technology.label()))
    }

    /** 记录某个 subId 下系统原生状态栏正在显示的制式文本  */
    @JvmOverloads
    fun nativeTechnology(i: Int, str: String?, authoritative: Boolean = false) {
        val before = this.nativeTechnologies.label(i)
        val text: String = TechnologyText.Companion.sanitize(str)
        if (authoritative) this.nativeTechnologies.emitted(i, text)
        else this.nativeTechnologies.sample(i, text)
        val after = this.nativeTechnologies.label(i)
        if (before != after) {
            applyTechnology()
            // 每一张卡的制式文本都会改控制中心双排整块的宽度（副卡同样如此），这里一律通知：
            // ModuleMain 的 changed() 会比对文本宽度，变了就清掉盒宽记忆并重新排一次。
            this.changed.run()
        }
    }

    /**
     * 指定 subId 的制式文本：系统原生 showName → 模块映射 → 用户自定义规则。
     * 控制中心双排信号用它给每一排各显示一张卡的制式。
     */
    fun technology(i: Int): String? {
        val str = this.nativeTechnologies.label(i)
        if (str != null && !str.isEmpty()) {
            return this.technologyText.apply(str)
        }
        return this.technologyText.apply(this.technology.label())
    }

    /** 设置制式文本自定义规则串  */
    fun setTechnologyText(str: String?) {
        val strSanitize: String = TechnologyText.Companion.sanitize(str)
        if (strSanitize == this.technologyTextSpec) {
            return
        }
        this.technologyTextSpec = strSanitize
        this.technologyText = TechnologyText.Companion.parse(strSanitize)
        val str2 = this.state.mobileTechnology
        applyTechnology()
        if (str2 != this.state.mobileTechnology) {
            this.changed.run()
        }
    }

    fun refreshTechnology() {
        if (this.phone == null || !this.state.simPresent || !this.state.inService || this.state.airplane) {
            this.technology.clear()
            applyTechnology()
            return
        }
        try {
            val dataNetworkType = this.phone!!.getDataNetworkType()
            if (dataNetworkType != this.technology.networkType()) {
                invalidateNativeTechnology()
                this.technologySource = "data-network"
            }
            this.technology.networkChanged(dataNetworkType)
            applyTechnology()
        } catch (e: RuntimeException) {
            this.technologySource = "read-unavailable"
            this.technology.clear()
            applyTechnology()
        }
    }

    fun setNativeTechnologyRefresh(refresh: Runnable?) {
        this.nativeTechnologyRefresh = refresh
    }

    private fun invalidateNativeTechnology() {
        this.nativeTechnologies.invalidate(this.state.subId)
        if (this.nativeTechnologyRefresh != null) this.nativeTechnologyRefresh!!.run()
    }

    private fun retainNativeTechnologies(active: MutableList<SubscriptionInfo>) {
        val ids = IntArray(active.size)
        for (i in ids.indices) ids[i] = active.get(i).getSubscriptionId()
        this.nativeTechnologies.retain(ids)
    }

    fun setNativeSignal(i: Int, i2: Int) {
        if (i < 0) {
            return
        }
        if (this.nativeLevels.size > 8) {
            this.nativeLevels.clear()
        }
        val map = this.nativeLevels
        if (i2 == -1) {
            map.remove(i)
        } else {
            map.put(i, i2)
        }
        if (!this.running || this.state.subId != i) {
            return
        }
        applyLevel()
        recovered()
        this.changed.run()
    }

    fun applyLevel() {
        val num = this.viewLevels.get(this.state.subId)
        val num2 = this.nativeLevels.get(this.state.subId)
        val num3 = if (num != null) num else num2
        this.mobileSource = if (num != null) "native-view" else if (num2 != null) "native-vm" else "platform"
        this.state.mobileLevel = Motion.signalLevel(this.rawLevel, num3)
        var z = false
        this.state.nativeMobile = num3 != null
        this.state.rawMobileLevel = this.rawLevel
        if (num3 != null) {
            this.state.mobileKnown = true
            if (this.nativeServiceAuthoritative) {
                val iconState = this.state
                if (num3 >= 0 && !this.state.airplane) {
                    z = true
                }
                iconState.inService = z
            }
        }
        applyTechnology()
    }

    override fun recovered() {
        if (this.receiverRegistered
            && this.wifiMonitor != null && this.wifiMonitor!!.isStarted
            && this.subsRegistered
            && !WifiRecovery.Companion.needsMeasurement(
                this.state.wifiKnown,
                this.state.wifiEnabled,
                this.state.wifiConnected,
                this.state.wifiLevel
            ) && this.state.mobileKnown
        ) {
            this.healthy = true
            this.retryAttempt = 0
            if (!this.state.simPresent || (this.phone != null && this.cell != null)) {
                this.main.removeCallbacks(this.retry)
                this.retryPending = false
            }
        }
    }

    init {
        this.wifiLevels = WifiLevelPolicy(
            wifiCalculator(true),
            wifiCalculator(false),
            object : WifiLevelPolicy.Calculator {
                @Throws(Exception::class)
                override fun calculate(enabled: Boolean, rssi: Int): Int {
                    return this@NetworkSource.calculatePlatformWifiLevel(
                        enabled, rssi
                    )
                }
            })
        this.performance = PerformanceModeSource(context, this.main, state, changed)
        this.dataRegistration = ObserverRegistration(object : ObserverRegistration.Platform {
            override fun register(index: Int) {
                val key = if (index == 0) "mobile_data" else "mobile_data" + (index - 1)
                this@NetworkSource.context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(key), false, this@NetworkSource.dataObserver
                )
            }

            override fun unregister() {
                this@NetworkSource.context.getContentResolver()
                    .unregisterContentObserver(this@NetworkSource.dataObserver)
            }

            override fun failed(phase: String?, error: Throwable?) {
                this@NetworkSource.failures.record("settings", phase, error)
            }
        })
        this.secondaryBinding = SecondarySubscriptionBinding<SecondaryConnection>(
            object : SecondarySubscriptionBinding.Platform<SecondaryConnection> {
                override fun subscription(): Int {
                    return this@NetworkSource.secondarySubscription()
                }

                override fun create(subId: Int): SecondaryConnection {
                    this@NetworkSource.clearSecondary(subId)
                    val phone = (this@NetworkSource.context
                        .getSystemService<TelephonyManager?>(TelephonyManager::class.java) as TelephonyManager).createForSubscriptionId(
                        subId
                    )
                    return SecondaryConnection(phone, this@NetworkSource.mobileMonitor.newSecondary(subId))
                }

                override fun register(connection: SecondaryConnection) {
                    connection.phone.registerTelephonyCallback(
                        this@NetworkSource.context.getMainExecutor(), connection.callback!!
                    )
                }

                override fun refresh(connection: SecondaryConnection) {
                    this@NetworkSource.applySecondary(connection.phone.getServiceState())
                    this@NetworkSource.state.secondaryRawLevel = this@NetworkSource.miuiAwareLevel(
                        connection.phone.getSignalStrength()
                    )
                    this@NetworkSource.applySecondaryLevel()
                    this@NetworkSource.changed.run()
                }

                override fun close(connection: SecondaryConnection) {
                    connection.phone.unregisterTelephonyCallback(connection.callback!!)
                }

                override fun unavailable(subId: Int) {
                    this@NetworkSource.clearSecondary(subId)
                    if (this@NetworkSource.running) this@NetworkSource.changed.run()
                }

                override fun failed(error: Throwable?) {
                    this@NetworkSource.failures.record("telephony", "secondary-binding", error)
                }
            }, object : SecondarySubscriptionBinding.Scheduler {
                override fun post(task: Runnable?, delay: Long) {
                    this@NetworkSource.main.postDelayed(task!!, delay)
                }

                override fun remove(task: Runnable?) {
                    this@NetworkSource.main.removeCallbacks(task!!)
                }
            })
    }

    @Throws(Exception::class)
    fun calculatePlatformWifiLevel(z: Boolean, i: Int): Int {
        return this.wifi!!.calculateSignalLevel(i)
    }

    private fun wifiCalculator(z: Boolean): WifiLevelPolicy.Calculator? {
        try {
            val cls = Class.forName(
                if (z) "com.android.wifitrackerlib.WifiEntryUtilsStub" else "android.net.wifi.MiuiWifiManager",
                false,
                this.context.getClassLoader()
            )
            val declaredMethod = if (z) cls.getDeclaredMethod(
                "miuiCalculateSignalLevel",
                Context::class.java,
                Boolean::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                WifiManager::class.java
            ) else cls.getDeclaredMethod("calculateSignalLevel", Context::class.java, Boolean::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            if (declaredMethod.getReturnType() == Int::class.javaPrimitiveType!! && Modifier.isStatic(declaredMethod.getModifiers())) {
                declaredMethod.setAccessible(true)
                return object : WifiLevelPolicy.Calculator {
                    @Throws(Exception::class)  // com.cyzah.circular.core.network.WifiLevelPolicy.Calculator
                    override fun calculate(z2: Boolean, i: Int): Int {
                        return this@NetworkSource.calculateReflectiveWifiLevel(z, declaredMethod, z2, i)
                    }
                }
            }
            return null
        } catch (e: LinkageError) {
            return null
        } catch (e: ReflectiveOperationException) {
            return null
        } catch (e: RuntimeException) {
            return null
        }
    }

    @Throws(Exception::class)
    fun calculateReflectiveWifiLevel(z: Boolean, method: Method, z2: Boolean, i: Int): Int {
        val context = this.context
        return ((if (z) method.invoke(null, context, z2, i, this.wifi) else method.invoke(null, context, z2, i)) as Int)
    }

    override fun error(str: String?, th: Throwable?) {
        this.healthy = false
        val jElapsedRealtime = SystemClock.elapsedRealtime()
        if (jElapsedRealtime - this.lastError > 15000 || this.lastError == 0L) {
            this.lastError = jElapsedRealtime
            Log.e("Circular", str + "; will retry", th)
        }
        scheduleBindingRetry()
        this.changed.run()
    }

    private fun scheduleBindingRetry() {
        if (this.requested && !this.retryPending && !awaitingWifiCleanup()) {
            this.retryPending = true
            val handler = this.main
            val runnable = this.retry
            val i = this.retryAttempt
            this.retryAttempt = i + 1
            handler.postDelayed(runnable, min(BurnInDrift.Companion.INTERVAL_MS, (2000 shl min(5, i)).toLong()))
        }
    }

    override fun running(): Boolean {
        return this.running
    }

    override fun main(): Handler {
        return this.main
    }

    override fun changed() {
        this.uiReadiness.observe(
            SystemClock.elapsedRealtime(),
            true,
            this.healthy
        )
        this.changed.run()
    }

    fun uiReady(): Boolean {
        val now = SystemClock.elapsedRealtime()
        this.uiReadiness.observe(now, true, this.healthy)
        return this.uiReadiness.ready(now, this.healthy)
    }

    fun consumeWifiDisconnect(): Boolean {
        return this.wifiDisconnect.consume()
    }

    override fun recordFailure(phase: String?, error: Throwable?) {
        this.failures.record("network", phase, error)
    }

    override fun isCurrentCell(callback: MobileNetworkMonitor.Cell): Boolean {
        return this.running
                && this.state.subId == callback.subscriptionId() && this.cell == callback
    }

    override fun isCurrentSecondary(callback: MobileNetworkMonitor.Secondary): Boolean {
        val connection = this.secondaryBinding.connection()
        return this.running && connection != null && connection.callback == callback && this.secondaryBinding.matches(
            connection,
            callback.subscriptionId()
        )
    }

    override fun isCurrentRadio(callback: MobileNetworkMonitor.Radio): Boolean {
        return this.running
                && this.state.subId == callback.subscriptionId() && this.radio == callback
    }

    override fun signalLevel(signalStrength: SignalStrength?): Int {
        return miuiAwareLevel(signalStrength)
    }

    override fun primaryRawLevel(level: Int) {
        this.rawLevel = level
    }

    override fun applyPrimaryLevel() {
        applyLevel()
    }

    override fun applyPrimaryService(serviceState: ServiceState?) {
        applyService(serviceState)
    }

    override fun secondaryRawLevel(level: Int) {
        this.state.secondaryRawLevel = level
    }

    override fun applySecondaryService(serviceState: ServiceState?) {
        applySecondary(serviceState)
    }

    override fun onDisplayInfo(networkType: Int, overrideType: Int) {
        if (networkType != this.technology.networkType() || overrideType != this.technology.overrideType()) {
            invalidateNativeTechnology()
        }
        this.technology.displayChanged(networkType, overrideType)
        this.technologySource = "display-info"
        applyTechnology()
    }

    override fun onDataConnectionState(networkType: Int) {
        if (networkType != this.technology.networkType()) {
            invalidateNativeTechnology()
            this.technologySource = "data-callback"
        }
        this.technology.networkChanged(networkType)
        applyTechnology()
    }

    fun start() {
        this.requested = true
        if (!this.running) {
            startInternal()
            return
        }
        this.performance.start()
        refreshPowerSave()
        refreshWifi(true)
        recovered()
    }

    private fun startInternal() {
        if (this.running) {
            return
        }
        val transaction = StartupTransaction(object : Runnable {
            override fun run() {
                this@NetworkSource.stopInternal()
            }
        })
        transaction.begin()
        this.running = true
        this.healthy = true
        this.performance.start()
        try {
            refreshPowerSave()
            this.wifi = this.context.getSystemService<WifiManager?>(WifiManager::class.java)
            this.connectivity = this.context.getSystemService<ConnectivityManager?>(ConnectivityManager::class.java)
            this.subscriptions = this.context.getSystemService<SubscriptionManager?>(SubscriptionManager::class.java)
            check(!(this.wifi == null || this.connectivity == null || this.subscriptions == null)) { "Network service unavailable" }
            this.state.airplane = Settings.Global.getInt(this.context.getContentResolver(), "airplane_mode_on", 0) != 0
            val intentFilter = IntentFilter()
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
            intentFilter.addAction("android.net.wifi.RSSI_CHANGED")
            intentFilter.addAction("android.net.wifi.STATE_CHANGE")
            intentFilter.addAction("android.intent.action.AIRPLANE_MODE")
            intentFilter.addAction(DEFAULT_DATA_CHANGED)
            intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED")
            intentFilter.addAction("android.intent.action.SCREEN_ON")
            intentFilter.addAction("android.intent.action.USER_PRESENT")
            intentFilter.addAction("android.intent.action.USER_SWITCHED")
            intentFilter.addAction("android.intent.action.ANY_DATA_STATE")
            this.context.registerReceiver(
                this.receiver,
                intentFilter,
                null,
                this.main,
                Context.RECEIVER_EXPORTED
            )
            this.receiverRegistered = true
            if (this.wifiMonitor == null) {
                this.wifiMonitor = WifiNetworkMonitor(checkNotNull(this.connectivity), this)
            }
            this.wifiMonitor!!.start()
            this.subscriptions!!.addOnSubscriptionsChangedListener(this.context.getMainExecutor(), this.subsListener)
            this.subsRegistered = true
            this.dataRegistration.start()
            refreshWifi(true)
            bindPhone()
            bindSecondary()
            refreshDataEnabled()
            recovered()
            transaction.commit()
        } catch (th: Throwable) {
            transaction.rollback()
            error("Start network state", th)
        }
    }

    override fun refreshWifi(z: Boolean) {
        if (this.wifi == null || this.connectivity == null) {
            this.healthy = false
            this.changed.run()
            return
        }
        val enabled = this.wifi!!.isWifiEnabled()
        var rssi = -127
        var source = "disconnected"
        val wifiNetworks = if (this.wifiMonitor == null) mutableMapOf<Network?, WifiInfo?>() else
            this.wifiMonitor!!.networks()
        if (!enabled) {
            if (this.wifiMonitor != null) {
                this.wifiMonitor!!.clear()
            }
            applyWifiSample(false, null, "disabled")
            return
        }
        val activeNetwork = this.connectivity!!.getActiveNetwork()
        var connectionInfo: WifiInfo? = null
        var wifiInfo = if (activeNetwork != null) wifiNetworks.get(activeNetwork) else null
        if (wifiInfo == null && !wifiNetworks.isEmpty()) {
            for (wifiInfo2 in wifiNetworks.values) {
                if (connected(wifiInfo2) && (wifiInfo == null || wifiInfo2!!.getRssi() > wifiInfo.getRssi())) {
                    wifiInfo = wifiInfo2
                }
            }
        }
        if (wifiInfo != null) {
            source = "callback"
        }
        val z2 = connected(wifiInfo) && WifiRecovery.Companion.validRssi(wifiInfo!!.getRssi())
        if (z || !z2) {
            try {
                connectionInfo = this.wifi!!.getConnectionInfo()
            } catch (e: RuntimeException) {
                if (!z2) {
                    throw e
                }
            }
            val zConnected: Boolean = connected(wifiInfo)
            val rssi2 = if (wifiInfo == null) -127 else wifiInfo.getRssi()
            val zConnected2: Boolean = connected(connectionInfo)
            if (connectionInfo != null) {
                rssi = connectionInfo.getRssi()
            }
            if (connectionInfo != null
                && (!z2 || !zConnected2 || WifiRecovery.Companion.preferFresh(z, zConnected, rssi2, zConnected2, rssi))
            ) {
                source = "connection-info"
                wifiInfo = connectionInfo
            }
        }
        applyWifiSample(true, wifiInfo, source)
    }

    private fun applyWifiSample(enabled: Boolean, info: WifiInfo?, source: String) {
        // Commit only a successful read. A failed API read must not manufacture
        // a disconnect edge or discard the last usable presentation state.
        val connected = enabled && connected(info)
        val rssi = if (connected) info!!.getRssi() else -127
        val frequency = if (connected) info!!.getFrequency() else 0
        this.wifiLevels.clear()
        val measuredLevel = if (connected)
            this.wifiLevels.calculate(frequency, rssi)
        else
            -1
        val wasPending = this.wifiDisplayLevel.needsMeasurement()
        val displayLevel = this.wifiDisplayLevel.resolve(
            SystemClock.elapsedRealtime(), enabled, connected, measuredLevel
        )
        this.state.wifiKnown = true
        this.state.wifiEnabled = enabled
        this.state.wifiConnected = connected
        this.state.wifiLevel = displayLevel
        this.wifiRssi = rssi
        this.wifiFrequency = frequency
        this.wifiMeasuredLevel = measuredLevel
        this.wifiSource = source
        this.wifiDisconnect.observe(true, connected)
        updateWifiRetry()
        if (wasPending != this.wifiDisplayLevel.needsMeasurement()) {
            Log.i(
                "Circular", ("wifiMeasurementPending="
                        + this.wifiDisplayLevel.needsMeasurement()
                        + " connected=" + connected
                        + " raw=" + measuredLevel
                        + " display=" + displayLevel
                        + " retry=" + this.wifiRecovery.pending())
            )
        }
    }

    private fun unbindPhone() {
        if (this.phone != null && this.cell != null) {
            try {
                this.phone!!.unregisterTelephonyCallback(this.cell!!)
            } catch (th: Throwable) {
                Log.w("Circular", "Unregister phone", th)
            }
        }
        if (this.phone != null && this.radio != null && this.radioRegistered) {
            try {
                this.phone!!.unregisterTelephonyCallback(this.radio!!)
            } catch (e: RuntimeException) {
                this.failures.record("telephony", "radio-unregister", e)
            }
        }
        this.phone = null
        this.cell = null
        this.radio = null
        this.radioRegistered = false
        this.technology.clear()
        this.state.mobileTechnology = ""
        this.technologySource = "pending"
        this.state.mobileSlot = -1
    }

    /** Each secondary connection and its retry belong to one independent owner.  */
    fun bindSecondary() {
        if (this.running) {
            this.secondaryBinding.refresh()
            StartupAppearanceCache.saveTopology(this.context, this.state)
        }
    }

    private fun secondarySubscription(): Int {
        var active = this.subscriptions!!.getActiveSubscriptionInfoList()
        if (active == null) active = mutableListOf<SubscriptionInfo?>()
        retainNativeTechnologies(active)
        val primarySlot = SubscriptionManager.getSlotIndex(this.state.subId)
        var fallback = -1
        for (info in active) {
            val id = info.getSubscriptionId()
            if (id == this.state.subId) continue
            if (fallback < 0) fallback = id
            if (SubscriptionManager.getSlotIndex(id) != primarySlot) return id
        }
        return fallback
    }

    private fun clearSecondary(subId: Int) {
        this.state.secondarySubId = subId
        this.state.secondarySlot = if (subId < 0) -1 else SubscriptionManager.getSlotIndex(subId)
        this.state.secondarySimPresent = subId >= 0
        this.state.secondaryKnown = false
        this.state.secondaryInService = false
        this.state.secondaryLevel = -1
        this.state.secondaryRawLevel = -1
    }

    private class SecondaryConnection(@JvmField val phone: TelephonyManager, @JvmField val callback: MobileNetworkMonitor.Secondary?)

    private fun applySecondary(serviceState: ServiceState?) {
        if (serviceState == null) {
            return
        }
        this.state.secondaryKnown = true
        this.state.secondaryInService = serviceState.getState() == 0
        if (!this.state.secondaryInService) {
            this.state.secondaryRawLevel = 0
            this.state.secondaryLevel = 0
        }
    }

    override fun applySecondaryLevel() {
        if (this.state.secondaryRawLevel >= 0) {
            this.state.secondaryKnown = true
            this.state.secondaryLevel = max(0, min(4, this.state.secondaryRawLevel))
        }
    }

    /**
     * 取 MIUI 自己计算的信号格数。
     * 小米在 CellSignalStrength* 里加了私有字段 miuiLevel，状态栏显示的正是它；
     * AOSP 的 SignalStrength.getLevel() 阈值不同（同一信号常年差 1~3 格），故优先用 miuiLevel，
     * 取不到（非小米或字段改名）时退回平台 level。
     */
    fun miuiAwareLevel(signalStrength: SignalStrength?): Int {
        val decoded = SignalLevelDecoder.decode(signalStrength)
        this.levelProbe = decoded.probe()
        return decoded.level()
    }

    fun bindPhone() {
        var z: Boolean
        if (this.running) {
            try {
                var defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()
                var activeSubscriptionInfoList = this.subscriptions!!.getActiveSubscriptionInfoList()
                if (activeSubscriptionInfoList == null) {
                    activeSubscriptionInfoList = mutableListOf<SubscriptionInfo?>()
                }
                retainNativeTechnologies(activeSubscriptionInfoList)
                val it: MutableIterator<SubscriptionInfo?> = activeSubscriptionInfoList.iterator()
                var z2 = false
                while (true) {
                    z = true
                    if (!it.hasNext()) {
                        break
                    } else if (it.next()!!.getSubscriptionId() == defaultDataSubscriptionId) {
                        z2 = true
                    }
                }
                if (!z2) {
                    if (activeSubscriptionInfoList.size == 1) {
                        defaultDataSubscriptionId = activeSubscriptionInfoList.get(0).getSubscriptionId()
                    } else {
                        defaultDataSubscriptionId = -1
                    }
                }
                if (defaultDataSubscriptionId < 0) {
                    unbindPhone()
                    this.state.subId = -1
                    this.state.mobileSlot = -1
                    this.state.simPresent = !activeSubscriptionInfoList.isEmpty()
                    this.state.inService = false
                    this.state.mobileLevel = -1
                    val iconState = this.state
                    if (!activeSubscriptionInfoList.isEmpty() && !this.state.airplane) {
                        z = false
                    }
                    iconState.mobileKnown = z
                    this.state.nativeMobile = false
                    this.state.rawMobileLevel = -1
                    recovered()
                    this.changed.run()
                    return
                }
                if (this.phone != null && this.cell != null && this.state.subId == defaultDataSubscriptionId) {
                    this.state.mobileSlot = SubscriptionManager.getSlotIndex(defaultDataSubscriptionId)
                    applyLevel()
                    refreshTechnology()
                    recovered()
                    this.changed.run()
                    return
                }
                unbindPhone()
                this.rawLevel = -1
                this.state.subId = defaultDataSubscriptionId
                this.state.mobileSlot = SubscriptionManager.getSlotIndex(defaultDataSubscriptionId)
                this.state.simPresent = true
                this.state.mobileKnown = false
                this.state.inService = false
                this.state.mobileLevel = -1
                this.phone =
                    (this.context.getSystemService<TelephonyManager?>(TelephonyManager::class.java) as TelephonyManager).createForSubscriptionId(
                        defaultDataSubscriptionId
                    )
                this.cell = mobileMonitor.newCell(defaultDataSubscriptionId)
                this.phone!!.registerTelephonyCallback(this.context.getMainExecutor(), this.cell!!)
                this.radio = mobileMonitor.newRadio(defaultDataSubscriptionId)
                try {
                    this.phone!!.registerTelephonyCallback(this.context.getMainExecutor(), this.radio!!)
                    this.radioRegistered = true
                } catch (e: RuntimeException) {
                    this.radio = null
                    this.radioRegistered = false
                    Log.w("Circular", "Network type display callback unavailable; using data network type", e)
                }
                applyService(this.phone!!.getServiceState())
                val signalStrength = this.phone!!.getSignalStrength()
                this.rawLevel = miuiAwareLevel(signalStrength)
                applyLevel()
                recovered()
                refreshDataEnabled()
                Log.i("Circular", "Listening to default-data subscription " + defaultDataSubscriptionId)
                this.changed.run()
            } catch (th: Throwable) {
                unbindPhone()
                this.state.mobileKnown = false
                error("Bind default SIM", th)
            }
        }
    }

    fun applyService(serviceState: ServiceState?) {
        if (serviceState == null) {
            return
        }
        var z = true
        this.state.mobileKnown = true
        this.state.inService = serviceState.getState() == 0
        if (!this.state.inService) {
            try {
                val iconState = this.state
                if ((ServiceState::class.java.getMethod("getDataRegistrationState", *arrayOfNulls<Class<*>>(0))
                        .invoke(serviceState, *arrayOfNulls<Any>(0)) as Int) != 0
                ) {
                    z = false
                }
                iconState.inService = z
            } catch (e: ReflectiveOperationException) {
            }
        }
        if (this.state.inService) {
            refreshTechnology()
        } else {
            this.rawLevel = 0
            this.technology.clear()
            applyTechnology()
        }
        applyLevel()
    }

    fun stop() {
        this.requested = false
        this.main.removeCallbacks(this.retry)
        this.retryPending = false
        stopInternal()
    }

    private fun stopInternal() {
        this.uiReadiness.reset()
        this.wifiDisconnect.reset()
        this.running = false
        this.performance.stop()
        this.main.removeCallbacks(this.wifiRetry)
        this.wifiRecovery.reset()
        this.wifiDisplayLevel.reset()
        this.wifiMeasuredLevel = -1
        unbindPhone()
        this.secondaryBinding.stop()
        if (this.receiverRegistered) {
            try {
                this.context.unregisterReceiver(this.receiver)
            } catch (th: Throwable) {
                this.failures.record("network", "receiver-unregister", th)
            }
        }
        if (this.wifiMonitor != null) {
            this.wifiMonitor!!.stop()
            if (!this.wifiMonitor!!.ownsRegistration()) {
                this.wifiMonitor = null
            }
        }
        if (this.subsRegistered) {
            try {
                this.subscriptions!!.removeOnSubscriptionsChangedListener(this.subsListener)
            } catch (th3: Throwable) {
                this.failures.record("telephony", "subscriptions-unregister", th3)
            }
        }
        this.dataRegistration.stop()
        this.nativeTechnologies.retain(IntArray(0))
        this.subsRegistered = false
        this.receiverRegistered = false
        val iconState = this.state
        this.state.mobileKnown = false
        iconState.wifiKnown = false
        this.wifiLevels.clear()
    }

    companion object {
        private const val DEFAULT_DATA_CHANGED = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"
        private fun connected(wifiInfo: WifiInfo?): Boolean {
            return wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED
        }
    }
}
