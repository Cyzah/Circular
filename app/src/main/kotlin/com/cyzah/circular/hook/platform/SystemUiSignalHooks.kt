package com.cyzah.circular.hook.platform

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.cyzah.circular.core.graphics.PresentationPolicy
import com.cyzah.circular.core.network.SignalResourcePolicy
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.hook.platform.Compatibility
import com.cyzah.circular.hook.runtime.IconSession
import com.cyzah.circular.hook.runtime.ModuleMain
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Predicate
import kotlin.math.max

class SystemUiSignalHooks(private val module: ModuleMain) {
    private val compatibility: Compatibility.Result get() = checkNotNull(module.compatibility)
    private var nativeTechnologyHook = false
    private var technologyRefreshStep = 0
    private var technologyExpansionPhase = 0
    private val technologyRefresh: Runnable = object : Runnable {
        override fun run() {
            if (module.networks?.running() != true) return
            refreshNativeTechnology()
            // A short burst per platform event handles asynchronous StateFlow updates.
            // With the verified sink installed, a later emission always corrects
            // an upstream value that has not caught up with the platform yet.
            if (++technologyRefreshStep < 3) {
                module.main.postDelayed(this, if (technologyRefreshStep == 1) 120L else 480L)
            }
        }
    }
    private val requestTechnologyRefresh: Runnable = object : Runnable {
        override fun run() {
            module.main.removeCallbacks(technologyRefresh)
            technologyRefreshStep = 0
            module.main.post(technologyRefresh)
        }
    }

    @Throws(Exception::class)
    fun hookKeyguard(classLoader: ClassLoader?, cls: Class<*>) {
        val keyguardType =
            ModuleMain.Companion.cls(classLoader, "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView")
        module.keyguardType = keyguardType
        if (compatibility.legacyKeyguard) {
            module.legacyToAod = ModuleMain.Companion.field(cls, "mToAod")
        } else {
            module.toLockScreen = ModuleMain.Companion.field(keyguardType, "mToLockScreen")
        }
        if (compatibility.isAvailable(Compatibility.Capability.KEYGUARD_EVENTS)) {
            val size = module.installed.size
            try {
                module.installed.add(
                    module.hook(
                        ModuleMain.Companion.method(
                            keyguardType,
                            "initCallback",
                            String::class.java
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiSignalHooks.onKeyguardCallbackInitialized(chain)
                        }
                    })
                )
                module.installed.add(
                    module.hook(
                        ModuleMain.Companion.method(
                            ModuleMain.Companion.cls(
                                classLoader,
                                OemCompatibility.KEYGUARD
                            ), "setVisibility", Int::class.javaPrimitiveType!!
                        )
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiSignalHooks.onKeyguardVisibilityChanged(chain)
                        }
                    })
                )
            } catch (th: Throwable) {
                module.rollback(size)
                compatibility.markUnavailable(
                    Compatibility.Capability.KEYGUARD_EVENTS,
                    "install",
                    th.toString()
                )
            }
        }
        if (compatibility.isAvailable(Compatibility.Capability.AOD_EVENTS)) {
            val size2 = module.installed.size
            try {
                val cls2: Class<*> = ModuleMain.Companion.cls(
                    classLoader,
                    "com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject"
                )
                val field: Field = ModuleMain.Companion.field(cls2, "keyguardStatusBarView")
                module.installed.add(
                    module.hook(
                        if (compatibility.legacyAod) ModuleMain.Companion.method(
                            cls2,
                            "animateFullAod",
                            Boolean::class.javaPrimitiveType!!
                        ) else ModuleMain.Companion.method(cls2, "animateFullAod", Boolean::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!)
                    ).intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiSignalHooks.onAodAnimationBoundary(field, chain)
                        }
                    })
                )
                for (str in if (compatibility.legacyAod) arrayOf<String>("setIsAodAnimate") else arrayOf<String>(
                    "toggleAodMode",
                    "setIsAodAnimate"
                )) {
                    module.installed.add(
                        module.hook(ModuleMain.Companion.method(cls, str, Boolean::class.javaPrimitiveType!!)).intercept(object : Hooker {
                            @Throws(Throwable::class)
                            override fun intercept(chain: XposedInterface.Chain): Any? {
                                return this@SystemUiSignalHooks.onBatteryAodStateChanged(chain)
                            }
                        })
                    )
                }
            } catch (th: Throwable) {
                module.rollback(size2)
                compatibility.markUnavailable(
                    Compatibility.Capability.AOD_EVENTS, "install", th.toString()
                )
                if (compatibility.legacyKeyguard) {
                    throw IllegalStateException("Legacy AOD gate unavailable", th)
                }
            }
        }
    }

    @Throws(Throwable::class)
    fun onKeyguardCallbackInitialized(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        reconcileKeyguardOnMainThread(chain.getThisObject())
        return objProceed
    }

    @Throws(Throwable::class)
    fun onKeyguardVisibilityChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        reconcileKeyguardOnMainThread(chain.getThisObject())
        return objProceed
    }

    @Throws(Throwable::class)
    fun onAodAnimationBoundary(field: Field, chain: XposedInterface.Chain): Any? {
        if (compatibility.legacyKeyguard) {
            try {
                val view = field.get(chain.getThisObject()) as View?
                if (view != null) {
                    module.legacyLockTargets.put(view, chain.getArg(0) as Boolean?)
                }
            } catch (th: Throwable) {
            }
        }
        val objProceed = chain.proceed()
        try {
            reconcileKeyguardOnMainThread(field.get(chain.getThisObject()))
        } catch (th2: Throwable) {
            module.log(5, ModuleMain.Companion.TAG, "AOD boundary update unavailable", th2)
        }
        return objProceed
    }

    @Throws(Throwable::class)
    fun onBatteryAodStateChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val session = module.batteries.get(chain.getThisObject())
        if (session != null) {
            session.changed()
            module.report(false)
        }
        return objProceed
    }

    @Throws(IllegalAccessException::class)
    fun isBrightKeyguard(view: View?, view2: View): Boolean {
        if (compatibility.legacyKeyguard) {
            val bool = module.legacyLockTargets.get(view)
            return if (bool != null) bool else !checkNotNull(module.legacyToAod).getBoolean(view2)
        }
        if (checkNotNull(module.toLockScreen).getBoolean(view)) {
            return true
        }
        // HyperOS 4 实测：亮屏锁屏下 mToLockScreen 仍为 false，会把亮屏锁屏误判成 AOD，
        // 于是 IconPainter 强制使用原生着色 → 充电 / 低电 / 省电 / 性能配色在锁屏上全部丢失。
        // 补判：屏幕处于交互态 + 状态栏仍处于锁屏状态 ⇒ 判定为亮屏锁屏。
        val display = view2.getDisplay()
        return PresentationPolicy.brightKeyguard(
            module.screenInteractive(view2),
            display != null && display.getState() == Display.STATE_ON,
            module.keyguardStatusBarState(view2)
        )
    }

    /** 屏幕是否处于交互态（AOD 息屏时为 false）  */
    fun screenInteractive(view: View?): Boolean {
        try {
            if (module.power == null && view != null) {
                module.power = view.getContext().getSystemService<PowerManager?>(PowerManager::class.java)
            }
            return module.power?.isInteractive() == true
        } catch (th: Throwable) {
            return false
        }
    }

    /** 电量视图记录的 mStatusBarState 是否仍处于锁屏（1=KEYGUARD，2=SHADE_LOCKED）  */
    fun keyguardStatusBarState(view: View?): Boolean {
        try {
            if (view == null) return false
            val statusState = module.statusState ?: return false
            val i = statusState.getInt(view)
            return i == 1 || i == 2
        } catch (th: Throwable) {
            return false
        }
    }

    fun reconcileKeyguardOnMainThread(obj: Any?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            module.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@SystemUiSignalHooks.reconcileKeyguardOnMainThread(obj)
                }
            })
            return
        }
        val session = module.keyguards.get(obj)
        if (session != null) {
            session.changed()
            module.report(false)
        }
    }

    fun setupSignalResources(classLoader: ClassLoader?) {
        try {
            val cls: Class<*> =
                ModuleMain.Companion.cls(classLoader, "com.miui.systemui.statusbar.mobile.TelephonyIcons")
            val field: Field = ModuleMain.Companion.field(cls, "TELEPHONY_SIGNAL_STRENGTH")
            val field2: Field = ModuleMain.Companion.field(cls, "TELEPHONY_SIGNAL_NO_VOICE_STRENGTH")
            require(
                !(field.getType() != IntArray::class.java || field2.getType() != IntArray::class.java || !Modifier.isStatic(
                    field.getModifiers()
                ) || !Modifier.isStatic(field2.getModifiers()))
            ) { "table signature" }
            module.signalPolicy = SignalResourcePolicy(field.get(null) as IntArray?, field2.get(null) as IntArray?)
            module.signalAdapter = if (module.signalPolicy.hasTable()) "resource-table" else "resource-name"
        } catch (th: Throwable) {
            module.signalAdapter = "resource-name (table unavailable)"
        }
        try {
            module.imageResource = ModuleMain.Companion.field(ImageView::class.java, "mResource")
            if (checkNotNull(module.imageResource).getType() != Int::class.javaPrimitiveType!!) {
                module.imageResource = null
            }
        } catch (e2: Exception) {
            module.imageResource = null
        }
    }

    fun refreshNativeTechnologyForExpansion(control: Float, shade: Float) {
        val expansion = max(control, shade)
        val phase = if (expansion <= 0.0f) 0 else if (expansion >= 1.0f) 2 else 1
        if (phase == this.technologyExpansionPhase) {
            return
        }
        this.technologyExpansionPhase = phase
        if (module.networks?.running() == true) {
            // Gesture boundaries share the event-driven burst, never a per-frame scan.
            this.requestTechnologyRefresh.run()
        }
    }

    /**
     * 读取系统原生状态栏当前显示的制式文本（`MiuiMobileIconVMImpl.showName`），
     * 按 subId 记入 NetworkSource，作为模块制式文本的首选来源。
     */
    fun refreshNativeTechnology() {
        val showName = module.vmShowName ?: return
        val showNameValue = module.vmShowNameValue ?: return
        val networks = module.networks ?: return
        networks.setNativeTechnologyRefresh(requestTechnologyRefresh)
        for (entry in module.boundSignalModels.entries) {
            val view = entry.key
            val weakReference = entry.value
            val obj = if (weakReference == null) null else weakReference.get()
            if (view == null || !view.isAttachedToWindow() || obj == null) {
                continue
            }
            try {
                val objShowName = showName.invoke(obj, *arrayOfNulls<Any>(0))
                if (objShowName == null) {
                    continue
                }
                val objValue = showNameValue.invoke(objShowName, *arrayOfNulls<Any>(0))
                if (objValue !is String) {
                    continue
                }
                var iSubId = -1
                try {
                    val objInteractor = module.vmSignalInteractor?.get(obj)
                    val signalSub = module.vmSignalSub
                    iSubId = if (objInteractor == null || signalSub == null) {
                        checkNotNull(module.mobileSubId).getInt(view)
                    } else {
                        signalSub.getInt(objInteractor)
                    }
                } catch (th2: Throwable) {
                    iSubId = -1
                }
                if (iSubId < 0 || checkNotNull(module.mobileSubId).getInt(view) != iSubId) {
                    continue
                }
                networks.nativeTechnology(iSubId, objValue, nativeTechnologyHook)
            } catch (th3: Throwable) {
            }
        }
    }

    @Throws(Exception::class)
    private fun signalFields(classLoader: ClassLoader?) {
        val cls: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl"
        )
        module.vmSignalResource = ModuleMain.Companion.field(cls, "lastSignalId")
        try {
            module.vmSignalContext = ModuleMain.Companion.field(cls, "context")
        } catch (e: NoSuchFieldException) {
            module.vmSignalContext = null
        }
        module.vmSignalInteractor = ModuleMain.Companion.field(cls, "iconInteractor")
        module.vmSignalSub = ModuleMain.Companion.field(
            ModuleMain.Companion.cls(
                classLoader,
                "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl"
            ), "subId"
        )
        try {
            // 系统自己算出的制式文本：MiuiMobileIconVMImpl.showName（StateFlow<String>）
            module.vmShowName = ModuleMain.Companion.method(cls, "getShowName", *arrayOfNulls<Class<*>>(0))
            module.vmShowNameValue = ModuleMain.Companion.method(
                ModuleMain.Companion.cls(classLoader, "kotlinx.coroutines.flow.StateFlow"),
                "getValue",
                *arrayOfNulls<Class<*>>(0)
            )
        } catch (th: Throwable) {
            module.vmShowName = null
            module.vmShowNameValue = null
        }
        require(
            !(checkNotNull(module.vmSignalResource).getType() != Int::class.javaPrimitiveType!! || checkNotNull(module.vmSignalSub).getType() != Int::class.javaPrimitiveType!! || (module.vmSignalContext?.type?.let { !Context::class.java.isAssignableFrom(it) } == true))
        ) { "signal field types" }
    }

    fun resourceName(context: Context?, i: Int): String? {
        var resourceEntryName: String?
        if (i <= 0) {
            return "none"
        }
        if (context == null) {
            return "id:" + Integer.toHexString(i)
        }
        val str = module.signalResources.get(i)
        if (str != null) {
            return str
        }
        try {
            resourceEntryName = context.getResources().getResourceEntryName(i)
        } catch (e: NotFoundException) {
            resourceEntryName = "id:" + Integer.toHexString(i)
        }
        if (module.signalResources.size > 128) {
            module.signalResources.clear()
        }
        module.signalResources.put(i, resourceEntryName)
        return resourceEntryName
    }

    fun refreshBoundNativeSignal(obj: Any?) {
        if (obj == null) return
        val signalResource = module.vmSignalResource ?: return
        try {
            val i = checkNotNull(module.vmSignalSub).getInt(checkNotNull(module.vmSignalInteractor).get(obj))
            if (i < 0) {
                return
            }
            val i2 = signalResource.getInt(obj)
            val strResourceName = resourceName(
                module.vmSignalContext.let { field -> if (field == null) module.context else field.get(obj) as Context? },
                i2
            )
            val iLevel = module.signalPolicy.level(i2, strResourceName)
            module.vmCapture = "sub=" + i + " resource=" + strResourceName + " level=" + iLevel
            if (module.nativeCache.get(i) == iLevel && module.nativeResourceNames.get(i) == strResourceName) {
                return
            }
            if (module.nativeCache.size > 8) {
                module.nativeCache.clear()
                module.nativeResourceNames.clear()
            }
            module.nativeCache.put(i, iLevel)
            module.nativeResourceNames.put(i, strResourceName)
            module.networks?.setNativeSignal(i, iLevel)
        } catch (e: Exception) {
            module.vmCapture = "read-failed:" + e.javaClass.getSimpleName()
        } catch (e: LinkageError) {
            module.vmCapture = "read-failed:" + e.javaClass.getSimpleName()
        }
    }

    @Throws(Exception::class)
    fun hookNativeSignal(classLoader: ClassLoader?) {
        signalFields(classLoader)
        val cls: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl\$signalIconId$1$1"
        )
        val field: Field = ModuleMain.Companion.field(cls, "this$0")
        module.installed.add(
            module.hook(ModuleMain.Companion.method(cls, "invokeSuspend", Any::class.java)).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@SystemUiSignalHooks.onNativeSignalSnapshot(field, chain)
                }
            })
        )
        for (method in cls.getDeclaredMethods()) {
            if (method.getName() == "invoke") {
                try {
                    module.deoptimize(method)
                } catch (th: Throwable) {
                }
            }
        }
        hookNativeTechnology(classLoader)
    }

    @Throws(Throwable::class)
    fun onNativeSignalSnapshot(field: Field, chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        try {
            val obj = field.get(chain.getThisObject())
            module.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@SystemUiSignalHooks.refreshBoundNativeSignal(obj)
                }
            })
        } catch (e: Exception) {
        }
        return objProceed
    }

    @Throws(Exception::class)
    fun hookSignalSnapshot(classLoader: ClassLoader?) {
        signalFields(classLoader)
        val method: Method = ModuleMain.Companion.method(
            ModuleMain.Companion.cls(
                classLoader,
                "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder"
            ),
            "bind",
            ViewGroup::class.java,
            ModuleMain.Companion.cls(
                classLoader,
                "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel"
            ),
            ModuleMain.Companion.cls(
                classLoader,
                "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel"
            ),
            ModuleMain.Companion.cls(classLoader, "com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger")
        )
        require(Modifier.isStatic(method.getModifiers())) { "binder must be static" }
        module.installed.add(module.hook(method).intercept(object : Hooker {
            @Throws(Throwable::class)
            override fun intercept(chain: XposedInterface.Chain): Any? {
                return this@SystemUiSignalHooks.onMobileViewCreated(chain)
            }
        }))
        for (method2 in ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        ).getDeclaredMethods()) {
            if (method2.getName() == "constructAndBind") {
                try {
                    module.deoptimize(method2)
                } catch (th: Throwable) {
                }
            }
        }
        hookNativeTechnology(classLoader)
    }

    @Throws(Throwable::class)
    fun onMobileViewCreated(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        val view = chain.getArg(0) as View?
        val arg = chain.getArg(2)
        module.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                this@SystemUiSignalHooks.bindMobileSignalModel(arg, view)
            }
        })
        return objProceed
    }

    fun bindMobileSignalModel(obj: Any?, view: View?) {
        if (checkNotNull(module.vmSignalResource).getDeclaringClass().isInstance(obj)) {
            module.networks?.setNativeTechnologyRefresh(requestTechnologyRefresh)
            if (module.boundSignalModels.size > 32) {
                module.boundSignalModels.clear()
            }
            module.boundSignalModels.put(view, WeakReference<Any?>(obj))
            refreshBoundNativeSignal(obj)
            scanSignal(view, 0)
            refreshNativeTechnology()
        }
    }

    /** Verified OEM binder sink used by showName.collectLatest and native rebind.  */
    private fun hookNativeTechnology(loader: ClassLoader?) {
        if (nativeTechnologyHook) return
        try {
            val binder: Class<*> = ModuleMain.Companion.cls(
                loader,
                "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder"
            )
            val method: Method = ModuleMain.Companion.method(
                binder, "updateMobileTypeLayoutParams",
                ModuleMain.Companion.cls(loader, "com.miui.systemui.statusbar.views.MobileTypeDrawable"),
                String::class.java, ImageView::class.java
            )
            require(!(!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != Void.TYPE)) { "native technology signature" }
            module.installed.add(module.hook(method).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    val result = chain.proceed()
                    queueNativeTechnology(chain.getArg(2) as View?, chain.getArg(1) as String?)
                    return result
                }
            }))
            nativeTechnologyHook = true
        } catch (error: Exception) {
            module.failures.record("signal", "native-technology-hook", error)
        } catch (error: LinkageError) {
            module.failures.record("signal", "native-technology-hook", error)
        }
    }

    private fun queueNativeTechnology(child: View?, text: String?) {
        try {
            var parent = child
            var depth = 0
            while (parent != null && depth < 12) {
                if ("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
                    == parent.javaClass.getName()
                ) {
                    val view: View? = parent
                    val id = checkNotNull(module.mobileSubId).getInt(view)
                    val reference = module.boundSignalModels.get(view)
                    val model = if (reference == null) null else reference.get()
                    if (id >= 0 && model != null) module.main.post(object : Runnable {
                        override fun run() {
                            captureNativeTechnology(view!!, id, model, text)
                        }
                    })
                    return
                }
                parent = if (parent.getParent() is View) parent.getParent() as View? else null
                depth++
            }
        } catch (error: Exception) {
            module.failures.record("signal", "native-technology-capture", error)
        } catch (error: LinkageError) {
            module.failures.record("signal", "native-technology-capture", error)
        }
    }

    private fun captureNativeTechnology(view: View, id: Int, model: Any?, text: String?) {
        val networks = module.networks ?: return
        if (!networks.running()) return
        networks.setNativeTechnologyRefresh(requestTechnologyRefresh)
        try {
            val current = module.boundSignalModels.get(view)
            if (view.isAttachedToWindow() && checkNotNull(module.mobileSubId).getInt(view) == id && current != null && current.get() === model) {
                networks.nativeTechnology(id, text, true)
            }
        } catch (error: Exception) {
            module.failures.record("signal", "native-technology-event", error)
        } catch (error: LinkageError) {
            module.failures.record("signal", "native-technology-event", error)
        }
    }

    @Throws(Exception::class)
    fun hookViewSignal() {
        module.installed.add(
            module.hook(
                ModuleMain.Companion.method(
                    ImageView::class.java,
                    "setImageResource",
                    Int::class.javaPrimitiveType!!
                )
            ).intercept(object : Hooker {
                @Throws(Throwable::class)
                override fun intercept(chain: XposedInterface.Chain): Any? {
                    return this@SystemUiSignalHooks.onSignalImageResourceChanged(chain)
                }
            })
        )
        try {
            module.installed.add(
                module.hook(ModuleMain.Companion.method(View::class.java, "setTag", Any::class.java))
                    .intercept(object : Hooker {
                        @Throws(Throwable::class)
                        override fun intercept(chain: XposedInterface.Chain): Any? {
                            return this@SystemUiSignalHooks.onSignalTagChanged(chain)
                        }
                    })
            )
            module.signalTagHook = true
        } catch (e: Exception) {
            module.signalTagHook = false
        } catch (e: LinkageError) {
            module.signalTagHook = false
        }
    }

    @Throws(Throwable::class)
    fun onSignalImageResourceChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        captureSignalView((chain.getThisObject() as android.view.View?)!!, (chain.getArg(0) as Int))
        return objProceed
    }

    @Throws(Throwable::class)
    fun onSignalTagChanged(chain: XposedInterface.Chain): Any? {
        val objProceed = chain.proceed()
        if ((chain.getThisObject() is ImageView) && (chain.getArg(0) is Int)) {
            captureSignalView((chain.getThisObject() as android.view.View?)!!, (chain.getArg(0) as Int))
        }
        return objProceed
    }

    /**
     * Wi-Fi 图标有更新（连上 / 断开 / 换网络）时这一行的构成会变，必须让会话立刻重排一次；
     * 否则要等下一次下拉（重新 measure）才纠正，屏幕上会先叠出原生 Wi-Fi 或错位的块。
     */
    private fun requestSignalRelayout(view: View?) {
        val session = module.owner(view)
        if (session != null) {
            session.requestRelayout()
        }
    }

    private fun captureSignalView(view: View, i: Int) {
        var i = i
        if (module.mode == 0 || (view !is ImageView) || view.getId() == -1) {
            return
        }
        try {
            if (isWifiSignalView(view)) {
                requestSignalRelayout(view)
                return
            }
            if ("mobile_signal" == view.getResources().getResourceEntryName(view.getId())) {
                var view2: View = view
                var i3 = 0
                while (i3 < 12 && "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView" != view2.javaClass.getName()) {
                    if (view2.getParent() !is View) {
                        module.signalCapture = "signal-view-awaiting-parent"
                        return
                    }
                    view2 = view2.getParent() as View
                    i3++
                }
                if ("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView" != view2.javaClass.getName()) {
                    module.signalCapture = "mobile-parent-not-found"
                    return
                }
                val i4 = checkNotNull(module.mobileSubId).getInt(view2)
                if (i4 < 0) {
                    module.signalCapture = "subscription-not-set"
                    return
                }
                val iIntValue = if (view.getTag() is Int) (view.getTag() as Int) else 0
                if (i <= 0) {
                    i = iIntValue
                }
                var strResourceName = resourceName(view.getContext(), i)
                var iLevel = module.signalPolicy.level(i, strResourceName)
                if (iLevel == -1 && iIntValue > 0 && iIntValue != i) {
                    strResourceName = resourceName(view.getContext(), iIntValue)
                    iLevel = module.signalPolicy.level(iIntValue, strResourceName)
                }
                if (iLevel == -1) {
                    val imageResourceId = module.imageResource?.getInt(view)
                    if (imageResourceId != null && imageResourceId > 0) {
                        strResourceName = resourceName(view.getContext(), imageResourceId)
                        iLevel = module.signalPolicy.level(imageResourceId, strResourceName)
                    }
                }
                module.signalCapture = "sub=" + i4 + " resource=" + strResourceName + " level=" + iLevel
                if (Looper.myLooper() == Looper.getMainLooper() && module.nativeViewCache.get(i4) == iLevel && module.viewResourceNames.get(
                        i4
                    ) == strResourceName
                ) {
                    return
                }
                val capturedLevel = iLevel
                val capturedResource = strResourceName
                module.main.post(object : Runnable {
                    // java.lang.Runnable
                    override fun run() {
                        this@SystemUiSignalHooks.applyCapturedViewSignal(i4, capturedLevel, capturedResource)
                    }
                })
            }
        } catch (e: Exception) {
            module.signalCapture = "read-failed:" + e.javaClass.getSimpleName()
        } catch (e: LinkageError) {
            module.signalCapture = "read-failed:" + e.javaClass.getSimpleName()
        }
    }

    fun applyCapturedViewSignal(i: Int, i2: Int, str: String?) {
        if (module.nativeViewCache.get(i) == i2 && module.viewResourceNames.get(i) == str) {
            return
        }
        if (module.nativeViewCache.size > 8) {
            module.nativeViewCache.clear()
            module.viewResourceNames.clear()
        }
        module.nativeViewCache.put(i, i2)
        module.viewResourceNames.put(i, str)
        module.networks?.let { networks ->
            networks.recordViewResource(i, str)
            networks.setViewSignal(i, i2)
        }
    }

    fun scanSignal(view: View?, i: Int) {
        if (i > 12) return
        val obj = if (i == 0) module.boundSignalModels[view]?.get() else null
        if (obj != null) {
            try {
                if (module.nativeResourceNames.get(checkNotNull(module.vmSignalSub).getInt(checkNotNull(module.vmSignalInteractor).get(obj))) != resourceName(
                        module.vmSignalContext.let { field -> if (field == null) checkNotNull(view).context else field.get(obj) as Context? },
                        checkNotNull(module.vmSignalResource).getInt(obj)
                    )
                ) {
                    module.main.post(object : Runnable {
                        // java.lang.Runnable
                        override fun run() {
                            this@SystemUiSignalHooks.refreshBoundNativeSignal(obj)
                        }
                    })
                }
            } catch (e: Exception) {
                module.failures.record("signal", "model-refresh", e)
            }
        }
        if (view is ImageView) {
            captureSignalView(view, 0)
        }
        if (view is ViewGroup) {
            val viewGroup = view
            for (i2 in 0..<viewGroup.getChildCount()) {
                module.scanSignal(viewGroup.getChildAt(i2), i + 1)
            }
        }
    }

    fun owner(obj: Any?): IconSession? {
        if (obj !is View) {
            return null
        }
        val session2 = module.signals.get(obj)
        if (session2 != null) {
            return session2
        }
        val view = obj
        val name = view.javaClass.getName()
        if ("com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView" != name && "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView" != name) {
            return null
        }
        val parent = view.parent as? View ?: return null
        val container = parent.parent as? View ?: return null
        val session = module.containers[container] ?: return null
        module.signals.put(view, session)
        return session
    }

    fun onStatusBarViewAttached(view: View) {
        var i: Int
        var view2: View?
        if (!view.isAttachedToWindow() || module.batteries.containsKey(view)) {
            return
        }
        try {
            val i2 = checkNotNull(module.layoutFrom).getInt(view)
            if (view.getParent() is ViewGroup) {
                val viewGroup2 = view.getParent() as ViewGroup
                if (ModuleMain.Companion.CONTAINER == viewGroup2.javaClass.getName()) {
                    val viewGroup = checkNotNull(module.iconsField).get(viewGroup2) as ViewGroup? ?: return
                    val keyguardType = module.keyguardType
                    if (compatibility.isAvailable(Compatibility.Capability.KEYGUARD) && keyguardType != null) {
                        i = i2
                        view2 = null
                        var parent = viewGroup2.getParent()
                        while (parent is View) {
                            if (keyguardType.isInstance(parent)) {
                                i = 1
                                view2 = parent as View
                                break
                            }
                            parent = parent.getParent()
                        }
                    } else {
                        i = i2
                        view2 = null
                    }
                    if (module.attachOutcomes.size < 12 || module.attachOutcomes.containsKey(i2)) {
                        module.attachOutcomes.put(
                            i2,
                            "role=" + i + " keyguardParent=" + (view2 != null) + " parent=" + (if (viewGroup2.getParent() == null) "none" else viewGroup2.getParent().javaClass.getSimpleName())
                        )
                    }
                    if (DisplayPolicy.attach(
                            i,
                            compatibility.isAvailable(Compatibility.Capability.MOTION),
                            compatibility.isAvailable(Compatibility.Capability.KEYGUARD)
                        )
                    ) {
                        if (i == 1 && view2 == null) {
                            return
                        }
                        val session = IconSession(module, view, viewGroup2, viewGroup, i, view2)
                        module.batteries.put(view, session)
                        module.containers.put(viewGroup2, session)
                        if ((i == 5 || i == 6)
                            && compatibility.isAvailable(
                                Compatibility.Capability.CLOSED_ANCHOR
                            )
                        ) {
                            var parent2 = viewGroup2.getParent()
                            while (true) {
                                if (parent2 !is View) {
                                    break
                                }
                                if ((if (i == 5) ModuleMain.Companion.FAKE_STATUS else ModuleMain.Companion.PANEL_STATUS) == parent2.javaClass.getName()) {
                                    module.visualCopies.put(parent2 as View, session)
                                    break
                                }
                                parent2 = parent2.getParent()
                            }
                        }
                        if (view2 != null) {
                            module.keyguards.put(view2, session)
                            session.observeKeyguard()
                        }
                        session.observeMotion()
                        module.ensureContext(view.getContext())
                        module.powerSaveField?.let { field ->
                            try {
                                checkNotNull(module.networks).seedNativePowerSave(field.getBoolean(view))
                            } catch (e: IllegalAccessException) {
                            }
                        }
                        module.performanceField?.let { field ->
                            try {
                                checkNotNull(module.networks).performance.seedNative(field.getBoolean(view))
                            } catch (e2: IllegalAccessException) {
                            }
                        }
                        module.batteryLayoutListener?.let { field ->
                            try {
                                val obj = field.get(view)
                                if (obj != null) {
                                    module.deoptimizeListener(obj.javaClass)
                                }
                            } catch (th: Throwable) {
                                compatibility.markUnavailable(
                                    Compatibility.Capability.HANDOFF_HINTS,
                                    "runtime",
                                    th.toString()
                                )
                            }
                        }
                        session.changed()
                        if (module.configRegistered && module.apkOk && module.mode > 0) {
                            checkNotNull(module.networks).start()
                        }
                        module.loadConfig()
                    }
                }
            }
        } catch (th2: Throwable) {
            module.log(6, ModuleMain.Companion.TAG, "Attach failed; stock retained", th2)
        }
    }

    fun detach(view: View?) {
        val sessionRemove = module.batteries.remove(view)
        if (sessionRemove == null) {
            return
        }
        module.visualCopies.entries.removeIf(object : Predicate<Any?> {
            // java.util.function.Predicate
            override fun test(obj: Any?): Boolean {
                return Companion.`lambda$43`(
                    sessionRemove,
                    (obj as kotlin.collections.MutableMap.MutableEntry<*, *>?)!!
                )
            }
        })
        module.containers.remove(sessionRemove.host)
        module.signals.entries.removeIf(object : Predicate<Any?> {
            // java.util.function.Predicate
            override fun test(obj: Any?): Boolean {
                return Companion.`lambda$44`(
                    sessionRemove,
                    (obj as kotlin.collections.MutableMap.MutableEntry<*, *>?)!!
                )
            }
        })
        module.main.removeCallbacks(sessionRemove.retry)
        if (sessionRemove.keyguard != null) {
            module.keyguards.remove(sessionRemove.keyguard)
        }
        sessionRemove.removeKeyguardObserver()
        sessionRemove.removeMotionObserver()
        sessionRemove.closed = true
        sessionRemove.merge = false
        sessionRemove.draw = false
        try {
            sessionRemove.reconcileIsland()
        } catch (th: Throwable) {
            module.log(5, ModuleMain.Companion.TAG, "Restore island on detach", th)
        }
        try {
            checkNotNull(module.updateChargeAndText).invoke(sessionRemove.battery, *arrayOfNulls<Any>(0))
        } catch (th2: Throwable) {
        }
        if (module.batteries.isEmpty()) module.networks?.stop()
        module.scheduleBurnIn()
    }

    companion object {
        /** 这个 ImageView 是否属于 Wi-Fi 图标（ModernStatusBarWifiView 之内）  */
        private fun isWifiSignalView(view: View?): Boolean {
            var view2 = view
            var i = 0
            while (i < 12 && view2 != null) {
                if (ModuleMain.Companion.WIFI == view2.javaClass.getName()) {
                    return true
                }
                val viewParent = view2.getParent()
                view2 = if (viewParent is View) viewParent as View else null
                i++
            }
            return false
        }

        @JvmStatic fun `lambda$43`(session: IconSession, entry: MutableMap.MutableEntry<*, *>): Boolean {
            if (entry.value !== session) {
                return false
            }
            session.restoreClosedAnchor(entry.key as View)
            return true
        }

        @JvmStatic fun `lambda$44`(session: IconSession?, entry: MutableMap.MutableEntry<*, *>): Boolean {
            return entry.value === session
        }
    }
}
