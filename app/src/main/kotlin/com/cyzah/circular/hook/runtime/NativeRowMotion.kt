package com.cyzah.circular.hook.runtime

import android.graphics.Matrix
import android.view.View
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.NativeRowMotionPolicy
import java.util.ArrayList
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max

/** Keeps the native neighbour and copied row on one screen-space boundary.  */
class NativeRowMotion {
    private val targetHost = Translation()
    private val sourceHost = Translation()
    private val targetIcons = Translation()
    private val screenMatrix = Matrix()
    private val screenPoint = FloatArray(2)
    private val childTranslations: MutableMap<View?, Translation?> = IdentityHashMap<View?, Translation?>()
    private val iconPacking = NativeIconPacking<View?>(
        object : NativeIconPacking.Nodes<View?> {
            override fun slot(view: View?): String? {
                return slots.get(view)
            }

            override fun visible(view: View?): Boolean {
                return view!!.getVisibility() == View.VISIBLE
            }

            override fun width(view: View?): Float {
                return this@NativeRowMotion.screenWidth(view!!)
            }

            override fun place(node: View?, desiredRight: Float) {
                val view = node!!
                var lease = childTranslations.get(view)
                if (lease == null) {
                    lease = Translation()
                    childTranslations.put(view, lease)
                }
                lease.apply(view, desiredRight - right(view) + lease.appliedScreen(view))
            }

            override fun opacity(node: View?, value: Float) {
                val view = node!!
                var alpha = alphas.get(view)
                if (value < 1.0f) {
                    if (alpha == null) {
                        alpha = NativeRowMotionPolicy.Translation()
                        alphas.put(view, alpha)
                    }
                    val current = view.getAlpha()
                    val base = current - alpha.applied(current)
                    view.setAlpha(alpha.apply(current, base * (value - 1.0f)))
                } else if (alpha != null) {
                    view.setAlpha(alpha.release(view.getAlpha()))
                    alphas.remove(view)
                }
            }

            override fun release(view: View?) {
                val lease = childTranslations.remove(view)
                if (lease != null) lease.release()
                opacity(view, 1.0f)
            }
        })
    private val openingSlots = NativeRowMotionPolicy.OpeningSlots()
    private val endpoints = NativeRowMotionPolicy.Endpoints()
    private var endpointTarget: View? = null
    private var endpointSource: View? = null
    private var endpointStatus: View? = null
    private var endpointSettings: SessionSettings? = null
    private var endpointStatusSettings: SessionSettings? = null
    private var endpointConfiguration = 0
    private var endpointRootWidth = 0
    private var endpointRootHeight = 0
    private val targetChildren = ArrayList<View>()
    private val sourceChildren = ArrayList<View>()
    private val statusChildren = ArrayList<View>()
    private val slots: MutableMap<View?, String?> = IdentityHashMap<View?, String?>()
    private val alphas: MutableMap<View?, NativeRowMotionPolicy.Translation?> =
        IdentityHashMap<View?, NativeRowMotionPolicy.Translation?>()
    private val location = IntArray(2)

    fun prepare(
        target: IconSession?, source: IconSession?, expandedSourceBoundary: Float,
        sourceRowWidth: Int
    ) {
        if (target == null || source == null || target == source || target.closed || source.closed || !target.merge || !source.merge || target.from != 5 || source.from != 6 || sourceRowWidth <= 0 || !java.lang.Float.isFinite(
                expandedSourceBoundary
            )
        ) {
            reset()
            return
        }
        try {
            if (!target.battery.isAttachedToWindow() || !source.battery.isAttachedToWindow()) {
                reset()
                return
            }
            // Release a detached/replaced pair before sampling the new pair.
            if (!this.targetHost.matches(target.host) || !this.sourceHost.matches(source.host)) {
                discard()
            }
            val rawSource = rawSourceBoundary(source, sourceRowWidth)
            if (!java.lang.Float.isFinite(rawSource)) {
                reset()
                return
            }
            val correction = NativeRowMotionPolicy.sourceCorrection(
                expandedSourceBoundary, rawSource
            )
            // These child hosts are below the native animated outer containers.
            // Native updateLocation ignores transforms, so this cannot feed back
            // into its measured status-icon or battery-width offsets.
            if (!this.sourceHost.apply(source.host, correction)
                || !this.targetHost.apply(target.host, correction)
            ) {
                reset()
            }
        } catch (unavailable: RuntimeException) {
            reset()
        } catch (unavailable: LinkageError) {
            reset()
        }
    }

    fun settleIcons(target: IconSession?, desiredBatteryLeft: Float) {
        if (target == null || target.closed || !target.draw || !target.merge || target.from != 5 || !java.lang.Float.isFinite(
                desiredBatteryLeft
            )
        ) {
            this.targetIcons.release()
            return
        }
        try {
            target.battery.getLocationOnScreen(this.location)
            // The native gap already exists between icons and the battery.
            // Translate by the battery-boundary error to preserve that gap.
            val correction = NativeRowMotionPolicy.iconCorrection(
                this.location[0].toFloat(), desiredBatteryLeft
            )
            if (!this.targetIcons.apply(target.icons, correction)) {
                this.targetIcons.release()
            }
        } catch (unavailable: RuntimeException) {
            this.targetIcons.release()
        } catch (unavailable: LinkageError) {
            this.targetIcons.release()
        }
    }

    fun alignIcons(target: IconSession?, source: IconSession?, status: IconSession?, progress: Float) {
        if (target == null || source == null || status == null || !target.merge || !source.merge || target.closed || source.closed || status.closed
            || target.ambient || source.ambient || !target.module.stackedOn() || !target.battery.isAttachedToWindow() || !source.battery.isAttachedToWindow() || !status.battery.isAttachedToWindow() || target.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        ) {
            discard()
            return
        }
        try {
            val configuration = target.battery.getResources().getConfiguration().hashCode()
            val rootWidth = target.battery.getRootView().getWidth()
            val rootHeight = target.battery.getRootView().getHeight()
            if (endpointTarget !== target.host || endpointSource !== source.host || endpointStatus !== status.host || endpointSettings != target.module.controlCenterSettings() || endpointStatusSettings != target.module.sessionSettings() || endpointConfiguration != configuration || endpointRootWidth != rootWidth || endpointRootHeight != rootHeight) {
                if (endpointTarget != null) discard()
                endpointTarget = target.host
                endpointSource = source.host
                endpointStatus = status.host
                endpointSettings = target.module.controlCenterSettings()
                endpointStatusSettings = target.module.sessionSettings()
                endpointConfiguration = configuration
                endpointRootWidth = rootWidth
                endpointRootHeight = rootHeight
            }
            slots.clear()
            collect(target, targetChildren)
            collect(source, sourceChildren)
            collect(status, statusChildren)
            val p = Motion.clamp(progress)
            openingSlots.sample(p, target.expandedNeighbourCapture.closing())
            if (p <= BatteryTransferConfig.HANDOFF_MAX) {
                endpoints.closed(nativeRightmost(status, statusChildren, null), p)
                iconPacking.release()
                targetIcons.release()
                return
            }
            if (!java.lang.Float.isFinite(endpoints.closed)) {
                endpoints.closed(nativeRightmost(status, statusChildren, null), p)
            }
            val rawSource = nativeRightmost(source, sourceChildren, sourceHost)
            var outer: View? = source.host
            while (outer != null && ModuleMain.Companion.PANEL_STATUS != outer.javaClass.getName()) {
                outer = if (outer.getParent() is View) outer.getParent() as View? else null
            }
            val endpoint = if (outer == null) Float.NaN else (rawSource
                    - outer.getTranslationX() * sourceHost.parentXScale(outer))
            endpoints.expanded(endpoint, p, target.expandedNeighbourCapture.closing())
            if (p >= 1.0f && target.module.control >= 1.0f) {
                endpoints.expanded(rawSource, p, false)
                iconPacking.release()
                targetIcons.release()
                return
            }
            if (!java.lang.Float.isFinite(endpoints.closed) || !java.lang.Float.isFinite(endpoints.expanded)) {
                iconPacking.release()
                return
            }
            val desired = NativeRowMotionPolicy.speedBoundary(
                endpoints.closed, endpoints.expanded,
                NativeNeighbourMotion.remaining(
                    p,
                    target.module.controlCenterSettings().batteryNumberPlacement()
                )
            )
            // A pending layout may still put the container at its old boundary.
            // Child leases align both copies directly; the container bridge is redundant.
            targetIcons.release()
            val spacing = target.module.spacingField!!.getInt(target.icons)
            iconPacking.apply(
                targetChildren, sourceChildren, desired,
                spacing * abs(
                    sourceHost.parentXScale(
                        if (targetChildren.isEmpty())
                            target.battery
                        else
                            targetChildren.get(0)
                    )
                ),
                openingSlots.space(), openingSlots.alpha()
            )
        } catch (unavailable: ReflectiveOperationException) {
            iconPacking.release()
        } catch (unavailable: RuntimeException) {
            iconPacking.release()
        } catch (unavailable: LinkageError) {
            iconPacking.release()
        }
    }

    @Throws(ReflectiveOperationException::class)
    private fun collect(session: IconSession, result: ArrayList<View>) {
        result.clear()
        val ignored = session.module.ignoredSlots!!.get(session.icons) as MutableList<*>
        for (index in 0..<session.icons.getChildCount()) {
            val child = session.icons.getChildAt(index)
            val name = child.javaClass.getName()
            if (ModuleMain.Companion.WIFI == name || ModuleMain.Companion.MOBILE == name
                || child.getVisibility() != View.VISIBLE || child.getWidth() <= 0
            ) continue
            val slot = session.module.displaySlot!!.invoke(child) as String?
            if ((true != session.module.displayVisible!!.invoke(child)) || true == session.module.displayBlocked!!.invoke(
                    child
                )
                || ignored.contains(slot) || slot == null
            ) continue
            slots.put(child, slot)
            result.add(child)
        }
    }

    private fun nativeRightmost(
        session: IconSession,
        children: MutableList<View>,
        hostLease: Translation?
    ): Float {
        if (children.isEmpty()) {
            session.battery.getLocationOnScreen(this.location)
            return this.location[0] - (if (hostLease == null) 0f else hostLease.appliedScreen(session.host))
        }
        var result = Float.NEGATIVE_INFINITY
        for (child in children) {
            val edge = (right(child) - childAppliedScreen(child)
                    - targetIcons.appliedScreen(session.icons)
                    - (if (hostLease == null) 0.0f else hostLease.appliedScreen(session.host)))
            result = max(result, edge)
        }
        return result
    }

    private fun right(view: View): Float {
        screenMatrix.reset()
        view.transformMatrixToGlobal(screenMatrix)
        screenPoint[0] = view.getWidth().toFloat()
        screenPoint[1] = 0.0f
        screenMatrix.mapPoints(screenPoint)
        return screenPoint[0]
    }

    private fun screenWidth(view: View): Float {
        screenMatrix.reset()
        view.transformMatrixToGlobal(screenMatrix)
        screenPoint[0] = view.getWidth().toFloat()
        screenPoint[1] = 0.0f
        screenMatrix.mapVectors(screenPoint)
        return abs(screenPoint[0])
    }

    private fun childAppliedScreen(view: View): Float {
        val lease = childTranslations.get(view)
        return if (lease == null) 0.0f else lease.appliedScreen(view)
    }

    fun rawSourceBoundary(source: IconSession?, sourceRowWidth: Int): Float {
        if (source == null || sourceRowWidth <= 0) return Float.NaN
        try {
            source.battery.getLocationOnScreen(this.location)
            return (this.location[0] + source.battery.getWidth() - sourceRowWidth
                    - this.sourceHost.appliedScreen(source.host))
        } catch (unavailable: RuntimeException) {
            return Float.NaN
        } catch (unavailable: LinkageError) {
            return Float.NaN
        }
    }

    fun rawTargetBatteryLeft(target: IconSession?): Float {
        if (target == null) return Float.NaN
        try {
            target.battery.getLocationOnScreen(this.location)
            return this.location[0] - this.targetHost.appliedScreen(target.host)
        } catch (unavailable: RuntimeException) {
            return Float.NaN
        } catch (unavailable: LinkageError) {
            return Float.NaN
        }
    }

    fun reset() {
        this.iconPacking.release()
        this.targetIcons.release()
        this.targetHost.release()
        this.sourceHost.release()
    }

    /** Geometry/lifecycle invalidation, distinct from a per-frame lease release.  */
    fun discard() {
        reset()
        endpoints.invalidate()
        openingSlots.reset()
        endpointStatus = null
        endpointSource = endpointStatus
        endpointTarget = endpointSource
        endpointStatusSettings = null
        endpointSettings = endpointStatusSettings
        endpointRootHeight = 0
        endpointRootWidth = endpointRootHeight
        endpointConfiguration = endpointRootWidth
        targetChildren.clear()
        sourceChildren.clear()
        statusChildren.clear()
        slots.clear()
    }

    private class Translation {
        private val lease = NativeRowMotionPolicy.Translation()
        private val matrix = Matrix()
        private val vector = FloatArray(2)
        private var view: View? = null

        fun matches(candidate: View?): Boolean {
            return this.view == null || this.view === candidate
        }

        fun apply(candidate: View?, screenCorrection: Float): Boolean {
            if (this.view !== candidate) {
                release()
                this.view = candidate
            }
            if (candidate == null) return false
            val scale = parentXScale(candidate)
            if (!java.lang.Float.isFinite(scale) || abs(scale) < 0.0001f) return false
            val current = candidate.getTranslationX()
            val next = this.lease.apply(current, screenCorrection / scale)
            if (next != current) candidate.setTranslationX(next)
            return true
        }

        fun appliedScreen(candidate: View): Float {
            if (this.view !== candidate) return 0.0f
            val applied = this.lease.applied(candidate.getTranslationX())
            return if (applied == 0.0f) 0.0f else applied * parentXScale(candidate)
        }

        fun release() {
            val previous = this.view
            this.view = null
            if (previous == null) return
            try {
                val current = previous.getTranslationX()
                val restored = this.lease.release(current)
                if (restored != current) previous.setTranslationX(restored)
            } catch (unavailable: RuntimeException) {
                this.lease.release(Float.NaN)
            } catch (unavailable: LinkageError) {
                this.lease.release(Float.NaN)
            }
        }

        fun parentXScale(candidate: View): Float {
            val parent = candidate.getParent()
            if (parent !is View) return 1.0f
            this.matrix.reset()
            (parent as View).transformMatrixToGlobal(this.matrix)
            this.vector[0] = 1.0f
            this.vector[1] = 0.0f
            this.matrix.mapVectors(this.vector)
            return this.vector[0]
        }
    }
}
