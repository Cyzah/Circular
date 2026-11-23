package com.cyzah.circular.hook.runtime

import android.view.View
import com.cyzah.circular.core.graphics.animation.BatteryTransferPolicy
import com.cyzah.circular.core.graphics.geometry.CompactGeometry
import kotlin.math.min

/** Reserves the same attached row that the painter projects into each host.  */
object ControlCenterRowReservation {
    @JvmStatic fun measuredWidth(session: IconSession): Int {
        session.neighbourBoundaryActive = false
        if (!session.module.stackedOn() || (session.from != 5 && session.from != 6)) {
            return 0
        }
        if (session.from == 6) {
            return session.controlCenterRow.measuredHostWidthPx()
        }
        invalidateExpandedBoundaryIfGeometryChanged(session)
        val status = session.module.transferHost(0)
        val statusAvailable = status != null && !status.closed && status.battery.isAttachedToWindow()
                && status.battery.getWidth() > 0
        val closedWidth = closedHostWidth(session)
        if (session.notificationCopy()) {
            // The notification copy stays in its native row with a complete
            // local icon; a projected narrower CC handoff slot would clip it.
            return closedWidth
        }
        session.battery.getLocationOnScreen(session.transitionRowOrigin)
        if (statusAvailable) {
            status.battery.getLocationOnScreen(status.transitionRowOrigin)
            // The closed endpoint belongs to host 0, even if the panel host
            // detached or discarded its expanded snapshot during the last frame.
            if (BatteryTransferPolicy.closedSettled(
                    session.module.control,
                    0.0f, session.module.morphValue()
                )
            ) {
                return ControlCenterRowHostPolicy.projectedClosedWidth(
                    closedWidth,
                    status.transitionRowOrigin[0].toFloat(), status.battery.getWidth().toFloat(),
                    session.transitionRowOrigin[0].toFloat(), session.battery.getWidth().toFloat()
                )
            }
        }
        val primary = session.module.preferredAttachedControlCenterHost()
        val available =
            primary != null && primary != session && primary.from == 6 && primary.battery.isAttachedToWindow()
                    && primary.controlCenterRow.currentSnapshot().valid()
        if (!SessionPaintPolicy.reusesPrimaryRowSnapshot(
                session.from, available, session.battery.isAttachedToWindow()
            )
        ) {
            return 0
        }
        val width = primary!!.controlCenterRow.measuredHostWidthPx()
        primary.battery.getLocationOnScreen(primary.transitionRowOrigin)
        if (!statusAvailable) {
            return ControlCenterRowHostPolicy.projectedHostWidth(
                width, primary.transitionRowOrigin[0].toFloat(), primary.battery.getWidth().toFloat(),
                session.transitionRowOrigin[0].toFloat(), session.battery.getWidth().toFloat()
            )
        }
        val progress = session.module.morphValue()
        if ((!BatteryTransferPolicy.openSettled(session.module.control, progress)
                    || session.expandedNeighbourCapture.closing())
            && java.lang.Float.isFinite(session.expandedNeighbourBoundary)
            && session.battery.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL && width > 0 && session.battery.getWidth() > 0
        ) {
            val hostRight = (session.transitionRowOrigin[0] + session.battery.getWidth()).toFloat()
            val sourceBoundary = (primary.transitionRowOrigin[0]
                    + primary.battery.getWidth() - width).toFloat()
            val closedBoundary = (status.transitionRowOrigin[0]
                    + status.battery.getWidth() - closedWidth).toFloat()
            val expandedBoundary = session.expandedNeighbourBoundary
            if (expandedBoundary <= closedBoundary) {
                session.neighbourBoundaryActive = true
                return NativeNeighbourMotion.reservedWidth(
                    hostRight, expandedBoundary,
                    sourceBoundary, closedBoundary,
                    NativeNeighbourMotion.remaining(
                        progress,
                        session.module.controlCenterSettings().batteryNumberPlacement()
                    )
                )
            }
        }
        // Until an expanded frame commits, retain the original projection.
        return ControlCenterRowHostPolicy.projectedTransitionWidth(
            width, closedWidth,
            primary.transitionRowOrigin[0].toFloat(), primary.battery.getWidth().toFloat(),
            status.transitionRowOrigin[0].toFloat(), status.battery.getWidth().toFloat(),
            session.transitionRowOrigin[0].toFloat(), session.battery.getWidth().toFloat(),
            progress,
            session.module.controlCenterSettings().batteryNumberPlacement()
        )
    }

    private fun closedHostWidth(session: IconSession): Int {
        val settings = session.module.sessionSettings()
        val status = session.module.transferHost(0)
        val ruler = if (status == null) session else status
        val size = ruler.dp(settings.size())
        val unit = if (ruler.stockHeight > 0) min(size, ruler.stockHeight) else size
        val requested = if (settings.capsuleDual())
            Math.round(unit * session.module.capsuleReserveUnits() / 120.0f)
        else
            size
        return if (ruler.hasBatteryWidthMeasureSpec)
            resolveWidth(ruler, requested, ruler.batteryWidthMeasureSpec)
        else
            requested
    }

    @JvmStatic fun readyAfterAlignment(session: IconSession): Boolean {
        captureExpandedBoundary(session)
        if (session.from != 5) return true
        if (session.from != 5 || !session.draw || !session.merge || session.ambient
            || !session.battery.isAttachedToWindow() || !session.hasBatteryWidthMeasureSpec
        ) {
            session.widthPreDrawGuard.reset()
            session.nativeRowMotion.discard()
            return true
        }
        val closedEndpoint = BatteryTransferPolicy.closedSettled(
            session.module.control,
            0.0f, session.module.morphValue()
        )
        val primary = session.module.preferredAttachedControlCenterHost()
        var anchored =
            primary != null && primary.from == 6 && !closedEndpoint && java.lang.Float.isFinite(session.expandedRowBoundary)
                    && java.lang.Float.isFinite(session.expandedNeighbourBoundary)
                    && session.battery.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL && (!BatteryTransferPolicy.openSettled(
                session.module.control,
                session.module.morphValue()
            )
                    || session.expandedNeighbourCapture.closing())
        if (anchored) {
            session.nativeRowMotion.prepare(
                session, primary, session.expandedRowBoundary,
                primary!!.controlCenterRow.measuredHostWidthPx()
            )
        } else {
            session.nativeRowMotion.reset()
        }
        var required = measuredWidth(session)
        if (anchored && !session.neighbourBoundaryActive) {
            // Left-moving endpoint layouts retain their original native route.
            session.nativeRowMotion.reset()
            required = measuredWidth(session)
            anchored = false
        }
        val resolved = resolveWidth(session, required, session.batteryWidthMeasureSpec)
        if (anchored && session.neighbourBoundaryActive && resolved > 0) {
            session.battery.getLocationOnScreen(session.transitionRowOrigin)
            val desiredBoundary = (session.transitionRowOrigin[0] + session.battery.getWidth() - resolved).toFloat()
            session.nativeRowMotion.settleIcons(session, desiredBoundary)
        }
        session.nativeRowMotion.alignIcons(
            session, primary, session.module.transferHost(0),
            session.module.morphValue()
        )
        if (!ControlCenterRowHostPolicy.needsWidthBeforeDraw(
                session.battery.getWidth(), resolved, closedEndpoint, session.neighbourBoundaryActive
            )
        ) {
            session.widthPreDrawGuard.reset()
            session.widthPreDrawFallbackLogged = false
            return true
        }
        // Bridge the current frame with a reversible native-icon translation.
        // Layout catches up without cancelling the whole window's draw pass.
        if (!session.battery.isLayoutRequested()) session.requestLayout()
        session.battery.invalidate()
        return true
    }

    private fun captureExpandedBoundary(session: IconSession) {
        // Native SystemUI fades host 5 to alpha zero when fully expanded.
        // Its committed layout remains the starting position for closing.
        if (session.from != 5 || !session.module.stackedOn()) {
            return
        }
        val ready = session.battery.isAttachedToWindow() && session.hasBatteryWidthMeasureSpec
                && session.battery.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL && !session.host.isLayoutRequested() && !session.battery.isLayoutRequested()
        if (!session.expandedNeighbourCapture.sample(
                session.module.control,
                session.module.morphValue(), ready
            )
        ) {
            return
        }
        val resolved = resolveWidth(session, measuredWidth(session), session.batteryWidthMeasureSpec)
        if (resolved <= 0 || session.battery.getWidth() < resolved) {
            return
        }
        // A wider committed slot is legitimate: record the neighbour's actual
        // position instead of waiting forever for an unnecessary shrink.
        session.battery.getLocationOnScreen(session.transitionRowOrigin)
        if (!java.lang.Float.isFinite(session.expandedNeighbourBoundary)) {
            session.module.log(
                4, ModuleMain.Companion.TAG, ("neighbourAnchorCaptured control="
                        + session.module.control + " morph=" + session.module.morphValue()
                        + " screenLeft=" + session.transitionRowOrigin[0])
            )
        }
        session.expandedNeighbourBoundary = session.nativeRowMotion.rawTargetBatteryLeft(session)
        val primary = session.module.preferredAttachedControlCenterHost()
        session.expandedRowBoundary = if (primary != null && primary.from == 6)
            session.nativeRowMotion.rawSourceBoundary(primary, primary.controlCenterRow.measuredHostWidthPx())
        else Float.NaN
        session.expandedNeighbourSettings = session.module.controlCenterSettings()
        session.expandedNeighbourRtl = session.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        session.expandedNeighbourConfiguration = session.battery.getResources()
            .getConfiguration().hashCode()
        session.expandedNeighbourRootWidth = session.battery.getRootView().getWidth()
        session.expandedNeighbourRootHeight = session.battery.getRootView().getHeight()
    }

    @JvmStatic fun resolveWidth(session: IconSession, requested: Int, measureSpec: Int): Int {
        if (requested <= 0) {
            return 0
        }
        val resolved = View.resolveSize(requested, measureSpec)
        return if (View.MeasureSpec.getMode(measureSpec) == View.MeasureSpec.EXACTLY)
            CompactGeometry.width(requested, resolved, session.parentWidthLimit)
        else
            resolved
    }

    private fun invalidateExpandedBoundaryIfGeometryChanged(session: IconSession) {
        if (!java.lang.Float.isFinite(session.expandedNeighbourBoundary)) {
            return
        }
        if (session.expandedNeighbourSettings != session.module.controlCenterSettings() || session.expandedNeighbourRtl != (session.battery.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) || session.expandedNeighbourConfiguration != session.battery.getResources()
                .getConfiguration().hashCode() || session.expandedNeighbourRootWidth != session.battery.getRootView()
                .getWidth() || session.expandedNeighbourRootHeight != session.battery.getRootView().getHeight()
        ) {
            session.expandedNeighbourBoundary = Float.NaN
            session.expandedRowBoundary = Float.NaN
            session.nativeRowMotion.discard()
            session.expandedNeighbourSettings = null
        }
    }
}
