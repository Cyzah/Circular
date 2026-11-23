package com.cyzah.circular.hook.runtime

import com.cyzah.circular.core.graphics.IconPainter
import com.cyzah.circular.core.graphics.geometry.BatteryTransferGeometryKey
import com.cyzah.circular.core.graphics.geometry.RingGeometry
import com.cyzah.circular.core.network.IconState

/**
 * Caches the actual host-0 ring geometry used by control-center endpoint
 * projection. Dynamic content changes invalidate only this geometry cache.
 */
class BatteryTransferRuntime(private val module: ModuleMain) {
    private val ruler = IconPainter()
    private val transferGeometry = RingGeometry()
    private var contentUnit = Float.NaN
    private val geometryKey = BatteryTransferGeometryKey()

    fun geometry(state: IconState): RingGeometry {
        val currentSettings = this.module.sessionSettings()
        val currentDuoProgress = this.module.currentDuoWifiProgress()
        val unit = this.module.contentPixelUnit(currentSettings)
        if (!this.geometryKey.matches(state, currentSettings, currentDuoProgress)
            || java.lang.Float.compare(contentUnit, unit) != 0
        ) {
            this.module.oemPainter(this.ruler)
            this.ruler.updateBatteryRingGeometry(state)
            contentUnit = unit
            this.geometryKey.store(state, currentSettings, currentDuoProgress)
        }
        this.transferGeometry.copyForTransferFrom(
            this.ruler.batteryRingGeometry
        )
        return this.transferGeometry
    }
}
