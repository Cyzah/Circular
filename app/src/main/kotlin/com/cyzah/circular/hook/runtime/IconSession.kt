package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import com.cyzah.circular.core.graphics.BatteryShapeRenderer
import com.cyzah.circular.core.graphics.BatteryTransferRingRenderer
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.PresentationPolicy
import com.cyzah.circular.core.graphics.StackedOverlayRenderer
import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferProjection
import com.cyzah.circular.core.graphics.animation.BatteryTransferState
import com.cyzah.circular.core.graphics.animation.ClosedAnchor
import com.cyzah.circular.core.graphics.animation.KeyguardHandoff
import com.cyzah.circular.core.graphics.animation.MotionHandoff
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.core.graphics.animation.StartupReveal
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DisplayPolicy
import com.cyzah.circular.hook.platform.Compatibility
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Modifier
import java.util.ArrayList
import java.util.HashMap
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class IconSession(
    @JvmField val module: ModuleMain,
    @JvmField val battery: View,
    @JvmField val host: ViewGroup,
    @JvmField val icons: ViewGroup,
    @JvmField val from: Int,
    @JvmField val keyguard: View?
) {
    @JvmField var ambient: Boolean = false
    @JvmField var cachedStatus: Int = 0
    @JvmField var closed: Boolean = false
    @JvmField var draw: Boolean = false
    @JvmField var driftX: Float = 0f
    @JvmField var driftY: Float = 0f
    @JvmField var failedUntil: Long = 0
    @JvmField var failures: Int = 0
    @JvmField var islandDepth: Int = 0
    @JvmField var lastError: Long = 0
    @JvmField var lastTint: Int = 0
    private val startupReveal = StartupReveal()
    @JvmField var merge: Boolean = false
    @JvmField var motionTree: ViewTreeObserver? = null
    @JvmField var networkWidth: Int = 0
    @JvmField val controlCenterRow: ControlCenterRowCoordinator
    @JvmField var controlCenterRowSolved: Boolean = false
    private val rowTransition: IconSessionRowTransition
    @JvmField val transitionRowOrigin: IntArray = IntArray(2)
    @JvmField var observedTree: ViewTreeObserver? = null
    @JvmField var parentHeightLimit: Int = 0
    @JvmField var parentWidthLimit: Int = 0
    @JvmField var progress: Float = 0f
    private var nativeRowAlpha = 1.0f
    private var notificationCopyActive = false
    @JvmField var reflowBlocks: Int = 0
    @JvmField var returnCapturePending: Boolean = false
    @JvmField var rootShown: Boolean = false
    @JvmField var sizeAdjusted: Boolean = false
    @JvmField var stockHeight: Int = 0
    @JvmField var stockWidth: Int = 0
    @JvmField var batteryWidthMeasureSpec: Int = 0
    @JvmField var hasBatteryWidthMeasureSpec: Boolean = false
    @JvmField val widthPreDrawGuard: WidthPreDrawGuard = WidthPreDrawGuard()
    @JvmField var widthPreDrawFallbackLogged: Boolean = false
    @JvmField var expandedNeighbourBoundary: Float = Float.NaN
    @JvmField var expandedRowBoundary: Float = Float.NaN
    @JvmField val nativeRowMotion: NativeRowMotion = NativeRowMotion()
    @JvmField val expandedNeighbourCapture: ExpandedNeighbourCapture = ExpandedNeighbourCapture()
    @JvmField var neighbourBoundaryActive: Boolean = false
    @JvmField var expandedNeighbourSettings: SessionSettings? = null
    @JvmField var expandedNeighbourRtl: Boolean = false
    @JvmField var expandedNeighbourConfiguration: Int = 0
    @JvmField var expandedNeighbourRootWidth: Int = 0
    @JvmField var expandedNeighbourRootHeight: Int = 0
    @JvmField val painter: IconPainter = IconPainter()
    @JvmField val renderer: IconSessionPainter = IconSessionPainter()
    @JvmField val batteryShapeRenderer: BatteryShapeRenderer = BatteryShapeRenderer()
    @JvmField val batteryShapeGeometry: BatteryShapeGeometry = BatteryShapeGeometry()
    @JvmField var lastBatteryShapeStage: BatteryShapeGeometry.Stage? = null
    @JvmField val batteryRingRenderer: BatteryTransferRingRenderer = BatteryTransferRingRenderer()
    @JvmField val batteryTransferProjection: BatteryTransferProjection = BatteryTransferProjection()
    @JvmField val swappedWifiTransfer: SwappedWifiTransfer = SwappedWifiTransfer()
    @JvmField val batteryShapeFrameClip: Rect = Rect()
    @JvmField val batteryShapeFrameLocation: IntArray = IntArray(2)
    @JvmField var batteryShapeFrameCaptured: Boolean = false
    @JvmField var batteryPointFrameLogged: Boolean = false
    @JvmField var wifiScaleDiagnosticBucket: Int = -1
    @JvmField var signalStageDiagnosticBucket: Int = -1
    @JvmField var lastLifecycleDiagnostic: String = ""
    @JvmField val parts: ArrayList<View> = ArrayList<View>()
    @JvmField val nativeDrawStack: DrawCallStack = DrawCallStack(4)
    @JvmField val nativeDrawAdapter: NativeDraw = object : NativeDraw {
        @Throws(Throwable::class)
        override fun run(): Any? {
            val current = nativeDrawStack.current()
            check(current is XposedInterface.Chain) { "Missing native draw chain" }
            return current.proceed()
        }
    }
    @JvmField val handoff: MotionHandoff = MotionHandoff()
    @JvmField val keyguardHandoff: KeyguardHandoff = KeyguardHandoff()
    @JvmField val closedAnchor: ClosedAnchor = ClosedAnchor()

    /** 控制中心双排信号画笔与状态  */
    @JvmField val stacked: StackedSignals = StackedSignals()
    @JvmField val stackedTarget: StackedSignals.Target = StackedSignals.Target()
    @JvmField val stackedOverlay: StackedOverlayRenderer = StackedOverlayRenderer()
    @JvmField var stackedActive: Boolean = false
    @JvmField var stackedDrawing: Boolean = false
    @JvmField var stackedAlpha: Float = 0f
    @JvmField var stackedLeft: Float = Float.NaN
    @JvmField var stackedRight: Float = Float.NaN

    /** 双排盒宽只增不减，避免原生槽位 settle 时整块跳动。  */
    @JvmField var stackedBoxWidth: Float = 0f
    @JvmField var stackedStableStockWidth: Float = 0f
    @JvmField var stackedStockWidth: Int = 0

    /** 上次占位宽度；制式文本变化时清掉盒宽记忆并重新测量。  */
    @JvmField var stackedReserveSeen: Float = -1.0f

    /** 双排生效时被改写宽度的原生槽位；按 View 身份索引。  */
    @JvmField val stackedSlotWidths: HashMap<View?, Float?> = HashMap<View?, Float?>()

    /** 形变期间交给模块绘制的部分（信号点 / 中心制式文本）在 IconPainter 里屏蔽掉的位  */
    @JvmField var painterMask: Int = 0
    @JvmField var reason: String = "pending"
    @JvmField var privacy: String = "unknown"
    @JvmField var lastCapture: String = ""
    private val relayout: IconSessionRelayout
    private val diagnostics = IconSessionDiagnostics()
    @JvmField var lastMotionFrame: String = ""
    @JvmField var lastRealFrame: String = ""
    @JvmField var observedSurface: Long = Long.MIN_VALUE
    @JvmField val motionPreDraw: OnPreDrawListener = object : OnPreDrawListener {
        // android.view.ViewTreeObserver.OnPreDrawListener
        override fun onPreDraw(): Boolean {
            return this@IconSession.beforeMotionDraw()
        }
    }
    @JvmField val keyguardPreDraw: OnPreDrawListener = object : OnPreDrawListener {
        // android.view.ViewTreeObserver.OnPreDrawListener
        override fun onPreDraw(): Boolean {
            return this@IconSession.beforeKeyguardDraw()
        }
    }
    @JvmField val retry: Runnable = object : Runnable {
        // java.lang.Runnable
        override fun run() {
            this@IconSession.restoreClosedAnchorAfterAttach()
        }
    }

    fun beforeMotionDraw(): Boolean {
        if (this.closed) {
            return true
        }
        module.synchronizeAlignment(this)
        module.frameGate.run(
            this.battery.getDrawingTime(),
            module.frameWork
        )
        module.drawingClips.update(this)
        val rowAlpha = mergedNativeRowAlpha()
        if (rowAlpha != this.nativeRowAlpha) {
            this.nativeRowAlpha = rowAlpha
            this.battery.invalidate()
        }
        if (!ControlCenterRowReservation.readyAfterAlignment(this)) {
            return false
        }
        if (!this.host.isLayoutRequested() && !this.battery.isLayoutRequested()) {
            this.handoff.committed()
        }
        return true
    }

    fun restoreClosedAnchor(view: View) {
        if (this.closedAnchor.applied()) {
            val translationX = view.getTranslationX()
            val translationY = view.getTranslationY()
            this.closedAnchor.release(translationX, translationY)
            if (translationX != this.closedAnchor.x()) {
                view.setTranslationX(this.closedAnchor.x())
            }
            if (translationY != this.closedAnchor.y()) {
                view.setTranslationY(this.closedAnchor.y())
            }
        }
    }

    fun ownsMotion(): Boolean {
        if (!module.has(Compatibility.Capability.HANDOFF)) {
            return false
        }
        try {
            val z = module.batteryIsland != null && module.batteryIsland!!.getBoolean(this.battery)
            if (!KeyguardHandoff.Companion.suppressReflow(this.from, this.merge, this.ambient, z)) {
                if (!this.handoff.owns(this.merge, this.ambient, z)) {
                    return false
                }
            }
            return true
        } catch (e: IllegalAccessException) {
            return false
        }
    }

    fun observeMotion() {
        this.motionTree = this.battery.getViewTreeObserver()
        this.motionTree!!.addOnPreDrawListener(this.motionPreDraw)
    }

    fun removeMotionObserver() {
        this.expandedNeighbourBoundary = Float.NaN
        this.expandedRowBoundary = Float.NaN
        this.nativeRowMotion.discard()
        this.expandedNeighbourCapture.reset()
        this.expandedNeighbourSettings = null
        this.module.drawingClips.release(this)
        if (this.motionTree != null && this.motionTree!!.isAlive()) {
            this.motionTree!!.removeOnPreDrawListener(this.motionPreDraw)
        }
        if (this.motionTree != null && this.battery.getViewTreeObserver() != this.motionTree && this.battery.getViewTreeObserver()
                .isAlive()
        ) {
            this.battery.getViewTreeObserver().removeOnPreDrawListener(this.motionPreDraw)
        }
        this.motionTree = null
    }

    fun beforeKeyguardDraw(): Boolean {
        if (this.closed) {
            return true
        }
        // Pre-draw and paint share this traversal; getDrawingTime can advance between them.
        this.diagnostics.beginFrame()
        try {
            val jSurfaceStamp = surfaceStamp()
            if (jSurfaceStamp != this.observedSurface) {
                this.observedSurface = jSurfaceStamp
                changed()
                captureRealLockFrame()
                return !this.host.isLayoutRequested()
            }
            captureRealLockFrame()
        } catch (th: Throwable) {
            if (SystemClock.elapsedRealtime() >= this.failedUntil) {
                fail("keyguard visibility", th)
            }
        }
        return true
    }

    fun restoreClosedAnchorAfterAttach() {
        if (this.closed) {
            return
        }
        this.failedUntil = 0L
        changed()
        requestLayout()
    }

    fun observeKeyguard() {
        this.observedTree = this.keyguard!!.getViewTreeObserver()
        this.observedTree!!.addOnPreDrawListener(this.keyguardPreDraw)
    }

    fun removeKeyguardObserver() {
        if (this.observedTree != null && this.observedTree!!.isAlive()) {
            this.observedTree!!.removeOnPreDrawListener(this.keyguardPreDraw)
        }
        if (this.keyguard != null && this.keyguard.getViewTreeObserver() != this.observedTree && this.keyguard.getViewTreeObserver()
                .isAlive()
        ) {
            this.keyguard.getViewTreeObserver().removeOnPreDrawListener(this.keyguardPreDraw)
        }
        this.observedTree = null
    }

    @Throws(Exception::class)
    fun surfaceStamp(): Long {
        val visibility = (((if (this.keyguard!!.isShown()) 1 else 0) or (if (module.isBrightKeyguard(
                this.keyguard,
                this.battery
            )
        ) 2 else 0) or (if (module.hideBatteryField!!.getBoolean(this.host)) 4 else 0)).toLong()) or ((this.battery.getVisibility()
            .toLong()) shl 3)
        val obj = module.privacyField!!.get(this.host)
        return visibility or ((((if (obj is Enum<*>) obj.ordinal else 15) and 255).toLong()) shl 8) or ((module.statusState!!.getInt(
            this.battery
        ).toLong()) shl 32)
    }

    fun dp(i: Int): Int {
        return Math.round(i * this.battery.getResources().getDisplayMetrics().density)
    }

    fun captureRealLockFrame() {
        this.diagnostics.captureRealLockFrame(this)
    }

    fun expansion(): Float {
        if (module.sessionSettings().keepMergedShade()) {
            return 0.0f
        }
        return DisplayPolicy.expansion(this.from, this.ambient, module.shade, module.control)
    }

    fun prepareKeyguard(): Boolean {
        return this.keyguardHandoff.prepare(
            this.rootShown,
            !this.ambient,
            module.sessionSettings().mode() == 2 && module.sessionSettings().lockscreen()
                    && module.has(Compatibility.Capability.KEYGUARD),
            this.merge,
            module.control
        )
    }

    fun islandSummary(): String {
        if (module.storedBatteryIsland == null || module.batteryIsland == null) {
            return " island=unavailable"
        }
        try {
            return " islandRequested=" + module.storedBatteryIsland!!.getBoolean(this.battery) + " islandNative=" + module.batteryIsland!!.getBoolean(
                this.battery
            )
        } catch (e: IllegalAccessException) {
            return " island=read-error"
        }
    }

    @Throws(Exception::class)
    fun islandTakingBattery(): Boolean {
        return module.has(Compatibility.Capability.CHARGING_ISLAND)
                && !this.ambient && module.batteryIsland != null && module.batteryIsland!!.getBoolean(this.battery)
    }

    /** 锁屏 / AOD 判定诊断：打印判定结果与关键字段，便于核对机型差异  */
    fun keyguardProbe(): String {
        val sbAppend = StringBuilder()
        try {
            sbAppend.append("bright=").append(module.isBrightKeyguard(this.keyguard, this.battery))
        } catch (th: Throwable) {
            sbAppend.append("bright=error").append(th.javaClass.getSimpleName())
        }
        sbAppend.append(" interactive=").append(module.screenInteractive(this.battery))
        sbAppend.append(" keyguardState=").append(module.keyguardStatusBarState(this.battery))
        try {
            sbAppend.append(" isShown=").append(this.keyguard!!.isShown()).append(" vis=")
                .append(this.keyguard.getVisibility())
        } catch (th: Throwable) {
        }
        try {
            var cls: Class<*>? = this.keyguard!!.javaClass
            while (cls != null && cls != View::class.java) {
                for (field in cls.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue
                    }
                    val lowerCase = field.getName().lowercase()
                    if (!lowerCase.contains("lock") && !lowerCase.contains("aod") && !lowerCase.contains("doze") && !lowerCase.contains(
                            "dream"
                        ) && !lowerCase.contains("bright") && !lowerCase.contains("keyguard") && !lowerCase.contains("screen")
                    ) {
                        continue
                    }
                    val type = field.getType()
                    if (type != Boolean::class.javaPrimitiveType && type != Int::class.javaPrimitiveType) {
                        continue
                    }
                    field.setAccessible(true)
                    sbAppend.append(' ').append(field.getName()).append('=')
                    if (type == Boolean::class.javaPrimitiveType) {
                        sbAppend.append(field.getBoolean(this.keyguard))
                    } else {
                        sbAppend.append(field.getInt(this.keyguard))
                    }
                }
                cls = cls.getSuperclass()
            }
        } catch (th2: Throwable) {
            sbAppend.append(" scan=").append(th2.javaClass.getSimpleName())
        }
        return sbAppend.toString()
    }

    @Throws(Exception::class)
    fun reconcileIsland() {
        if (this.islandDepth > 0 || module.updateBatteryIsland == null || !module.has(Compatibility.Capability.CHARGING_ISLAND)) {
            return
        }
        val z = module.storedBatteryIsland!!.getBoolean(this.battery)
        if (module.batteryIsland!!.getBoolean(this.battery) == (z && (!this.merge || this.ambient || this.closed))) {
            return
        }
        this.islandDepth++
        try {
            module.updateBatteryIsland!!.invoke(this.battery, z, false)
        } finally {
            this.islandDepth--
        }
    }

    @Throws(Exception::class)
    fun eligible(): Boolean {
        this.cachedStatus = module.statusState!!.getInt(this.battery)
        this.rootShown = this.keyguard != null && this.keyguard.isShown()
        this.privacy = module.privacyField!!.get(this.host).toString()
        if (this.closed) {
            this.reason = "detached"
            return false
        }
        if (!module.sessionSettings().compatible()) {
            this.reason = "unsupported"
            return false
        }
        if (module.sessionSettings().mode() == 0) {
            this.reason = "mode-off"
            return false
        }
        if (SystemClock.elapsedRealtime() < this.failedUntil) {
            this.reason = "retry-pending"
            return false
        }
        val zPrepareKeyguard = if (this.from == 1) prepareKeyguard() else this.rootShown
        if (this.from == 1 && !zPrepareKeyguard) {
            this.reason = "keyguard-hidden"
            return false
        }
        val keyguardAvailable =
            module.has(Compatibility.Capability.KEYGUARD)
        if (!DisplayPolicy.enabled(
                this.from,
                this.cachedStatus,
                this.ambient,
                module.sessionSettings().lockscreen() && keyguardAvailable,
                module.sessionSettings().aod() && keyguardAvailable,
                zPrepareKeyguard
            )
        ) {
            this.reason = "surface-disabled"
            return false
        }
        val zIslandTakingBattery = islandTakingBattery()
        if (module.hideBatteryField!!.getBoolean(this.host) && !zIslandTakingBattery) {
            this.reason = "system-hide-battery"
            return false
        }
        val z = this.battery.getVisibility() == View.VISIBLE
        val zKeepPrivacyRenderer = DisplayPolicy.keepPrivacyRenderer(
            this.from,
            module.has(Compatibility.Capability.PRIVACY),
            this.privacy,
            z,
            module.homeBlock != null
                    && module.homeBlock!!.getBoolean(this.battery)
        )
        if (!zKeepPrivacyRenderer && (this.privacy == "START_SHOW_PRIVACY" || this.privacy == "COMPLETE_SHOW_PRIVACY")) {
            this.reason = "privacy-native-fallback"
            return false
        }
        if (!z && !zKeepPrivacyRenderer && (!zIslandTakingBattery
                    || this.battery.getVisibility() != View.INVISIBLE)
        ) {
            this.reason = "battery-hidden"
            return false
        }
        if (zKeepPrivacyRenderer) {
            this.reason = "privacy-transition"
            return true
        }
        if (zIslandTakingBattery) {
            this.reason = "charging-island-retained"
            return true
        }
        if (this.from != 1 || this.rootShown) {
            this.reason = "ready"
            return true
        }
        this.reason = "keyguard-layout-retained"
        return true
    }

    fun changed() {
        if (this.closed || this.islandDepth > 0) {
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            module.main.post(object : Runnable {
                // java.lang.Runnable
                override fun run() {
                    this@IconSession.changed()
                }
            })
            return
        }
        try {
            val z = if (this.keyguard == null || module.isBrightKeyguard(this.keyguard, this.battery)) false else true
            val z2 = this.ambient xor z
            this.ambient = z
            val i = if (module.firstLevel!!.getBoolean(this.battery)) -1 else module.level!!.getInt(this.battery)
            if (i >= 0 && i <= 100) {
                module.state.battery = i
                module.state.charging = module.charging!!.getBoolean(this.battery)
            }
            val zEligible = eligible()
            if (zEligible && module.state.battery < 0) {
                this.reason = "battery-not-ready"
            }
            val z3 = zEligible && module.state.battery >= 0
            val networkReady = module.state.readyForMerge() && module.networks?.uiReady() == true
            val z4 = PresentationPolicy.ownsMerged(
                zEligible, module.state.battery >= 0,
                module.sessionSettings().mode(), networkReady
            )
            if (z4 && !networkReady) {
                this.reason = "network-pending-module"
            }
            val fExpansion = expansion()
            val zStacked = stackedWanted(z4)
            var z5 =
                if (z4 == this.draw && z4 == this.merge && !z2 && this.progress == fExpansion && zStacked == this.stackedActive) false else true
            // 制式文本（5GA↔5G、卡失服、自定义制式规则）宽度变了：原生槽位占位与双排盒子都要重新量。
            // 只重画不重排的话，条带会停在旧左缘、文本按新宽度右对齐，屏幕上就是「双排信号栏没更新位置」。
            if (this.from == 5 || this.from == 6) {
                val fReserveNow = stackedReservePx(this.battery.getResources().getDisplayMetrics().density)
                if (this.stackedReserveSeen >= 0.0f && abs(fReserveNow - this.stackedReserveSeen) > 0.5f) {
                    this.stackedBoxWidth = 0.0f
                    z5 = true
                }
                this.stackedReserveSeen = fReserveNow
            }
            this.draw = z4
            this.merge = z4
            this.progress = fExpansion
            this.stackedActive = zStacked
            if (!this.draw || !this.merge || this.ambient) {
                this.module.drawingClips.release(this)
                this.nativeRowMotion.discard()
            }
            val lifecycleDiagnostic = ("sessionState host=" + this.from
                    + " draw=" + this.draw
                    + " merge=" + this.merge
                    + " hold=" + this.stackedHold
                    + " ambient=" + this.ambient
                    + " reason=" + this.reason
                    + " controlOpen="
                    + (module.control > 0.02f)
                    + " shadeOpen="
                    + (module.shade > 0.02f)
                    + (if (!z4 && z3) " readiness=" + module.state.wifiKnown + "/" + module.state.wifiEnabled + "/" + module.state.wifiConnected + "/" + module.state.wifiLevel + " mobile=" + module.state.mobileKnown + "/" + module.state.simPresent + "/" + module.state.inService + "/" + module.state.mobileLevel + " network=" + (module.networks?.diagnostic() ?: "missing") else ""))
            if ((this.from == 0 || this.from == 1 || this.from == 5 || this.from == 6)
                && lifecycleDiagnostic != this.lastLifecycleDiagnostic
            ) {
                this.lastLifecycleDiagnostic = lifecycleDiagnostic
                module.log(6, ModuleMain.Companion.TAG, lifecycleDiagnostic)
            }
            if (!module.stackedOn()) {
                this.stackedHold = false
            }
            this.handoff.progress(this.progress)
            if (!this.merge || this.ambient) {
                this.returnCapturePending = false
            }
            if (!this.merge) {
                this.keyguardHandoff.clear()
            }
            if (module.sessionSettings()
                    .mode() != 2 || ((this.from == 1 || this.from == 5 || this.from == 6) && (!this.merge || this.ambient))
            ) {
                module.releaseClosedAnchors()
            }
            reconcileIsland()
            if (z5) {
                if (!this.draw) {
                    module.updateChargeAndText!!.invoke(this.battery, *arrayOfNulls<Any>(0))
                }
                requestLayout()
                invalidateSignals()
            }
            if (this.draw) {
                this.battery.setContentDescription(module.state.description())
            }
            captureKeyguard()
            this.battery.invalidate()
            module.scheduleBurnIn()
            if (this.from == 0 || this.from == 5 || this.from == 6) {
                module.refreshBatteryTransferMotion()
            }
        } catch (th: Throwable) {
            fail("state", th)
        }
    }

    fun captureKeyguard() {
        if (this.keyguard == null) {
            return
        }
        if (!this.rootShown) {
            this.lastCapture = ""
            return
        }
        val str =
            "ambient=" + this.ambient + " draw=" + this.draw + " merge=" + this.merge + " reason=" + this.reason + " state=" + this.cachedStatus + " batteryVis=" + this.battery.getVisibility() + " privacy=" + this.privacy + " progress=" + this.progress + " rawShade=" + module.shade + " rawControl=" + module.control + " nativeAlpha=" + (if (this.ambient || !this.merge) 0.0f else PanelMotionFrame.nativeAlpha(
                this.progress
            )) + " size=" + this.battery.getWidth() + "x" + this.battery.getHeight()
        if (str == this.lastCapture) {
            return
        }
        this.lastCapture = str
        val str2: String = ModuleMain.Companion.stamp(System.currentTimeMillis()) + " " + str
        val z = this.ambient
        val moduleMain = module
        if (z) {
            moduleMain.lastAodSnapshot = str2
        } else {
            moduleMain.lastLockSnapshot = str2
        }
    }

    fun expansionChanged() {
        // 控制中心换行的那一两帧里 merge 会短暂为 false，这里把「继续隐藏原生信号视图」
        // 的保持标志挂住，避免原生角标在控制中心那行的区域里露出来、压在模块画的内容上。
        if ((this.from == 5 || this.from == 6) && module.stackedOn() && module.sessionSettings()
                .mode() == 2 && !this.ambient && (module.control > 0.02f || module.shade > 0.02f)
        ) {
            this.stackedHold = true
        } else if (!module.stackedOn() || (module.control <= 0.02f && module.shade <= 0.02f)) {
            this.stackedHold = false
        }
        if (this.keyguard != null) {
            prepareKeyguard()
        }
        val fExpansion = expansion()
        if (this.progress != fExpansion) {
            this.progress = fExpansion
            this.handoff.progress(fExpansion)
            if (this.merge) {
                requestLayout()
                this.battery.invalidate()
            }
        }
        captureKeyguard()
    }

    /**
     * 重排这一行：合并 120ms 内的显隐变化，同时保留最后一次变化。
     * 只 requestLayout 不够 —— 必须先走 changed() 让状态（merge / stackedActive / 宽度）重新算一遍。
     */
    fun requestRelayout() {
        this.relayout.request()
    }

    fun requestLayout() {
        this.handoff.geometryChanged()
        this.battery.requestLayout()
        this.icons.requestLayout()
        this.host.requestLayout()
    }

    fun invalidateSignals() {
        var i = 0
        while (true) {
            val childCount = this.icons.getChildCount()
            val viewGroup = this.icons
            if (i >= childCount) {
                viewGroup.invalidate()
                return
            } else {
                viewGroup.getChildAt(i).invalidate()
                i++
            }
        }
    }

    @Throws(Exception::class)
    fun wanted(view: View?): Boolean {
        if (true == module.displayBlocked!!.invoke(view, *NO_ARGS) ||
            (module.ignoredSlots!!.get(this.icons) as MutableList<*>).contains(module.displaySlot!!.invoke(view, *NO_ARGS))) return false
        val binding = module.bindingField!!.get(view) ?: return false
        return true == module.shouldBeVisible!!.invoke(binding, *NO_ARGS)
    }

    /**
     * 控制中心双排信号是否生效：开关打开、合并显示中、非 AOD、
     * 宿主是控制中心（5 = 假状态栏副本，6 = 面板状态栏），且信号已就绪
     * （双卡两张都读到、单卡唯一那张读到）。
     */
    fun stackedWanted(z: Boolean): Boolean {
        return z && module.stackedOn() && module.sessionSettings()
            .mode() == 2 && !this.ambient && (this.from == 5 || this.from == 6) && module.state.stackedReady()
    }

    /**
     * 双排是否按「整块宽度」占位（把两张原型卡槽位多出来的宽度还给左右邻居）。
     * 
     * 
     * 条件与 [.stackedWanted] 一致（少一个依赖 `parts` 的 `coversSignals`），
     * 所以只有真的在画双排时才收窄；别的场景（无服务、飞行模式、手机自己的状态栏）保持原样。
     */
    fun stackedReserve(): Boolean {
        return this.draw && module.stackedOn() && module.sessionSettings()
            .mode() == 2 && !this.ambient && (this.from == 5 || this.from == 6)
                && module.state.stackedReady()
    }

    /** 控制中心/通知栏开启期间保持原生信号隐藏，避免换行帧露出角标。  */
    @JvmField var stackedHold: Boolean = false

    init {
        this.controlCenterRow = ControlCenterRowCoordinator(this)
        this.rowTransition = IconSessionRowTransition(this)
        this.relayout = IconSessionRelayout(this)
    }

    /** 双排整块在「铺满」布局下占的宽度（px）：条带 + 间距 + 制式文本  */
    fun stackedReservePx(density: Float): Float {
        val fSize: Float = StackedSignals.Companion.labelSize() * density
        var fText = 0.0f
        // 双排一律带制式文本（原「双排显示网络类型」开关已移除）：取两排里最宽的那一个
        val iconState = module.state
        if (iconState.dualSim()) {
            val str1 = module.stackedLabel(iconState, true)
            val str2 = module.stackedLabel(iconState, false)
            if (str1 != null) {
                fText = max(fText, this.painter.measureTechnology(str1, fSize))
            }
            if (str2 != null) {
                fText = max(fText, this.painter.measureTechnology(str2, fSize))
            }
        } else {
            val str = module.stackedLabel(iconState)
            if (str != null) {
                fText = max(fText, this.painter.measureTechnology(str, fSize))
            }
        }
        val controlCenterSettings =
            this.module.controlCenterSettings()
        val placement =
            controlCenterSettings.batteryNumberPlacement()
        val reserve: Float = StackedSignals.Companion.fillWidthPx(
            fText,
            density,
            placement
        )
        val minimumPx = this.dp(
            controlCenterSettings.batterySize()
        ).toFloat()
        var rowReserve = BatteryShapeLayout.rowReservePx(
            minimumPx,
            placement,
            density
        )
        if (placement == BatteryNumberPlacement.OUTSIDE) {
            val currentReserve =
                BatteryShapeLayout.outsideNumberOverflowReservePx(
                    minimumPx,
                    this.batteryShapeRenderer
                        .outsideNumberOverflowUnits(
                            this.painter,
                            iconState
                        )
                )
            val maximumReserve =
                BatteryShapeLayout.outsideNumberOverflowReservePx(
                    minimumPx,
                    this.batteryShapeRenderer
                        .outsideNumberMaximumOverflowUnits(
                            this.painter
                        )
                )
            rowReserve += currentReserve - maximumReserve
        }
        return reserve + rowReserve
    }

    /** 本宿主待绘制的信号视图里是否含有原生蜂窝图标（双排信号的接管对象）  */
    fun coversSignals(): Boolean {
        for (i in this.parts.indices) {
            if (ModuleMain.Companion.MOBILE == this.parts.get(i).javaClass.getName()) {
                return true
            }
        }
        return false
    }

    @Throws(Exception::class)
    fun plan(i: Int, i2: Int) {
        this.parentWidthLimit = if (View.MeasureSpec.getMode(i) == View.MeasureSpec.UNSPECIFIED)
            0
        else max(
            0, (View.MeasureSpec.getSize(i) - this.host.getPaddingLeft())
                    - this.host.getPaddingRight()
        )
        this.parentHeightLimit = if (View.MeasureSpec.getMode(i2) == View.MeasureSpec.UNSPECIFIED)
            this.host.getHeight()
        else max(
            0, (View.MeasureSpec.getSize(i2) - this.host.getPaddingTop())
                    - this.host.getPaddingBottom()
        )
        changed()
        this.parts.clear()
        this.networkWidth = 0
        module.signals.entries.removeIf(object : Predicate<Any?> {
            // java.util.function.Predicate
            override fun test(obj: Any?): Boolean {
                return this@IconSession.isSignalParentStale((obj as kotlin.collections.MutableMap.MutableEntry<*, *>?)!!)
            }
        })
        val i3 = module.spacingField!!.getInt(this.icons)
        var i4 = 0
        this.stackedSlotWidths.clear()
        val zReserve = stackedReserve()
        for (i5 in 0..<this.icons.getChildCount()) {
            val childAt = this.icons.getChildAt(i5)
            val name = childAt.javaClass.getName()
            if ("com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView" != name && "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView" != name) {
                if (true == module.displayVisible!!.invoke(
                        childAt,
                        *arrayOfNulls<Any>(0)
                    ) && (true != module.displayBlocked!!.invoke(
                        childAt,
                        *arrayOfNulls<Any>(0)
                    )) && !(module.ignoredSlots!!.get(this.icons) as MutableList<*>).contains(
                        module.displaySlot!!.invoke(
                            childAt,
                            *arrayOfNulls<Any>(0)
                        )
                    )
                ) {
                    i4++
                }
            } else {
                module.signals.put(childAt, this)
                if ("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView" == name) {
                    module.scanSignal(childAt, 0)
                }
                if (this.merge && !this.ambient) {
                    val zPart = wanted(childAt)
                    // 合并态下原生的 Wi-Fi / 蜂窝图标一律隐藏（模块自己画）：wanted 只决定它要不要参与排布，
                    // 不参与的那些也要藏，否则 MIUI 中途点亮（Wi-Fi 刚连上）时会在模块的块上叠一个原生图标
                    module.visibleState!!.invoke(childAt, 0, false)
                    if (zPart) {
                        module.measureChild!!.invoke(
                            this.icons, childAt, View.MeasureSpec.makeMeasureSpec(
                                View.MeasureSpec.getSize(i),
                                View.MeasureSpec.UNSPECIFIED
                            ), i2
                        )
                        if (childAt.getMeasuredWidth() > 0) {
                            this.parts.add(childAt)
                            this.networkWidth += childAt.getMeasuredWidth() + childAt.getPaddingStart() + childAt.getPaddingEnd() + i3
                        }
                    }
                }
            }
        }
        val zNoRoom = i4 != 0 || this.parts.isEmpty()
        if (!zNoRoom) {
            this.networkWidth -= i3
        }
        // 注意：这里不能在 i4 != 0 那条 return 之后执行——控制中心这一路 i4 通常就不为 0
        // （容器里还有别的图标），放在后面等于永远不生效。
        if (zReserve && !this.parts.isEmpty()) {
            reserveStackedSlots(i3)
        }
        if (this.from == 6 && module.stackedOn()
            && !this.controlCenterRow.transitioning() && this.controlCenterRow.updateStable(
                this.rowTransition.controlCenterRowGeneration()
            )
        ) {
            val snapshot =
                this.controlCenterRow.stableSnapshot()
            if (snapshot.visible(
                    ControlCenterRowComponent.BATTERY
                )
            ) {
                val batteryWidth = snapshot.width(
                    ControlCenterRowComponent.BATTERY
                )
                this.networkWidth = ControlCenterRowHostPolicy.hostExtraWidthPx(snapshot.rowWidth(), batteryWidth)
            }
        }
        // 控制中心几乎展开完、两行都接近静止时，用系统视图自己的位置采样两行的实际差值
        // （收起时面板行不会被测量，只能在这里采）
        if (this.from == 6 && this.progress >= 0.95f) {
            module.sampleRowAlign(this.battery)
        }
        if (zNoRoom) {
            return
        }
    }

    fun beginCollapsedRowCapture(): Boolean {
        return this.rowTransition.beginCollapsedRowCapture()
    }

    fun captureWifiRowTarget(centerX: Float, width: Float) {
        this.rowTransition.captureWifiRowTarget(centerX, width)
    }

    fun captureCellularRowTarget(left: Float, right: Float) {
        this.rowTransition.captureCellularRowTarget(left, right)
    }

    fun finishCollapsedRowCapture(batteryOriginX: Float, minimum: Float) {
        this.rowTransition.finishCollapsedRowCapture(batteryOriginX, minimum)
    }

    fun synchronizeControlCenterRowTransition() {
        this.rowTransition.synchronizeControlCenterRowTransition()
    }

    fun rowBatteryForContent(liveBattery: Int): Int {
        return this.rowTransition.rowBatteryForContent(liveBattery)
    }

    fun rowNumberPlacement(livePlacement: BatteryNumberPlacement?): BatteryNumberPlacement? {
        return this.rowTransition.rowNumberPlacement(livePlacement)
    }

    fun rowDualSim(liveDual: Boolean): Boolean {
        return this.rowTransition.rowDualSim(liveDual)
    }

    fun rowWifiVisible(liveVisible: Boolean): Boolean {
        return this.rowTransition.rowWifiVisible(liveVisible)
    }

    fun rowIconState(liveState: IconState): IconState {
        return this.rowTransition.rowIconState(liveState)
    }

    fun rowStackedLabel(state: IconState, first: Boolean): String? {
        return this.rowTransition.rowStackedLabel(state, first)
    }

    /**
     * 把原生蜂窝槽位收成「双排整块」的宽度。
     * 
     * 
     * 链条是从 `parts` 末尾往前摆的，所以末尾那个蜂窝视图就是最右边那张卡，
     * 块画在它原来的位置上；其余蜂窝视图不再占位。省下来的宽度会自动还给左边的图标
     * （NFC、网速），于是块与邻居之间只剩原生图标间距，条带左边、文本右边都不会再空一块。
     */
    fun reserveStackedSlots(i3: Int) {
        val fReserve = stackedReservePx(this.icons.getResources().getDisplayMetrics().density)
        var iRightmost = -1
        for (i in this.parts.indices.reversed()) {
            if (ModuleMain.Companion.MOBILE == this.parts.get(i).javaClass.getName()) {
                iRightmost = i
                break
            }
        }
        if (iRightmost < 0) {
            return
        }
        for (i in this.parts.indices.reversed()) {
            val view = this.parts.get(i)
            if (ModuleMain.Companion.MOBILE != view.javaClass.getName()) {
                continue
            }
            val fOld = view.getMeasuredWidth().toFloat()
            if (i == iRightmost) {
                this.networkWidth = (this.networkWidth + (fReserve - fOld)).toInt()
                this.stackedSlotWidths.put(view, fReserve)
            } else {
                this.networkWidth =
                    (this.networkWidth - (fOld + view.getPaddingStart() + view.getPaddingEnd() + i3)).toInt()
                this.parts.removeAt(i)
            }
        }
        val rowShift = SessionPaintPolicy.stableRowAlignmentShift(
            module.rowAlignSpeedShift(),
            module.state.charging
        )
        if (this.from == 5 && rowShift != 0.0f) {
            // 只加在「宿主实测宽度」上（不动槽位宽度，块的大小与位置因此不受影响）：
            // 宿主右边缘固定，宽度变大 → 它左边的图标（网速）左移同样的量，
            // 就落到控制中心那一行的实际位置上。
            this.networkWidth = (this.networkWidth + rowShift).toInt()
        }
    }

    fun isSignalParentStale(entry: MutableMap.MutableEntry<*, *>): Boolean {
        return entry.value === this && (entry.key as View).getParent() !== this.icons
    }

    fun layoutSignals() {
        for (view in this.parts) {
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight())
        }
    }

    @Throws(Throwable::class)
    fun paint(canvas: Canvas, nativeDraw: NativeDraw?) {
        var alpha = mergedNativeRowAlpha()
        val tintReady = module.nativeBatteryTints.containsKey(this.battery)
        val reveal = startupReveal.alpha(SystemClock.uptimeMillis(), tintReady)
        if (tintReady && reveal < 1.0f) this.battery.postInvalidateOnAnimation()
        alpha *= reveal
        if (alpha <= 0.0f) {
            return
        }
        val save = if (alpha < 1.0f) canvas.saveLayerAlpha(
            null as RectF?, Math.round(alpha * 255.0f)
        ) else -1
        var drawFailure: Throwable? = null
        try {
            this.renderer.paint(this, canvas, nativeDraw)
        } catch (error: Throwable) {
            drawFailure = error
            throw error
        } finally {
            if (save >= 0) {
                NativeSessionDraw.restoreCanvas(canvas, save, drawFailure)
            }
        }
    }

    fun notificationCopy(): Boolean {
        this.notificationCopyActive = SessionPaintPolicy.notificationCopyActive(
            this.from, this.notificationCopyActive, module.control, module.shade
        )
        val copy = SessionPaintPolicy.drawsMergedNotificationCopy(
            this.notificationCopyActive, module.control, module.morphValue()
        )
        if (!copy || module.sessionSettings().keepMergedShade()) {
            return copy
        }
        val primary = module.preferredAttachedControlCenterHost()
        val rowTransition = this.controlCenterRow.transitioning()
                || primary != null && primary.controlCenterRow.transitioning()
        // A notification-row flag may appear during the last CC frame. Both
        // reservation and drawing must keep the same projected endpoint then.
        return SessionPaintPolicy.usesLocalNotificationCopy(
            copy, rowTransition,
            module.batteryTransferState().phase() != BatteryTransferState.Phase.NORMAL_RING
        )
    }

    private fun mergedNativeRowAlpha(): Float {
        if ((this.from != 0 && this.from != 5) || !this.draw || !this.merge || this.ambient
            || !BatteryTransferPolicy.closedSettled(module.control, 0.0f, module.morphValue())
        ) {
            return 1.0f
        }
        return NativeIconVisibility.alpha(this.battery, this.icons)
    }

    fun fail(str: String, th: Throwable?) {
        val jElapsedRealtime = SystemClock.elapsedRealtime()
        val i = this.failures
        this.failures = i + 1
        this.failedUntil = min(BurnInDrift.Companion.INTERVAL_MS, (2000 shl min(5, i)).toLong()) + jElapsedRealtime
        this.merge = false
        this.draw = false
        this.module.drawingClips.release(this)
        this.reason = "error-" + str
        captureKeyguard()
        module.scheduleBurnIn()
        if (this.lastError == 0L || jElapsedRealtime - this.lastError > 15000) {
            this.lastError = jElapsedRealtime
            module.log(
                6,
                ModuleMain.Companion.TAG,
                str + " host=" + this.from + "; stock fallback, retry scheduled",
                th
            )
        }
        module.main.removeCallbacks(this.retry)
        module.main.postDelayed(this.retry, this.failedUntil - jElapsedRealtime)
        module.main.post(object : Runnable {
            // java.lang.Runnable
            override fun run() {
                this@IconSession.finishKeyguardHandoff()
            }
        })
    }

    fun finishKeyguardHandoff() {
        if (this.closed) {
            return
        }
        try {
            reconcileIsland()
        } catch (th: Throwable) {
        }
        requestLayout()
        invalidateSignals()
        this.battery.invalidate()
    }

    companion object {
        private val NO_ARGS = arrayOfNulls<Any>(0)
    }
}
