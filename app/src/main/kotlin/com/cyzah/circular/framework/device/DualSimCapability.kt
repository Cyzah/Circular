package com.cyzah.circular.framework.device

import android.content.Context
import android.telephony.TelephonyManager
import kotlin.math.min

object DualSimCapability {
    @JvmStatic fun available(context: Context): Boolean {
        try {
            val telephony = context.getSystemService(
                Context.TELEPHONY_SERVICE
            ) as TelephonyManager?
            if (telephony == null) {
                return false
            }
            val slots = min(2, telephony.getActiveModemCount())
            var present = 0
            for (slot in 0..<slots) {
                val state = telephony.getSimState(slot)
                if (state != TelephonyManager.SIM_STATE_ABSENT
                    && state != TelephonyManager.SIM_STATE_UNKNOWN
                ) {
                    present++
                }
            }
            return present >= 2
        } catch (ignored: Throwable) {
            return true
        }
    }
}
