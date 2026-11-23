package com.cyzah.circular.hook.runtime

import com.cyzah.circular.domain.settings.AppearanceProjectionOracle
import com.cyzah.circular.domain.settings.BatteryNumberPlacement
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues
import com.cyzah.circular.hook.runtime.SessionSettingsTest
import java.util.HashMap
import org.junit.Assert
import org.junit.Test

class SessionSettingsTest {
    @Test
    @Throws(Exception::class)
    fun visualProjectionMatchesLiteralOracleAcrossAllStylesAndAliases() {
        for (style in DualStyle.entries) for (keep in booleanArrayOf(false, true)) for (legacy in booleanArrayOf(
            false,
            true
        )) for (placement in 0..2) {
            val snapshot = AppearanceProjectionOracle.snapshot(
                style, keep, legacy, placement
            )
            val normal: SessionSettings = SessionSettings.Companion.from(snapshot, true)
            AppearanceProjectionOracle.assertProjection(
                normal, style, false, keep, legacy, placement
            )
            AppearanceProjectionOracle.assertProjection(
                normal.controlCenterAppearance(), style, true, keep, legacy, placement
            )
        }
    }

    @Test
    fun defaultsMatchCurrentInitialValues() {
        val settings: SessionSettings = SessionSettings.Companion.defaults()

        Assert.assertEquals(0, settings.mode().toLong())
        Assert.assertEquals(24, settings.size().toLong())
        Assert.assertEquals(26, settings.batterySize().toLong())
        Assert.assertEquals(100, settings.weight().toLong())
        Assert.assertEquals(20, settings.lowBatteryThreshold().toLong())
        Assert.assertFalse(settings.compatible())
        Assert.assertEquals(
            BatteryNumberPlacement.OUTSIDE,
            settings.batteryNumberPlacement()
        )
    }

    @Test
    fun copiesSnapshotAndCompatibilityState() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("mode", 2)
        values.put("size", 31)
        values.put("weight_v2", 135)
        values.put("grouped_dots", true)
        values.put("dual_dots_grouped", true)
        values.put("colour", false)
        values.put("show_percent", true)
        values.put("battery_number_placement", 1)
        values.put(
            "style_profile_duo",
            "v1|27|100|95|110|95|110|1|100|140|0"
        )
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(values))

        val settings: SessionSettings = SessionSettings.Companion.from(snapshot, true)

        Assert.assertEquals(2, settings.mode().toLong())
        Assert.assertEquals(29, settings.size().toLong())
        Assert.assertEquals(27, settings.batterySize().toLong())
        Assert.assertEquals(135, settings.weight().toLong())
        Assert.assertEquals(DualStyle.GROUPED, settings.dualStyle())
        Assert.assertFalse(settings.coloured())
        Assert.assertTrue(settings.showPercent())
        Assert.assertTrue(settings.compatible())
        Assert.assertEquals(
            BatteryNumberPlacement.INSIDE,
            settings.batteryNumberPlacement()
        )
    }

    @Test
    fun incompatibleSnapshotDisablesMergedMode() {
        val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(MapValues(HashMap<String?, Any?>()))

        val settings: SessionSettings = SessionSettings.Companion.from(snapshot, false)

        Assert.assertFalse(settings.compatible())
    }

    @Test
    fun capsuleControlCenterKeepsUserBodyAndNetworkMetrics() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("mode", 2)
        values.put("capsule_dual", true)
        values.put("size", 29)
        values.put("weight_v2", 80)
        values.put("wifi_scale", 50)
        values.put("wifi_weight", 120)
        values.put("technology_scale_v2", 150)
        values.put("technology_weight_v2", 130)
        values.put("show_percent", true)
        values.put("percent_scale_v2", 100)
        values.put("percent_weight_v2", 100)
        values.put("swap_contents", true)
        values.put("colour", false)
        values.put("battery_number_placement", 1)
        values.put("low_battery_threshold", 5)
        values.put("charge_color", -0x1000000)
        values.put("low_color", -0xeeeeef)
        values.put("saver_color", -0xddddde)
        values.put("performance_color", -0xcccccd)
        values.put(
            "style_profile_duo",
            "v1|27|100|95|110|95|110|1|90|140|0"
        )
        val source: SessionSettings =
            SessionSettings.Companion.from(
                SettingsSnapshot.Companion.from(MapValues(values)),
                true
            )
        try {
            val method =
                SessionSettings::class.java.getDeclaredMethod(
                    "controlCenter"
                )
            method.setAccessible(true)
            val locked =
                method.invoke(source) as SessionSettings

            Assert.assertEquals(29, source.size().toLong())
            Assert.assertEquals(29, locked.size().toLong())
            Assert.assertEquals(27, locked.batterySize().toLong())
            Assert.assertEquals(80, locked.weight().toLong())
            Assert.assertEquals(50, locked.wifiScale().toLong())
            Assert.assertEquals(120, locked.wifiWeight().toLong())
            Assert.assertEquals(150, locked.technologyScale().toLong())
            Assert.assertEquals(130, locked.technologyWeight().toLong())
            Assert.assertTrue(locked.showPercent())
            Assert.assertEquals(100, locked.percentScale().toLong())
            Assert.assertEquals(100, locked.percentWeight().toLong())
            Assert.assertTrue(locked.swapContents())
            Assert.assertFalse(locked.coloured())
            Assert.assertEquals(-0x1000000, locked.chargeColor().toLong())
            Assert.assertEquals(-0xeeeeef, locked.lowColor().toLong())
            Assert.assertEquals(-0xddddde, locked.saverColor().toLong())
            Assert.assertEquals(-0xcccccd, locked.performanceColor().toLong())
            Assert.assertEquals(5, locked.lowBatteryThreshold().toLong())
            Assert.assertEquals(
                BatteryNumberPlacement.INSIDE,
                locked.batteryNumberPlacement()
            )
        } catch (error: ReflectiveOperationException) {
            throw AssertionError(
                "Missing capsule control-center settings",
                error
            )
        }
    }

    @Test
    fun nonCapsuleControlCenterStylesUseTheDuoTemplate() {
        val expected =
            ControlCenterAppearance.profile(DualStyle.DUO)!!

        for (style in DualStyle.entries) {
            if (style == DualStyle.CAPSULE) {
                continue
            }
            val actual =
                ControlCenterAppearance.profile(style)!!
            Assert.assertEquals(expected.size.toLong(), actual.size.toLong())
            Assert.assertEquals(expected.weight.toLong(), actual.weight.toLong())
            Assert.assertEquals(expected.wifiScale.toLong(), actual.wifiScale.toLong())
            Assert.assertEquals(expected.wifiWeight.toLong(), actual.wifiWeight.toLong())
            Assert.assertEquals(expected.technologyScale.toLong(), actual.technologyScale.toLong())
            Assert.assertEquals(expected.technologyWeight.toLong(), actual.technologyWeight.toLong())
        }
    }

    @Test
    fun keepMergedShadeUsesTheUserStatusBarAppearance() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("mode", 2)
        values.put("keep_merged_shade", true)
        values.put("stacked_shade", false)
        values.put("capsule_dual", false)
        values.put("grouped_dots", true)
        values.put("dual_dots_grouped", false)
        values.put("size", 31)
        values.put("weight_v2", 125)
        values.put("wifi_scale", 115)
        values.put("wifi_weight", 105)
        values.put("technology_scale_v2", 125)
        values.put("technology_weight_v2", 120)
        values.put("show_percent", false)
        values.put("percent_scale_v2", 75)
        values.put("percent_weight_v2", 155)
        values.put(
            "style_profile_grouped",
            "v1|31|125|115|105|125|120|0|75|155|0"
        )
        val source: SessionSettings = SessionSettings.Companion.from(
            SettingsSnapshot.Companion.from(MapValues(values)),
            true
        )

        val effective =
            source.controlCenterAppearance()

        Assert.assertEquals(source.size().toLong(), effective.size().toLong())
        Assert.assertEquals(source.weight().toLong(), effective.weight().toLong())
        Assert.assertEquals(source.wifiScale().toLong(), effective.wifiScale().toLong())
        Assert.assertEquals(source.wifiWeight().toLong(), effective.wifiWeight().toLong())
        Assert.assertEquals(source.technologyScale().toLong(), effective.technologyScale().toLong())
        Assert.assertEquals(source.technologyWeight().toLong(), effective.technologyWeight().toLong())
        Assert.assertEquals(source.showPercent(), effective.showPercent())
        Assert.assertEquals(source.percentScale().toLong(), effective.percentScale().toLong())
        Assert.assertEquals(source.percentWeight().toLong(), effective.percentWeight().toLong())
    }

    @Test
    fun lockedControlCenterProfileKeepsTheUserOverallSize() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("mode", 2)
        values.put("size", 31)
        values.put("weight_v2", 100)
        values.put("wifi_scale", 100)
        values.put("wifi_weight", 100)
        values.put("technology_scale_v2", 100)
        values.put("technology_weight_v2", 100)
        values.put("grouped_dots", true)
        values.put(
            "style_profile_grouped",
            "v1|29|100|100|100|100|100|1|100|100|0"
        )
        val source: SessionSettings = SessionSettings.Companion.from(
            SettingsSnapshot.Companion.from(MapValues(values)),
            true
        )

        val locked = source.controlCenter()

        Assert.assertEquals(source.size().toLong(), locked.size().toLong())
        Assert.assertEquals(27, locked.batterySize().toLong())
    }

    @Test
    fun flatControlCenterProfileKeepsUserSwapAndPercentChoice() {
        val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        values.put("mode", 2)
        values.put("dual_dots_grouped", true)
        values.put("swap_contents", true)
        values.put("size", 31)
        values.put("weight_v2", 130)
        values.put("wifi_scale", 70)
        values.put(
            "style_profile_grouped",
            "v1|31|130|70|120|140|150|0|80|160|1"
        )
        val source: SessionSettings = SessionSettings.Companion.from(
            SettingsSnapshot.Companion.from(MapValues(values)),
            true
        )

        val locked = source.controlCenter()

        Assert.assertTrue(locked.swapContents())
        Assert.assertEquals(source.size().toLong(), locked.size().toLong())
        Assert.assertEquals(source.weight().toLong(), locked.weight().toLong())
        Assert.assertEquals(source.wifiScale().toLong(), locked.wifiScale().toLong())
        Assert.assertEquals(source.wifiWeight().toLong(), locked.wifiWeight().toLong())
        Assert.assertEquals(source.technologyScale().toLong(), locked.technologyScale().toLong())
        Assert.assertEquals(source.technologyWeight().toLong(), locked.technologyWeight().toLong())
        Assert.assertFalse(locked.showPercent())
        Assert.assertEquals(source.percentScale().toLong(), locked.percentScale().toLong())
        Assert.assertEquals(source.percentWeight().toLong(), locked.percentWeight().toLong())
    }

    @Test
    fun disabledFlatMergedPercentStaysHiddenForEveryExpandedNumberPlacement() {
        assertFlatControlCenterPercent(false)
    }

    @Test
    fun enabledFlatMergedPercentStaysVisibleForEveryExpandedNumberPlacement() {
        assertFlatControlCenterPercent(true)
    }

    private class MapValues(private val values: MutableMap<String?, Any?>) : SettingsValues {
        override fun contains(key: String?): Boolean {
            return values.containsKey(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val value = values.get(key)
            return if (value is Int) value else fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val value = values.get(key)
            return if (value is Boolean) value else fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            val value = values.get(key)
            return if (value is String) value else fallback
        }
    }

    companion object {
        private fun assertFlatControlCenterPercent(showPercent: Boolean) {
            for (style in arrayOf<DualStyle>(DualStyle.LAYERED, DualStyle.GROUPED)) {
                for (placement in BatteryNumberPlacement.values()) {
                    val values: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                    values.put("mode", 2)
                    values.put("dual_dots_grouped", style == DualStyle.GROUPED)
                    values.put("show_percent", showPercent)
                    values.put("battery_number_placement", placement.value)
                    values.put("size", 29)
                    values.put("weight_v2", 130)
                    values.put("wifi_scale", 70)
                    values.put("wifi_weight", 125)
                    values.put("technology_scale_v2", 140)
                    values.put("technology_weight_v2", 150)
                    values.put("percent_scale_v2", 75)
                    values.put("percent_weight_v2", 160)
                    values.put("swap_contents", true)
                    val source: SessionSettings = SessionSettings.Companion.from(
                        SettingsSnapshot.Companion.from(MapValues(values)), true
                    )
                    val expanded = source.controlCenter()
                    val scenario = style.toString() + "/" + placement + "/percent=" + showPercent

                    Assert.assertEquals(scenario, style, source.dualStyle())
                    Assert.assertEquals(scenario, showPercent, source.showPercent())
                    Assert.assertEquals(scenario, showPercent, expanded.showPercent())
                    // Full-battery readouts have a separate setting, including when
                    // merged percentages are disabled during the ownership handoff.
                    Assert.assertEquals(scenario, placement, source.batteryNumberPlacement())
                    Assert.assertEquals(scenario, placement, expanded.batteryNumberPlacement())
                    Assert.assertEquals(scenario, source.size().toLong(), expanded.size().toLong())
                    Assert.assertEquals(scenario, 27, expanded.batterySize().toLong())
                    Assert.assertEquals(scenario, source.weight().toLong(), expanded.weight().toLong())
                    Assert.assertEquals(scenario, source.wifiScale().toLong(), expanded.wifiScale().toLong())
                    Assert.assertEquals(scenario, source.wifiWeight().toLong(), expanded.wifiWeight().toLong())
                    Assert.assertEquals(
                        scenario,
                        source.technologyScale().toLong(),
                        expanded.technologyScale().toLong()
                    )
                    Assert.assertEquals(
                        scenario,
                        source.technologyWeight().toLong(),
                        expanded.technologyWeight().toLong()
                    )
                    Assert.assertEquals(scenario, source.percentScale().toLong(), expanded.percentScale().toLong())
                    Assert.assertEquals(scenario, source.percentWeight().toLong(), expanded.percentWeight().toLong())
                    Assert.assertTrue(scenario, expanded.swapContents())
                }
            }
        }
    }
}
