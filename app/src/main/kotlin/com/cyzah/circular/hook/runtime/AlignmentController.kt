package com.cyzah.circular.hook.runtime

import android.graphics.Matrix
import android.os.Looper
import android.view.View
import com.cyzah.circular.core.graphics.ChargingAlignmentPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.ClosedAnchor
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.hook.platform.Compatibility
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.abs

/** Owns native alignment reflection, row calibration and lockscreen coordinate transforms.  */
class AlignmentController(private val module: ModuleMain) {
    private var alignmentCallback: Field? = null
    private var alignmentController: Any? = null
    private var alignmentDiff: Field? = null
    private var alignmentIsland: Field? = null
    private var alignmentOuter: Field? = null
    private var alignmentProgress: Method? = null
    private var alignmentReal: Field? = null
    var isSynchronizing: Boolean = false
        private set
    private var alignmentUpdate: Method? = null
    private var lastAlignmentApplied = Int.MIN_VALUE
    private val chargingAlignmentPolicy = ChargingAlignmentPolicy()
    private var alignedLayoutEpoch = Long.MIN_VALUE
    private val anchorMatrix = Matrix()
    private val anchorInverse = Matrix()
    private val anchorPoint = FloatArray(2)
    private val anchorVector = FloatArray(2)
    private var lastClosedAnchor = "尚未捕获锁屏收起副本"
    private var lastAnchorSample = ""
    private var lastClosedCorrection = "尚未发生终点校正"
    private var lockMotionGate = "pending"
    private val rowCalibration = RowAlignmentCalibration()
    private val panelLocation = IntArray(2)
    private val statusLocation = IntArray(2)
    private var panelHost: View? = null
    private var statusHost: View? = null
    private var rowDisplayId = -1
    private var rowLayoutDirection = -1

    fun sampleRowAlign(panel: View) {
        val displayId = if (panel.getDisplay() == null) -1 else panel.getDisplay().getDisplayId()
        val direction = panel.getLayoutDirection()
        if (this.panelHost !== panel || this.rowDisplayId != displayId || this.rowLayoutDirection != direction) {
            resetRowCalibration()
            this.panelHost = panel
            this.rowDisplayId = displayId
            this.rowLayoutDirection = direction
        }
        // Accepted zero is a complete calibration too. Do not scan hosts or read
        // screen positions again: applying compensation changes those positions.
        if (this.rowCalibration.isCalibrated) {
            return
        }
        var status: View? = null
        for (session in this.module.batteries.values) {
            if (session.from == 5) {
                status = session.battery
                break
            }
        }
        if (this.statusHost !== status) {
            this.rowCalibration.reset()
            this.statusHost = status
        }
        if (status == null || !panel.isAttachedToWindow() || !status.isAttachedToWindow() || !sameDisplay(
                panel,
                status
            )
        ) {
            // An interrupted observation cannot establish two consecutive samples.
            this.rowCalibration.reset()
            return
        }
        panel.getLocationOnScreen(this.panelLocation)
        status.getLocationOnScreen(this.statusLocation)
        this.rowCalibration.sample(this.panelLocation[0].toFloat(), this.statusLocation[0].toFloat())
    }

    fun onHostDetached(host: View?) {
        if (host === this.panelHost || host === this.statusHost) {
            resetRowCalibration()
        }
    }

    private fun resetRowCalibration() {
        this.rowCalibration.reset()
        this.panelHost = null
        this.statusHost = null
        this.rowDisplayId = -1
        this.rowLayoutDirection = -1
    }

    fun rowAlignHostShift(): Float {
        return this.rowCalibration.hostShift()
    }

    fun rowAlignSpeedShift(): Float {
        return this.rowCalibration.speedShift()
    }

    fun controller(): Any? {
        return this.alignmentController
    }

    fun resetAnchorSample() {
        this.lastAnchorSample = ""
    }

    fun motionGate(): String {
        return this.lockMotionGate
    }

    fun lastClosedAnchor(): String {
        return this.lastClosedAnchor
    }

    fun lastClosedCorrection(): String {
        return this.lastClosedCorrection
    }

    fun captureController(callback: Any?) {
        if (this.alignmentOuter == null || !this.module.compatibility!!.isAvailable(
                Compatibility.Capability.MOTION_ALIGNMENT
            )
        ) {
            return
        }
        try {
            this.alignmentController = this.alignmentOuter!!.get(callback)
        } catch (error: Throwable) {
            disableAlignment(error)
        }
    }

    fun restoreOffset(controller: Any?, offset: Int) {
        try {
            this.alignmentDiff!!.setInt(controller, offset)
        } catch (error: Throwable) {
            disableAlignment(error)
        }
    }

    @Throws(Exception::class)
    fun hookAlignment(classLoader: ClassLoader?) {
        val cls: Class<*> = ModuleMain.Companion.cls(
            classLoader,
            "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController"
        )
        this.alignmentReal = ModuleMain.Companion.field(cls, "realSystemIcons")
        this.alignmentDiff = ModuleMain.Companion.field(cls, "batteryWidthDiff")
        this.alignmentIsland = if (this.module.compatibility!!.isAvailable(
                Compatibility.Capability.ALIGNMENT_ISLAND
            )
        )
            ModuleMain.Companion.field(cls, "isAddBatteryIsland")
        else
            null
        this.alignmentCallback = ModuleMain.Companion.field(cls, "controlCenterCallback")
        this.alignmentUpdate = ModuleMain.Companion.method(cls, "updateLocation", *arrayOfNulls<Class<*>>(0))
        this.alignmentOuter = ModuleMain.Companion.field(
            ModuleMain.Companion.cls(
                classLoader,
                cls.getName() + "\$controlCenterCallback$1"
            ), "this$0"
        )
        this.alignmentProgress = ModuleMain.Companion.method(
            ModuleMain.Companion.cls(
                classLoader,
                cls.getName() + "\$controlCenterCallback$1"
            ), "onExpansionChanged", Float::class.javaPrimitiveType
        )
        this.module.installed.add(this.module.hook(this.alignmentUpdate!!).intercept(object : Hooker {
            @Throws(Throwable::class)
            override fun intercept(chain: XposedInterface.Chain): Any? {
                return this@AlignmentController.handleAlignmentProgress(chain)
            }
        }))
    }

    @Throws(Throwable::class)
    fun handleAlignmentProgress(chain: XposedInterface.Chain): Any? {
        val thisObject = chain.getThisObject()
        this.alignmentController = thisObject
        val zMaskIslandAlignment = maskIslandAlignment(thisObject)
        try {
            return chain.proceed()
        } finally {
            if (zMaskIslandAlignment) {
                try {
                    this.alignmentIsland!!.setBoolean(thisObject, true)
                } catch (th: Throwable) {
                    disableAlignment(th)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun alignmentPanel(): IconSession? {
        val controller = this.alignmentController ?: return null
        if (this.alignmentIsland?.getBoolean(controller) == true && !retainsIslandBattery(controller)) return null
        val session = this.module.containers[this.alignmentReal!!.get(controller)] ?: return null
        if (!session.merge || session.battery.getVisibility() != View.VISIBLE) return null
        var session2: IconSession? = null
        for (session3 in this.module.batteries.values) {
            if (session3.from == 6 && session3.merge && session3.battery.getDisplay() != null && session.battery.getDisplay() != null && session3.battery.getDisplay()
                    .getDisplayId() == session.battery.getDisplay().getDisplayId()
            ) {
                if (session2 != null) {
                    return null
                }
                session2 = session3
            }
        }
        return session2
    }

    @Throws(Exception::class)
    private fun retainsIslandBattery(obj: Any?): Boolean {
        if (!this.module.compatibility!!.isAvailable(Compatibility.Capability.CHARGING_ISLAND)) return false
        val batteryIsland = this.module.batteryIsland ?: return false
        val session = this.module.containers[this.alignmentReal!!.get(obj)] ?: return false
        return session.merge && !session.ambient && !batteryIsland.getBoolean(session.battery)
    }

    private fun maskIslandAlignment(obj: Any?): Boolean {
        if (obj != null) {
            try {
                if (this.alignmentIsland != null && this.module.compatibility!!.isAvailable(
                        Compatibility.Capability.MOTION_ALIGNMENT
                    )
                    && this.alignmentIsland!!.getBoolean(obj)
                    && retainsIslandBattery(obj)
                ) {
                    this.alignmentIsland!!.setBoolean(obj, false)
                    return true
                }
            } catch (th: Throwable) {
                disableAlignment(th)
            }
            return false
        }
        return false
    }

    fun applyAlignment(): Int? {
        if (!this.module.compatibility!!.isAvailable(
                Compatibility.Capability.MOTION_ALIGNMENT
            )
            || this.alignmentReal == null
        ) {
            return null
        }
        try {
            val sessionAlignmentPanel = alignmentPanel()
            if (sessionAlignmentPanel == null) {
                return null
            }
            val session: IconSession = this.module.containers.get(this.alignmentReal!!.get(this.alignmentController))!!
            if (session.battery.getWidth() > 0 && sessionAlignmentPanel.battery.getWidth() > 0) {
                val i = this.alignmentDiff!!.getInt(this.alignmentController)
                val width = session.battery.getWidth()
                val width2 = sessionAlignmentPanel.battery.getWidth()
                val z = (session.battery.getLayoutDirection()
                        == View.LAYOUT_DIRECTION_RTL)
                val rawOffset = Motion.batteryWidthOffset(width, width2, z)
                val appliedOffset = this.chargingAlignmentPolicy.resolve(
                    rawOffset,
                    this.module.state.charging,
                    this.module.stackedOn(),
                    this.module.control <= BatteryTransferConfig.HANDOFF_MAX
                )
                this.alignmentDiff!!.setInt(this.alignmentController, appliedOffset)
                if (appliedOffset != this.lastAlignmentApplied) {
                    this.lastAlignmentApplied = appliedOffset
                    this.module.log(
                        5, ModuleMain.Companion.TAG, ("alignmentFrame raw=" + rawOffset + " applied="
                                + appliedOffset + " charging=" + this.module.state.charging
                                + " stacked=" + this.module.stackedOn() + " widths=" + width + "x" + width2)
                    )
                }
                return i
            }
            return null
        } catch (th: Throwable) {
            disableAlignment(th)
            return null
        }
    }

    fun synchronizeAlignment(session: IconSession) {
        if (this.isSynchronizing
            || !this.module.compatibility!!.isAvailable(
                Compatibility.Capability.MOTION_ALIGNMENT
            ) || this.alignmentController == null || this.alignedLayoutEpoch == this.module.layoutEpoch
        ) {
            return
        }
        try {
            val sessionAlignmentPanel = alignmentPanel()
            if (sessionAlignmentPanel == null) {
                return
            }
            val session2 = this.module.containers.get(this.alignmentReal!!.get(this.alignmentController))
            if (session != sessionAlignmentPanel && session != session2 && (session.from != 5 || !session.merge || !sameDisplay(
                    session.battery,
                    session2!!.battery
                ))
            ) {
                return
            }
            this.isSynchronizing = true
            this.alignmentUpdate!!.invoke(this.alignmentController, *arrayOfNulls<Any>(0))
            this.alignmentProgress!!.invoke(this.alignmentCallback!!.get(this.alignmentController), this.module.control)
            this.alignedLayoutEpoch = this.module.layoutEpoch
        } catch (th: Throwable) {
            disableAlignment(th)
        } finally {
            this.isSynchronizing = false
        }
    }

    fun globalEdge(view: View, z: Boolean): Float {
        return globalX(view, (if (z) 0 else view.getWidth()).toFloat(), view.getHeight() / 2.0f)
    }

    private fun globalX(view: View, f: Float, f2: Float): Float {
        this.anchorMatrix.reset()
        view.transformMatrixToGlobal(this.anchorMatrix)
        this.anchorPoint[0] = f
        this.anchorPoint[1] = f2
        this.anchorMatrix.mapPoints(this.anchorPoint)
        return this.anchorPoint[0]
    }

    fun slotEdge(session: IconSession, z: Boolean): Float {
        return globalX(
            session.host,
            (if (z) session.host.getPaddingEnd() else session.host.getWidth() - session.host.getPaddingEnd()).toFloat(),
            session.host.getHeight() / 2.0f
        )
    }

    private fun expandedSlotEdge(session: IconSession, view: View, z: Boolean): Float {
        val fSlotEdge = slotEdge(session, z)
        if (view.getParent() !is View) {
            return Float.NaN
        }
        this.anchorMatrix.reset()
        (view.getParent() as View).transformMatrixToGlobal(this.anchorMatrix)
        this.anchorVector[0] = view.getTranslationX()
        this.anchorVector[1] = view.getTranslationY() - session.closedAnchor.nativeY(view.getTranslationY())
        this.anchorMatrix.mapVectors(this.anchorVector)
        return fSlotEdge - this.anchorVector[0]
    }

    @Throws(Exception::class)
    private fun alignLockMotion(
        session: IconSession,
        view: View,
        session2: IconSession,
        f: Float,
        f2: Float,
        z: Boolean
    ): Boolean {
        if (!session.merge || session.ambient || !view.isAttachedToWindow() || session.battery.getWidth() <= 0 || !sameDisplay(
                view,
                session2.battery
            )
        ) {
            return false
        }
        val fGlobalEdge = globalEdge(session.battery, z)
        if (!java.lang.Float.isFinite(fGlobalEdge)) {
            return false
        }
        val fOffset: Float = ClosedAnchor.Companion.offset(this.module.control, f, f2, fGlobalEdge)
        if (view.getParent() !is View) {
            return false
        }
        this.anchorMatrix.reset()
        (view.getParent() as View).transformMatrixToGlobal(this.anchorMatrix)
        if (!this.anchorMatrix.invert(this.anchorInverse)) {
            return false
        }
        this.anchorVector[0] = fOffset
        this.anchorVector[1] = 0.0f
        this.anchorInverse.mapVectors(this.anchorVector)
        if (!java.lang.Float.isFinite(this.anchorVector[0]) || !java.lang.Float.isFinite(this.anchorVector[1])) {
            return false
        }
        val str =
            "host=" + session.from + " p=" + this.module.control + " realEdge=" + f + " expandedEdge=" + f2 + " copyEdge=" + fGlobalEdge + " correctionX=" + fOffset + " realShown=" + session2.keyguard!!.isShown() + " rootTx=" + view.getTranslationX()
        if (str != this.lastAnchorSample) {
            this.lastAnchorSample = str
            this.lastClosedAnchor = ModuleMain.Companion.stamp(System.currentTimeMillis()) + " " + str
            if (abs(fOffset) > 0.25f) {
                this.lastClosedCorrection = this.lastClosedAnchor
            }
        }
        if (abs(fOffset) > 0.25f) {
            session.closedAnchor.move(
                view.getTranslationX(),
                view.getTranslationY(),
                this.anchorVector[0],
                this.anchorVector[1]
            )
            view.setTranslationX(session.closedAnchor.x())
            view.setTranslationY(session.closedAnchor.y())
        }
        val str2 =
            "h=" + session.from + " p=" + Math.round(this.module.control * 1000.0f) + " x=" + Math.round(fGlobalEdge) + ">" + Math.round(
                fGlobalEdge + fOffset
            ) + " ref=" + Math.round(f) + " exp=" + Math.round(f2) + " w=" + session.battery.getWidth() + " bt=" + Math.round(
                session.battery.getTranslationX()
            ) + " a=" + Math.round(view.getAlpha() * 100.0f) + " show=" + view.isShown() + " lock=" + session2.keyguard!!.isShown() + " lr=" + session.host.isLayoutRequested()
        if (str2 != session.lastMotionFrame) {
            session.lastMotionFrame = str2
            this.module.lockMotionTrace.add(str2)
        }
        return true
    }

    fun releaseClosedAnchors() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return
        }
        for (entry in this.module.visualCopies.entries) {
            entry.value.restoreClosedAnchor(entry.key)
        }
    }

    fun refreshClosedAnchors() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return
        }
        try {
            if (this.module.compatibility!!.isAvailable(Compatibility.Capability.CLOSED_ANCHOR)
                && this.module.compatibility!!.isAvailable(
                    Compatibility.Capability.MOTION_ALIGNMENT
                )
                && this.alignmentController != null && this.module.mode == 2
            ) {
                val session = this.module.containers.get(this.alignmentReal!!.get(this.alignmentController))
                if (session != null) {
                    var z = true
                    if (session.from == 1 && session.merge && !session.ambient && session.keyguard != null && this.module.isBrightKeyguard(
                            session.keyguard,
                            session.battery
                        )
                    ) {
                        val sessionAlignmentPanel = alignmentPanel()
                        var key: View? = null
                        for (entry in this.module.visualCopies.entries) {
                            if (entry.value == sessionAlignmentPanel) {
                                key = entry.key
                            }
                        }
                        if (sessionAlignmentPanel != null && key != null && session.battery.isAttachedToWindow() && session.host.getWidth() > 0 && sessionAlignmentPanel.host.getWidth() > 0) {
                            if (session.battery.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
                                z = false
                            }
                            val z2 = z
                            val fSlotEdge = slotEdge(session, z2)
                            val fExpandedSlotEdge = expandedSlotEdge(sessionAlignmentPanel, key, z2)
                            if (java.lang.Float.isFinite(fSlotEdge) && java.lang.Float.isFinite(fExpandedSlotEdge)) {
                                this.lockMotionGate = "shared-trajectory"
                                for (entry2 in this.module.visualCopies.entries) {
                                    if (!alignLockMotion(
                                            entry2.value,
                                            entry2.key,
                                            session,
                                            fSlotEdge,
                                            fExpandedSlotEdge,
                                            z2
                                        )
                                    ) {
                                        entry2.value.restoreClosedAnchor(entry2.key)
                                    }
                                }
                                return
                            }
                            this.lockMotionGate = "invalid-coordinates"
                            releaseClosedAnchors()
                            return
                        }
                        this.lockMotionGate = "geometry-unavailable"
                        releaseClosedAnchors()
                        return
                    }
                }
                this.lockMotionGate =
                    "reference=" + (if (session == null) "none" else session.from.toString() + " merge=" + session.merge + " aod=" + session.ambient)
                releaseClosedAnchors()
                return
            }
            this.lockMotionGate = "adapter-or-mode-off"
            releaseClosedAnchors()
        } catch (th: Throwable) {
            releaseClosedAnchors()
            this.module.compatibility!!.markUnavailable(
                Compatibility.Capability.CLOSED_ANCHOR, "runtime", th.toString()
            )
            this.lockMotionGate = "error"
            this.module.log(5, ModuleMain.Companion.TAG, "Lockscreen trajectory alignment unavailable", th)
        }
    }

    fun disableAlignment(th: Throwable) {
        this.module.compatibility!!.markUnavailable(
            Compatibility.Capability.MOTION_ALIGNMENT, "runtime", th.toString()
        )
        this.module.log(5, ModuleMain.Companion.TAG, "Native layout synchronization unavailable", th)
    }

    companion object {
        private fun sameDisplay(view: View, view2: View): Boolean {
            return if (view.getDisplay() == null || view2.getDisplay() == null || view.getDisplay()
                    .getDisplayId() != view2.getDisplay().getDisplayId()
            ) false else true
        }
    }
}
