package com.cyzah.circular.hook.runtime

import android.view.View
import com.cyzah.circular.core.graphics.animation.LockFrameSample
import kotlin.math.abs

/** Lock-screen diagnostics, kept separate from session state and layout decisions.  */
class IconSessionDiagnostics {
    private val realLockFrame = LockFrameSample()
    private var lastShown = false

    fun beginFrame() {
        realLockFrame.beginFrame()
    }

    fun captureRealLockFrame(owner: IconSession) {
        if (owner.keyguard == null || owner.ambient) {
            return
        }
        val module = owner.module
        val shown = owner.keyguard.isShown()
        if (!shown && module.control == 0.0f && !lastShown) {
            return
        }
        lastShown = shown
        val rtl = owner.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        val needsGeometry = realLockFrame.prepare(
            module.control, shown, owner.merge, owner.battery.getWidth(),
            owner.battery.getTranslationX(), owner.host.getTranslationX(),
            owner.keyguard.getTranslationX(), owner.keyguard.getScaleX(),
            owner.reflowBlocks, owner.host.isLayoutRequested(), rtl
        )
        val reset = owner.lastRealFrame.isEmpty()
        if (!needsGeometry && !reset) {
            return
        }
        val frame = realLockFrame.record(
            Math.round(module.globalEdge(owner.battery, rtl)),
            Math.round(module.slotEdge(owner, rtl)), reset
        )
        if (frame == null) {
            return
        }
        owner.lastRealFrame = frame
        module.realLockTrace.add(frame)
        captureTransforms(owner)
    }

    private fun captureTransforms(owner: IconSession) {
        val transforms = StringBuilder()
        var view: View? = owner.battery
        var transformed = false
        var depth = 0
        while (view != null && depth < 7) {
            if (transforms.length > 0) {
                transforms.append(" / ")
            }
            transforms.append(view.javaClass.getSimpleName())
                .append(" l=").append(view.getLeft())
                .append(" r=").append(view.getRight())
                .append(" tx=").append(view.getTranslationX())
                .append(" sx=").append(view.getScaleX())
                .append(" px=").append(view.getPivotX())
                .append(" a=").append(view.getAlpha())
                .append(" vis=").append(view.getVisibility())
                .append(" anim=").append(
                    if (view.getAnimation() == null)
                        "none"
                    else
                        view.getAnimation().javaClass.getSimpleName()
                )
            transformed =
                transformed or (abs(view.getTranslationX()) > 0.001f || abs(view.getScaleX() - 1.0f) > 0.001f || view.getAnimation() != null)
            view = if (view.getParent() is View) view.getParent() as View? else null
            depth++
        }
        if (transformed || owner.module.lastLockTransforms.startsWith("尚未")) {
            owner.module.lastLockTransforms = (ModuleMain.Companion.stamp(System.currentTimeMillis())
                    + " " + transforms)
        }
    }
}
