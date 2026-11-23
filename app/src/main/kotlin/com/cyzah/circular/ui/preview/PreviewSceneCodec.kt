package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import kotlin.math.max
import kotlin.math.min

object PreviewSceneCodec {
    private const val VERSION = "2;"

    @JvmStatic fun encode(state: IconState, expansion: Float, expanded: Boolean): String {
        return (VERSION
                + state.battery + "," + flag(state.charging)
                + "," + flag(state.powerSave) + "," + flag(state.performanceMode && !state.powerSave)
                + "," + flag(state.wifiEnabled) + "," + state.wifiLevel
                + "," + flag(state.dataEnabled) + "," + state.mobileLevel
                + "," + state.secondaryLevel + "," + flag(state.simPresent)
                + "," + flag(state.secondarySimPresent) + "," + flag(state.inService)
                + "," + flag(state.secondaryInService) + "," + flag(state.airplane)
                + "," + safe(state.mobileTechnology)
                + "," + Math.round(expansion * 1000.0f)
                + "," + flag(expanded))
    }

    @JvmStatic fun decode(encoded: String?, state: IconState?): Result? {
        if (encoded == null || encoded.isEmpty() || state == null) {
            return null
        }
        try {
            if (encoded.startsWith(VERSION)) {
                return decodeCurrent(encoded.substring(VERSION.length).split(",".toRegex()).toTypedArray(), state)
            }
            return decodeLegacy(encoded.split(",".toRegex()).toTypedArray(), state)
        } catch (ignored: Throwable) {
            return null
        }
    }

    private fun decodeCurrent(values: Array<String>, state: IconState): Result? {
        if (values.size < 17) {
            return null
        }
        state.battery = values[0].toInt()
        state.charging = bool(values[1])
        state.powerSave = bool(values[2])
        state.performanceMode = bool(values[3])
        state.wifiEnabled = bool(values[4])
        state.wifiLevel = values[5].toInt()
        state.dataEnabled = bool(values[6])
        state.mobileLevel = values[7].toInt()
        state.secondaryLevel = values[8].toInt()
        state.simPresent = bool(values[9])
        state.secondarySimPresent = bool(values[10])
        state.inService = bool(values[11])
        state.secondaryInService = bool(values[12])
        state.airplane = bool(values[13])
        state.mobileTechnology = values[14]
        return applyDerivedState(
            state, values[15].toInt() / 1000.0f, bool(values[16])
        )
    }

    private fun decodeLegacy(values: Array<String>, state: IconState): Result? {
        if (values.size < 16) {
            return null
        }
        state.battery = values[0].toInt()
        state.charging = bool(values[1])
        state.powerSave = bool(values[2])
        state.performanceMode = bool(values[3])
        state.wifiEnabled = bool(values[4])
        state.wifiLevel = values[6].toInt()
        state.dataEnabled = true
        state.mobileLevel = values[7].toInt()
        state.secondaryLevel = values[8].toInt()
        state.simPresent = bool(values[9])
        state.secondarySimPresent = bool(values[10])
        state.inService = bool(values[11])
        state.secondaryInService = bool(values[12])
        state.airplane = bool(values[13])
        state.mobileTechnology = values[14]
        return applyDerivedState(
            state,
            values[15].toInt() / 1000.0f,
            values.size > 16 && bool(values[16])
        )
    }

    private fun applyDerivedState(
        state: IconState, expansion: Float, expanded: Boolean
    ): Result {
        PreviewPowerMode.normalize(state)
        state.wifiKnown = true
        state.wifiConnected = PreviewWifiPolicy.connected(state.wifiEnabled)
        state.mobileKnown = state.simPresent
        state.secondaryKnown = state.secondarySimPresent
        state.mobileSlot = 1
        state.secondarySlot = 0
        return Result(
            max(0.0f, min(1.0f, expansion)),
            expanded
        )
    }

    private fun flag(value: Boolean): Int {
        return if (value) 1 else 0
    }

    private fun bool(value: String?): Boolean {
        return "1" == value
    }

    private fun safe(value: String?): String {
        return if (value == null) "" else value
    }

    class Result(@JvmField val expansion: Float, @JvmField val expanded: Boolean)
}
