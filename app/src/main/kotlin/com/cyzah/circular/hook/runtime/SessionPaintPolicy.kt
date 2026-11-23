package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.animation.Motion
import com.cyzah.circular.core.graphics.geometry.IconGeometry
import com.cyzah.circular.domain.settings.DualStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object SessionPaintPolicy {
    @JvmStatic fun defersClosedControlCenterCopy(
        controlCenterHost: Boolean,
        statusHostAvailable: Boolean, control: Float, shade: Float, morph: Float
    ): Boolean {
        return controlCenterHost && statusHostAvailable
                && BatteryTransferPolicy.closedSettled(control, 0.0f, morph)
    }

    @JvmStatic fun usesMergedControlCenterEndpoint(
        controlCenterHost: Boolean,
        statusHostAvailable: Boolean, control: Float, morph: Float
    ): Boolean {
        return controlCenterHost && !statusHostAvailable && BatteryTransferPolicy.closedSettled(control, 0.0f, morph)
    }

    @JvmStatic fun notificationCopyActive(from: Int, previous: Boolean, control: Float, shade: Float): Boolean {
        return from == 5 && control <= BatteryTransferConfig.HANDOFF_MAX && (previous || shade > BatteryTransferConfig.HANDOFF_MAX)
    }

    @JvmStatic fun drawsMergedNotificationCopy(notificationCopy: Boolean, control: Float, morph: Float): Boolean {
        return notificationCopy && BatteryTransferPolicy.closedSettled(control, 0.0f, morph)
    }

    @JvmStatic fun usesLocalNotificationCopy(
        notificationCopy: Boolean,
        rowTransition: Boolean, transferActive: Boolean
    ): Boolean {
        return notificationCopy && !rowTransition && !transferActive
    }

    @JvmStatic fun ringHostContentMask(ringOnly: Boolean): Int {
        return if (ringOnly) (BatteryTransferPolicy.MASK_HIDE_BATTERY_RING
                or BatteryTransferPolicy.MASK_HIDE_CENTRE
                or BatteryTransferPolicy.MASK_HIDE_WIFI
                or BatteryTransferPolicy.MASK_HIDE_TECHNOLOGY
                or BatteryTransferPolicy.MASK_HIDE_SIGNALS) else 0
    }

    @JvmStatic fun solvedRowWifiVisible(
        liveVisible: Boolean, solvedRowActive: Boolean, solvedWifiVisible: Boolean
    ): Boolean {
        return liveVisible && (!solvedRowActive || solvedWifiVisible)
    }

    private const val TECHNOLOGY_HIDE_AT_CONTROL = 0.25f
    @JvmStatic fun usesSolvedControlCenterRow(
        from: Int,
        enabled: Boolean,
        snapshotValid: Boolean
    ): Boolean {
        return from == 6 && enabled && snapshotValid
    }

    @JvmStatic fun reusesPrimaryRowSnapshot(
        from: Int,
        primaryValid: Boolean,
        targetAvailable: Boolean
    ): Boolean {
        return from == 5 && ControlCenterRowHostPolicy.host5CanReuse(
            primaryValid,
            targetAvailable
        )
    }

    @JvmStatic fun primaryRowTranslation(
        hostOriginDelta: Float,
        appliedTranslation: Float
    ): Float {
        return ControlCenterRowHostPolicy.translateHorizontal(
            0.0f,
            appliedTranslation,
            hostOriginDelta
        )
    }

    @JvmStatic fun targetGenerationLocalPaintHash(
        hostSpecific: Boolean,
        localPaintHash: Long
    ): Long {
        return if (hostSpecific) localPaintHash else 0L
    }

    @JvmStatic fun rowTransitionTranslation(
        liveHostOriginDelta: Float,
        appliedCanvasShift: Float,
        stackedAlpha: Float
    ): Float {
        return (liveHostOriginDelta
                - (appliedCanvasShift * Motion.clamp(stackedAlpha)))
    }

    @JvmStatic fun rowTranslationWithDrift(
        translation: Float,
        driftX: Float,
        fade: Float
    ): Float {
        return translation + (driftX * Motion.clamp(fade))
    }

    @JvmStatic fun canUseCollapsedRowTarget(
        hostZeroAttached: Boolean,
        targetCurrent: Boolean
    ): Boolean {
        return hostZeroAttached && targetCurrent
    }

    @JvmStatic fun localRowProgress(
        startExpansion: Float,
        currentExpansion: Float,
        expanding: Boolean
    ): Float {
        val start = Motion.clamp(startExpansion)
        val current = Motion.clamp(currentExpansion)
        if (expanding) {
            val remaining = 1.0f - start
            return if (remaining <= 0.0001f)
                1.0f
            else
                Motion.clamp(
                    (current - start) / remaining
                )
        }
        return if (start <= 0.0001f)
            1.0f
        else
            Motion.clamp((start - current) / start)
    }

    @JvmStatic fun beginsRowTransitionExpanded(
        previousExpansion: Float,
        currentExpansion: Float
    ): Boolean {
        return (Motion.clamp(currentExpansion)
                > Motion.clamp(previousExpansion) + 0.0001f)
    }

    @JvmStatic fun transitionTargetCurrent(
        targetValid: Boolean,
        targetGeneration: Long,
        currentGeneration: Long
    ): Boolean {
        return targetValid
                && targetGeneration == currentGeneration
    }

    @JvmStatic fun rowTransitionVisibilityCurrent(
        frozenWifi: Boolean,
        liveWifi: Boolean,
        frozenCellular: Boolean,
        liveCellular: Boolean
    ): Boolean {
        return frozenWifi == liveWifi
                && frozenCellular == liveCellular
    }

    @JvmStatic fun controlCenterRowFallbackReason(
        host: Int,
        stackedEnabled: Boolean,
        primaryAvailable: Boolean,
        targetAvailable: Boolean,
        snapshotValid: Boolean
    ): String {
        if (!stackedEnabled) {
            return "stacked-disabled"
        }
        if (host == 5 && !primaryAvailable) {
            return "primary-unavailable"
        }
        if (host == 5 && !targetAvailable) {
            return "target-unavailable"
        }
        return if (snapshotValid) "none" else "layout-invalid"
    }

    @JvmStatic fun logsControlCenterRowDiagnostic(
        host: Int,
        reusesPrimaryRow: Boolean
    ): Boolean {
        return host == 6
                || host == 5 && !reusesPrimaryRow
    }

    @JvmStatic fun controlCenterRowDiagnosticSignature(
        host: Int,
        generation: Long,
        style: DualStyle?,
        batteryVisible: Boolean,
        wifiVisible: Boolean,
        cellularVisible: Boolean,
        fallbackReason: String?
    ): Long {
        var hash = host.toLong()
        hash = (hash * 31L) + generation
        hash = ((hash * 31L)
                + (if (style == null) -1 else style.ordinal))
        hash = (hash * 31L) + (if (batteryVisible) 1 else 0)
        hash = (hash * 31L) + (if (wifiVisible) 1 else 0)
        hash = (hash * 31L) + (if (cellularVisible) 1 else 0)
        return ((hash * 31L)
                + (if (fallbackReason == null)
            0
        else
            fallbackReason.hashCode()))
    }

    @JvmStatic fun controlCenterRowDiagnostic(
        host: Int,
        style: DualStyle?,
        row: ControlCenterRowSnapshot?,
        fallbackReason: String?
    ): String {
        val output = StringBuilder(256)
        output.append("controlCenterRow host=").append(host)
            .append(" style=").append(style)
        if (row == null || !row.valid()) {
            return output.append(" generation=0 batteryRect=invalid")
                .append(" wifiRect=invalid cellularRect=invalid")
                .append(" batteryWifiGap=nan wifiCellularGap=nan")
                .append(" outerLeft=nan outerRight=nan")
                .append(" rowWidth=0 batteryRightAnchor=0")
                .append(" fallbackReason=").append(fallbackReason)
                .toString()
        }
        output.append(" generation=").append(row.generation())
        appendRect(
            output, "batteryRect", row,
            ControlCenterRowComponent.BATTERY
        )
        appendRect(
            output, "wifiRect", row,
            ControlCenterRowComponent.WIFI
        )
        appendRect(
            output, "cellularRect", row,
            ControlCenterRowComponent.CELLULAR
        )
        output.append(" batteryWifiGap=").append(
            formatGap(
                row,
                ControlCenterRowComponent.BATTERY,
                ControlCenterRowComponent.WIFI
            )
        )
        output.append(" wifiCellularGap=").append(
            formatGap(
                row,
                ControlCenterRowComponent.WIFI,
                ControlCenterRowComponent.CELLULAR
            )
        )
        val outerLeft = outerLeft(row)
        val outerRight = if (row.visible(ControlCenterRowComponent.BATTERY))
            row.rowRight() - row.right(
                ControlCenterRowComponent.BATTERY
            )
        else Float.NaN
        output.append(" outerLeft=").append(format(outerLeft))
        output.append(" outerRight=").append(format(outerRight))
        output.append(" rowWidth=").append(format(row.rowWidth()))
        output.append(" batteryRightAnchor=").append(
            if (row.visible(ControlCenterRowComponent.BATTERY))
                format(
                    row.right(
                        ControlCenterRowComponent.BATTERY
                    )
                )
            else
                "nan"
        )
        output.append(" fallbackReason=").append(fallbackReason)
        return output.toString()
    }

    private fun appendRect(
        output: StringBuilder,
        name: String?,
        row: ControlCenterRowSnapshot,
        component: ControlCenterRowComponent
    ) {
        output.append(' ').append(name).append('=')
            .append(
                if (row.visible(component))
                    (format(row.left(component))
                            + ".." + format(row.right(component))
                            + ",width=" + format(row.width(component)))
                else
                    "hidden"
            )
    }

    private fun formatGap(
        row: ControlCenterRowSnapshot,
        right: ControlCenterRowComponent,
        left: ControlCenterRowComponent
    ): String {
        if (!row.visible(right) || !row.visible(left)) {
            return "nan"
        }
        return format(row.left(right) - row.right(left))
    }

    private fun outerLeft(row: ControlCenterRowSnapshot): Float {
        val components = arrayOf<ControlCenterRowComponent>(
            ControlCenterRowComponent.CELLULAR,
            ControlCenterRowComponent.WIFI,
            ControlCenterRowComponent.BATTERY
        )
        for (component in components) {
            if (row.visible(component)) {
                return row.left(component) - row.rowLeft()
            }
        }
        return Float.NaN
    }

    private fun format(value: Float): String {
        return if (java.lang.Float.isFinite(value)) String.format(Locale.US, "%.3f", value) else
            "nan"
    }

    @JvmStatic fun transitionBattery(
        liveBattery: Int,
        frozenBattery: Int,
        active: Boolean
    ): Int {
        return if (active && frozenBattery >= 0)
            frozenBattery
        else
            liveBattery
    }

    @JvmStatic fun transitionText(
        liveText: String?,
        frozenText: String?,
        active: Boolean
    ): String? {
        return if (active && frozenText != null)
            frozenText
        else
            liveText
    }

    @JvmStatic fun solvedBatteryOriginX(
        solvedLeft: Float,
        solvedRight: Float,
        batteryLeftAtZero: Float
    ): Float {
        if (!java.lang.Float.isFinite(solvedLeft) || !java.lang.Float.isFinite(solvedRight) || !java.lang.Float.isFinite(batteryLeftAtZero) || solvedRight < solvedLeft) {
            return Float.NaN
        }
        return solvedLeft - batteryLeftAtZero
    }

    @JvmStatic fun defersControlCenterRow(from: Int, control: Float): Boolean {
        return (from == 0 || from == 1) && control > 0.25f
    }

    @JvmStatic fun defersBatteryHostDuringTransfer(
        from: Int,
        control: Float,
        batteryShapeActive: Boolean,
        ringTransferOwner: Boolean
    ): Boolean {
        return !ringTransferOwner
                && (defersControlCenterRow(from, control)
                || ((from == 0 || from == 1) && batteryShapeActive))
    }

    @JvmStatic @JvmOverloads
    fun usesSimpleIcon(
        ambient: Boolean,
        merge: Boolean,
        detailedTransferOwner: Boolean = false
    ): Boolean {
        return ambient || (!merge && !detailedTransferOwner)
    }

    @JvmStatic fun drawsStacked(
        active: Boolean,
        morph: Float,
        enabled: Boolean,
        covers: Boolean
    ): Boolean {
        return (active || morph > 0.002f) && enabled && covers
    }

    @JvmStatic fun drawsNativeBattery(
        batteryReplacementActive: Boolean,
        nativeAlpha: Float
    ): Boolean {
        return !batteryReplacementActive && drawsAlpha(nativeAlpha)
    }

    @JvmStatic fun stableRowAlignmentShift(
        rowAlignmentShift: Float,
        charging: Boolean
    ): Float {
        return if (charging) 0.0f else rowAlignmentShift
    }

    @JvmStatic fun mergedControlCenterWidth(
        baseWidth: Int,
        networkWidth: Int,
        progress: Float
    ): Int {
        val safeBase = max(0, baseWidth)
        val safeNetwork = max(0, networkWidth)
        return Motion.width(
            safeBase,
            safeBase + safeNetwork,
            progress
        )
    }

    @JvmStatic fun controlCenterHostBaseWidth(
        closedBaseWidth: Int,
        componentBaseWidth: Int,
        progress: Float
    ): Int {
        return Motion.width(
            max(0, closedBaseWidth),
            max(0, componentBaseWidth),
            progress
        )
    }

    @JvmStatic @JvmOverloads
    fun hidesNativeSignalState(
        from: Int,
        merge: Boolean,
        stackedHold: Boolean,
        stackedEnabled: Boolean,
        ambient: Boolean,
        transitionHold: Boolean = false
    ): Boolean {
        return !ambient
                && (transitionHold
                || merge
                || stackedHold
                || (stackedEnabled && (from == 5 || from == 6)))
    }

    @JvmStatic fun hidesNativeSignalState(
        from: Int,
        merge: Boolean,
        stackedHold: Boolean,
        stackedEnabled: Boolean,
        ambient: Boolean,
        transitionHold: Boolean,
        rendererAvailable: Boolean
    ): Boolean {
        return rendererAvailable && hidesNativeSignalState(
            from, merge, stackedHold,
            stackedEnabled, ambient, transitionHold
        )
    }

    @JvmStatic fun shouldHoldNativeIcons(
        expansion: Float,
        stackedEnabled: Boolean,
        dualRowMode: Boolean
    ): Boolean {
        return stackedEnabled
                && dualRowMode
                && Motion.clamp(expansion) > 0.02f
    }

    @JvmStatic fun forceHidesNativeIcons(holdActive: Boolean): Boolean {
        return holdActive
    }

    @JvmStatic fun suppressesNativeRowDraw(
        moduleActive: Boolean,
        ambient: Boolean
    ): Boolean {
        return moduleActive && !ambient
    }

    @JvmStatic fun resetsNativeRowOwnership(
        stackedEnabled: Boolean,
        keepMergedShade: Boolean
    ): Boolean {
        return !stackedEnabled || keepMergedShade
    }

    @JvmStatic fun batteryHostPaintAvailable(
        from: Int,
        visibleAvailable: Boolean,
        attachedAvailable: Boolean
    ): Boolean {
        return if (from == 5 || from == 6)
            attachedAvailable
        else
            visibleAvailable
    }

    @JvmStatic fun preferredControlCenterHost(
        host6Available: Boolean,
        host5Available: Boolean
    ): Int {
        if (host6Available) {
            return 6
        }
        return if (host5Available) 5 else -1
    }

    @JvmStatic fun preferredBatteryPaintHost(
        host6Visible: Boolean,
        host5Visible: Boolean,
        host6Attached: Boolean,
        host5Attached: Boolean
    ): Int {
        val visible = preferredControlCenterHost(host6Visible, host5Visible)
        return if (visible != -1)
            visible
        else
            preferredControlCenterHost(host6Attached, host5Attached)
    }

    @JvmStatic fun ownsControlCenterWifi(from: Int, preferredHost: Int): Boolean {
        return (from != 5 && from != 6) || from == preferredHost
    }

    @JvmStatic fun ownsControlCenterWifi(from: Int, preferredHost: Int, sharedGeometry: Boolean): Boolean {
        return sharedGeometry || ownsControlCenterWifi(from, preferredHost)
    }

    @JvmStatic fun suppressesNativeBattery(
        holdActive: Boolean,
        paintHostAvailable: Boolean
    ): Boolean {
        return holdActive && paintHostAvailable
    }

    @JvmStatic fun hidesTopRingContentDuringTransfer(
        style: DualStyle?,
        transferActive: Boolean,
        swapContents: Boolean
    ): Boolean {
        return transferActive
                && swapContents
                && (style == DualStyle.LAYERED
                || style == DualStyle.GROUPED)
    }

    @JvmStatic fun drawsSwappedTopWifi(
        ringTransfer: Boolean,
        ringHandoffHold: Boolean
    ): Boolean {
        return !ringTransfer && !ringHandoffHold
    }

    @JvmStatic fun usesBaseWifiForSwappedFlat(
        style: DualStyle?,
        swapContents: Boolean
    ): Boolean {
        return swapContents
                && (style == DualStyle.LAYERED
                || style == DualStyle.GROUPED)
    }

    @JvmStatic fun usesControlCenterWifiSlot(
        controlCenterHost: Boolean,
        useBaseWifi: Boolean
    ): Boolean {
        return controlCenterHost || !useBaseWifi
    }

    @JvmStatic fun usesSwappedStatusBarWifiTarget(
        style: DualStyle?,
        swapContents: Boolean
    ): Boolean {
        return swapContents
                && (style == DualStyle.LAYERED
                || style == DualStyle.GROUPED)
    }

    @JvmStatic fun stackedContentPaintMask(useBaseWifi: Boolean): Int {
        return (IconPainter.Companion.MASK_HIDE_SIGNALS
                or (if (useBaseWifi)
            IconPainter.Companion.MASK_HIDE_TECHNOLOGY
        else
            IconPainter.Companion.MASK_HIDE_CENTRE))
    }

    @JvmStatic fun topContentTransferActive(
        batteryShapeVisible: Boolean,
        ringTransfer: Boolean,
        handoffHold: Boolean,
        expansion: Float
    ): Boolean {
        return ringTransfer
                || handoffHold
                || batteryShapeVisible
                && Motion.clamp(expansion) < 0.999f
    }

    @JvmStatic fun usesControlCenterTransferProjection(from: Int, keepMergedShade: Boolean): Boolean {
        // A retained merged icon owns its local frame, independent of host 0.
        return !keepMergedShade && (from == 5 || from == 6)
    }

    @JvmStatic fun transferTargetOrigin(
        projectedOrigin: Float,
        fallbackOrigin: Float,
        projectedAvailable: Boolean
    ): Float {
        return if (projectedAvailable && java.lang.Float.isFinite(projectedOrigin))
            projectedOrigin
        else
            fallbackOrigin
    }

    @JvmStatic fun clampIconCentreY(
        centreY: Float,
        halfHeight: Float,
        clipTop: Float,
        clipBottom: Float
    ): Float {
        if (!java.lang.Float.isFinite(centreY) || !java.lang.Float.isFinite(halfHeight) || !java.lang.Float.isFinite(clipTop) || !java.lang.Float.isFinite(
                clipBottom
            ) || clipBottom < clipTop
        ) {
            return centreY
        }
        val safeHalf = max(0.0f, halfHeight)
        val minimum = clipTop + safeHalf
        val maximum = clipBottom - safeHalf
        if (maximum < minimum) {
            return (clipTop + clipBottom) / 2.0f
        }
        return max(minimum, min(maximum, centreY))
    }

    @JvmStatic fun fitsStackedLabels(controlCenterHost: Boolean): Boolean {
        return !controlCenterHost
    }

    @JvmStatic fun keepsStaticSignalEndpoint(
        controlCenterHost: Boolean,
        stackedDrawing: Boolean
    ): Boolean {
        return controlCenterHost && stackedDrawing
    }

    @JvmStatic fun showsStackedTechnology(
        controlCenterHost: Boolean,
        morph: Float
    ): Boolean {
        return true
    }

    @JvmStatic fun centreTechnologyAlpha(
        morph: Float,
        capsuleWifi: Boolean,
        wifiVisible: Boolean
    ): Float {
        val value = Motion.clamp(morph)
        if (capsuleWifi) {
            return value
        }
        if (!wifiVisible) {
            return 1.0f
        }
        val remaining = Motion.clamp(
            (value - TECHNOLOGY_HIDE_AT_CONTROL)
                    / (1.0f - TECHNOLOGY_HIDE_AT_CONTROL)
        )
        return remaining * remaining * (3.0f - (2.0f * remaining))
    }

    @JvmStatic fun stackedTechnologyAlpha(
        morph: Float,
        centreRow: Boolean,
        capsuleWifi: Boolean,
        wifiVisible: Boolean
    ): Float {
        if (wifiVisible && !capsuleWifi) {
            return centreTechnologyAlpha(
                morph,
                false,
                true
            )
        }
        if (centreRow) {
            return if (capsuleWifi)
                Motion.clamp(morph)
            else
                1.0f
        }
        return Motion.clamp(morph)
    }

    @JvmStatic fun expandedTrinityCenterX(
        solvedRowActive: Boolean,
        solvedBatteryOriginX: Float,
        minimum: Float,
        fallbackCenterX: Float
    ): Float {
        return if (solvedRowActive
            && java.lang.Float.isFinite(solvedBatteryOriginX)
            && java.lang.Float.isFinite(minimum)
            && minimum > 0.0f
        )
            solvedBatteryOriginX + (minimum / 2.0f)
        else
            fallbackCenterX
    }

    @JvmStatic fun stableControlCenterBaseWidth(
        measuredStockWidth: Float,
        baseWidth: Float
    ): Float {
        val safeBase = if (java.lang.Float.isFinite(baseWidth)) max(0.0f, baseWidth) else
            0.0f
        return if (java.lang.Float.isFinite(measuredStockWidth)) min(max(0.0f, measuredStockWidth), safeBase) else
            safeBase
    }

    @JvmStatic fun controlCenterBaseWidth(
        measuredStockWidth: Float,
        baseWidth: Float,
        replacementActive: Boolean,
        expandedProgress: Float
    ): Float {
        val safeStock = if (java.lang.Float.isFinite(measuredStockWidth)) max(0.0f, measuredStockWidth) else max(
            0.0f,
            if (java.lang.Float.isFinite(baseWidth)) baseWidth else 0.0f
        )
        if (!replacementActive) {
            return safeStock
        }
        return Motion.mix(
            safeStock,
            stableControlCenterBaseWidth(safeStock, baseWidth),
            expandedProgress
        )
    }

    @JvmStatic fun controlCenterLayoutSize(
        styleSize: Int,
        controlCenterHost: Boolean
    ): Int {
        return max(1, styleSize)
    }

    @JvmStatic fun prefersModuleWifi(
        batteryReplacementActive: Boolean,
        wifiVisible: Boolean
    ): Boolean {
        return batteryReplacementActive && wifiVisible
    }

    @JvmStatic fun moduleWifiPaintMask(mask: Int, moduleWifiOwner: Boolean): Int {
        return if (moduleWifiOwner)
            mask or BatteryTransferPolicy.MASK_HIDE_WIFI
        else
            mask
    }

    @JvmStatic fun drawsNativeWifi(
        moduleWifiOwner: Boolean,
        nativeAlpha: Float
    ): Boolean {
        return !moduleWifiOwner && drawsAlpha(nativeAlpha)
    }

    @JvmStatic fun endpointUnit(
        configuredSizePx: Float,
        availableHeightPx: Float
    ): Float {
        if (!java.lang.Float.isFinite(configuredSizePx)
            || !java.lang.Float.isFinite(availableHeightPx)
        ) {
            return 0.0f
        }
        val safeSize = max(0.0f, configuredSizePx)
        val safeHeight = max(0.0f, availableHeightPx)
        return min(safeSize, safeHeight) / 120.0f
    }

    @JvmStatic fun controlCenterWifiCanvasScale(
        startScale: Float,
        expandedProgress: Float
    ): Float {
        if (!java.lang.Float.isFinite(startScale)) {
            return 0.0f
        }
        return Motion.mix(
            max(0.0f, startScale),
            ControlCenterAppearance.WIFI_FINAL_CANVAS_SCALE,
            expandedProgress
        )
    }

    @JvmStatic fun controlCenterWifiStartCanvasScale(
        style: DualStyle?,
        statusBarUnit: Float,
        statusBarScale: Float,
        capsuleScale: Float
    ): Float {
        val unit = if (java.lang.Float.isFinite(statusBarUnit)) max(0.0f, statusBarUnit) else
            0.0f
        val configuredScale = if (style == DualStyle.CAPSULE)
            capsuleScale
        else
            statusBarScale
        if (!java.lang.Float.isFinite(configuredScale)
            || configuredScale <= 0.0f
        ) {
            return 0.0f
        }
        return unit * configuredScale
    }

    @JvmStatic fun controlCenterWifiSourceX(
        style: DualStyle?,
        regularX: Float,
        capsuleX: Float
    ): Float {
        if (style == DualStyle.CAPSULE && java.lang.Float.isFinite(capsuleX)) {
            return capsuleX
        }
        return if (java.lang.Float.isFinite(regularX)) regularX else capsuleX
    }

    /**
     * Legacy fallback shift for an unsolved row. The solved control-center row
     * uses its component rectangles and must return zero here.
     */
    @JvmStatic fun capsuleSignalRowShiftX(
        style: DualStyle?,
        controlCenterHost: Boolean,
        dualSignals: Boolean,
        rtl: Boolean,
        spacingPx: Float,
        morph: Float
    ): Float {
        if (style != DualStyle.CAPSULE || !controlCenterHost || !dualSignals || rtl
            || !java.lang.Float.isFinite(spacingPx) || spacingPx <= 0.0f
        ) {
            return 0.0f
        }
        return spacingPx * Motion.ease(Motion.clamp(morph))
    }

    @JvmStatic fun controlCenterSignalTargetX(
        row: Int,
        slot: Int,
        motion: Float
    ): Float {
        if (row <= 0) {
            return IconGeometry.innerDotX(
                min(2, max(0, slot)),
                motion
            )
        }
        return IconGeometry.outerDotX(
            max(0, min(3, slot))
        )
    }

    @JvmStatic fun controlCenterSignalTargetX(
        row: Int,
        slot: Int,
        motion: Float,
        weightScale: Float
    ): Float {
        if (row <= 0) {
            return IconGeometry.innerDotX(
                min(2, max(0, slot)),
                motion,
                weightScale
            )
        }
        return IconGeometry.outerDotX(
            max(0, min(3, slot)),
            weightScale
        )
    }

    @JvmStatic fun controlCenterSignalTargetX(
        style: DualStyle?,
        styleTargetX: Float,
        row: Int,
        slot: Int,
        motion: Float,
        weightScale: Float
    ): Float {
        if ((style == DualStyle.CAPSULE || style == DualStyle.DUO || style == DualStyle.GROUPED)
            && java.lang.Float.isFinite(styleTargetX)
        ) {
            return styleTargetX
        }
        return controlCenterSignalTargetX(row, slot, motion, weightScale)
    }

    @JvmStatic fun drawsAlpha(alpha: Float): Boolean {
        return java.lang.Float.isFinite(alpha) && alpha > 0.0f
    }

    @JvmStatic fun needsAlphaLayer(alpha: Float): Boolean {
        return drawsAlpha(alpha) && alpha < 0.999f
    }

    @JvmStatic fun controlCenterReplacementActive(
        controlCenterHost: Boolean,
        panelEngaged: Boolean,
        replacementEnabled: Boolean,
        hostAvailable: Boolean
    ): Boolean {
        return controlCenterHost
                && panelEngaged
                && replacementEnabled
                && hostAvailable
    }

    @JvmStatic fun isPreferredBatteryOwner(
        replacementActive: Boolean,
        preferredHost: Boolean,
        hostAvailable: Boolean,
        ringHandoffHold: Boolean
    ): Boolean {
        return replacementActive
                && preferredHost
                && hostAvailable
                && !ringHandoffHold
    }

    @JvmStatic fun redrawsCapsuleContent(
        style: DualStyle?,
        ringTransfer: Boolean
    ): Boolean {
        return ringTransfer && style == DualStyle.CAPSULE
    }

    @JvmStatic fun capsuleContentRedrawMask(): Int {
        return (7
                or BatteryTransferPolicy.MASK_HIDE_BATTERY
                or BatteryTransferPolicy.MASK_HIDE_SIGNALS)
    }

    @JvmStatic fun capsuleRingPaintMask(
        mask: Int,
        style: DualStyle?,
        ringTransfer: Boolean
    ): Int {
        return if (ringTransfer && style == DualStyle.CAPSULE)
            mask or BatteryTransferPolicy.MASK_HIDE_SIGNALS
        else
            mask
    }

    @JvmStatic fun capsuleOverlaySignalAlpha(
        style: DualStyle?,
        ringTransfer: Boolean
    ): Float {
        return 1.0f
    }

    @JvmStatic fun keepsCapsuleRingSignalOwner(
        style: DualStyle?,
        ringTransfer: Boolean,
        coversSignals: Boolean
    ): Boolean {
        return ringTransfer
                && style == DualStyle.CAPSULE && coversSignals
    }

    @JvmStatic fun keepsCapsuleControlCenterSignalOwner(
        style: DualStyle?,
        controlCenterHost: Boolean,
        ringTransfer: Boolean,
        controlCenterActive: Boolean,
        coversSignals: Boolean,
        stackedEnabled: Boolean
    ): Boolean {
        return stackedEnabled
                && style == DualStyle.CAPSULE && coversSignals
                && (ringTransfer
                || (controlCenterHost && controlCenterActive))
    }
}
