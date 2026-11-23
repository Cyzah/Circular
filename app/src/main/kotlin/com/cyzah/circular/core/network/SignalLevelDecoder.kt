package com.cyzah.circular.core.network

import android.telephony.SignalStrength
import kotlin.math.max
import kotlin.math.min

object SignalLevelDecoder {
    @JvmStatic fun decode(signalStrength: SignalStrength?): Decoded {
        if (signalStrength == null) {
            return Decoded(-1, "")
        }
        var miuiLevel = -1
        val probe = StringBuilder()
        try {
            for (strength in signalStrength.getCellSignalStrengths()) {
                val type: Class<*> = strength.javaClass
                probe.append(type.getSimpleName()).append('{')
                var current: Class<*>? = type
                while (current != null && current != Any::class.java
                ) {
                    for (declared in current.getDeclaredFields()) {
                        val name = declared.getName()
                        val lower = name.lowercase()
                        if (declared.getType() != Integer.TYPE
                            || !lower.contains("level")
                        ) {
                            continue
                        }
                        var value = -999
                        try {
                            declared.setAccessible(true)
                            value = declared.getInt(strength)
                        } catch (ignored: Throwable) {
                        }
                        probe.append(name).append('=').append(value).append(' ')
                        if (lower.contains("miui")
                            && value >= 0 && value <= 4 && value > miuiLevel
                        ) {
                            miuiLevel = value
                        }
                    }
                    current = current.getSuperclass()
                }
                probe.append("} ")
            }
        } catch (error: Throwable) {
            probe.append("err:").append(error.javaClass.getSimpleName())
        }
        val diagnostic = if (probe.length > 220)
            probe.substring(0, 220)
        else
            probe.toString()
        if (miuiLevel > 0) {
            return Decoded(miuiLevel, diagnostic)
        }
        try {
            return Decoded(
                max(0, min(4, signalStrength.getLevel())),
                diagnostic
            )
        } catch (ignored: Throwable) {
            return Decoded(miuiLevel, diagnostic)
        }
    }

    class Decoded(private val level: Int, private val probe: String?) {
        fun level(): Int {
            return level
        }

        fun probe(): String? {
            return probe
        }
    }
}
