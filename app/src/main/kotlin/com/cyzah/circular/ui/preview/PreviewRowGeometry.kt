package com.cyzah.circular.ui.preview

import com.cyzah.circular.hook.runtime.ControlCenterRowComponent
import com.cyzah.circular.hook.runtime.ControlCenterRowInput
import com.cyzah.circular.hook.runtime.ControlCenterRowLayout
import com.cyzah.circular.hook.runtime.ControlCenterRowMargins
import com.cyzah.circular.hook.runtime.ControlCenterRowReflow
import com.cyzah.circular.hook.runtime.ControlCenterRowSnapshot
import kotlin.math.max
import kotlin.math.min

/** Physical control-center row geometry, centred before preview magnification.  */
class PreviewRowGeometry {
    private val input = ControlCenterRowInput()
    private val layout = ControlCenterRowLayout()
    private val target = ControlCenterRowSnapshot()
    private val reflow = ControlCenterRowReflow()
    private var stageWidth = 0f

    fun update(
        batteryWidth: Float, wifiWidth: Float, cellularWidth: Float,
        batteryVisible: Boolean, wifiVisible: Boolean, cellularVisible: Boolean,
        density: Float
    ): Boolean {
        val valid = update(
            batteryWidth, wifiWidth, cellularWidth,
            batteryVisible, wifiVisible, cellularVisible, density, 0L, false
        )
        // The static API retains its compact centred layout.
        this.stageWidth = targetRowWidth()
        return valid
    }

    fun update(
        batteryWidth: Float, wifiWidth: Float, cellularWidth: Float,
        batteryVisible: Boolean, wifiVisible: Boolean, cellularVisible: Boolean,
        density: Float, now: Long, animate: Boolean
    ): Boolean {
        this.input.reset()
        this.input.set(
            ControlCenterRowComponent.BATTERY,
            batteryVisible, batteryWidth, 0.0f, 0.0f, 0.0f, 0.0f
        )
        this.input.set(
            ControlCenterRowComponent.WIFI,
            wifiVisible, wifiWidth, 0.0f, 0.0f, 0.0f, 0.0f
        )
        this.input.set(
            ControlCenterRowComponent.CELLULAR,
            cellularVisible, cellularWidth, 0.0f, 0.0f, 0.0f, 0.0f
        )
        ControlCenterRowMargins.apply(this.input, density)
        val requiredWidth: Float = ControlCenterRowLayout.Companion.requiredWidth(this.input)
        // Reserve the connected scene's extent even when Wi-Fi is absent.
        this.input.setVisible(ControlCenterRowComponent.WIFI, positive(wifiWidth))
        this.stageWidth = max(
            requiredWidth,
            ControlCenterRowLayout.Companion.requiredWidth(this.input)
        )
        this.input.setVisible(ControlCenterRowComponent.WIFI, wifiVisible)
        // Solve the complete physical row before applying any viewport limit.
        this.input.setBounds(requiredWidth, requiredWidth)
        if (!this.layout.solve(this.input)) {
            this.target.clear()
            this.reflow.clear()
            this.stageWidth = 0.0f
            return false
        }
        this.target.copyFrom(this.layout, 0L)
        this.reflow.update(this.target, now, animate)
        this.reflow.sample(now)
        return valid()
    }

    fun active(): Boolean {
        return this.reflow.active()
    }

    fun targetRowWidth(): Float {
        return this.target.rowWidth()
    }

    fun stageRowWidth(): Float {
        return this.stageWidth
    }

    fun valid(): Boolean {
        return this.reflow.snapshot().valid()
    }

    fun visible(component: ControlCenterRowComponent): Boolean {
        return valid() && this.reflow.snapshot().visible(component)
    }

    fun left(component: ControlCenterRowComponent): Float {
        return if (visible(component))
            this.reflow.snapshot().left(component) + stageRowWidth() / 2.0f
        else
            0.0f
    }

    fun right(component: ControlCenterRowComponent): Float {
        return if (visible(component))
            this.reflow.snapshot().right(component) + stageRowWidth() / 2.0f
        else
            0.0f
    }

    fun centre(component: ControlCenterRowComponent): Float {
        return (left(component) + right(component)) / 2.0f
    }

    fun width(component: ControlCenterRowComponent): Float {
        return right(component) - left(component)
    }

    fun rowWidth(): Float {
        return this.reflow.snapshot().rowWidth()
    }

    companion object {
        @JvmStatic fun fitScale(
            availableWidth: Float, availableHeight: Float,
            preferredScale: Float, contentWidth: Float, contentHeight: Float
        ): Float {
            if (!positive(availableWidth) || !positive(availableHeight) || !positive(preferredScale) || !positive(
                    contentWidth
                ) || !positive(contentHeight)
            ) {
                return 0.0f
            }
            return min(
                preferredScale, min(
                    availableWidth / contentWidth, availableHeight / contentHeight
                )
            )
        }

        private fun positive(value: Float): Boolean {
            return java.lang.Float.isFinite(value) && value > 0.0f
        }
    }
}
