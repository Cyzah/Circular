package com.cyzah.circular.hook.runtime

import android.graphics.Canvas
import android.os.SystemClock
import android.text.TextUtils
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.ExpansionPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.animation.PanelMotionFrame
import com.cyzah.circular.hook.platform.OemCompatibility
import com.cyzah.circular.hook.platform.OemStatusAdapter
import io.github.libxposed.api.XposedInterface
import java.util.ArrayList
import kotlin.math.min

class OemIconSession(
    private val host: OemStatusAdapter,
    @JvmField val battery: View,
    @JvmField val icons: ViewGroup,
    @JvmField val root: View,
    @JvmField val keyguard: Boolean,
    @JvmField val panel: Boolean
) {
    interface NativeDraw {
        @Throws(Throwable::class)
        fun run(): Any?
    }

    @JvmField var active: Boolean = false
    @JvmField var closed: Boolean = false
    @JvmField var failedUntil: Long = 0
    @JvmField var layoutQueued: Boolean = false
    @JvmField var mirrorVisible: Boolean = false
    @JvmField var networkWidth: Int = 0
    @JvmField var progress: Float = 0f
    @JvmField var savedDescription: CharSequence? = null
    @JvmField var stockWidth: Int = 0
    @JvmField var tree: ViewTreeObserver? = null
    @JvmField val painter: IconPainter = IconPainter()
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
    @JvmField var reason: String = "pending"
    @JvmField var lastTint: Int = -1
    @JvmField var lastSize: Int = -1
    @JvmField val listener: OnPreDrawListener = object : OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            return this@OemIconSession.beforeDraw()
        }
    }

    fun beforeDraw(): Boolean {
        if (this.closed) {
            return true
        }
        val wasActive = this.active
        reconcile()
        if (this.active) {
            this.host.readSignals(this)
        }
        return wasActive == this.active || !this.icons.isLayoutRequested()
    }

    fun observe() {
        this.tree = this.battery.getViewTreeObserver()
        this.tree!!.addOnPreDrawListener(this.listener)
    }

    fun close() {
        this.closed = true
        setActive(false)
        if (this.tree != null && this.tree!!.isAlive()) {
            this.tree!!.removeOnPreDrawListener(this.listener)
        }
        val current = this.battery.getViewTreeObserver()
        if (current == this.tree || !current.isAlive()) {
            return
        }
        current.removeOnPreDrawListener(this.listener)
    }

    fun pixels(): Int {
        return Math.round(
            this.host.module.oemSize()
                    * this.battery.getResources().getDisplayMetrics().density
        )
    }

    fun reconcile() {
        val eligible: Boolean
        try {
            val controller = this.host.controller.get()
            val status = if (controller == null) -1 else this.host.statusField!!.getInt(controller)
            val dozing = controller == null
                    || this.host.dozingField!!.getBoolean(controller)
            val display = this.battery.getDisplay()
            val displayUsable =
                display != null && display.getDisplayId() == Display.DEFAULT_DISPLAY && display.getState() == Display.STATE_ON
            val batteryVisible = this.battery.getVisibility() == View.VISIBLE && (this.host.batteryHidden == null
                    || !this.host.batteryHidden!!.getBoolean(this.battery))
                    && ((this.host.batteryDisabled == null
                    || !this.host.batteryDisabled!!.getBoolean(this.battery))
                    && (this.host.batteryAod == null
                    || !this.host.batteryAod!!.getBoolean(this.battery)))
            val stateReady = (this.host.batteryModel == null
                    || this.host.batteryModel!!.get(this.battery) != null)
                    && this.host.module.oemReady()
                    && this.host.receiverRegistered
                    && SystemClock.elapsedRealtime() >= this.failedUntil
            val sameParent = this.icons.getParent() === this.battery.getParent()
            var parent = this.battery.getParent()
            while (true) {
                if (parent !is View) {
                    eligible = false
                    break
                }
                if (parent === this.root) {
                    eligible = true
                    break
                }
                parent = parent.getParent()
            }
            this.mirrorVisible = this.host.mirrored(this)
            val rootVisible = this.root.isShown() || this.mirrorVisible
            var shouldBeActive = if (this.panel)
                (this.host.motionReady
                        && this.host.module.oemEnabled()
                        && stateReady
                        && batteryVisible
                        && rootVisible
                        && displayUsable
                        && !dozing && ExpansionPolicy.panelEnabled(
                    status,
                    this.host.module.oemLockscreen()
                ))
            else
                OemCompatibility.eligible(
                    this.host.module.oemEnabled(),
                    stateReady,
                    batteryVisible,
                    rootVisible,
                    displayUsable,
                    dozing,
                    this.keyguard,
                    this.host.module.oemLockscreen(),
                    status
                )
            shouldBeActive = shouldBeActive && sameParent && eligible && !this.closed
            val nextProgress = if (this.host.motionReady)
                ExpansionPolicy.progress(
                    this.host.module.oemKeepMergedShade(),
                    this.keyguard,
                    this.mirrorVisible,
                    this.host.fraction
                )
            else
                0.0f
            if (this.progress != nextProgress) {
                this.progress = nextProgress
                if (this.active) {
                    relayout()
                }
            }
            this.reason = if (shouldBeActive)
                "ready"
            else
                if (this.closed)
                    "detached"
                else
                    if (sameParent && eligible)
                        if (!this.host.module.oemEnabled())
                            "mode-off"
                        else
                            if (!stateReady)
                                "state-not-ready"
                            else
                                if (!displayUsable || dozing)
                                    "native-aod"
                                else
                                    if (!batteryVisible)
                                        "system-hidden"
                                    else
                                        if (!rootVisible)
                                            "root-hidden"
                                        else
                                            "surface-disabled"
                    else
                        "host-changed"
            setActive(shouldBeActive)
            val size = pixels()
            if (this.active && size != this.lastSize) {
                this.lastSize = size
                relayout()
            }
            if (this.active) {
                val description = this.host.module.oemState().description()
                if (!TextUtils.equals(description, this.battery.getContentDescription())) {
                    this.battery.setContentDescription(description)
                }
            }
        } catch (error: Throwable) {
            fail(error)
        }
    }

    fun setActive(active: Boolean) {
        if (this.active == active) {
            return
        }
        if (active) {
            this.savedDescription = this.battery.getContentDescription()
        }
        this.active = active
        if (!active) {
            this.battery.setContentDescription(this.savedDescription)
            this.savedDescription = null
        }
        relayout()
    }

    fun relayout() {
        this.battery.requestLayout()
        this.icons.requestLayout()
        this.battery.invalidate()
        this.icons.invalidate()
    }

    fun fail(error: Throwable) {
        this.failedUntil = SystemClock.elapsedRealtime() + 30000
        this.reason = "fallback:" + error.javaClass.getSimpleName()
        this.host.failure = this.reason
        setActive(false)
    }

    @Throws(Exception::class)
    fun wanted(view: View): Boolean {
        val model = this.host.binding!!.get(view)
        return !(view.getVisibility() == View.GONE || model == null || (true != this.host.shouldBeVisible!!.invoke(
            model,
            *arrayOfNulls<Any>(0)
        )) || true == this.host.isBlocked!!.invoke(view, *arrayOfNulls<Any>(0))
                || (this.host.ignoredSlots!!.get(this.icons) as MutableList<*>).contains(
            this.host.getSlot!!.invoke(view, *arrayOfNulls<Any>(0))
        ))
    }

    @Throws(Exception::class)
    fun measureSignals(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val previousWidth = this.networkWidth
        this.parts.clear()
        this.networkWidth = 0
        val spacing = if (this.host.iconSpacing == null)
            0
        else
            this.host.iconSpacing!!.getInt(this.icons)
        for (index in 0..<this.icons.getChildCount()) {
            val child = this.icons.getChildAt(index)
            if ((this.host.mobileType!!.isInstance(child)
                        || this.host.wifiType!!.isInstance(child))
                && wanted(child!!)
            ) {
                this.host.visibleState!!.invoke(child, 0, false)
                this.host.measureChild!!.invoke(
                    this.icons,
                    child,
                    View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.getSize(widthMeasureSpec),
                        View.MeasureSpec.UNSPECIFIED
                    ),
                    heightMeasureSpec
                )
                if (child.getMeasuredWidth() > 0) {
                    this.parts.add(child)
                    this.networkWidth += (child.getMeasuredWidth()
                            + child.getPaddingStart()
                            + child.getPaddingEnd()
                            + spacing)
                }
            }
        }
        if (!this.parts.isEmpty()) {
            this.networkWidth += this.icons.getPaddingEnd()
        }
        if (previousWidth == this.networkWidth || this.layoutQueued) {
            return
        }
        this.layoutQueued = true
        this.host.main.post(object : Runnable {
            override fun run() {
                onSessionLayoutQueued()
            }
        })
    }

    fun onSessionLayoutQueued() {
        this.layoutQueued = false
        if (this.closed || !this.active) {
            return
        }
        relayout()
    }

    fun layoutSignals() {
        for (view in this.parts) {
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight())
        }
    }

    @Throws(Throwable::class)
    fun paint(canvas: Canvas, nativeDraw: NativeDraw) {
        this.host.module.oemPainter(this.painter)
        this.lastTint = if (this.host.batteryTint != null)
            this.host.batteryTint!!.getInt(this.battery)
        else
            this.host.modelTint!!.getInt(this.host.batteryModel!!.get(this.battery))
        val size = min(
            pixels(),
            min(this.battery.getWidth(), this.battery.getHeight())
        ).toFloat()
        if (size <= 0.0f) {
            return
        }
        val driftX: Float = BurnInDrift.Companion.offset(
            this.host.drift.x(),
            BurnInDrift.Companion.roomX(
                this.battery.getWidth().toFloat(),
                size,
                this.host.module.oemWeight()
            ),
            false
        )
        val driftY: Float = BurnInDrift.Companion.offset(
            this.host.drift.y(),
            BurnInDrift.Companion.roomY(
                this.battery.getHeight().toFloat(),
                size,
                this.host.module.oemWeight()
            ),
            false
        )
        if (!this.host.motionReady || this.progress == 0.0f) {
            this.painter.draw(
                canvas,
                ((this.battery.getWidth() - size) / 2.0f) + driftX,
                ((this.battery.getHeight() - size) / 2.0f) + driftY,
                size,
                this.host.module.oemState(),
                this.lastTint,
                this.host.module.oemColoured()
            )
            return
        }
        val state = this.host.module.oemState()
        val rtl = this.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        val progress = this.progress
        val nativeAlpha = PanelMotionFrame.nativeAlpha(progress)
        val width = this.battery.getWidth().toFloat()
        val height = this.battery.getHeight().toFloat()
        val halfSize = size / 2.0f
        val trinityX = if (rtl) halfSize else width - halfSize
        val centreY = height / 2.0f
        val nativeLeft = if (rtl) 0.0f else width - this.stockWidth
        val nativeCentreX = (this.stockWidth / 2.0f) + nativeLeft
        val trinityAlpha = 1.0f - nativeAlpha
        part(
            canvas,
            0,
            trinityX,
            centreY,
            nativeCentreX,
            centreY,
            size,
            progress,
            trinityAlpha,
            driftX,
            driftY
        )
        if (nativeAlpha > 0.0f) {
            val layer = canvas.saveLayerAlpha(
                0.0f,
                0.0f,
                width,
                height,
                Math.round(nativeAlpha * 255.0f)
            )
            val previousBypass = this.host.bypass
            var drawFailure: Throwable? = null
            try {
                canvas.translate(
                    Motion.mix(
                        nativeCentreX - (this.stockWidth / 2.0f),
                        nativeLeft,
                        progress
                    ),
                    0.0f
                )
                this.host.bypass = true
                nativeDraw.run()
            } catch (error: Throwable) {
                drawFailure = error
                throw error
            } finally {
                this.host.bypass = previousBypass
                NativeSessionDraw.restoreCanvas(canvas, layer, drawFailure)
            }
        }
        var cursor = if (rtl)
            (
                    this.stockWidth + this.icons.getPaddingEnd()
                    ).toFloat()
        else
            (width - this.stockWidth) - this.icons.getPaddingEnd()
        val wifiY = (centreY
                + (((this.painter.anchorY(1, state) - 60.0f) * size) / 120.0f))
        val spacing = if (this.host.iconSpacing == null)
            0
        else
            this.host.iconSpacing!!.getInt(this.icons)
        var batteryAdjustment = 0.0f
        if (this.host.coordBattery != null) {
            batteryAdjustment = (this.host.coordBattery!!.invoke(
                this.battery,
                *arrayOfNulls<Any>(0)
            ) as Float)
        }
        var lastCentreX = trinityX
        var wifiDrawn = false
        var mobileDrawn = false
        for (index in this.parts.indices.reversed()) {
            val view = this.parts.get(index)
            val measuredWidth = view.getMeasuredWidth().toFloat()
            val paddingEnd: Float
            val paddingStart: Float
            if (rtl) {
                paddingEnd = cursor + view.getPaddingEnd()
                paddingStart = (paddingEnd
                        + measuredWidth
                        + view.getPaddingStart()
                        + spacing)
            } else {
                paddingEnd = (cursor - view.getPaddingEnd()) - measuredWidth
                paddingStart = (paddingEnd - view.getPaddingStart()) - spacing
            }
            val wifi = this.host.wifiType!!.isInstance(view)
            var childAdjustment = 0.0f
            val method = if (wifi)
                this.host.coordWifi
            else
                this.host.coordMobile
            if (method != null && method.getDeclaringClass().isInstance(view)) {
                childAdjustment = (method.invoke(
                    view,
                    *arrayOfNulls<Any>(0)
                ) as Float)
            }
            val mobileY = wifiY + ((size * 45.5f) / 120.0f)
            val childX = ((paddingEnd + (measuredWidth / 2.0f))
                    + childAdjustment
                    - batteryAdjustment)
            lastCentreX = childX
            if (wifi) {
                if (state.showWifi() && !wifiDrawn) {
                    part(
                        canvas,
                        1,
                        trinityX,
                        wifiY,
                        childX,
                        centreY,
                        size,
                        progress,
                        trinityAlpha,
                        driftX,
                        driftY
                    )
                    wifiDrawn = true
                }
            } else if (!mobileDrawn
                && this.host.subId!!.getInt(view) == state.subId
            ) {
                if (!state.showWifi() && !wifiDrawn) {
                    part(
                        canvas,
                        1,
                        trinityX,
                        wifiY,
                        childX,
                        centreY,
                        size,
                        progress,
                        trinityAlpha,
                        driftX,
                        driftY
                    )
                    wifiDrawn = true
                }
                part(
                    canvas,
                    2,
                    trinityX,
                    mobileY,
                    childX,
                    centreY,
                    size,
                    progress,
                    trinityAlpha,
                    driftX,
                    driftY
                )
                mobileDrawn = true
            }
            if (nativeAlpha > 0.0f) {
                val layer = canvas.saveLayerAlpha(
                    0.0f,
                    0.0f,
                    width,
                    height,
                    Math.round(nativeAlpha * 255.0f)
                )
                val previousBypass = this.host.bypass
                var drawFailure: Throwable? = null
                try {
                    canvas.translate(
                        ExpansionPolicy.nativeTranslation(
                            childX - (measuredWidth / 2.0f),
                            paddingEnd,
                            progress,
                            batteryAdjustment,
                            childAdjustment
                        ),
                        Motion.mix(centreY, mobileY, progress)
                                - (view.getHeight() / 2.0f)
                    )
                    this.host.bypass = true
                    view.draw(canvas)
                } catch (error: Throwable) {
                    drawFailure = error
                    throw error
                } finally {
                    this.host.bypass = previousBypass
                    NativeSessionDraw.restoreCanvas(canvas, layer, drawFailure)
                }
            }
            cursor = paddingStart
        }
        if (!wifiDrawn) {
            part(
                canvas,
                1,
                trinityX,
                wifiY,
                lastCentreX,
                centreY,
                size,
                progress,
                trinityAlpha,
                driftX,
                driftY
            )
        }
        if (!mobileDrawn) {
            part(
                canvas,
                2,
                trinityX,
                centreY + ((size * 45.5f) / 120.0f),
                lastCentreX,
                centreY,
                size,
                progress,
                trinityAlpha,
                driftX,
                driftY
            )
        }
    }

    @Throws(Throwable::class)
    fun part(
        canvas: Canvas,
        part: Int,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        size: Float,
        progress: Float,
        alpha: Float,
        driftX: Float,
        driftY: Float
    ) {
        if (alpha <= 0.0f) {
            return
        }
        this.painter.setMotion(progress)
        this.painter.drawPart(
            canvas,
            (Motion.mix(fromX, toX, progress) + (BurnInDrift.Companion.fade(progress) * driftX))
                    - (size / 2.0f),
            (Motion.mix(fromY, toY, progress) + (BurnInDrift.Companion.fade(progress) * driftY))
                    - ((this.painter.anchorY(part, this.host.module.oemState()) * size)
                    / 120.0f),
            size,
            this.host.module.oemState(),
            this.lastTint,
            this.host.module.oemColoured(),
            part,
            alpha
        )
    }
}
