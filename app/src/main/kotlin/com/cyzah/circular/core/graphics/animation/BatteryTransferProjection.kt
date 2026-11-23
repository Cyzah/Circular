package com.cyzah.circular.core.graphics.animation

import android.graphics.Matrix
import android.view.View
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.hook.runtime.IconSession
import kotlin.math.hypot
import kotlin.math.min

/**
 * Converts the closed host-0 ring endpoints into screen space and then into the
 * active control-center owner's local canvas coordinates.
 */
class BatteryTransferProjection {
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val point = FloatArray(2)
    private val ringFrame = FloatArray(6)

    @JvmField var startScreenX: Float = 0f
    @JvmField var startScreenY: Float = 0f
    @JvmField var endScreenX: Float = 0f
    @JvmField var endScreenY: Float = 0f
    @JvmField var startLocalX: Float = 0f
    @JvmField var startLocalY: Float = 0f
    @JvmField var endLocalX: Float = 0f
    @JvmField var endLocalY: Float = 0f
    @JvmField var pointScreenX: Float = 0f
    @JvmField var pointScreenY: Float = 0f
    @JvmField var ringOriginX: Float = 0f
    @JvmField var ringOriginY: Float = 0f
    @JvmField var ringLocalX: Float = 0f
    @JvmField var ringLocalY: Float = 0f
    @JvmField var ringUnit: Float = 0f
    @JvmField var ringUnitX: Float = 0f
    @JvmField var ringUnitY: Float = 0f

    @JvmOverloads
    fun project(
        ringHost: IconSession?, geometry: RingGeometry?, shapeHost: IconSession?,
        canvasShift: Float = if (shapeHost != null && shapeHost.from == 5)
            shapeHost.module.rowAlignHostShift() * shapeHost.stackedAlpha
        else
            0.0f
    ): Boolean {
        if (ringHost == null || geometry == null || !geometry.valid() || shapeHost == null) {
            return false
        }
        val minimum = min(
            ringHost.dp(ringHost.module.sessionSettings().size()),
            ringHost.battery.getHeight()
        ).toFloat()
        if (minimum <= 0.0f || ringHost.battery.getWidth() <= 0 || ringHost.battery.getHeight() <= 0) {
            return false
        }
        this.ringUnit = minimum / 120.0f
        this.ringOriginX = BatteryRingFrame.closedOriginX(
            ringHost.battery.getWidth().toFloat(), minimum,
            ringHost.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL,
            ringHost.driftX
        )
        this.ringOriginY = BatteryRingFrame.closedOriginY(
            ringHost.battery.getHeight().toFloat(), minimum, ringHost.driftY
        )

        toScreen(
            ringHost.battery,
            this.ringOriginX + (geometry.startX * this.ringUnit),
            this.ringOriginY + (geometry.startY * this.ringUnit)
        )
        this.startScreenX = this.point[0]
        this.startScreenY = this.point[1]
        toScreen(
            ringHost.battery,
            this.ringOriginX + (geometry.endX * this.ringUnit),
            this.ringOriginY + (geometry.endY * this.ringUnit)
        )
        this.endScreenX = this.point[0]
        this.endScreenY = this.point[1]
        this.matrix.reset()
        shapeHost.battery.transformMatrixToGlobal(this.matrix)
        if (!this.matrix.invert(this.inverse)) {
            return false
        }
        this.ringFrame[0] = this.ringOriginX
        this.ringFrame[1] = this.ringOriginY
        this.ringFrame[2] = this.ringOriginX + this.ringUnit
        this.ringFrame[3] = this.ringOriginY
        this.ringFrame[4] = this.ringOriginX
        this.ringFrame[5] = this.ringOriginY + this.ringUnit
        this.matrix.reset()
        ringHost.battery.transformMatrixToGlobal(this.matrix)
        this.matrix.mapPoints(this.ringFrame)
        this.inverse.mapPoints(this.ringFrame)
        this.ringLocalX = this.ringFrame[0]
        this.ringLocalY = this.ringFrame[1]
        this.ringUnitX = hypot(
            (this.ringFrame[2] - this.ringFrame[0]).toDouble(),
            (this.ringFrame[3] - this.ringFrame[1]).toDouble()
        ).toFloat()
        this.ringUnitY = hypot(
            (this.ringFrame[4] - this.ringFrame[0]).toDouble(),
            (this.ringFrame[5] - this.ringFrame[1]).toDouble()
        ).toFloat()
        if (shapeHost.from == 5) {
            this.ringLocalX -= canvasShift
        }
        this.point[0] = this.endScreenX
        this.point[1] = this.endScreenY
        this.inverse.mapPoints(this.point)
        if (shapeHost.from == 5) {
            this.point[0] -= canvasShift
        }
        this.endLocalX = this.point[0]
        this.endLocalY = this.point[1]
        this.point[0] = this.startScreenX
        this.point[1] = this.startScreenY
        this.inverse.mapPoints(this.point)
        if (shapeHost.from == 5) {
            this.point[0] -= canvasShift
        }
        this.startLocalX = this.point[0]
        this.startLocalY = this.point[1]
        return java.lang.Float.isFinite(this.startLocalX)
                && java.lang.Float.isFinite(this.startLocalY)
                && java.lang.Float.isFinite(this.endLocalX)
                && java.lang.Float.isFinite(this.endLocalY)
                && java.lang.Float.isFinite(this.ringLocalX)
                && java.lang.Float.isFinite(this.ringLocalY)
                && java.lang.Float.isFinite(this.ringUnitX) && this.ringUnitX > 0.0f && java.lang.Float.isFinite(this.ringUnitY) && this.ringUnitY > 0.0f
    }

    fun projectPoint(
        ringHost: IconSession?,
        geometry: RingGeometry?,
        progress: Float
    ): Boolean {
        if (ringHost == null || geometry == null || !geometry.valid()) {
            return false
        }
        val distance = geometry.length * Motion.clamp(progress)
        toScreen(
            ringHost.battery,
            this.ringOriginX + (geometry.pointX(distance) * this.ringUnit),
            this.ringOriginY + (geometry.pointY(distance) * this.ringUnit)
        )
        this.pointScreenX = this.point[0]
        this.pointScreenY = this.point[1]
        return java.lang.Float.isFinite(this.pointScreenX)
                && java.lang.Float.isFinite(this.pointScreenY)
    }

    private fun toScreen(view: View, x: Float, y: Float) {
        this.matrix.reset()
        view.transformMatrixToGlobal(this.matrix)
        this.point[0] = x
        this.point[1] = y
        this.matrix.mapPoints(this.point)
    }
}
