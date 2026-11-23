package com.cyzah.circular.core.graphics.animation

import android.graphics.Matrix
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.hook.runtime.BurnInDrift
import com.cyzah.circular.hook.runtime.ControlCenterAppearance
import com.cyzah.circular.hook.runtime.ControlCenterRowComponent
import com.cyzah.circular.hook.runtime.IconSession
import com.cyzah.circular.hook.runtime.SessionPaintFrame
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import kotlin.math.hypot
import kotlin.math.min

/** Projects both CC Wi-Fi copies from the same expanded native row.  */
class SharedWifiFrame {
    private val source = Matrix()
    private val target = Matrix()
    private val inverse = Matrix()
    private val transform = Matrix()
    private val point = FloatArray(2)
    @JvmField var x: Float = 0f
    @JvmField var y: Float = 0f
    @JvmField var scale: Float = 0f

    fun resolve(owner: IconSession, frame: SessionPaintFrame, canvasShift: Float): Boolean {
        val primary = frame.primaryRowHost
        if (!frame.solvedRowActive || primary == null || primary.from != 6 || !primary.battery.isAttachedToWindow()) return false
        val row = primary.controlCenterRow.currentSnapshot()
        if (!row.valid() || !row.visible(ControlCenterRowComponent.WIFI)) return false
        var rowShift = if (primary.controlCenterRow.usesMeasuredGeometry())
            primary.battery.getWidth() - row.rowRight()
        else
            0.0f
        rowShift = SessionPaintPolicy.rowTranslationWithDrift(
            rowShift, primary.driftX,
            BurnInDrift.Companion.fade(owner.progress)
        )
        point[0] = (row.left(ControlCenterRowComponent.WIFI)
                + row.right(ControlCenterRowComponent.WIFI)) / 2.0f + rowShift
        val minimum = min(
            primary.dp(ControlCenterAppearance.BATTERY_SIZE),
            primary.battery.getHeight()
        ).toFloat()
        point[1] = (primary.battery.getHeight() / 2.0f
                + (if (owner.module.controlCenterSettings().capsuleDual())
            IconPainter.Companion.CAPSULE_WIFI_END_DY * minimum / 120.0f
        else
            0.0f))
        source.reset()
        primary.battery.transformMatrixToGlobal(source)
        target.reset()
        owner.battery.transformMatrixToGlobal(target)
        target.preTranslate(canvasShift, 0.0f)
        if (!target.invert(inverse)) return false
        transform.setConcat(inverse, source)
        transform.mapPoints(point)
        x = point[0]
        y = point[1]
        point[0] = 0f
        point[1] = 1f
        transform.mapVectors(point)
        scale = hypot(point[0].toDouble(), point[1].toDouble()).toFloat()
        return java.lang.Float.isFinite(x) && java.lang.Float.isFinite(y) && java.lang.Float.isFinite(scale) && scale > 0
    }
}
