package com.cyzah.circular.data.settings

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.BuildConfig
import com.cyzah.circular.data.settings.StartupAppearanceCodecTest
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.ContentOffsets
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues
import java.util.HashMap
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class StartupAppearanceCodecTest {
    @Test
    fun githubKeepsLegacyCacheNamesAndRepositoryEditionUsesItsOwn() {
        val prefix = when (BuildConfig.APPLICATION_ID) {
            "com.cyzah.circular" -> ""
            "io.github.cyzah.circular" -> "io.github.cyzah.circular."
            else -> error("Unexpected application ID ${BuildConfig.APPLICATION_ID}")
        }
        for (name in listOf("circluar_startup_appearance", "circluar_startup_topology")) {
            assertEquals(prefix + name, StartupAppearanceCache.preferenceName(name))
        }
    }

    @Test
    fun startupCachePreservesEveryOffsetAndOlderRecordsDefaultToZero() {
        val values = MapValues()
        values.data.put(SettingsSchema.MODE, 2)
        values.data.put(SettingsSchema.TOP_CONTENT_OFFSET_X_PX, -24)
        values.data.put(SettingsSchema.TOP_CONTENT_OFFSET_Y_PX, 4)
        values.data.put(SettingsSchema.CENTER_CONTENT_OFFSET_X_PX, -7)
        values.data.put(SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX, 12)
        values.data.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX, 9)
        values.data.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX, 24)
        var restored = StartupAppearanceCodec.restoreAppearance(
            StartupAppearanceCodec.appearance(values, 45, true), 45
        )
        Assert.assertNotNull(restored)
        val migrated: ContentOffsets = SettingsSnapshot.Companion.from(restored!!).contentOffsets()
        Assert.assertEquals(-48, migrated.topX.toLong())
        Assert.assertEquals(8, migrated.topY.toLong())
        Assert.assertEquals(-14, migrated.centerX.toLong())
        Assert.assertEquals(24, migrated.centerY.toLong())
        Assert.assertEquals(18, migrated.bottomX.toLong())
        Assert.assertEquals(48, migrated.bottomY.toLong())
        values.data.clear()
        values.data.put(SettingsSchema.MODE, 2)
        restored = StartupAppearanceCodec.restoreAppearance(
            StartupAppearanceCodec.appearance(values, 45, true), 45
        )
        Assert.assertNotNull(restored)
        Assert.assertEquals(ContentOffsets.DEFAULT, SettingsSnapshot.Companion.from(restored!!).contentOffsets())
    }

    @Test
    fun startupCacheRoundTripsAllSixCanonicalHalfPixelColumns() {
        val values = MapValues()
        values.data.put(SettingsSchema.MODE, 2)
        val keys = arrayOf<String?>(
            "top_content_offset_x_half_px", "top_content_offset_y_half_px",
            "center_content_offset_x_half_px", "center_content_offset_y_half_px",
            "bottom_content_offset_x_half_px", "bottom_content_offset_y_half_px"
        )
        val halfSteps = intArrayOf(-47, 47, 43, -9, 47, -31)
        for (i in keys.indices) values.data.put(keys[i], halfSteps[i])
        val record = StartupAppearanceCodec.appearance(values, 45, true)
        for (i in keys.indices) Assert.assertEquals(halfSteps[i], record.get(keys[i]))
        val restored = StartupAppearanceCodec.restoreAppearance(record, 45)
        Assert.assertNotNull(restored)
        val decoded: ContentOffsets = SettingsSnapshot.Companion.from(restored!!).contentOffsets()
        Assert.assertEquals(-47, decoded.topX.toLong())
        Assert.assertEquals(47, decoded.topY.toLong())
        Assert.assertEquals(43, decoded.centerX.toLong())
        Assert.assertEquals(-9, decoded.centerY.toLong())
        Assert.assertEquals(47, decoded.bottomX.toLong())
        Assert.assertEquals(-31, decoded.bottomY.toLong())
    }

    @Test
    fun restoredAppearanceKeepsChosenStyleAndDoesNotRepeatDiagnostics() {
        val values = MapValues()
        values.data.put(SettingsSchema.MODE, 2)
        values.data.put(SettingsSchema.CAPSULE_DUAL, true)
        values.data.put(SettingsSchema.SIZE, 27)
        values.data.put(SettingsSchema.TECHNOLOGY_WEIGHT_V2, 125)
        values.data.put(SettingsSchema.TECHNOLOGY_TEXT, "5G=5GA")
        values.data.put(SettingsSchema.DIAGNOSTIC_REQUEST, "old-request")
        values.data.put("wifiLevel", 4)

        val record = StartupAppearanceCodec.appearance(values, 45, true)
        val restored = StartupAppearanceCodec.restoreAppearance(record, 45)

        Assert.assertNotNull(restored)
        val settings: SettingsSnapshot = SettingsSnapshot.Companion.from(restored!!)
        Assert.assertEquals(2, settings.mode().toLong())
        Assert.assertEquals(DualStyle.CAPSULE, settings.dualStyle())
        Assert.assertEquals(27, settings.size().toLong())
        Assert.assertEquals(125, settings.technologyWeight().toLong())
        Assert.assertEquals("5G=5GA", settings.technologyText())
        Assert.assertEquals("", settings.diagnosticRequest())
        Assert.assertFalse(record.containsKey(SettingsSchema.DIAGNOSTIC_REQUEST))
        Assert.assertFalse(record.containsKey("wifiLevel"))
    }

    @Test
    fun absentMigrationKeysStayAbsentAndModeOffIsRemembered() {
        val values = MapValues()
        values.data.put(SettingsSchema.MODE, 0)
        values.data.put(SettingsSchema.WEIGHT, 100)

        val restored = StartupAppearanceCodec.restoreAppearance(
            StartupAppearanceCodec.appearance(values, 45, true), 45
        )

        Assert.assertNotNull(restored)
        Assert.assertFalse(restored!!.contains(SettingsSchema.WEIGHT_V2))
        Assert.assertEquals(0, SettingsSnapshot.Companion.from(restored!!).mode().toLong())
        Assert.assertEquals(
            AppearanceScale.weightFromLegacy(100).toLong(),
            SettingsSnapshot.Companion.from(restored!!).weight().toLong()
        )
    }

    @Test
    fun missingIncompatibleAndOldVersionRecordsCannotEnableAStyle() {
        val values = MapValues()
        Assert.assertNull(StartupAppearanceCodec.restoreAppearance(HashMap<String?, Any?>(), 45))
        Assert.assertTrue(StartupAppearanceCodec.appearance(values, 45, true).isEmpty())
        values.data.put(SettingsSchema.MODE, 2)
        Assert.assertTrue(StartupAppearanceCodec.appearance(values, 45, false).isEmpty())
        Assert.assertNull(
            StartupAppearanceCodec.restoreAppearance(
                StartupAppearanceCodec.appearance(values, 44, true), 45
            )
        )
    }

    @Test
    fun malformedStoredTypesDoNotSilentlyEnableDefaultAppearance() {
        val values = MapValues()
        values.data.put(SettingsSchema.MODE, 2)
        val record: MutableMap<String?, Any?> = HashMap<String?, Any?>(
            StartupAppearanceCodec.appearance(values, 45, true)
        )
        record.put(SettingsSchema.CAPSULE_DUAL, "true")

        Assert.assertNull(StartupAppearanceCodec.restoreAppearance(record, 45))
    }

    @Test
    fun topologyRestoresTheDualShapeWithoutInventingNetworkReadiness() {
        val source: IconState = dualState()
        source.mobileLevel = 4
        source.secondaryLevel = 3
        source.wifiKnown = true
        source.wifiConnected = true
        source.wifiLevel = 4
        source.battery = 86
        val restored = IconState()

        Assert.assertTrue(
            StartupAppearanceCodec.restoreTopology(
                StartupAppearanceCodec.topology(source, 45), 45, restored
            )
        )

        Assert.assertTrue(restored.dualSim())
        Assert.assertEquals(1, restored.mobileSlot.toLong())
        Assert.assertEquals(0, restored.secondarySlot.toLong())
        Assert.assertFalse(restored.mobileKnown)
        Assert.assertFalse(restored.secondaryKnown)
        Assert.assertFalse(restored.wifiKnown)
        Assert.assertFalse(restored.inService)
        Assert.assertFalse(restored.secondaryInService)
        Assert.assertEquals(-1, restored.mobileLevel.toLong())
        Assert.assertEquals(-1, restored.secondaryLevel.toLong())
        Assert.assertEquals(-1, restored.wifiLevel.toLong())
        Assert.assertEquals(-1, restored.battery.toLong())
        Assert.assertEquals(-1, restored.subId.toLong())
        Assert.assertEquals(-1, restored.secondarySubId.toLong())
    }

    @Test
    fun incompleteTopologyDoesNotReplaceLastKnownShape() {
        val pending: IconState = dualState()
        pending.mobileKnown = false
        Assert.assertTrue(StartupAppearanceCodec.topology(pending, 45).isEmpty())
        pending.mobileKnown = true
        pending.secondarySlot = -1
        Assert.assertTrue(StartupAppearanceCodec.topology(pending, 45).isEmpty())
        pending.secondarySlot = 1
        Assert.assertTrue(StartupAppearanceCodec.topology(pending, 45).isEmpty())
    }

    @Test
    fun liveTopologyWinsOverAStoredStartupHint() {
        val live = IconState()
        live.mobileKnown = true
        live.simPresent = true
        live.mobileSlot = 0

        Assert.assertFalse(
            StartupAppearanceCodec.restoreTopology(
                StartupAppearanceCodec.topology(dualState(), 45), 45, live
            )
        )
        Assert.assertFalse(live.dualSim())
        Assert.assertEquals(0, live.mobileSlot.toLong())
    }

    @Test
    fun confirmedNoSimClearsTheDualShapeHint() {
        val noSim = IconState()
        noSim.mobileKnown = true
        val restored = IconState()
        restored.simPresent = true
        restored.secondarySimPresent = true
        restored.mobileSlot = 1
        restored.secondarySlot = 0

        Assert.assertTrue(
            StartupAppearanceCodec.restoreTopology(
                StartupAppearanceCodec.topology(noSim, 45), 45, restored
            )
        )
        Assert.assertFalse(restored.simPresent)
        Assert.assertFalse(restored.secondarySimPresent)
        Assert.assertEquals(-1, restored.mobileSlot.toLong())
        Assert.assertEquals(-1, restored.secondarySlot.toLong())
    }

    private class MapValues : SettingsValues {
        @JvmField val data: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        override fun contains(key: String?): Boolean {
            return data.containsKey(key)
        }

        override fun getInt(key: String?, fallback: Int): Int {
            val value = data.get(key)
            return if (value is Int) value else fallback
        }

        override fun getBoolean(key: String?, fallback: Boolean): Boolean {
            val value = data.get(key)
            return if (value is Boolean) value else fallback
        }

        override fun getString(key: String?, fallback: String?): String? {
            val value = data.get(key)
            return if (value is String) value else fallback
        }
    }

    companion object {
        private fun dualState(): IconState {
            val state = IconState()
            state.mobileKnown = true
            state.secondaryKnown = true
            state.simPresent = true
            state.secondarySimPresent = true
            state.mobileSlot = 1
            state.secondarySlot = 0
            return state
        }
    }
}
