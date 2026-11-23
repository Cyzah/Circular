package com.cyzah.circular.domain.settings

import java.util.function.IntConsumer
import org.junit.Assert
import org.junit.Test

class BatteryNumberPickerActionsTest {
    @Test
    fun choosingPlacementPersistsNormalizedValueThenRefreshesControl() {
        val events = StringBuilder()

        BatteryNumberPickerActions.apply(
            99,
            IntConsumer { value: Int -> events.append("save:").append(value) },
            Runnable { events.append("|refresh") })

        Assert.assertEquals("save:2|refresh", events.toString())
    }
}
