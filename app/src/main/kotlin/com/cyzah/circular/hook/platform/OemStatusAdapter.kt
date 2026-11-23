package com.cyzah.circular.hook.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.LinearLayout
import com.cyzah.circular.core.graphics.animation.ExpansionPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.hook.runtime.BurnInDrift
import com.cyzah.circular.hook.runtime.ModuleMain
import com.cyzah.circular.hook.runtime.OemIconSession
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.WeakHashMap
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

class OemStatusAdapter(@JvmField val module: ModuleMain, private val loader: ClassLoader?, @JvmField val profile: String?) {
    @JvmField var batteryAod: Field? = null
    @JvmField var batteryDisabled: Field? = null
    @JvmField var batteryHidden: Field? = null
    @JvmField var batteryModel: Field? = null
    @JvmField var batteryTint: Field? = null
    private var batteryType: Class<*>? = null
    @JvmField var binding: Field? = null
    @JvmField var bypass: Boolean = false
    private var cellularType: Class<*>? = null
    private var commonImpl: Field? = null
    private var context: Context? = null
    @JvmField var coordBattery: Method? = null
    @JvmField var coordMobile: Method? = null
    @JvmField var coordWifi: Method? = null
    private var dimensions: Method? = null
    @JvmField var dozingField: Field? = null
    private var fakeSource: Field? = null
    private var fakeType: Field? = null
    private var flowType: Class<*>? = null
    private var flowValue: Method? = null
    @JvmField var fraction: Float = 0f
    private var getIcon: Method? = null
    private var getSignalIcon: Method? = null
    @JvmField var getSlot: Method? = null
    private var iconInteractor: Field? = null
    @JvmField var iconSpacing: Field? = null
    private var iconsType: Class<*>? = null
    @JvmField var ignoredSlots: Field? = null
    @JvmField var installed: Boolean = false
    @JvmField var isBlocked: Method? = null
    private var keyguardType: Class<*>? = null
    @JvmField var measureChild: Method? = null
    private var mirrorReady = false
    @JvmField var mobileType: Class<*>? = null
    @JvmField var modelTint: Field? = null
    @JvmField var motionReady: Boolean = false
    private var nativeChanging: Field? = null
    private var nativeCount: Field? = null
    private var nativeInService: Field? = null
    private var nativeLevel: Field? = null
    private var nativeShowSignal: Field? = null
    private var phoneType: Class<*>? = null
    private val hookInstaller: OemHookInstaller
    private var queued = false
    @JvmField var receiverRegistered: Boolean = false
    @JvmField var shouldBeVisible: Method? = null
    private var signalReader = false
    @JvmField var statusField: Field? = null
    @JvmField var subId: Field? = null
    private var tickPending = false
    private var updating = false
    @JvmField var visibleState: Method? = null
    @JvmField var wifiType: Class<*>? = null
    @JvmField val main: Handler = Handler(Looper.getMainLooper())
    private val hooks = ArrayList<HookHandle?>()
    private val batteries = IdentityHashMap<View?, OemIconSession>()
    private val containers = IdentityHashMap<View?, OemIconSession?>()
    private val models = WeakHashMap<View?, WeakReference<Any?>?>()
    private val signalLevels = HashMap<Int?, Int?>()
    @JvmField val drift: BurnInDrift = BurnInDrift()
    private val mirrors = WeakHashMap<View, Boolean?>()
    private val panelRoots = WeakHashMap<View?, WeakReference<View?>?>()
    private var motionStatus = "native-parent"

    @JvmField @Volatile
    var controller: WeakReference<Any?> = WeakReference<Any?>(null)
    private var signalStatus = "platform"
    @JvmField var failure: String = ""
    private var attachStatus = "waiting-for-status-bar"
    private val tick: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@OemStatusAdapter.onTick()
        }
    }
    private val refresh: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@OemStatusAdapter.onRefreshQueued()
        }
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        // android.content.BroadcastReceiver
        override fun onReceive(context: Context?, intent: Intent) {
            if ("android.intent.action.BATTERY_CHANGED" == intent.getAction()) {
                val intExtra = intent.getIntExtra("level", -1)
                val intExtra2 = intent.getIntExtra("scale", 100)
                if (intExtra >= 0 && intExtra2 > 0 && intExtra <= intExtra2) {
                    val iconStateOemState = this@OemStatusAdapter.module.oemState()
                    iconStateOemState.battery = Math.round((intExtra * 100.0f) / intExtra2)
                    var z = false
                    val intExtra3 = intent.getIntExtra("status", 0)
                    if (intExtra3 == 2 || (intExtra3 == 5 && intent.getIntExtra("plugged", 0) != 0)) {
                        z = true
                    }
                    iconStateOemState.charging = z
                }
            }
            this@OemStatusAdapter.queue()
        }
    }

    fun onTick() {
        this.tickPending = false
        update()
    }

    fun onRefreshQueued() {
        this.queued = false
        update()
    }

    init {
        this.hookInstaller = OemHookInstaller(loader)
    }

    @Throws(ClassNotFoundException::class)
    private fun type(str: String?): Class<*> {
        return Class.forName(str, false, this.loader)
    }

    fun install(): Boolean {
        try {
            val zEquals = OemCompatibility.OPLUS == this.profile
            this.batteryType = type(if (zEquals) OemCompatibility.OPLUS_BATTERY else OemCompatibility.SAMSUNG_BATTERY)
            this.iconsType = type(OemCompatibility.ICONS)
            this.mobileType = type(OemCompatibility.MOBILE)
            this.wifiType = type(OemCompatibility.WIFI)
            this.phoneType = type(OemCompatibility.PHONE)
            this.keyguardType = type(OemCompatibility.KEYGUARD)
            this.subId = Companion.field(this.mobileType!!, "subId")
            this.dimensions = method(View::class.java, "setMeasuredDimension", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            val cls = this.batteryType!!
            if (zEquals) {
                this.batteryModel = field(cls, "batteryIcon")
                this.modelTint = field(this.batteryModel!!.getType(), "tintColor")
                this.batteryHidden = Companion.field(this.batteryType!!, "hidden")
                this.batteryDisabled = Companion.field(this.batteryType!!, "disable")
            } else {
                this.batteryTint = field(cls, "mTextColor")
                this.batteryAod = Companion.field(this.batteryType!!, "mIsOnAodBattery")
            }
            val clsType = type("com.android.systemui.statusbar.StatusBarStateControllerImpl")
            this.dozingField = field(clsType, "mIsDozing")
            this.statusField = field(clsType, "mState")
            for (constructor in clsType.getDeclaredConstructors()) {
                this.hooks.add(this.module.hook(constructor).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@OemStatusAdapter.onStatusBarControllerCreated(chain)
                    }
                }))
            }
            val methodArr = arrayOf<Method?>(
                method(clsType, "setState", Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                method(clsType, "setDozeAmountInternal", Float::class.javaPrimitiveType!!)
            )
            for (i in 0..1) {
                this.hooks.add(this.module.hook(methodArr[i]!!).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@OemStatusAdapter.onStatusBarControllerStateChanged(chain)
                    }
                }))
            }
            val cls2 = (if (zEquals) this.batteryType else ViewGroup::class.java)!!
            this.hooks.add(
                this.module.hook(method(cls2, "onAttachedToWindow", *arrayOfNulls<Class<*>>(0)))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onOemStatusViewAttached(chain)
                        }
                    })
            )
            this.hooks.add(
                this.module.hook(method(cls2, "onDetachedFromWindow", *arrayOfNulls<Class<*>>(0)))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onOemStatusViewDetached(chain)
                        }
                    })
            )
            this.hooks.add(
                this.module.hook(Companion.method(this.iconsType!!, "onMeasure", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onIconContainerMeasure(chain)
                        }
                    })
            )
            this.hooks.add(
                this.module.hook(
                    method(
                        type(OemCompatibility.MODERN),
                        "isIconVisible",
                        *arrayOfNulls<Class<*>>(0)
                    )
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@OemStatusAdapter.onModernIconVisibility(chain)
                    }
                })
            )
            this.hooks.add(
                this.module.hook(method(LinearLayout::class.java, "onMeasure", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onBatteryLinearLayoutMeasure(chain)
                        }
                    })
            )
            this.hooks.add(
                this.module.hook(method(ViewGroup::class.java, "dispatchDraw", Canvas::class.java))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onStatusBarDispatchDraw(chain)
                        }
                    })
            )
            this.module.deoptimize(Companion.method(this.iconsType!!, "onMeasure", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!))
            this.module.deoptimize(
                Companion.method(
                    this.iconsType!!,
                    "onLayout",
                    Boolean::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!
                )
            )
            val cls3 = this.batteryType!!
            this.hooks.add(
                this.module.hook(
                    if (zEquals) method(
                        cls3,
                        "setBatteryIcon",
                        this.batteryModel!!.getType()
                    ) else method(cls3, "updateColors", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@OemStatusAdapter.onBatteryAppearanceChanged(chain)
                    }
                })
            )
            this.installed = true
            installMotion(zEquals)
            installSignalReader()
            return true
        } catch (th: Throwable) {
            this.failure = th.javaClass.getSimpleName() + ": " + th.message
            while (!this.hooks.isEmpty()) {
                try {
                    this.hooks.removeAt(this.hooks.size - 1)!!.unhook()
                } catch (th2: Throwable) {
                }
            }
            return false
        }
    }

    @Throws(Throwable::class)
    fun onStatusBarControllerCreated(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        this.controller = WeakReference<Any?>(chain.getThisObject())
        queue()
        return objProceed
    }

    @Throws(Throwable::class)
    fun onStatusBarControllerStateChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        this.controller = WeakReference<Any?>(chain.getThisObject())
        queue()
        return objProceed
    }

    @Throws(Throwable::class)
    fun onOemStatusViewAttached(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val thisObject = chain.getThisObject()
        if (this.batteryType!!.isInstance(thisObject)) {
            this.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@OemStatusAdapter.registerBatteryHost(thisObject as View)
                }
            })
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onOemStatusViewDetached(chain: XposedInterface.Chain): Any? {
        if (this.batteryType!!.isInstance(chain.getThisObject())) {
            detach(chain.getThisObject() as View?)
        }
        return chain.proceed()
    }

    @Throws(Throwable::class)
    fun onIconContainerMeasure(chain: XposedInterface.Chain): Any? {
        val session = this.containers.get(chain.getThisObject())
        if (session != null) {
            session.reconcile()
        }
        val objProceed = chain.proceed()
        if (session != null && session.active && this.motionReady) {
            try {
                session.measureSignals((chain.getArg(0) as Int), (chain.getArg(1) as Int))
            } catch (th: Throwable) {
                session.fail(th)
            }
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onModernIconVisibility(chain: XposedInterface.Chain): Any? {
        val sessionOwner = owner(chain.getThisObject())
        if (this.bypass || sessionOwner == null || !sessionOwner.active) {
            return chain.proceed()
        }
        return false
    }

    @Throws(Throwable::class)
    fun onBatteryLinearLayoutMeasure(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = this.batteries.get(chain.getThisObject())
        if (session != null && session.active) {
            try {
                val iIntValue = (chain.getArg(1) as Int)
                val iIntValue2 = (chain.getArg(0) as Int)
                var measuredHeight = session.battery.getMeasuredHeight()
                val iPixels = session.pixels()
                val mode = View.MeasureSpec.getMode(iIntValue)
                if (mode != 1073741824) {
                    measuredHeight = max(measuredHeight, iPixels)
                }
                if (mode != 0) {
                    measuredHeight = min(measuredHeight, View.MeasureSpec.getSize(iIntValue))
                }
                session.stockWidth = session.battery.getMeasuredWidth()
                var iMin = min(iPixels, measuredHeight)
                if (this.motionReady) {
                    iMin = ExpansionPolicy.width(iMin, session.stockWidth, session.networkWidth, session.progress)
                }
                if (View.MeasureSpec.getMode(iIntValue2) != View.MeasureSpec.UNSPECIFIED) {
                    iMin = min(iMin, View.MeasureSpec.getSize(iIntValue2))
                }
                if (iMin < 1 || measuredHeight < 1) {
                    session.fail(IllegalStateException("no-battery-space"))
                } else {
                    this.dimensions!!.invoke(session.battery, iMin, measuredHeight)
                }
            } catch (th: Throwable) {
                session.fail(th)
            }
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onStatusBarDispatchDraw(chain: XposedInterface.Chain): Any? {
        val session = this.batteries.get(chain.getThisObject())
        if (!this.bypass && session != null && session.active) {
            val canvas = chain.getArg(0) as Canvas
            val iSave = canvas.save()
            var drawFailure: Throwable? = null
            session.nativeDrawStack.push(chain)
            try {
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
                session.fail(drawFailure)
            }
            // Pixels already drawn cannot be undone by restoring the Canvas.
            // Relinquish ownership for the next traversal without drawing twice here.
            return null
        }
        val sessionOwner = owner(chain.getThisObject())
        if (this.bypass || sessionOwner == null || !sessionOwner.active) {
            return chain.proceed()
        }
        return null
    }

    @Throws(Throwable::class)
    fun onBatteryAppearanceChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = this.batteries.get(chain.getThisObject())
        if (session != null) {
            session.battery.invalidate()
        }
        return objProceed
    }

    private fun installMotion(z: Boolean) {
        val size = this.hooks.size
        try {
            check(
                this.hookInstaller.matches(
                    OemExpansion.COMMON,
                    if (z) OemExpansion.OPLUS else OemExpansion.SAMSUNG
                )
            ) { "motion-contract-unavailable" }
            val clsType = type(OemCompatibility.MODERN)
            if (z) {
                this.coordBattery =
                    Companion.method(this.batteryType!!, "getTransXForCoord", *arrayOfNulls<Class<*>>(0))
                this.coordMobile = method(
                    type("com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarMobileView"),
                    "getTransXForCoord",
                    *arrayOfNulls<Class<*>>(0)
                )
                this.coordWifi = method(
                    type("com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarWifiView"),
                    "getTransXForCoord",
                    *arrayOfNulls<Class<*>>(0)
                )
            }
            this.binding = field(clsType, "binding")
            this.ignoredSlots = Companion.field(this.iconsType!!, "mIgnoredSlots")
            if (!z) {
                this.iconSpacing = Companion.field(this.iconsType!!, "mIconSpacing")
            }
            this.shouldBeVisible =
                type("com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding").getMethod(
                    "getShouldIconBeVisible",
                    *arrayOfNulls<Class<*>>(0)
                )
            val clsType2 = type("com.android.systemui.statusbar.StatusIconDisplayable")
            this.isBlocked = clsType2.getMethod("isIconBlocked", *arrayOfNulls<Class<*>>(0))
            this.getSlot = clsType2.getMethod("getSlot", *arrayOfNulls<Class<*>>(0))
            this.visibleState = method(clsType, "setVisibleState", Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!)
            this.measureChild =
                method(ViewGroup::class.java, "measureChild", View::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            this.hooks.add(this.module.hook(this.visibleState!!).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@OemStatusAdapter.onModernVisibleState(chain)
                }
            }))
            this.hooks.add(
                this.module.hook(
                    Companion.method(
                        this.batteryType!!,
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
                        return this@OemStatusAdapter.onBatteryLayout(chain)
                    }
                })
            )
            this.hooks.add(
                this.module.hook(
                    Companion.method(
                        this.iconsType!!,
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
                        return this@OemStatusAdapter.onIconContainerLayout(chain)
                    }
                })
            )
            if (z) {
                val clsType3 = type(OemExpansion.OPLUS_STATE)
                val field: Field = field(clsType3, "panelExpandFraction")
                this.hooks.add(
                    this.module.hook(
                        method(
                            clsType3,
                            "notifyExpandFractionChange",
                            Float::class.javaPrimitiveType!!,
                            Int::class.javaPrimitiveType!!
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onOplusPanelFraction(field, chain)
                        }
                    })
                )
                val clsType4 = type(OemExpansion.OPLUS_HEADER)
                val field2: Field = field(clsType4, "mBatteryView")
                val field3: Field = field(clsType4, "mIconContainer")
                this.hooks.add(
                    this.module.hook(method(clsType4, "onFinishInflate", *arrayOfNulls<Class<*>>(0)))
                        .intercept(object : Hooker {
                            @Throws(Throwable::class)
                            override fun intercept(chain: XposedInterface.Chain): Any? {
                                return this@OemStatusAdapter.registerOplusPanel(field2, field3, chain)
                            }
                        })
                )
            } else {
                val clsType5 = type(OemExpansion.PANEL)
                val field4: Field = field(clsType5, "mExpandedFraction")
                this.hooks.add(
                    this.module.hook(
                        method(
                            clsType5,
                            "updateExpansionAndVisibility",
                            *arrayOfNulls<Class<*>>(0)
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onSamsungPanelFraction(field4, chain)
                        }
                    })
                )
                this.hooks.add(
                    this.module.hook(
                        method(
                            type("com.android.systemui.qs.QSImpl"),
                            "setQsExpansion",
                            Float::class.javaPrimitiveType!!,
                            Float::class.javaPrimitiveType!!,
                            Float::class.javaPrimitiveType!!,
                            Float::class.javaPrimitiveType!!
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onQuickSettingsExpansion(chain)
                        }
                    })
                )
                val clsType6 = type(OemExpansion.SHADE)
                val field5: Field = field(clsType6, "batteryIcon")
                val field6: Field = field(clsType6, "iconContainer")
                val field7: Field = field(clsType6, "header")
                this.hooks.add(
                    this.module.hook(method(clsType6, "onViewAttached", *arrayOfNulls<Class<*>>(0)))
                        .intercept(object : Hooker {
                            @Throws(Throwable::class)
                            override fun intercept(chain: XposedInterface.Chain): Any? {
                                return this@OemStatusAdapter.registerSamsungPanel(field5, field6, field7, chain)
                            }
                        })
                )
            }
            this.motionReady = true
            this.motionStatus = "miui-style-native-fraction"
            if (z) {
                installMirrors()
            }
        } catch (th: Throwable) {
            while (this.hooks.size > size) {
                try {
                    this.hooks.removeAt(this.hooks.size - 1)!!.unhook()
                } catch (th2: Throwable) {
                }
            }
            this.motionReady = false
            this.motionStatus = "native-parent (" + th.javaClass.getSimpleName() + ")"
        }
    }

    @Throws(Throwable::class)
    fun onModernVisibleState(chain: XposedInterface.Chain): Any? {
        val sessionOwner = owner(chain.getThisObject())
        return if (sessionOwner == null || !sessionOwner.active) chain.proceed() else chain.proceed(
            arrayOf<Any>(
                0,
                false
            )
        )
    }

    @Throws(Throwable::class)
    fun onBatteryLayout(chain: XposedInterface.Chain): Any? {
        val session = this.batteries.get(chain.getThisObject())
        return if (session == null || !session.active || session.stockWidth <= 0) chain.proceed() else chain.proceed(
            arrayOf<Any?>(
                chain.getArg(0),
                chain.getArg(1),
                chain.getArg(2),
                (chain.getArg(1) as Int) + session.stockWidth,
                chain.getArg(4)
            )
        )
    }

    @Throws(Throwable::class)
    fun onIconContainerLayout(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = this.containers.get(chain.getThisObject())
        if (session != null && session.active) {
            session.layoutSignals()
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onOplusPanelFraction(field: Field, chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        applyPanelExpansion(field.getFloat(chain.getThisObject()))
        return objProceed
    }

    @Throws(Throwable::class)
    fun registerOplusPanel(field: Field, field2: Field, chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val thisObject = chain.getThisObject()
        registerPanel(field.get(thisObject) as View?, field2.get(thisObject) as ViewGroup?, thisObject as View?)
        return objProceed
    }

    @Throws(Throwable::class)
    fun onSamsungPanelFraction(field: Field, chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        applyPanelExpansion(field.getFloat(chain.getThisObject()))
        return objProceed
    }

    @Throws(Throwable::class)
    fun onQuickSettingsExpansion(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        applyPanelExpansion((chain.getArg(1) as Float))
        return objProceed
    }

    @Throws(Throwable::class)
    fun registerSamsungPanel(field: Field, field2: Field, field3: Field, chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val thisObject = chain.getThisObject()
        registerPanel(
            field.get(thisObject) as View?,
            field2.get(thisObject) as ViewGroup?,
            field3.get(thisObject) as View?
        )
        return objProceed
    }

    fun applyPanelExpansion(f: Float) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            this.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@OemStatusAdapter.applyPanelExpansion(f)
                }
            })
            return
        }
        val fClamp = Motion.clamp(f)
        if (this.fraction == fClamp) {
            return
        }
        this.fraction = fClamp
        this.drift.update(SystemClock.elapsedRealtime(), this.fraction > 0.0f && this.fraction < 1.0f)
        for (session in this.batteries.values) {
            session.reconcile()
            session.battery.invalidate()
        }
    }

    private fun registerPanel(view: View?, viewGroup: ViewGroup?, view2: View?) {
        if (!this.batteryType!!.isInstance(view) || !this.iconsType!!.isInstance(viewGroup) || view2 == null || view!!.getParent() !== viewGroup!!.getParent()) {
            return
        }
        this.panelRoots.put(view, WeakReference<View?>(view2))
        this.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                this@OemStatusAdapter.registerBatteryHost(view)
            }
        })
    }

    private fun installMirrors() {
        val size = this.hooks.size
        try {
            if (this.hookInstaller.matches(OemExpansion.FAKE_MEMBERS)) {
                val clsType = type(OemExpansion.FAKE)
                this.fakeSource = field(clsType, "mView")
                this.fakeType = field(clsType, "type")
                this.hooks.add(
                    this.module.hook(
                        method(
                            clsType,
                            "setFakeView",
                            View::class.java,
                            type("com.oplus.systemui.seamless.FakeType")
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onMirrorAttached(chain)
                        }
                    })
                )
                this.hooks.add(
                    this.module.hook(method(clsType, "dispatchDraw", Canvas::class.java)).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@OemStatusAdapter.onMirrorDispatched(chain)
                        }
                    })
                )
                this.mirrorReady = true
            }
        } catch (th: Throwable) {
            while (this.hooks.size > size) {
                this.hooks.removeAt(this.hooks.size - 1)!!.unhook()
            }
            this.mirrorReady = false
        }
    }

    @Throws(Throwable::class)
    fun onMirrorAttached(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        this.mirrors.put(chain.getThisObject() as View?, true)
        queue()
        return objProceed
    }

    @Throws(Throwable::class)
    fun onMirrorDispatched(chain: XposedInterface.Chain): Any? {
        val view = chain.getThisObject() as View?
        if (ExpansionPolicy.fakeType(this.fakeType!!.get(view).toString())) {
            this.mirrors.put(view, true)
            val it = this.batteries.values.iterator()
            while (it.hasNext()) {
                it.next()!!.reconcile()
            }
        }
        return chain.proceed()
    }

    fun mirrored(session: OemIconSession): Boolean {
        if (!this.mirrorReady || this.fraction <= 0.0f) {
            return false
        }
        for (view in this.mirrors.keys) {
            try {
                if (view.isAttachedToWindow() && view.isShown() && view.getAlpha() > 0.0f && view.getDisplay() != null && session.battery.getDisplay() != null && view.getDisplay()
                        .getDisplayId() == session.battery.getDisplay()
                        .getDisplayId() && ExpansionPolicy.fakeType(this.fakeType!!.get(view).toString())
                ) {
                    val obj = this.fakeSource!!.get(view)
                    var parent = session.battery.getParent()
                    while (parent is View) {
                        if (parent === obj) {
                            return true
                        }
                        parent = parent.getParent()
                    }
                }
            } catch (e: ReflectiveOperationException) {
            }
        }
        return false
    }

    private fun installSignalReader() {
        val size = this.hooks.size
        try {
            val clsType =
                type("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel")
            this.flowType = type("kotlinx.coroutines.flow.StateFlow")
            this.flowValue = this.flowType!!.getMethod("getValue", *arrayOfNulls<Class<*>>(0))
            this.getIcon = clsType.getMethod("getIcon", *arrayOfNulls<Class<*>>(0))
            if (OemCompatibility.SAMSUNG == this.profile) {
                this.commonImpl = field(clsType, "commonImpl")
                this.iconInteractor = field(
                    type("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MobileIconViewModel"),
                    "iconInteractor"
                )
                this.getSignalIcon =
                    type("com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MobileIconInteractor").getMethod(
                        "getSignalLevelIcon",
                        *arrayOfNulls<Class<*>>(0)
                    )
            }
            this.cellularType =
                type("com.android.systemui.statusbar.pipeline.mobile.domain.model.SignalIconModel\$Cellular")
            this.nativeLevel = Companion.field(this.cellularType!!, "level")
            this.nativeCount = Companion.field(this.cellularType!!, "numberOfLevels")
            this.nativeChanging = Companion.field(this.cellularType!!, "carrierNetworkChange")
            try {
                this.nativeInService = Companion.field(this.cellularType!!, "isInService")
            } catch (e: Exception) {
            }
            try {
                this.nativeShowSignal = Companion.field(this.cellularType!!, "showSignal")
            } catch (e2: Exception) {
            }
            this.hooks.add(
                this.module.hook(
                    Companion.method(
                        this.mobileType!!,
                        "constructAndBind",
                        Context::class.java,
                        type("com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger"),
                        String::class.java,
                        clsType
                    )
                ).intercept(object : Hooker {
                    @Throws(Throwable::class)
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        return this@OemStatusAdapter.onNativeSignalFlow(chain)
                    }
                })
            )
            this.signalReader = true
            this.signalStatus = "native-flow-ready; platform-until-sampled"
        } catch (th: Throwable) {
            while (this.hooks.size > size) {
                this.hooks.removeAt(this.hooks.size - 1)!!.unhook()
            }
            this.signalStatus = "platform (native-flow-unavailable)"
        }
    }

    @Throws(Throwable::class)
    fun onNativeSignalFlow(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        if (objProceed is View) {
            val view = objProceed
            val arg = chain.getArg(3)
            this.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@OemStatusAdapter.bindOemSignalModel(view, arg)
                }
            })
        }
        return objProceed
    }

    fun bindOemSignalModel(view: View?, obj: Any?) {
        this.models.put(view, WeakReference<Any?>(obj))
        queue()
    }

    fun readSignals(session: OemIconSession) {
        if (this.signalReader) {
            for (i in 0..<session.icons.getChildCount()) {
                val childAt = session.icons.getChildAt(i)
                if (this.mobileType!!.isInstance(childAt)) {
                    try {
                        val i2 = this.subId!!.getInt(childAt)
                        if (i2 >= 0) {
                            val weakReference = this.models.get(childAt)
                            val obj = if (weakReference == null) null else weakReference.get()
                            var objInvoke = if (obj == null) null else this.getIcon!!.invoke(obj, *arrayOfNulls<Any>(0))
                            if (!this.flowType!!.isInstance(objInvoke) && obj != null && this.commonImpl != null) {
                                val obj2 = this.commonImpl!!.get(obj)
                                if (this.iconInteractor!!.getDeclaringClass().isInstance(obj2)) {
                                    objInvoke = this.getSignalIcon!!.invoke(
                                        this.iconInteractor!!.get(obj2),
                                        *arrayOfNulls<Any>(0)
                                    )
                                }
                            }
                            val objInvoke2 = if (this.flowType!!.isInstance(objInvoke)) this.flowValue!!.invoke(
                                objInvoke,
                                *arrayOfNulls<Any>(0)
                            ) else null
                            val iNormalizedLevel =
                                if (this.cellularType!!.isInstance(objInvoke2) && !this.nativeChanging!!.getBoolean(
                                        objInvoke2
                                    ) && (this.nativeInService == null || this.nativeInService!!.getBoolean(objInvoke2)) && (this.nativeShowSignal == null || this.nativeShowSignal!!.getBoolean(
                                        objInvoke2
                                    ))
                                ) OemCompatibility.normalizedLevel(
                                    this.nativeLevel!!.getInt(objInvoke2),
                                    this.nativeCount!!.getInt(objInvoke2)
                                ) else -1
                            val numPut = this.signalLevels.put(i2, iNormalizedLevel)
                            if (numPut == null || numPut != iNormalizedLevel) {
                                this.module.oemSignal(i2, iNormalizedLevel)
                            }
                            if (i2 == this.module.oemState().subId) {
                                this.signalStatus =
                                    if (iNormalizedLevel >= 0) "native-flow level=" + iNormalizedLevel else "platform (no-readable-native-sample)"
                            }
                        }
                    } catch (th: Throwable) {
                        try {
                            val i3 = this.subId!!.getInt(childAt)
                            val numPut2 = this.signalLevels.put(i3, -1)
                            if (i3 >= 0 && (numPut2 == null || numPut2 != -1)) {
                                this.module.oemSignal(i3, -1)
                            }
                        } catch (th2: Throwable) {
                        }
                        this.signalStatus = "platform (native-read-failed:" + th.javaClass.getSimpleName() + ")"
                    }
                }
            }
        }
    }

    private fun owner(obj: Any?): OemIconSession? {
        if (obj !is View) {
            return null
        }
        if (this.mobileType!!.isInstance(obj) || this.wifiType!!.isInstance(obj)) {
            return (obj.parent as? View)?.let(this.containers::get)
        }
        return null
    }

    fun registerBatteryHost(view: View) {
        var view = view
        if (!this.installed || !view.isAttachedToWindow() || this.batteries.containsKey(view)) {
            return
        }
        try {
            if (view.getParent() is ViewGroup) {
                val viewGroup = view.getParent() as ViewGroup
                var viewGroup2: ViewGroup? = null
                var i = 0
                while (i < viewGroup.getChildCount()) {
                    val view3 = view
                    if (this.iconsType!!.isInstance(viewGroup.getChildAt(i))) {
                        if (viewGroup2 != null) {
                            return
                        } else {
                            viewGroup2 = viewGroup.getChildAt(i) as ViewGroup?
                        }
                    }
                    i++
                    view = view3
                }
                if (viewGroup2 == null) {
                    this.attachStatus = "no-sibling-signal-container"
                    return
                }
                val weakReference = this.panelRoots.get(view)
                val view4 = if (weakReference != null) weakReference.get() else null
                val z2 = view4 != null
                var view2 = view4
                var z = false
                var parent: ViewParent? = viewGroup
                while (view2 == null && (parent is View)) {
                    if (this.keyguardType!!.isInstance(parent)) {
                        view2 = parent as View
                        z = true
                        break
                    } else {
                        if (this.phoneType!!.isInstance(parent)) {
                            view2 = parent as View
                            z = false
                            break
                        }
                    }
                    parent = parent.getParent()
                }
                if (view2 == null) {
                    this.attachStatus = "native-only-parent:" + viewGroup.javaClass.getSimpleName()
                    return
                }
                if (this.containers.containsKey(viewGroup2)) {
                    return
                }
                val context = view.context.applicationContext ?: view.context
                this.context = context
                this.module.oemContext(context)
                val view5 = view
                val session = OemIconSession(this, view5, viewGroup2, view2, z, z2)
                this.batteries.put(view5, session)
                this.containers.put(viewGroup2, session)
                session.observe()
                this.attachStatus = "paired"
                this.module.oemHostsChanged()
                update()
            }
        } catch (th: Throwable) {
            this.failure = "attach:" + th.javaClass.getSimpleName()
        }
    }

    private fun detach(view: View?) {
        val sessionRemove = this.batteries.remove(view)
        if (sessionRemove == null) {
            return
        }
        this.containers.remove(sessionRemove.icons)
        sessionRemove.close()
        this.module.oemHostsChanged()
        queue()
    }

    fun hasHosts(): Boolean {
        return !this.batteries.isEmpty()
    }

    fun queue() {
        if (this.queued) {
            return
        }
        this.queued = true
        this.main.post(this.refresh)
    }

    fun update() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            queue()
            return
        }
        if (this.updating) {
            return
        }
        this.updating = true
        try {
            try {
                val z = this.installed && hasHosts() && this.module.oemEnabled()
                if (z && !this.receiverRegistered && this.context != null) {
                    try {
                        val intentFilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
                        intentFilter.addAction("android.intent.action.SCREEN_ON")
                        intentFilter.addAction("android.intent.action.SCREEN_OFF")
                        val intentRegisterReceiver = this.context!!.registerReceiver(
                            this.receiver,
                            intentFilter,
                            null,
                            this.main,
                            Context.RECEIVER_EXPORTED
                        )
                        this.receiverRegistered = true
                        if (intentRegisterReceiver != null) {
                            this.receiver.onReceive(this.context, intentRegisterReceiver)
                        }
                    } catch (e: RuntimeException) {
                        this.failure = "battery-receiver:" + e.javaClass.getSimpleName()
                    }
                }
                if (!z && this.receiverRegistered) {
                    this.context!!.unregisterReceiver(this.receiver)
                    this.receiverRegistered = false
                }
                this.drift.update(
                    SystemClock.elapsedRealtime(),
                    this.motionReady && this.fraction > 0.0f && this.fraction < 1.0f
                )
                var z2 = false
                for (session in this.batteries.values) {
                    session.reconcile()
                    if (session.active && session.battery.isShown()) {
                        z2 = true
                    }
                    session.battery.invalidate()
                }
                if (z2 && !this.tickPending) {
                    this.tickPending = true
                    this.main.postDelayed(this.tick, BurnInDrift.Companion.delay(SystemClock.elapsedRealtime()))
                }
                if (!z2 && this.tickPending) {
                    this.main.removeCallbacks(this.tick)
                    this.tickPending = false
                }
            } catch (e2: RuntimeException) {
                this.failure = "update:" + e2.javaClass.getSimpleName()
            }
        } finally {
            this.updating = false
        }
    }

    fun diagnostic(): String {
        val sbAppend =
            StringBuilder("profile=").append(this.profile).append(" core=").append(this.installed).append(" hosts=")
                .append(this.batteries.size)
                .append("\nsurfaces=status-bar,bright-keyguard,verified-panel aod=native motion=")
                .append(this.motionStatus).append(" fraction=").append(this.fraction).append(" mirror=")
                .append(this.mirrorReady).append(" keepMergedShade=").append(this.module.oemKeepMergedShade())
                .append("\nsignalAdapter=").append(this.signalStatus).append(" attach=").append(this.attachStatus)
                .append(" batteryReceiver=").append(this.receiverRegistered)
        for (session in this.batteries.values) {
            sbAppend.append("\nhost=")
                .append(if (session.panel) "panel" else if (session.keyguard) "keyguard" else "status-bar")
                .append(" merge=").append(session.active).append(" progress=").append(session.progress)
                .append(" networkWidth=").append(session.networkWidth).append(" reason=").append(session.reason)
                .append(" size=").append(session.battery.getWidth()).append('x').append(session.battery.getHeight())
                .append(" tint=#").append(
                    Integer.toHexString(session.lastTint)
                )
        }
        if (!this.failure.isEmpty()) {
            sbAppend.append("\nadapterError=").append(this.failure)
        }
        return sbAppend.toString()
    }

    companion object {
        @Throws(Exception::class)
        private fun field(cls: Class<*>, str: String): Field {
            val declaredField = cls.getDeclaredField(str)
            declaredField.setAccessible(true)
            return declaredField
        }

        @Throws(Exception::class)
        private fun method(cls: Class<*>, str: String, vararg clsArr: Class<*>?): Method {
            val declaredMethod = cls.getDeclaredMethod(str, *clsArr)
            declaredMethod.setAccessible(true)
            return declaredMethod
        }
    }
}
