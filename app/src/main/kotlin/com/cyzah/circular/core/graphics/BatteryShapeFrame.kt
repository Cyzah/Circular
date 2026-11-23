package com.cyzah.circular.core.graphics

import android.graphics.Matrix
import android.graphics.RectF
import android.view.View
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.BatteryTransferProjection
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.hook.runtime.BurnInDrift
import com.cyzah.circular.hook.runtime.ControlCenterRowSnapshot
import com.cyzah.circular.hook.runtime.IconSession
import com.cyzah.circular.hook.runtime.IconSessionPainter
import com.cyzah.circular.hook.runtime.SessionPaintGeometry
import com.cyzah.circular.hook.runtime.SessionPaintPolicy
import kotlin.math.min

/** One battery shape sample shared by text avoidance and battery painting.  */
class BatteryShapeFrame {
    @JvmField val bounds: RectF = RectF()
    @JvmField val pointBounds: RectF = RectF()
    @JvmField val solidBounds: RectF = RectF()
    @JvmField val opening: OpeningBatteryMorph = OpeningBatteryMorph()
    private val obstacleGeometry = BatteryShapeGeometry()
    private val obstacleProjection = BatteryTransferProjection()
    private val sourceMatrix = Matrix()
    private val targetMatrix = Matrix()
    private val targetInverse = Matrix()
    private val projectionMatrix = Matrix()
    private val sourceLocation = IntArray(2)
    private val primaryLocation = IntArray(2)
    @JvmField var originX: Float = 0f
    @JvmField var originY: Float = 0f
    @JvmField var unit: Float = 0f
    @JvmField var canonicalMinimum: Float = 0f
    @JvmField var frameMinimum: Float = 0f
    @JvmField var collapse: Float = 0f
    @JvmField var endX: Float = 0f
    @JvmField var endY: Float = 0f
    @JvmField var placement: BatteryNumberPlacement? = null
    @JvmField var contentBattery: Int = 0
    @JvmField var valid: Boolean = false

    fun prepare(owner: IconSession, originX: Float, originY: Float, minimum: Float): Boolean {
        return prepare(
            owner, originX, originY, minimum,
            owner.batteryShapeGeometry, owner.batteryTransferProjection
        )
    }

    fun prepareObstacle(source: IconSession?, target: IconSession, targetCanvasShift: Float): Boolean {
        this.valid = false
        this.bounds.setEmpty()
        this.pointBounds.setEmpty()
        if (source == null) {
            return false
        }
        val primary = source.module.preferredAttachedControlCenterHost()
        val primaryRow = if (primary == null)
            null
        else
            primary.controlCenterRow.currentSnapshot()
        val reusePrimary = SessionPaintPolicy.reusesPrimaryRowSnapshot(
            source.from,
            source.module.stackedOn() && primary != null && primary != source && primary.from == 6 && primary.battery.isAttachedToWindow()
                    && primaryRow != null && primaryRow.valid(),
            source.battery.isAttachedToWindow()
        )
        val rowHost = if (reusePrimary) primary else source
        val row: ControlCenterRowSnapshot = (if (reusePrimary)
            primaryRow
        else
            source.controlCenterRow.currentSnapshot())!!
        val rowActive = reusePrimary || SessionPaintPolicy.usesSolvedControlCenterRow(
            source.from, source.module.stackedOn(), row.valid()
        )
        var translation = 0.0f
        if (reusePrimary) {
            source.battery.getLocationOnScreen(this.sourceLocation)
            primary!!.battery.getLocationOnScreen(this.primaryLocation)
            translation = SessionPaintPolicy.rowTransitionTranslation(
                (this.primaryLocation[0] - this.sourceLocation[0]).toFloat(),
                source.module.rowAlignHostShift(), source.stackedAlpha
            )
        }
        if (rowActive && rowHost!!.controlCenterRow.usesMeasuredGeometry()) {
            translation += rowHost.battery.getWidth() - row.rowRight()
        }
        translation = SessionPaintPolicy.rowTranslationWithDrift(
            translation,
            rowHost!!.driftX, BurnInDrift.Companion.fade(source.progress)
        )
        val minimum = min(
            source.dp(
                SessionPaintPolicy.controlCenterLayoutSize(
                    IconSessionPainter.Companion.appearanceSettings(source).size(), true
                )
            ), source.battery.getHeight()
        ).toFloat()
        val stockWidth = SessionPaintPolicy.controlCenterBaseWidth(
            source.stockWidth.toFloat(), minimum, true, source.progress
        )
        val originX: Float = SessionPaintGeometry.Companion.batteryShapeOriginX(
            source, minimum, stockWidth, row, rowActive, translation
        )
        val originY: Float = SessionPaintGeometry.Companion.batteryShapeOriginY(source, minimum)
        if (!prepare(
                source, originX, originY, minimum,
                this.obstacleGeometry, this.obstacleProjection
            )
        ) {
            return false
        }
        // The hook's host-5 translation is outside View's global matrix.
        this.sourceMatrix.reset()
        source.battery.transformMatrixToGlobal(this.sourceMatrix)
        this.sourceMatrix.preTranslate(
            if (source.from == 5)
                source.renderer.canvasRowShift()
            else
                0.0f, 0.0f
        )
        this.targetMatrix.reset()
        target.battery.transformMatrixToGlobal(this.targetMatrix)
        this.targetMatrix.preTranslate(targetCanvasShift, 0.0f)
        if (!this.targetMatrix.invert(this.targetInverse)) {
            this.valid = false
            return false
        }
        this.projectionMatrix.setConcat(this.targetInverse, this.sourceMatrix)
        this.projectionMatrix.mapRect(this.bounds)
        this.projectionMatrix.mapRect(this.pointBounds)
        this.projectionMatrix.mapRect(this.solidBounds)
        if (this.opening.valid) {
            this.projectionMatrix.mapPoints(this.opening.points)
            this.projectionMatrix.mapPoints(this.opening.gapAnchor)
            this.opening.strokeWidth = this.projectionMatrix.mapRadius(this.opening.strokeWidth)
        }
        this.valid = finite(this.bounds) && finite(this.pointBounds)
        return this.valid
    }

    private fun prepare(
        owner: IconSession, originX: Float, originY: Float, minimum: Float,
        shape: BatteryShapeGeometry, projection: BatteryTransferProjection
    ): Boolean {
        this.valid = false
        this.bounds.setEmpty()
        this.pointBounds.setEmpty()
        this.opening.valid = false
        if (minimum <= 0.0f) {
            return false
        }
        val ringHost = owner.module.transferHost(0)
        val ringGeometry =
            owner.module.transferRingGeometry(owner.module.state)
        if (!projection.project(
                ringHost,
                ringGeometry,
                owner, owner.renderer.canvasRowShift()
            )
        ) {
            return false
        }
        val canonicalMinimum = BatteryShapeLayout.minimumPx(
            owner.dp(IconSessionPainter.Companion.appearanceSettings(owner).batterySize()).toFloat()
        )
        if (canonicalMinimum <= 0.0f) {
            return false
        }
        val frameMinimum = BatteryShapeLayout.frameMinimumPx(
            owner.dp(IconSessionPainter.Companion.appearanceSettings(owner).batterySize()).toFloat()
        )
        val unit = canonicalMinimum / 120.0f
        val rtl = (owner.battery.getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL)
        val placement =
            owner.rowNumberPlacement(
                IconSessionPainter.Companion.appearanceSettings(owner)
                    .batteryNumberPlacement()
            )
        val contentBattery = owner.rowBatteryForContent(
            owner.module.state.battery
        )
        val fullWidth = BatteryShapeLayout.widthUnits(placement)
        val fullHeight = (fullWidth
                * BatteryTransferConfig.SVG_HEIGHT
                / BatteryTransferConfig.SVG_WIDTH)
        var fullCentreX =
            BatteryShapeLayout.centreXUnits(placement)
        fullCentreX -= (BatteryShapeLayout.controlCenterBatteryLeftShiftPx(
            owner.battery.getResources()
                .getDisplayMetrics().density
        )
                / unit)
        fullCentreX -= (BatteryShapeLayout.nonOutsideBatteryLeftShiftPx(
            placement,
            owner.battery.getResources()
                .getDisplayMetrics().density
        )
                / unit)
        if (placement == BatteryNumberPlacement.OUTSIDE) {
            fullCentreX += owner.batteryShapeRenderer
                .outsideNumberAdaptiveBatteryShiftUnits(
                    owner.painter,
                    contentBattery
                )
            fullCentreX -= (BatteryShapeLayout.outsideGroupLeftShiftPx(
                owner.battery.getResources()
                    .getDisplayMetrics().density
            )
                    / unit)
            fullCentreX -= (BatteryShapeLayout.outsideNumberRightShiftPx(
                owner.battery.getResources()
                    .getDisplayMetrics().density
            )
                    / unit)
        }
        val transferState =
            owner.module.batteryTransferState()
        val collapse = transferState.batteryCollapse(
            owner.module.control,
            owner.module.morphValue()
        )
        val shapeOriginX = BatteryShapeLayout.canonicalOriginX(
            originX,
            minimum,
            frameMinimum,
            rtl
        )
        val shapeOriginY = BatteryShapeLayout.canonicalOriginY(
            originY,
            minimum,
            frameMinimum
        )
        fullCentreX = BatteryShapeLayout.clampBatteryCentreXToRightEdge(
            fullCentreX,
            shapeOriginX,
            0.0f,
            fullWidth,
            unit,
            (owner.battery.getWidth() - owner.dp(1)).toFloat()
        )
        val startX = BatteryTransferPolicy.batteryAnchorX(
            projection.startLocalX,
            projection.endLocalX
        )
        val startY = BatteryTransferPolicy.batteryAnchorY(
            projection.startLocalY,
            projection.endLocalY
        )
        val endX = ((startX - shapeOriginX)
                / unit)
        val endY = ((startY - shapeOriginY)
                / unit)
        val pointDiameter = BatteryShapeLayout.transferPointDiameterUnits(
            ringGeometry!!.strokeWidth,
            min(
                projection.ringUnitX,
                projection.ringUnitY
            ), unit
        )
        shape.update(
            collapse,
            fullCentreX,
            BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
            fullWidth,
            fullHeight,
            endX,
            endY,
            pointDiameter,
            pointDiameter
        )

        this.originX = shapeOriginX
        this.originY = shapeOriginY
        this.unit = unit
        this.canonicalMinimum = canonicalMinimum
        this.frameMinimum = frameMinimum
        this.collapse = collapse
        this.endX = endX
        this.endY = endY
        this.placement = placement
        this.contentBattery = contentBattery
        val fullLeft = shapeOriginX + (fullCentreX - fullWidth / 2.0f) * unit
        val fullTop = (shapeOriginY
                + (BatteryTransferConfig.FULL_CENTRE_Y_UNITS - fullHeight / 2.0f) * unit)
        this.solidBounds.set(
            fullLeft, fullTop, fullLeft + fullWidth * unit,
            fullTop + fullHeight * unit
        )
        this.bounds.set(
            shapeOriginX + (shape.centreX - shape.width / 2.0f) * unit,
            shapeOriginY + (shape.centreY - shape.height / 2.0f) * unit,
            shapeOriginX + (shape.centreX + shape.width / 2.0f) * unit,
            shapeOriginY + (shape.centreY + shape.height / 2.0f) * unit
        )
        val pointRadius = pointDiameter * unit / 2.0f
        this.pointBounds.set(
            startX - pointRadius, startY - pointRadius,
            startX + pointRadius, startY + pointRadius
        )
        if (transferState.openingShape()) {
            this.opening.update(
                ringGeometry, projection.ringLocalX, projection.ringLocalY,
                projection.ringUnitX, projection.ringUnitY,
                fullLeft, fullTop, fullWidth * unit, fullHeight * unit,
                owner.module.morphValue()
            )
            if (this.opening.valid) {
                this.solidBounds.set(
                    fullLeft, fullTop, fullLeft + fullWidth * unit,
                    fullTop + fullHeight * unit
                )
                this.bounds.set(
                    this.opening.left, this.opening.top,
                    this.opening.right, this.opening.bottom
                )
                // The interior and terminal appear only after the outline has
                // reached this exact full battery frame.
                shape.update(
                    0.0f, fullCentreX, BatteryTransferConfig.FULL_CENTRE_Y_UNITS,
                    fullWidth, fullHeight, endX, endY, pointDiameter, pointDiameter
                )
            }
        }
        this.valid = true
        return true
    }

    companion object {
        private fun finite(value: RectF): Boolean {
            return !value.isEmpty() && java.lang.Float.isFinite(value.left) && java.lang.Float.isFinite(value.top)
                    && java.lang.Float.isFinite(value.right) && java.lang.Float.isFinite(value.bottom)
        }
    }
}
