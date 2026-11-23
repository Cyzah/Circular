package com.cyzah.circular.domain.settings

import java.util.function.IntConsumer

class BatteryNumberPickerActions private constructor() {
    companion object {
        @JvmStatic fun apply(requestedPlacement: Int, persist: IntConsumer, refresh: Runnable) {
            persist.accept(BatteryNumberPlacement.from(requestedPlacement).value)
            refresh.run()
        }
    }
}
