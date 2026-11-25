package com.cyzah.circular.domain.settings

import com.cyzah.circular.data.settings.SettingsMutationTest
import com.cyzah.circular.domain.settings.AppearanceProjectionOracle
import org.junit.Assert

/** Literal visual contract shared by normal and expanded appearance projections.  */
object AppearanceProjectionOracle {
    private val GETTERS = arrayOf<String?>(
        "mode", "size", "batterySize", "weight", "wifiWeight",
        "technologyWeight", "wifiScale", "technologyScale", "percentScale", "percentWeight",
        "swapContents", "keepMergedShade", "stackedShade", "groupedDots", "capsuleDual", "duoDual",
        "chargeColor", "lowColor", "saverColor", "performanceColor", "lowBatteryThreshold",
        "showPercent", "coloured", "dualStyle", "batteryNumberPlacement"
    )

    @JvmStatic fun snapshot(style: DualStyle?, keep: Boolean, legacy: Boolean, placement: Int): SettingsSnapshot {
        val values = SettingsMutationTest.Store()
        values.values.put("mode", 2)
        values.values.put("size", 23)
        values.values.put("dual_dots_grouped", style == DualStyle.GROUPED)
        values.values.put("capsule_dual", style == DualStyle.CAPSULE)
        values.values.put("duo_dual", style == DualStyle.DUO)
        values.values.put("keep_merged_shade", keep)
        values.values.put("stacked_shade", true)
        values.values.put("swap_contents", true)
        values.values.put("show_percent", false)
        values.values.put("colour", false)
        values.values.put("wifi_scale", 125)
        values.values.put("wifi_weight", 118)
        values.values.put("low_battery_threshold", 17)
        values.values.put("battery_number_placement", placement)
        values.values.put("charge_color", -0xeeddcd)
        values.values.put("low_color", -0xddccbc)
        values.values.put("saver_color", -0xccbbab)
        values.values.put("performance_color", -0xbbaa9a)
        values.values.put("style_profile_duo", "v1|28|100|95|110|95|115|1|90|145|0")
        if (legacy) {
            values.values.put("weight", 120)
            values.values.put("technology_weight", 138)
            values.values.put("technology_scale", 120)
            values.values.put("percent_scale", 130)
            values.values.put("percent_weight", 130)
        } else {
            values.values.put("weight_v2", 135)
            values.values.put("technology_weight_v2", 125)
            values.values.put("technology_scale_v2", 115)
            values.values.put("percent_scale_v2", 80)
            values.values.put("percent_weight_v2", 155)
        }
        return SettingsSnapshot.Companion.from(values)
    }

    @JvmStatic @Throws(Exception::class)
    fun assertProjection(
        projection: Any, style: DualStyle?, expanded: Boolean,
        keep: Boolean, legacy: Boolean, placement: Int
    ) {
        val cc = expanded && !keep
        val weight = if (cc) 100 else if (legacy) 100 else 135
        val expected = arrayOf<Any?>(
            2, 23, if (cc) 27 else (if (style == DualStyle.DUO) 23 else 28),
            weight,
            if (cc) 100 else 118,
            if (cc) 120 else if (legacy) 115 else 125,
            if (cc) 110 else 125,
            if (cc) 110 else if (legacy) 100 else 115,
            if (cc) 105 else if (legacy) 100 else 80,
            if (cc) 125 else if (legacy) 100 else 155, true, keep, true, style == DualStyle.GROUPED,
            style == DualStyle.CAPSULE, style == DualStyle.DUO,
            -0xeeddcd, -0xddccbc, -0xccbbab, -0xbbaa9a, 17,
            false,
            false, style, BatteryNumberPlacement.from(placement)
        )
        for (index in GETTERS.indices) {
            Assert.assertEquals(
                (style.toString() + " " + GETTERS[index] + " expanded=" + expanded
                        + " keep=" + keep + " legacy=" + legacy + " placement=" + placement),
                expected[index], projection.javaClass.getMethod(GETTERS[index]).invoke(projection)
            )
        }
    }
}
