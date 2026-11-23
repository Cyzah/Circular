package com.cyzah.circular.hook.platform

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Canvas
import android.os.BatteryManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cyzah.circular.core.diagnostics.HookOrderRecorder
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.NotificationMotionWork
import com.cyzah.circular.core.graphics.geometry.CompactGeometry
import com.cyzah.circular.core.network.NetworkSource
import com.cyzah.circular.hook.platform.Compatibility
import com.cyzah.circular.hook.runtime.ControlCenterRowReservation
import com.cyzah.circular.hook.runtime.ModuleMain
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

class SystemUiHookBootstrap(private val module: ModuleMain) {
    private val compatibility: Compatibility.Result get() = checkNotNull(module.compatibility)
    private val hookOrder = HookOrderRecorder()
    private lateinit var batteryClass: Class<*>
    private lateinit var containerClass: Class<*>
    private lateinit var iconsClass: Class<*>
    private lateinit var modernClass: Class<*>

    fun install(packageReadyParam: PackageReadyParam) {
        if (module.started || "com.android.systemui" != packageReadyParam.getPackageName()) {
            return
        }
        module.started = true
        module.ensureMotionController()
        val classLoader: ClassLoader? = packageReadyParam.getClassLoader()
        module.compatibility = Compatibility.probe(classLoader)
        this.hookOrder.reached(HookOrderRecorder.Stage.COMPATIBILITY)
        installReporter(classLoader, packageReadyParam.getApplicationInfo())
        if (!compatibility.isAvailable(Compatibility.Capability.CORE)) {
            val strProbe = OemCompatibility.probe(classLoader)
            if (!strProbe.isEmpty()) {
                module.oem = OemStatusAdapter(module, classLoader, strProbe)
                module.setupReady = checkNotNull(module.oem).install()
                return
            } else {
                module.log(
                    5,
                    ModuleMain.Companion.TAG,
                    "Unsupported SystemUI contract: " + compatibility.details()
                )
                return
            }
        }
        try {
            installStages(classLoader)
            module.setupReady = true
            return
        } catch (th13: Throwable) {
            rollback(0)
            module.setupFailure = th13.toString()
            compatibility.markFailed(
                Compatibility.Capability.CORE, "install", module.setupFailure
            )
            module.log(6, ModuleMain.Companion.TAG, "Setup failed; stock UI retained", th13)
        }
    }

    @Throws(Throwable::class)
    private fun installStages(classLoader: ClassLoader?) {
        val units: MutableList<HookInstallUnit> = ArrayList()
        units.add(CoreHookInstaller(this, classLoader))
        units.addAll(OptionalHookInstallers.all(this, classLoader))
        HookInstallPipeline.install(
            units,
            ModuleHookInstallContext(module),
            this.hookOrder
        )
        this.hookOrder.reached(HookOrderRecorder.Stage.COMPLETE)
    }

    @Throws(Exception::class)
    fun installCoreViews(classLoader: ClassLoader?) {
        this.batteryClass = ModuleMain.Companion.cls(classLoader, ModuleMain.Companion.BATTERY)
        this.containerClass = ModuleMain.Companion.cls(classLoader, ModuleMain.Companion.CONTAINER)
        this.modernClass = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
        )
        this.iconsClass = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
        )
        val mobileClass: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        )

        module.level = ModuleMain.Companion.field(this.batteryClass, "mLevel")
        module.charging = ModuleMain.Companion.field(this.batteryClass, "mCharging")
        module.firstLevel = ModuleMain.Companion.field(this.batteryClass, "mFirstLevel")
        module.batteryText = ModuleMain.Companion.field(this.batteryClass, "mBatteryPercentView")
        module.statusState = ModuleMain.Companion.field(this.batteryClass, "mStatusBarState")
        module.layoutFrom = ModuleMain.Companion.field(this.batteryClass, "mLayoutFromTag")
        module.iconsField = ModuleMain.Companion.field(this.containerClass, "mStatusIcon")
        module.privacyField = ModuleMain.Companion.field(this.containerClass, "mPrivacyState")
        module.hideBatteryField = ModuleMain.Companion.field(this.containerClass, "mIsHideBattery")
        module.spacingField = ModuleMain.Companion.field(this.iconsClass, "iconSpacing")
        module.bindingField = ModuleMain.Companion.field(this.modernClass, "binding")
        module.ignoredSlots = ModuleMain.Companion.field(this.iconsClass, "ignoredSlots")
        module.mobileSubId = ModuleMain.Companion.field(mobileClass, "subId")

        val displayable: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.StatusIconDisplayable"
        )
        module.displayVisible = displayable.getMethod("isIconVisible", *arrayOfNulls<Class<*>>(0))
        module.displayBlocked = displayable.getMethod("isIconBlocked", *arrayOfNulls<Class<*>>(0))
        module.displaySlot = displayable.getMethod("getSlot", *arrayOfNulls<Class<*>>(0))
        module.shouldBeVisible = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"
        )
            .getMethod("getShouldIconBeVisible", *arrayOfNulls<Class<*>>(0))
        module.measuredDimension = ModuleMain.Companion.method(
            View::class.java,
            "setMeasuredDimension",
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!
        )
        module.measureChild = ModuleMain.Companion.method(
            ViewGroup::class.java,
            "measureChild",
            View::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!
        )
        module.visibleState = ModuleMain.Companion.method(
            this.modernClass,
            "setVisibleState",
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!
        )
        module.updateChargeAndText = ModuleMain.Companion.method(
            this.batteryClass,
            "updateChargeAndText",
            *arrayOfNulls<Class<*>>(0)
        )
        module.setFastChargeBadgeStatus("disabled")

        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.batteryClass,
                    "onAttachedToWindow",
                    *arrayOfNulls<Class<*>>(0)
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@SystemUiHookBootstrap.onBatteryAttached(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.batteryClass,
                    "onDetachedFromWindow",
                    *arrayOfNulls<Class<*>>(0)
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onBatteryDetached(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.containerClass,
                    "onMeasure",
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onContainerMeasure(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.batteryClass,
                    "onMeasure",
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onBatteryMeasure(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.batteryClass,
                    "onLayout",
                    Boolean::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onBatteryLayout(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.containerClass,
                    "onLayout",
                    Boolean::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onContainerLayout(chain)
                }
            })
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    this.modernClass,
                    "isIconVisible",
                    *arrayOfNulls<Class<*>>(0)
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onModernViewVisibility(chain)
                }
            })
        )

        val visibleTypes = arrayOf<Class<*>>(this.modernClass, mobileClass)
        for (visibleType in visibleTypes) {
            module.installed.add(
                module.hook(
                    ModuleMain.Companion.method(
                        visibleType,
                        "setVisibleState",
                        Int::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!
                    )
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return onModernVisibleState(chain)
                    }
                })
            )
        }

        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    ViewGroup::class.java,
                    "dispatchDraw",
                    Canvas::class.java
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return onStatusBarDispatchDraw(chain)
                }
            })
        )
        val batteryCallbacks = mutableListOf<String?>(
            "onBatteryLevelChanged",
            "onChargeStateChanged",
            "onDarkChangedInternal",
            "onLightDarkTintChanged",
            "updateState"
        )
        for (method in this.batteryClass!!.getDeclaredMethods()) {
            if (batteryCallbacks.contains(method.getName())) {
                val tintCallback = method.getName() == "onDarkChangedInternal"
                        || method.getName() == "onLightDarkTintChanged"
                module.installed.add(module.hook(method).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        val result = onBatteryStateChanged(chain)
                        if (tintCallback && chain.getThisObject() is View) {
                            val view = chain.getThisObject() as View
                            val text = checkNotNull(module.batteryText).get(view)
                            if (text is TextView) {
                                module.nativeBatteryTints.put(view, text.getCurrentTextColor())
                                view.invalidate()
                            }
                        }
                        return result
                    }
                }))
            }
        }
    }

    @Throws(Throwable::class)
    fun installPower() {
        if (compatibility.isAvailable(Compatibility.Capability.BATTERY_POWER)) {
            val installedCount = module.installed.size
            try {
                module.powerSaveField = ModuleMain.Companion.field(this.batteryClass, "mPowerSave")
                module.installed.add(
                    module.hook(
                        ModuleMain.Companion.method(
                            this.batteryClass,
                            "onPowerSaveChanged",
                            Boolean::class.javaPrimitiveType!!
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return onPowerSaveChanged(chain)
                        }
                    })
                )
            } catch (error: Throwable) {
                rollback(installedCount)
                module.powerSaveField = null
                compatibility.markUnavailable(
                    Compatibility.Capability.BATTERY_POWER,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installPerformance() {
        try {
            module.performanceField = ModuleMain.Companion.field(this.batteryClass, "mPerformanceMode")
        } catch (error: Throwable) {
            rollback(module.installed.size)
            module.performanceField = null
            module.performanceHook = false
        }
        if (ReflectionSupport.usableBooleanField(module.performanceField)) {
            module.installed.add(
                module.hook(
                    ModuleMain.Companion.method(
                        this.batteryClass,
                        "onPerformanceModeChanged",
                        Boolean::class.javaPrimitiveType!!
                    )
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return onPerformanceModeChanged(chain)
                    }
                })
            )
            module.performanceHook = true
        } else {
            module.performanceHook = false
            compatibility.markUnavailable(
                Compatibility.Capability.BATTERY_POWER,
                "install",
                "mPerformanceMode unavailable"
            )
        }
    }

    @Throws(Throwable::class)
    fun installPrivacy() {
        for (method in this.containerClass!!.getDeclaredMethods()) {
            if (method.getName() == "setPrivacyState"
                || method.getName() == "setIsHideBattery"
            ) {
                module.installed.add(module.hook(method).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return onPrivacyStateChanged(chain)
                    }
                }))
            }
        }
    }

    @Throws(Throwable::class)
    fun installMotion(classLoader: ClassLoader?) {
        if (compatibility.isAvailable(Compatibility.Capability.MOTION)) {
            val installedCount = module.installed.size
            try {
                hookProgress(
                    classLoader,
                    "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback$1",
                    true
                )
                hookProgress(
                    classLoader,
                    "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback$1",
                    false
                )
            } catch (error: Throwable) {
                rollback(installedCount)
                compatibility.markUnavailable(
                    Compatibility.Capability.MOTION,
                    "install",
                    error.toString()
                )
            }
            try {
                hookPanelTracking(classLoader)
            } catch (error: Throwable) {
                module.panelTrackingAvailable = false
                module.log(
                    5,
                    ModuleMain.Companion.TAG,
                    "panelTracking=unavailable",
                    error
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installAlignment(classLoader: ClassLoader?) {
        if (compatibility.isAvailable(Compatibility.Capability.MOTION)
            && compatibility.isAvailable(
                Compatibility.Capability.MOTION_ALIGNMENT
            )
        ) {
            val installedCount = module.installed.size
            try {
                module.hookAlignment(classLoader)
            } catch (error: Throwable) {
                rollback(installedCount)
                compatibility.markUnavailable(
                    Compatibility.Capability.MOTION_ALIGNMENT,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installNativeSignal(classLoader: ClassLoader?) {
        if (compatibility.isAvailable(Compatibility.Capability.NATIVE_SIGNAL)) {
            val installedCount = module.installed.size
            try {
                module.hookNativeSignal(classLoader)
            } catch (error: Throwable) {
                rollback(installedCount)
                compatibility.markUnavailable(
                    Compatibility.Capability.NATIVE_SIGNAL,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installSignalResources(classLoader: ClassLoader?) {
        module.setupSignalResources(classLoader)
    }

    fun installSignalSnapshot(classLoader: ClassLoader?) {
        try {
            module.hookSignalSnapshot(classLoader)
            module.signalSnapshotHook = true
        } catch (error: Throwable) {
            module.signalAdapter = "snapshot: " + error.javaClass.getSimpleName()
        }
    }

    fun installViewSignal() {
        try {
            module.hookViewSignal()
            module.viewSignalHook = true
        } catch (error: Throwable) {
            module.log(5, ModuleMain.Companion.TAG, "View signal observer unavailable", error)
        }
    }

    @Throws(Throwable::class)
    fun installHandoff() {
        if (compatibility.isAvailable(Compatibility.Capability.HANDOFF)) {
            val installedCount = module.installed.size
            try {
                val field: Field = ModuleMain.Companion.field(this.batteryClass, "mIsAddBatteryIsland")
                if (field.getType() == Boolean::class.javaPrimitiveType!! && !Modifier.isStatic(field.getModifiers())) {
                    module.batteryIsland = field
                }
            } catch (error: Throwable) {
                rollback(installedCount)
                module.batteryIsland = null
                compatibility.markUnavailable(
                    Compatibility.Capability.HANDOFF,
                    "install",
                    error.toString()
                )
            }
            module.installed.add(
                module.hook(
                    ModuleMain.Companion.method(
                        this.batteryClass,
                        "enableAnimation",
                        *arrayOfNulls<Class<*>>(0)
                    )
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@SystemUiHookBootstrap.onChargingIslandAnimationRequested(chain)
                    }
                })
            )
            val selected = Compatibility.selectMethod(
                this.iconsClass,
                "enableAnimation$1,enableAnimation",
                "()Z"
            )
            selected.setAccessible(true)
            module.installed.add(module.hook(selected).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@SystemUiHookBootstrap.onBatteryContainerAnimationRequested(chain)
                }
            }))
        }
        if (compatibility.isAvailable(Compatibility.Capability.HANDOFF)
            && compatibility.isAvailable(
                Compatibility.Capability.HANDOFF_HINTS
            )
        ) {
            try {
                module.deoptimize(
                    ModuleMain.Companion.method(
                        this.batteryClass,
                        "isNeedAnimateIsland",
                        *arrayOfNulls<Class<*>>(0)
                    )
                )
                module.batteryLayoutListener = ModuleMain.Companion.field(
                    this.batteryClass,
                    "onLayoutChangeListener"
                )
                val listenerType = checkNotNull(module.batteryLayoutListener).type
                if (!listenerType.isInterface) deoptimizeListener(listenerType)
            } catch (error: Throwable) {
                compatibility.markUnavailable(
                    Compatibility.Capability.HANDOFF_HINTS,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installChargingIsland() {
        if (compatibility.isAvailable(Compatibility.Capability.CHARGING_ISLAND)) {
            val installedCount = module.installed.size
            try {
                hookChargingIsland(this.batteryClass!!)
            } catch (error: Throwable) {
                rollback(installedCount)
                module.storedBatteryIsland = null
                module.updateBatteryIsland = null
                compatibility.markUnavailable(
                    Compatibility.Capability.CHARGING_ISLAND,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installKeyguard(classLoader: ClassLoader?) {
        if (compatibility.isAvailable(Compatibility.Capability.KEYGUARD)) {
            val installedCount = module.installed.size
            try {
                module.hookKeyguard(classLoader, this.batteryClass)
            } catch (error: Throwable) {
                rollback(installedCount)
                module.keyguardType = null
                module.toLockScreen = null
                compatibility.markUnavailable(
                    Compatibility.Capability.KEYGUARD,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Throwable::class)
    fun installHomePrivacy() {
        if (compatibility.isAvailable(Compatibility.Capability.PRIVACY)) {
            val installedCount = module.installed.size
            try {
                module.homeBlock = ModuleMain.Companion.field(this.batteryClass, "mHomeBlock")
                module.installed.add(
                    module.hook(
                        ModuleMain.Companion.method(
                            this.batteryClass,
                            "updateHomeBlock",
                            Boolean::class.javaPrimitiveType!!
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiHookBootstrap.onHomePrivacyChanged(chain)
                        }
                    })
                )
                module.installed.add(
                    module.hook(
                        Compatibility.selectMethod(
                            this.batteryClass,
                            "updateVisibility$6,updateVisibility$1$1",
                            "()V"
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiHookBootstrap.onHomePrivacyVisibilityChanged(chain)
                        }
                    })
                )
            } catch (error: Throwable) {
                rollback(installedCount)
                module.homeBlock = null
                compatibility.markUnavailable(
                    Compatibility.Capability.PRIVACY,
                    "install",
                    error.toString()
                )
            }
        }
    }

    @Throws(Exception::class)
    fun installDeoptimize() {
        module.deoptimize(
            ModuleMain.Companion.method(
                this.iconsClass,
                "onMeasure",
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!
            )
        )
        module.deoptimize(
            ModuleMain.Companion.method(
                this.iconsClass,
                "onLayout",
                Boolean::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!
            )
        )
    }

    fun hookOrderSummary(): String {
        return this.hookOrder.summary()
    }

    @Throws(Throwable::class)
    fun onBatteryAttached(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val view = chain.getThisObject() as View
        // Parent slots are already attached after the native callback. Register
        // before the first traversal so a restart cannot expose one stock frame.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            module.signalHooks().onStatusBarViewAttached(view)
            return objProceed
        }
        module.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                module.signalHooks().onStatusBarViewAttached(view)
            }
        })
        return objProceed
    }

    @Throws(Throwable::class)
    fun onBatteryDetached(chain: XposedInterface.Chain): Any? {
        val view = chain.getThisObject() as View?
        module.detach(view)
        // A transient control-center host can detach while the status-bar ring
        // remains valid. Let the runtime availability check decide whether the
        // transfer must be abandoned instead of cutting every ring animation.
        module.refreshBatteryTransferMotion()
        return chain.proceed()
    }

    @Throws(Throwable::class)
    fun onContainerMeasure(chain: XposedInterface.Chain): Any? {
        val session = module.containers.get(chain.getThisObject())
        if (session != null) {
            try {
                session.plan((chain.getArg(0) as Int), (chain.getArg(1) as Int))
            } catch (th: Throwable) {
                session.fail("plan", th)
            }
        }
        return chain.proceed()
    }

    @Throws(Throwable::class)
    fun onBatteryMeasure(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.batteries.get(chain.getThisObject())
        if (session != null) {
            try {
                session.stockWidth = session.battery.getMeasuredWidth()
                session.stockHeight = session.battery.getMeasuredHeight()
                session.batteryWidthMeasureSpec = (chain.getArg(0) as Int)
                session.hasBatteryWidthMeasureSpec = true
                if (session.draw) {
                    val iDp = session.dp(module.size)
                    val iControlDp = session.dp(
                        if (session.from == 5 || session.from == 6)
                            checkNotNull(module.controlCenterSettings()).size()
                        else
                            module.size
                    )
                    // 胶囊是宽扁的：图标视图按胶囊的宽高比加宽，否则会被状态栏按 dp 正方形给的
                    // 空间卡死、还会压到左边的网速图标。合并态（merge = 显示三合一）也要用这个宽基数，
                    // 不然容器不会为胶囊让位。
                    // 预留宽度的基准必须跟绘制时一致：painter 是按 fMin = min(尺寸, 宿主高度) 铺的，
                    // 这里若按尺寸 dp 算，框会比胶囊宽一截 —— 胶囊右边贴齐了、左边就剩一条空档。
                    val iCapUnit = if (session.stockHeight > 0) min(iDp, session.stockHeight) else iDp
                    val iClosedBase = if (module.capsuleDual)
                        Math.round(
                            iCapUnit
                                    * module.capsuleReserveUnits()
                                    / 120.0f
                        )
                    else
                        iDp
                    val iMergedBase = SessionPaintPolicy.controlCenterHostBaseWidth(
                        iClosedBase,
                        iControlDp,
                        session.progress
                    )
                    val solvedRowWidth = ControlCenterRowReservation.measuredWidth(session)
                    val iWidth = if (solvedRowWidth > 0)
                        solvedRowWidth
                    else
                        if (session.merge)
                            SessionPaintPolicy.mergedControlCenterWidth(
                                iMergedBase,
                                session.networkWidth,
                                session.progress
                            )
                        else
                            iClosedBase
                    val iMax = max(
                        session.stockHeight,
                        max(iDp, iControlDp)
                    )
                    var z = false
                    val iResolveSize = View.resolveSize(iWidth, (chain.getArg(0) as Int))
                    val iResolveSize2 = View.resolveSize(iMax, (chain.getArg(1) as Int))
                    val iWidth2 = ControlCenterRowReservation.resolveWidth(
                        session, iWidth, session.batteryWidthMeasureSpec
                    )
                    val iHeight =
                        CompactGeometry.height(iDp, session.stockHeight, iResolveSize2, session.parentHeightLimit)
                    if (iHeight != iResolveSize2 || iWidth2 != iResolveSize) {
                        z = true
                    }
                    session.sizeAdjusted = z
                    checkNotNull(module.measuredDimension).invoke(session.battery, iWidth2, iHeight)
                }
            } catch (th: Throwable) {
                session.fail("measure", th)
            }
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onBatteryLayout(chain: XposedInterface.Chain): Any? {
        val session = module.batteries.get(chain.getThisObject())
        if (session != null && session.draw) {
            val array = chain.getArgs().toTypedArray()
            array[3] = (array[1] as Int) + session.stockWidth
            return chain.proceed(array)
        }
        return chain.proceed()
    }

    @Throws(Throwable::class)
    fun onContainerLayout(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.containers.get(chain.getThisObject())
        if (session != null) {
            module.layoutEpoch++
        }
        if (session != null && session.merge) {
            try {
                session.layoutSignals()
            } catch (th: Throwable) {
                session.fail("signal layout", th)
            }
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onModernViewVisibility(chain: XposedInterface.Chain): Any? {
        val sessionOwner = module.owner(chain.getThisObject())
        if (!module.nativeStatusIconHold.ownsSignals(sessionOwner)) {
            return chain.proceed()
        }
        return false
    }

    @Throws(Throwable::class)
    fun onModernVisibleState(chain: XposedInterface.Chain): Any? {
        val sessionOwner = module.owner(chain.getThisObject())
        if (!module.nativeStatusIconHold.ownsSignals(sessionOwner)) {
            return chain.proceed()
        }
        // 图标被 MIUI 点亮 / 熄灭（Wi-Fi 刚连上就是这条路径）时那一行的构成会变，立刻让会话重新量一次：
        // 容器宽度与电池位置才会跟着更新，否则原生图标会先叠在模块的双排块上，得重开控制中心才纠正。
        sessionOwner!!.requestRelayout()
        return chain.proceed(arrayOf<Any>(0, false))
    }

    @Throws(Throwable::class)
    fun onStatusBarDispatchDraw(chain: XposedInterface.Chain): Any? {
        if (module.bypass) {
            return chain.proceed()
        }
        val session = module.batteries.get(chain.getThisObject())
        if (session != null && session.draw) {
            val canvas = chain.getArg(0) as Canvas
            val iSave = canvas.save()
            var drawFailure: Throwable? = null
            session.nativeDrawStack.push(chain)
            try {
                // 状态栏那一行（tag 5）整体比控制中心那一行靠右（实测差值 rowAlignHostShift）。
                // 平移量乘形变进度 stackedAlpha：steady 状态（0）谁都不动，形变结束（1）时
                // 双排块、三合一电量环、原生电池一起落到控制中心那一行的实际位置上；
                // 中途按比例渐变，所以不会出现"图标已定形却还在动"或电池留在原地的情况。
                val fRowShift = if (session.from == 5) (module.rowAlignHostShift() * session.stackedAlpha) else 0.0f
                if (fRowShift != 0.0f) {
                    canvas.translate(fRowShift, 0.0f)
                }
                session.paint(canvas, session.nativeDrawAdapter)
            } catch (error: Throwable) {
                drawFailure = error
            } finally {
                session.nativeDrawStack.pop()
                try {
                    canvas.restoreToCount(iSave)
                } catch (cleanup: Throwable) {
                    if (drawFailure == null) {
                        drawFailure = cleanup
                    } else if (drawFailure !== cleanup) {
                        drawFailure.addSuppressed(cleanup)
                    }
                }
            }
            if (drawFailure != null) {
                session.fail("draw", drawFailure)
            }
            // A native layer may already have proceeded inside this composite draw.
            // The failed frame is consumed; the next traversal uses the native fallback.
            return null
        }
        // Modern views may be recreated before the next container measurement.
        // Resolve their parent session just as the visibility hooks do.
        val session2 = module.owner(chain.getThisObject())
        if (module.nativeStatusIconHold.ownsSignals(session2)) {
            return null
        }
        return chain.proceed()
    }

    @Throws(Throwable::class)
    fun onBatteryStateChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.batteries.get(chain.getThisObject())
        if (session != null) {
            session.changed()
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onPowerSaveChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val zBooleanValue = (chain.getArg(0) as Boolean)
        val runnable: Runnable = object : Runnable {
            // java.lang.Runnable
            override fun run() {
                applyNativePowerSave(zBooleanValue)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            module.main.post(runnable)
        }
        return objProceed
    }

    fun applyNativePowerSave(z: Boolean) {
        module.pendingNativePower = z
        module.networks?.setNativePowerSave(z)
    }

    @Throws(Throwable::class)
    fun onPerformanceModeChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val zBooleanValue = (chain.getArg(0) as Boolean)
        val runnable: Runnable = object : Runnable {
            // java.lang.Runnable
            override fun run() {
                applyNativePerformance(zBooleanValue)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            module.main.post(runnable)
        }
        return objProceed
    }

    fun applyNativePerformance(z: Boolean) {
        module.pendingNativePerformance = z
        module.networks?.performance?.setNative(z)
    }

    @Throws(Throwable::class)
    fun onPrivacyStateChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.containers.get(chain.getThisObject())
        if (session != null) {
            session.changed()
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onChargingIslandAnimationRequested(chain: XposedInterface.Chain): Any? {
        val session = module.batteries.get(chain.getThisObject())
        if (session == null || !session.ownsMotion()) {
            return chain.proceed()
        }
        if (session.from == 1) {
            session.reflowBlocks++
        }
        return false
    }

    @Throws(Throwable::class)
    fun onBatteryContainerAnimationRequested(chain: XposedInterface.Chain): Any? {
        val thisObject = chain.getThisObject()
        val session = if (thisObject is View) (thisObject.parent as? View)?.let(module.containers::get) else null
        if (session == null || !session.ownsMotion()) {
            return chain.proceed()
        }
        if (session.from == 1) {
            session.reflowBlocks++
        }
        return false
    }

    @Throws(Throwable::class)
    fun onHomePrivacyChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.batteries.get(chain.getThisObject())
        if (session != null) {
            session.changed()
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onHomePrivacyVisibilityChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.batteries.get(chain.getThisObject())
        if (session != null) {
            session.changed()
        }
        return objProceed
    }

    fun rollback(i: Int) {
        while (module.installed.size > i) {
            try {
                checkNotNull(module.installed.removeAt(module.installed.size - 1)).unhook()
            } catch (th: Throwable) {
            }
        }
    }

    private fun installReporter(classLoader: ClassLoader?, applicationInfo: ApplicationInfo) {
        try {
            var name = applicationInfo.className
            if (name == null) {
                name = Application::class.java.getName()
            } else if (name.startsWith(".")) {
                name = applicationInfo.packageName + name
            }
            module.hook(ModuleMain.Companion.cls(classLoader, name).getMethod("onCreate", *arrayOfNulls<Class<*>>(0)))
                .intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@SystemUiHookBootstrap.onApplicationAttached(chain)
                    }
                })
        } catch (th: Throwable) {
            module.log(
                5,
                ModuleMain.Companion.TAG,
                "Startup diagnostic unavailable; battery attach will provide context",
                th
            )
        }
    }

    @Throws(Throwable::class)
    fun onApplicationAttached(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val context = chain.getThisObject() as Context
        module.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                this@SystemUiHookBootstrap.initializeApplicationContext(context)
            }
        })
        return objProceed
    }

    fun initializeApplicationContext(context: Context) {
        ensureContext(context)
        module.loadConfig()
    }

    fun ensureContext(context: Context) {
        var context = context
        if (module.context != null) {
            return
        }
        val applicationContext = context.getApplicationContext()
        if (applicationContext != null) {
            context = applicationContext
        }
        module.context = context
        try {
            val battery = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (battery != null) {
                val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) module.state.battery = min(100, Math.round(level * 100.0f / scale))
                val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                module.state.charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (unavailable: RuntimeException) {
            // Native battery callbacks provide the same values once attached.
        }
        val networks = NetworkSource(
            context,
            module.state,
            object : Runnable {
                override fun run() {
                    module.changedAll()
                }
            })
        module.networks = networks
        if (module.oem != null) networks.preservePlatformService()
        module.pendingNativePower?.let(networks::setNativePowerSave)
        module.pendingNativePerformance?.let(networks.performance::setNative)
        for ((subscription, level) in module.nativeCache) {
            networks.setNativeSignal(checkNotNull(subscription), checkNotNull(level))
        }
        for ((subscription, level) in module.nativeViewCache) {
            val id = checkNotNull(subscription)
            networks.recordViewResource(id, module.viewResourceNames[id])
            networks.setViewSignal(id, checkNotNull(level))
        }
        module.restoreStartupAppearance()
    }

    @Throws(Exception::class)
    fun deoptimizeListener(cls: Class<*>) {
        if (module.deoptimizedListeners.contains(cls)) {
            return
        }
        val declaredMethod = cls.getDeclaredMethod(
            "onLayoutChange",
            View::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!
        )
        if (declaredMethod.getReturnType() != Void.TYPE || Modifier.isStatic(declaredMethod.getModifiers())) {
            throw NoSuchMethodException("onLayoutChange signature")
        }
        declaredMethod.setAccessible(true)
        module.deoptimize(declaredMethod)
        module.deoptimizedListeners.add(cls)
    }

    @Throws(Exception::class)
    private fun hookProgress(classLoader: ClassLoader?, str: String?, z: Boolean) {
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    ModuleMain.Companion.cls(classLoader, str),
                    "onExpansionChanged",
                    Float::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@SystemUiHookBootstrap.onExpansionChanged(z, chain)
                }
            })
        )
    }

    @Throws(Exception::class)
    private fun hookPanelTracking(classLoader: ClassLoader?) {
        val repository: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.shade.data.repository.ShadeRepositoryImpl"
        )
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    repository,
                    "setLegacyShadeTracking",
                    Boolean::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val active =
                        (chain.getArg(0) as Boolean)
                    try {
                        return chain.proceed()
                    } finally {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            module.onPanelTrackingChanged(active)
                        } else {
                            module.main.post(object : Runnable {
                                override fun run() {
                                    module.onPanelTrackingChanged(active)
                                }
                            })
                        }
                    }
                }
            })
        )
        module.markPanelTrackingAvailable()
    }

    @Throws(Throwable::class)
    fun onExpansionChanged(z: Boolean, chain: XposedInterface.Chain): Any? {
        val fClamp = Motion.clamp((chain.getArg(0) as Float))
        if (z) {
            module.releaseClosedAnchors()
        }
        if (!module.alignmentSyncing()) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                applyExpansionChange(fClamp, z)
            } else {
                module.main.post(object : Runnable {
                    // java.lang.Runnable
                    override fun run() {
                        this@SystemUiHookBootstrap.applyExpansionChange(fClamp, z)
                    }
                })
            }
        }
        if (z) {
            module.captureAlignmentController(chain.getThisObject())
        }
        val obj = module.alignmentController()
        val numApplyAlignment = if (z) module.applyAlignment() else null
        var objProceed: Any?
        var nativeFailure: Throwable? = null
        try {
            objProceed = chain.proceed()
        } catch (error: Throwable) {
            nativeFailure = error
            throw error
        } finally {
            if (numApplyAlignment != null) {
                try {
                    module.restoreAlignment(obj, numApplyAlignment)
                } catch (cleanup: Throwable) {
                    if (nativeFailure != null && nativeFailure !== cleanup) {
                        nativeFailure.addSuppressed(cleanup)
                    }
                    module.disableAlignment(cleanup)
                }
            }
        }
        if (z) {
            module.refreshClosedAnchors()
        }
        return objProceed
    }

    fun applyExpansionChange(f: Float, z: Boolean) {
        val update = NotificationMotionWork.update(
            z, module.control, module.shade, f,
            module.batteryTransferState().phase() != BatteryTransferState.Phase.NORMAL_RING
        )
        if (z && module.control == 0.0f && f > 0.0f) {
            for (session in module.keyguards.values) {
                if (!session.ambient && (session.keyguard?.isShown == true || session.keyguardHandoff.retained())) {
                    module.lockMotionTrace.clear()
                    module.realLockTrace.clear()
                    module.resetAnchorSample()
                    session.lastRealFrame = ""
                    session.returnCapturePending = true
                }
            }
        }
        if (z) {
            module.control = f
        } else {
            module.shade = f
        }
        module.refreshNativeTechnologyForExpansion()
        if (!update) return
        if (SessionPaintPolicy.shouldHoldNativeIcons(
                max(module.control, module.shade),
                module.stackedOn(),
                module.mode == 2
            )
        ) {
            module.nativeStatusIconHold.activate()
        }
        module.advanceMorph()
        val it = module.batteries.values.iterator()
        while (it.hasNext()) {
            it.next()!!.expansionChanged()
        }
    }

    @Throws(Exception::class)
    private fun hookChargingIsland(cls: Class<*>) {
        module.batteryIsland = ModuleMain.Companion.field(cls, "mIsAddBatteryIsland")
        module.storedBatteryIsland = ModuleMain.Companion.field(cls, "mStoreIsAddBatteryIsland")
        module.updateBatteryIsland = ModuleMain.Companion.method(cls, "updateIslandChanged", Boolean::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!)
        module.installed.add(module.hook(checkNotNull(module.updateBatteryIsland)).intercept(object : Hooker {
            @Throws(Throwable::class)
            override fun intercept(chain: XposedInterface.Chain): Any? {
                return this@SystemUiHookBootstrap.onChargingIslandChanged(chain)
            }
        }))
        module.deoptimize(ModuleMain.Companion.method(cls, "onBatteryStyleChanged", Int::class.javaPrimitiveType!!))
        module.deoptimize(ModuleMain.Companion.method(cls, "updateControlCenterExpandState", Boolean::class.javaPrimitiveType!!))
        module.deoptimize(
            ModuleMain.Companion.method(
                cls,
                "updateIslandShowing",
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!
            )
        )
    }

    @Throws(Throwable::class)
    fun onChargingIslandChanged(chain: XposedInterface.Chain): Any? {
        val session = module.batteries.get(chain.getThisObject())
        if (session == null) {
            return chain.proceed()
        }
        val zBooleanValue = (chain.getArg(0) as Boolean)
        val z = session.merge && module.mode == 2 && !session.ambient && !session.closed
        session.islandDepth++
        var failure: Throwable? = null
        try {
            return if (z) chain.proceed(arrayOf<Any>(false, false)) else chain.proceed()
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            try {
                checkNotNull(module.storedBatteryIsland).setBoolean(session.battery, zBooleanValue)
            } catch (error: Throwable) {
                if (failure == null) {
                    failure = error
                    throw error
                }
                if (failure !== error) {
                    failure.addSuppressed(error)
                }
            } finally {
                session.islandDepth--
                if (session.islandDepth == 0) {
                    try {
                        session.changed()
                    } catch (error: Throwable) {
                        if (failure == null) {
                            throw error
                        }
                        if (failure !== error) {
                            failure.addSuppressed(error)
                        }
                    }
                }
            }
        }
    }
}
