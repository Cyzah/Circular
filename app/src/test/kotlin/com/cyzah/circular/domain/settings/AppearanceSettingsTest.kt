package com.cyzah.circular.domain.settings

import com.cyzah.circular.domain.settings.AppearanceProjectionOracle
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    @Throws(Exception::class)
    fun sharedProjectionMatchesPreExtractionOracleAcrossStylesAndAliases() {
        for (style in DualStyle.entries) for (keep in booleanArrayOf(false, true)) for (legacy in booleanArrayOf(
            false,
            true
        )) for (placement in 0..2) {
            val normal: AppearanceSettings = AppearanceSettings.Companion.from(
                AppearanceProjectionOracle.snapshot(style, keep, legacy, placement)
            )
            AppearanceProjectionOracle.assertProjection(normal, style, false, keep, legacy, placement)
            AppearanceProjectionOracle.assertProjection(
                normal.controlCenterAppearance(), style,
                true, keep, legacy, placement
            )
            if (keep) Assert.assertSame(normal, normal.controlCenterAppearance())
        }
    }
}
