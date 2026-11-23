package com.cyzah.circular.hook.runtime

import android.view.View
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.BatteryShapeLayout
import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Owns captured endpoints and frozen content for one session's row transition.  */
class IconSessionRowTransition(private val owner: IconSession) {
    private val expandedControlCenterRow = ControlCenterRowSnapshot()
    private val collapsedControlCenterRow = ControlCenterRowSnapshot()
    private val collapsedControlCenterRowLocal = ControlCenterRowSnapshot()
    private val rowTransitionSample = ControlCenterRowSnapshot()
    private val frozenRowState = IconState()
    private val collapsedRowVisible = BooleanArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val collapsedRowLeft = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val collapsedRowRight = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val collapsedRowWidth = FloatArray(ControlCenterRowInput.Companion.COMPONENT_COUNT)
    private val collapsedRowOrigin = IntArray(2)
    private var capturingCollapsedRow = false
    private var collapsedRowOriginX = Int.MIN_VALUE
    private var rowContentFrozen = false
    private var rowTransitionExpanding = false
    private var rowTransitionStartExpansion = 0f
    private var rowTransitionLastExpansion = 0f
    private var frozenRowBattery = -1
    private var frozenRowPlacement: BatteryNumberPlacement? = null
    private var frozenFirstRowLabel: String? = null
    private var frozenSecondRowLabel: String? = null
    private var frozenRowDual = false
    private var frozenRowWifiVisible = false
    private var frozenRowCellularVisible = false

    fun beginCollapsedRowCapture(): Boolean {
        if (owner.from != 0 || this.capturingCollapsedRow
            || owner.controlCenterRow.transitioning()
            || owner.module.state.battery < 0 || !owner.module.stackedOn() || owner.module.mode != 2 || !BatteryTransferPolicy.closedSettled(
                owner.module.control,
                0.0f,
                owner.module.morphValue()
            )
        ) {
            return false
        }
        Arrays.fill(this.collapsedRowVisible, false)
        Arrays.fill(this.collapsedRowLeft, 0.0f)
        Arrays.fill(this.collapsedRowRight, 0.0f)
        Arrays.fill(this.collapsedRowWidth, 0.0f)
        this.capturingCollapsedRow = true
        return true
    }

    fun captureWifiRowTarget(centerX: Float, width: Float) {
        if (!this.capturingCollapsedRow || !java.lang.Float.isFinite(centerX) || !java.lang.Float.isFinite(width) || width <= 0.0f) {
            return
        }
        val component = ControlCenterRowComponent.WIFI.ordinal
        this.collapsedRowVisible[component] = true
        this.collapsedRowLeft[component] = centerX - (width / 2.0f)
        this.collapsedRowRight[component] = centerX + (width / 2.0f)
        this.collapsedRowWidth[component] = width
    }

    fun captureCellularRowTarget(left: Float, right: Float) {
        if (!this.capturingCollapsedRow || !java.lang.Float.isFinite(left) || !java.lang.Float.isFinite(right) || right <= left) {
            return
        }
        val component = ControlCenterRowComponent.CELLULAR.ordinal
        if (!this.collapsedRowVisible[component]) {
            this.collapsedRowVisible[component] = true
            this.collapsedRowLeft[component] = left
            this.collapsedRowRight[component] = right
        } else {
            this.collapsedRowLeft[component] = min(
                this.collapsedRowLeft[component],
                left
            )
            this.collapsedRowRight[component] = max(
                this.collapsedRowRight[component],
                right
            )
        }
        this.collapsedRowWidth[component] =
            (this.collapsedRowRight[component]
                    - this.collapsedRowLeft[component])
    }

    fun finishCollapsedRowCapture(
        batteryOriginX: Float,
        minimum: Float
    ) {
        if (!this.capturingCollapsedRow) {
            return
        }
        this.capturingCollapsedRow = false
        val placement =
            owner.module.sessionSettings()
                .batteryNumberPlacement()
        val configuredMinimum =
            owner.dp(
                owner.module.sessionSettings()
                    .batterySize()
            ).toFloat()
        val canonicalMinimum =
            BatteryShapeLayout.minimumPx(configuredMinimum)
        val frameMinimum =
            BatteryShapeLayout.frameMinimumPx(configuredMinimum)
        val density = owner.battery.getResources()
            .getDisplayMetrics().density
        var batteryLeft = BatteryShapeLayout.fullBatteryLeftPx(
            batteryOriginX,
            minimum,
            frameMinimum,
            canonicalMinimum,
            placement,
            density
        )
        val digits: String? = IconPainter.Companion.NUMBERS[min(100, owner.module.state.battery)]
        val outsideWidth = if (placement == BatteryNumberPlacement.OUTSIDE)
            owner.batteryShapeRenderer.measureOutsideNumberSvg(
                owner.painter,
                digits
            )
        else
            0.0f
        if (placement == BatteryNumberPlacement.OUTSIDE) {
            batteryLeft += (owner.batteryShapeRenderer
                .outsideNumberAdaptiveBatteryShiftUnits(
                    owner.painter,
                    owner.module.state
                )
                    * canonicalMinimum
                    / 120.0f)
        }
        if (owner.battery.getLayoutDirection()
            == View.LAYOUT_DIRECTION_RTL
        ) {
            batteryLeft -= minimum - frameMinimum
        }
        val batteryWidth =
            ControlCenterRowMeasurementPolicy.batteryWidth(
                configuredMinimum,
                placement,
                outsideWidth
            )
        val batteryComponent =
            ControlCenterRowComponent.BATTERY.ordinal
        this.collapsedRowVisible[batteryComponent] = true
        this.collapsedRowLeft[batteryComponent] = batteryLeft
        this.collapsedRowRight[batteryComponent] =
            batteryLeft + batteryWidth
        this.collapsedRowWidth[batteryComponent] = batteryWidth
        owner.battery.getLocationOnScreen(this.collapsedRowOrigin)
        val generation: Long
        try {
            generation = controlCenterRowTargetGeneration()
        } catch (error: Exception) {
            this.collapsedControlCenterRow.clear()
            this.collapsedRowOriginX = Int.MIN_VALUE
            return
        }
        if (this.collapsedControlCenterRow.copyFromRects(
                this.collapsedRowVisible,
                this.collapsedRowLeft,
                this.collapsedRowRight,
                this.collapsedRowWidth,
                generation
            )
        ) {
            this.collapsedRowOriginX = this.collapsedRowOrigin[0]
        } else {
            this.collapsedRowOriginX = Int.MIN_VALUE
        }
    }

    fun synchronizeControlCenterRowTransition() {
        if (owner.from != 6) {
            return
        }
        if (!controlCenterRowTransitionReady()) {
            abortControlCenterRowTransition()
            return
        }
        val expansion = Motion.ease(Motion.clamp(owner.module.control))
        val morph = owner.module.morphValue()
        val openSettled =
            Motion.clamp(owner.module.control) >= 0.999f
                    && morph >= BatteryTransferConfig.OPEN_MORPH_MIN
        val active = owner.controlCenterRow.transitioning()
        if (active && (!owner.module.transferHostAttached(0)
                    || !ControlCenterRowMeasurer.Companion.nativeVisibilityMatches(
                owner,
                this.frozenRowWifiVisible,
                this.frozenRowCellularVisible
            ))
        ) {
            abortControlCenterRowTransition()
            return
        }
        val moved = abs(
            expansion - this.rowTransitionLastExpansion
        ) > 0.0001f
        val movingExpanded =
            expansion > this.rowTransitionLastExpansion
        if (active && moved
            && movingExpanded != this.rowTransitionExpanding
        ) {
            if (!beginControlCenterRowTransition(
                    movingExpanded,
                    expansion
                )
            ) {
                return
            }
        }
        if (openSettled) {
            if (owner.controlCenterRow.transitioning()) {
                if (!this.rowTransitionExpanding
                    && !beginControlCenterRowTransition(
                        true,
                        expansion
                    )
                ) {
                    return
                }
                owner.controlCenterRow.finishTransition()
            }
            owner.controlCenterRow.restoreMeasuredLayout()
            if (owner.controlCenterRow.stableSnapshot().valid()) {
                this.expandedControlCenterRow.copyFrom(
                    owner.controlCenterRow.stableSnapshot()
                )
            }
            clearFrozenRowContent()
            this.rowTransitionLastExpansion = expansion
            return
        }
        if (!owner.controlCenterRow.transitioning()) {
            if (expansion <= 0.0001f || !this.expandedControlCenterRow.valid() || !this.collapsedControlCenterRow.valid() || !prepareCollapsedControlCenterRow()) {
                return
            }
            val startsExpanded =
                SessionPaintPolicy.beginsRowTransitionExpanded(
                    this.rowTransitionLastExpansion,
                    expansion
                )
            if (!owner.controlCenterRow.beginTransition(
                    if (startsExpanded)
                        this.expandedControlCenterRow
                    else
                        this.collapsedControlCenterRowLocal,
                    if (startsExpanded)
                        this.expandedControlCenterRow.generation()
                    else
                        this.collapsedControlCenterRowLocal
                            .generation()
                )
            ) {
                abortControlCenterRowTransition()
                return
            }
            this.rowTransitionExpanding = startsExpanded
            this.rowTransitionStartExpansion =
                if (startsExpanded) 0.0f else 1.0f
            this.rowTransitionLastExpansion = expansion
            if (!freezeRowContent()) {
                abortControlCenterRowTransition()
                return
            }
        }
        val progress = SessionPaintPolicy.localRowProgress(
            this.rowTransitionStartExpansion,
            expansion,
            this.rowTransitionExpanding
        )
        if (!owner.controlCenterRow.sampleTransition(
                progress,
                this.rowTransitionSample
            )
        ) {
            abortControlCenterRowTransition()
            return
        }
        this.rowTransitionLastExpansion = expansion
        val finished = progress >= 0.999f
                && (if (this.rowTransitionExpanding)
            openSettled
        else
            BatteryTransferPolicy.closedSettled(
                owner.module.control,
                0.0f,
                morph
            )
                    && owner.module.panelGestureReleased())
        if (finished) {
            owner.controlCenterRow.finishTransition()
            if (this.rowTransitionExpanding
                && owner.controlCenterRow.stableSnapshot().valid()
            ) {
                this.expandedControlCenterRow.copyFrom(
                    owner.controlCenterRow.stableSnapshot()
                )
            }
            clearFrozenRowContent()
        }
    }

    fun rowBatteryForContent(liveBattery: Int): Int {
        return SessionPaintPolicy.transitionBattery(
            liveBattery,
            this.frozenRowBattery,
            rowContentFrozen()
        )
    }

    fun rowNumberPlacement(
        livePlacement: BatteryNumberPlacement?
    ): BatteryNumberPlacement? {
        return if (rowContentFrozen()
            && this.frozenRowPlacement != null
        )
            this.frozenRowPlacement
        else
            livePlacement
    }

    fun rowDualSim(liveDual: Boolean): Boolean {
        return if (rowContentFrozen())
            this.frozenRowDual
        else
            liveDual
    }

    fun rowWifiVisible(liveVisible: Boolean): Boolean {
        return if (rowContentFrozen())
            this.frozenRowWifiVisible
        else
            liveVisible
    }

    fun rowIconState(liveState: IconState): IconState {
        return if (rowContentFrozen())
            this.frozenRowState
        else
            liveState
    }

    fun rowStackedLabel(
        state: IconState,
        first: Boolean
    ): String? {
        val frozenActive = rowContentFrozen()
        val live = if (frozenActive)
            null
        else
            if (state.dualSim())
                owner.module.stackedLabel(state, first)
            else
                if (first) owner.module.stackedLabel(state) else null
        return SessionPaintPolicy.transitionText(
            live,
            if (first)
                this.frozenFirstRowLabel
            else
                this.frozenSecondRowLabel,
            frozenActive
        )
    }

    private fun controlCenterRowTransitionReady(): Boolean {
        return !owner.module.keepMergedShade && owner.module.stackedOn()
                && owner.module.mode == 2 && owner.module.state.battery >= 0 && owner.module.controlCenterSettings()
            .compatible()
                && !owner.ambient && owner.module.transferScreenInteractive()
                && !owner.module.transferKeyguardVisible() && !owner.module.transferPrivacyBlocked() && (owner.controlCenterRow.transitioning()
                || owner.module.state.readyForMerge()
                && owner.module.state.stackedReady())
    }

    private fun prepareCollapsedControlCenterRow(): Boolean {
        if (this.collapsedRowOriginX == Int.MIN_VALUE || !owner.battery.isAttachedToWindow() || !owner.module.transferHostAttached(
                0
            )
        ) {
            return false
        }
        val generation: Long
        try {
            generation = controlCenterRowTargetGeneration()
        } catch (error: Exception) {
            return false
        }
        if (!SessionPaintPolicy.canUseCollapsedRowTarget(
                owner.module.transferHostAttached(0),
                SessionPaintPolicy.transitionTargetCurrent(
                    this.collapsedControlCenterRow.valid(),
                    this.collapsedControlCenterRow.generation(),
                    generation
                )
            )
        ) {
            return false
        }
        owner.battery.getLocationOnScreen(owner.transitionRowOrigin)
        return this.collapsedControlCenterRowLocal
            .copyTranslatedFrom(
                this.collapsedControlCenterRow,
                (this.collapsedRowOriginX
                        - owner.transitionRowOrigin[0]).toFloat()
            )
    }

    private fun beginControlCenterRowTransition(
        expanding: Boolean,
        expansion: Float
    ): Boolean {
        if (!expanding && !prepareCollapsedControlCenterRow()) {
            abortControlCenterRowTransition()
            return false
        }
        val target = if (expanding)
            this.expandedControlCenterRow
        else
            this.collapsedControlCenterRowLocal
        if (!target.valid()
            || !owner.controlCenterRow.beginTransition(
                target,
                target.generation()
            )
        ) {
            abortControlCenterRowTransition()
            return false
        }
        this.rowTransitionExpanding = expanding
        this.rowTransitionStartExpansion = expansion
        return true
    }

    private fun freezeRowContent(): Boolean {
        val state = owner.module.state
        val wifi: ControlCenterRowMeasurementPolicy.NativeVisibility? =
            ControlCenterRowMeasurer.Companion.nativeVisibility(
                owner, ModuleMain.Companion.WIFI
            )
        val cellular: ControlCenterRowMeasurementPolicy.NativeVisibility? =
            ControlCenterRowMeasurer.Companion.nativeVisibility(
                owner, ModuleMain.Companion.MOBILE
            )
        if (wifi == ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE
            || cellular == ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE
        ) {
            return false
        }
        this.rowContentFrozen = true
        this.frozenRowState.copyFrom(state)
        this.frozenRowBattery = state.battery
        this.frozenRowPlacement =
            owner.module.controlCenterSettings()
                .batteryNumberPlacement()
        this.frozenRowDual = state.dualSim()
        this.frozenRowWifiVisible = (wifi
                == ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE)
        this.frozenRowCellularVisible = (cellular
                == ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE)
        this.frozenFirstRowLabel = if (this.frozenRowDual)
            owner.module.stackedLabel(state, true)
        else
            owner.module.stackedLabel(state)
        this.frozenSecondRowLabel = if (this.frozenRowDual)
            owner.module.stackedLabel(state, false)
        else
            null
        return true
    }

    private fun rowContentFrozen(): Boolean {
        return this.rowContentFrozen
                && owner.controlCenterRow.transitioning()
    }

    private fun clearFrozenRowContent() {
        this.rowContentFrozen = false
        this.frozenRowBattery = -1
        this.frozenRowPlacement = null
        this.frozenRowDual = false
        this.frozenRowWifiVisible = false
        this.frozenRowCellularVisible = false
        this.frozenFirstRowLabel = null
        this.frozenSecondRowLabel = null
        this.frozenRowState.battery = -1
    }

    private fun abortControlCenterRowTransition() {
        owner.controlCenterRow.invalidate()
        this.expandedControlCenterRow.clear()
        this.collapsedControlCenterRowLocal.clear()
        clearFrozenRowContent()
    }

    @Throws(Exception::class)
    fun controlCenterRowGeneration(): Long {
        return controlCenterRowGeneration(true)
    }

    @Throws(Exception::class)
    private fun controlCenterRowTargetGeneration(): Long {
        return controlCenterRowGeneration(false)
    }

    @Throws(Exception::class)
    private fun controlCenterRowGeneration(
        hostSpecific: Boolean
    ): Long {
        val settings =
            owner.module.controlCenterSettings()
        val state = owner.module.state
        val resources = owner.battery.getResources()
        var hash = if (hostSpecific) 17L else 23L
        if (hostSpecific) {
            hash = rowHash(hash, owner.from.toLong())
            hash = rowHash(hash, owner.battery.getWidth().toLong())
            hash = rowHash(hash, owner.battery.getHeight().toLong())
            hash = rowHash(hash, owner.stockWidth.toLong())
            hash = rowHash(hash, owner.parentWidthLimit.toLong())
            hash = rowHash(hash, owner.parentHeightLimit.toLong())
        }
        hash = rowHash(
            hash, java.lang.Float.floatToIntBits(
                resources.getDisplayMetrics().density
            ).toLong()
        )
        hash = rowHash(hash, settings.dualStyle().ordinal.toLong())
        hash = rowHash(
            hash,
            settings.batteryNumberPlacement().ordinal.toLong()
        )
        hash = rowHash(hash, settings.size().toLong())
        hash = rowHash(hash, settings.batterySize().toLong())
        hash = rowHash(hash, settings.weight().toLong())
        hash = rowHash(hash, settings.wifiWeight().toLong())
        hash = rowHash(hash, settings.technologyWeight().toLong())
        hash = rowHash(hash, settings.wifiScale().toLong())
        hash = rowHash(hash, settings.technologyScale().toLong())
        hash = rowHash(hash, settings.percentScale().toLong())
        hash = rowHash(hash, settings.percentWeight().toLong())
        if (!hostSpecific) {
            val statusSettings =
                owner.module.sessionSettings()
            hash = rowHash(
                hash,
                statusSettings.dualStyle().ordinal.toLong()
            )
            hash = rowHash(
                hash, statusSettings
                    .batteryNumberPlacement().ordinal.toLong()
            )
            hash = rowHash(hash, statusSettings.size().toLong())
            hash = rowHash(hash, statusSettings.batterySize().toLong())
            hash = rowHash(hash, statusSettings.weight().toLong())
            hash = rowHash(hash, statusSettings.wifiWeight().toLong())
            hash = rowHash(
                hash,
                statusSettings.technologyWeight().toLong()
            )
            hash = rowHash(hash, statusSettings.wifiScale().toLong())
            hash = rowHash(
                hash,
                statusSettings.technologyScale().toLong()
            )
            hash = rowHash(hash, statusSettings.percentScale().toLong())
            hash = rowHash(hash, statusSettings.percentWeight().toLong())
        }
        hash = rowHash(hash, state.battery.toLong())
        hash = rowHash(hash, state.wifiLevel.toLong())
        hash = rowHash(hash, state.mobileLevel.toLong())
        hash = rowHash(hash, state.rawMobileLevel.toLong())
        hash = rowHash(hash, state.secondaryLevel.toLong())
        hash = rowHash(hash, state.secondaryRawLevel.toLong())
        hash = rowHash(hash, state.subId.toLong())
        hash = rowHash(hash, state.secondarySubId.toLong())
        hash = rowHash(hash, state.mobileSlot.toLong())
        hash = rowHash(hash, state.secondarySlot.toLong())
        hash = rowHash(hash, (if (state.simPresent) 1 else 0).toLong())
        hash = rowHash(
            hash,
            (if (state.secondarySimPresent) 1 else 0).toLong()
        )
        hash = rowHash(hash, (if (state.mobileKnown) 1 else 0).toLong())
        hash = rowHash(hash, (if (state.secondaryKnown) 1 else 0).toLong())
        hash = rowHash(hash, (if (state.inService) 1 else 0).toLong())
        hash = rowHash(
            hash,
            (if (state.secondaryInService) 1 else 0).toLong()
        )
        hash = rowHash(hash, (if (state.showWifi()) 1 else 0).toLong())
        hash = rowHash(hash, stringHash(state.mobileTechnology))
        if (state.dualSim()) {
            hash = rowHash(
                hash,
                stringHash(owner.module.stackedLabel(state, true))
            )
            hash = rowHash(
                hash,
                stringHash(owner.module.stackedLabel(state, false))
            )
        } else {
            hash = rowHash(
                hash,
                stringHash(owner.module.stackedLabel(state))
            )
        }
        var localPaintHash = 0L
        if (hostSpecific) {
            val typeface =
                owner.painter.networkPaint.getTypeface()
            localPaintHash = if (typeface == null)
                0L
            else
                System.identityHashCode(typeface).toLong()
            localPaintHash = rowHash(
                localPaintHash,
                java.lang.Float.floatToIntBits(
                    owner.painter.networkPaint.getTextSize()
                ).toLong()
            )
        }
        hash = rowHash(
            hash,
            SessionPaintPolicy.targetGenerationLocalPaintHash(
                hostSpecific,
                localPaintHash
            )
        )
        hash = rowHash(
            hash, java.lang.Float.floatToIntBits(
                resources.getConfiguration().fontScale
            ).toLong()
        )
        hash = rowHash(
            hash, if (resources.getConfiguration()
                    .getLocales().isEmpty()
            )
                0L
            else
                resources.getConfiguration()
                    .getLocales().get(0).hashCode().toLong()
        )
        if (hostSpecific) {
            for (index in 0..<owner.icons.getChildCount()) {
                val child = owner.icons.getChildAt(index)
                val name = child.javaClass.getName()
                if (ModuleMain.Companion.WIFI == name
                    || ModuleMain.Companion.MOBILE == name
                ) {
                    hash = rowHash(hash, name.hashCode().toLong())
                    hash = rowHash(
                        hash,
                        if (owner.wanted(child)) 1L else 0L
                    )
                }
            }
        } else {
            hash = rowHash(
                hash,
                if (state.stackedReady()) 1L else 0L
            )
        }
        return hash
    }

    companion object {
        private fun rowHash(hash: Long, value: Long): Long {
            return (hash * 31L) + value
        }

        private fun stringHash(value: String?): Long {
            return if (value == null) 0L else value.hashCode().toLong()
        }
    }
}
