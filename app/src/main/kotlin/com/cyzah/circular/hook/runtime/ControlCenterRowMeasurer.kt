package com.cyzah.circular.hook.runtime

import android.graphics.Typeface
import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.StackedSignals
import com.cyzah.circular.core.graphics.animation.BatteryTransferConfig
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import kotlin.math.max
import kotlin.math.min

/**
 * Measures the three control-center top-level components into reusable input.
 */
class ControlCenterRowMeasurer {
    private val digitCache = ControlCenterTextMeasurementCache()
    private val firstLabelCache = ControlCenterTextMeasurementCache()
    private val secondLabelCache = ControlCenterTextMeasurementCache()

    fun measure(
        owner: IconSession,
        output: ControlCenterRowInput,
        rowRight: Float,
        availableWidth: Float
    ): Boolean {
        output.reset()
        output.setBounds(rowRight, availableWidth)

        val resources = owner.battery.getResources()
        val density = resources.getDisplayMetrics().density
        val settings = owner.module.controlCenterSettings()
        val state = owner.module.state
        val placement =
            settings.batteryNumberPlacement()

        val wifiVisibility: ControlCenterRowMeasurementPolicy.NativeVisibility? =
            nativeVisibility(owner, ModuleMain.Companion.WIFI)
        val cellularVisibility: ControlCenterRowMeasurementPolicy.NativeVisibility? =
            nativeVisibility(owner, ModuleMain.Companion.MOBILE)
        if ((wifiVisibility
                    == ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE)
            || (cellularVisibility
                    == ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE)
        ) {
            return false
        }

        owner.painter.setNetworkWeights(
            settings.wifiWeight(),
            settings.technologyWeight()
        )
        owner.painter.setBatteryWeightPercent(settings.percentWeight())
        owner.painter.setBatteryText(settings.percentScale())

        val batteryVisible = state.battery >= 0
        val batteryWidth = measureBatteryWidth(
            owner, settings, placement
        )
        val wifiWidth = owner.painter.wifiHalfWidth(
            ControlCenterAppearance.WIFI_FINAL_CANVAS_SCALE * settings.wifiScale() / 100.0f
        ) * 2.0f
        val cellularWidth = measureCellularWidth(
            owner, settings, density, placement
        )

        output.set(
            ControlCenterRowComponent.BATTERY,
            batteryVisible,
            batteryWidth,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        output.set(
            ControlCenterRowComponent.WIFI,
            false,
            wifiWidth,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        output.set(
            ControlCenterRowComponent.CELLULAR,
            false,
            cellularWidth,
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )
        if (!ControlCenterRowMeasurementPolicy.applyNativeVisibility(
                output,
                wifiVisibility,
                cellularVisibility
            )
        ) {
            return false
        }
        ControlCenterRowMargins.apply(output, density)
        return true
    }

    private fun measureBatteryWidth(
        owner: IconSession,
        settings: SessionSettings,
        placement: BatteryNumberPlacement?
    ): Float {
        val minimum = owner.dp(settings.batterySize()).toFloat()
        var outsideNumberWidthSvg = 0.0f
        if (placement != BatteryNumberPlacement.OUTSIDE
            || owner.module.state.battery < 0
        ) {
            return ControlCenterRowMeasurementPolicy.batteryWidth(
                minimum,
                placement,
                outsideNumberWidthSvg
            )
        }
        val digits: String? = IconPainter.Companion.NUMBERS[min(100, owner.module.state.battery)]
        val weight = BatteryTransferConfig.outsideNumberFontWeight(
            owner.painter.batteryWeightPercent()
        )
        val textSize = owner.batteryShapeRenderer.configuredNumberTextSizeSvg(
            owner.painter, BatteryNumberPlacement.OUTSIDE
        )
        val typeface = owner.batteryShapeRenderer
            .prepareOutsideNumberTypeface(owner.painter)
        outsideNumberWidthSvg = measureOutsideNumberCached(
            this.digitCache,
            owner,
            digits,
            textSize,
            weight,
            typeface
        )
        return ControlCenterRowMeasurementPolicy.batteryWidth(
            minimum,
            placement,
            outsideNumberWidthSvg
        )
    }

    private fun measureCellularWidth(
        owner: IconSession,
        settings: SessionSettings,
        density: Float,
        placement: BatteryNumberPlacement?
    ): Float {
        val textSize: Float = StackedSignals.Companion.labelSize(settings.technologyScale()) * density
        val weight = settings.technologyWeight()
        val typeface = owner.painter.networkPaint.getTypeface()
        val firstWidth: Float
        val secondWidth: Float
        if (owner.module.state.dualSim()) {
            firstWidth = measureCached(
                this.firstLabelCache,
                owner,
                owner.module.stackedLabel(owner.module.state, true),
                textSize,
                weight,
                typeface
            )
            secondWidth = measureCached(
                this.secondLabelCache,
                owner,
                owner.module.stackedLabel(owner.module.state, false),
                textSize,
                weight,
                typeface
            )
        } else {
            firstWidth = measureCached(
                this.firstLabelCache,
                owner,
                owner.module.stackedLabel(owner.module.state),
                textSize,
                weight,
                typeface
            )
            secondWidth = 0.0f
        }
        return StackedSignals.Companion.fillWidthPx(
            max(firstWidth, secondWidth),
            density,
            placement
        )
    }

    companion object {
        @JvmStatic fun nativeVisibility(
            owner: IconSession,
            className: String
        ): ControlCenterRowMeasurementPolicy.NativeVisibility? {
            if (owner.icons == null) {
                return ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE
            }
            var unavailable = false
            for (index in 0..<owner.icons.getChildCount()) {
                val child = owner.icons.getChildAt(index)
                if (child == null
                    || className != child.javaClass.getName()
                ) {
                    continue
                }
                try {
                    if (owner.wanted(child)) {
                        return ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE
                    }
                } catch (error: Exception) {
                    unavailable = true
                }
            }
            return ControlCenterRowMeasurementPolicy.aggregateNativeVisibility(
                false,
                unavailable
            )
        }

        @JvmStatic fun nativeVisibilityMatches(
            owner: IconSession,
            wifiVisible: Boolean,
            cellularVisible: Boolean
        ): Boolean {
            val wifi: ControlCenterRowMeasurementPolicy.NativeVisibility? =
                nativeVisibility(owner, ModuleMain.Companion.WIFI)
            val cellular: ControlCenterRowMeasurementPolicy.NativeVisibility? =
                nativeVisibility(owner, ModuleMain.Companion.MOBILE)
            return wifi != ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE && cellular != ControlCenterRowMeasurementPolicy.NativeVisibility.UNAVAILABLE && SessionPaintPolicy.rowTransitionVisibilityCurrent(
                wifiVisible,
                wifi == ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE,
                cellularVisible,
                cellular == ControlCenterRowMeasurementPolicy.NativeVisibility.VISIBLE
            )
        }

        private fun measureCached(
            cache: ControlCenterTextMeasurementCache,
            owner: IconSession,
            text: String?,
            textSize: Float,
            weight: Int,
            typeface: Typeface?
        ): Float {
            val safeText = if (text == null) "" else text
            val configuration =
                owner.battery.getResources().getConfiguration()
            val fontScale = configuration.fontScale
            val locale = if (configuration.getLocales().isEmpty())
                ""
            else
                configuration.getLocales().get(0).toLanguageTag()
            val generation = if (typeface == null)
                0L
            else System.identityHashCode(typeface).toLong()
            if (cache.matches(
                    safeText,
                    textSize,
                    fontScale,
                    locale,
                    weight,
                    typeface,
                    generation
                )
            ) {
                return cache.width()
            }
            owner.painter.networkPaint.setTextSize(textSize)
            val width = if (safeText.isEmpty())
                0.0f
            else
                owner.painter.measureTechnology(safeText, textSize)
            cache.store(
                safeText,
                textSize,
                fontScale,
                locale,
                weight,
                typeface,
                generation,
                width
            )
            return width
        }

        private fun measureOutsideNumberCached(
            cache: ControlCenterTextMeasurementCache,
            owner: IconSession,
            text: String?,
            textSize: Float,
            weight: Int,
            typeface: Typeface?
        ): Float {
            val safeText = if (text == null) "" else text
            val configuration =
                owner.battery.getResources().getConfiguration()
            val fontScale = configuration.fontScale
            val locale = if (configuration.getLocales().isEmpty())
                ""
            else
                configuration.getLocales().get(0).toLanguageTag()
            val generation = if (typeface == null)
                0L
            else System.identityHashCode(typeface).toLong()
            if (cache.matches(
                    safeText,
                    textSize,
                    fontScale,
                    locale,
                    weight,
                    typeface,
                    generation
                )
            ) {
                return cache.width()
            }
            val width = if (safeText.isEmpty())
                0.0f
            else
                owner.batteryShapeRenderer.measureOutsideNumberSvg(
                    owner.painter,
                    safeText
                )
            cache.store(
                safeText,
                textSize,
                fontScale,
                locale,
                weight,
                typeface,
                generation,
                width
            )
            return width
        }
    }
}
