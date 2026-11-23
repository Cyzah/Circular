package com.cyzah.circular.core.graphics.geometry

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.hook.runtime.SessionSettings

/** Allocation-free invalidation key for the measured host-0 battery ring.  */
class BatteryTransferGeometryKey {
    private var settings: SessionSettings? = null
    private var technology: String? = null
    private var battery = 0
    private var duoProgress = 0f
    private var cached = false
    private var charging = false
    private var wifi = false
    private var data = false
    private var mobileKnown = false
    private var simPresent = false
    private var dualSim = false
    private var inService = false
    private var airplane = false

    fun matches(state: IconState, settings: SessionSettings?, duoProgress: Float): Boolean {
        val technology = state.centreTechnology()
        val sameTechnology = if (this.technology == null)
            technology == null
        else
            (this.technology == technology)
        return this.cached
                && this.settings == settings && sameTechnology
                && this.battery == state.battery && java.lang.Float.compare(
            this.duoProgress,
            duoProgress
        ) == 0 && this.charging == state.charging && this.wifi == state.showWifi() && this.data == state.dataEnabled && this.mobileKnown == state.mobileKnown && this.simPresent == state.simPresent && this.dualSim == state.dualSim() && this.inService == state.inService && this.airplane == state.airplane
    }

    fun store(state: IconState, settings: SessionSettings?, duoProgress: Float) {
        this.settings = settings
        this.technology = state.centreTechnology()
        this.battery = state.battery
        this.duoProgress = duoProgress
        this.charging = state.charging
        this.wifi = state.showWifi()
        this.data = state.dataEnabled
        this.mobileKnown = state.mobileKnown
        this.simPresent = state.simPresent
        this.dualSim = state.dualSim()
        this.inService = state.inService
        this.airplane = state.airplane
        this.cached = true
    }
}
