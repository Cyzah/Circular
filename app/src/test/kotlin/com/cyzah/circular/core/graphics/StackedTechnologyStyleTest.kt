package com.cyzah.circular.core.graphics

import com.cyzah.circular.core.graphics.StackedTechnologyStyleTest
import com.cyzah.circular.domain.settings.AppearanceScale
import com.cyzah.circular.domain.settings.DualStyle
import com.cyzah.circular.domain.settings.SettingsSnapshot
import com.cyzah.circular.domain.settings.SettingsValues
import com.cyzah.circular.hook.runtime.SessionSettings
import org.junit.Assert
import org.junit.Test

class StackedTechnologyStyleTest {
    @Test
    fun expandedLabelUsesTheUsersScaleInDrawingAndMeasurement() {
        Assert.assertEquals(3.5f, StackedSignals.Companion.labelSize(50), .0001f)
        Assert.assertEquals(7f, StackedSignals.Companion.labelSize(100), .0001f)
        Assert.assertEquals(10.5f, StackedSignals.Companion.labelSize(150), .0001f)
    }

    @Test
    fun mergedTextUsesOnlyTheStatusBarFontAndExpandedTextOnlyTheHostFont() {
        Assert.assertEquals(0.8f, StackedTechnologyStyle.endpointAlpha(0.8f, 0.0f), EPSILON)
        Assert.assertEquals(0.0f, StackedTechnologyStyle.hostAlpha(0.8f, 0.0f), EPSILON)
        Assert.assertEquals(0.0f, StackedTechnologyStyle.endpointAlpha(0.8f, 1.0f), EPSILON)
        Assert.assertEquals(0.8f, StackedTechnologyStyle.hostAlpha(0.8f, 1.0f), EPSILON)
    }

    @Test
    fun fontTransitionTransfersCoverageBetweenTheTwoCachedPainters() {
        Assert.assertEquals(0.6f, StackedTechnologyStyle.endpointAlpha(0.8f, 0.25f), EPSILON)
        Assert.assertEquals(0.2f, StackedTechnologyStyle.hostAlpha(0.8f, 0.25f), EPSILON)
        for (sample in 0..100) {
            val expansion = sample / 100.0f
            Assert.assertEquals(
                0.8f,
                StackedTechnologyStyle.endpointAlpha(0.8f, expansion)
                        + StackedTechnologyStyle.hostAlpha(0.8f, expansion),
                EPSILON
            )
        }
    }

    @Test
    fun groupedAndLayeredClosingSizeMatchesTheStatusBarAtDefaultSettings() {
        val source: SessionSettings = SessionSettings.Companion.defaults()
        val host = source.controlCenter()
        for (style in arrayOf<DualStyle>(DualStyle.GROUPED, DualStyle.LAYERED)) {
            Assert.assertEquals(
                "Merged text must match the status-bar painter for " + style,
                7.2f,
                StackedTechnologyStyle.targetSize(
                    style,
                    AppearanceScale.technologyTextSize(source.technologyScale()) * 0.2f,
                    AppearanceScale.technologyTextSize(host.technologyScale()) * 0.2f
                ),
                EPSILON
            )
        }
    }

    @Test
    fun groupedAndLayeredClosingSizeTracksTheConfiguredStatusBarScale() {
        val source: SessionSettings = SessionSettings.Companion.from(
            SettingsSnapshot.Companion.from(object : SettingsValues {
                override fun contains(key: String?): Boolean {
                    return "technology_scale_v2" == key
                }

                override fun getInt(key: String?, fallback: Int): Int {
                    return if (contains(key)) 150 else fallback
                }

                override fun getBoolean(key: String?, fallback: Boolean): Boolean {
                    return fallback
                }

                override fun getString(key: String?, fallback: String?): String? {
                    return fallback
                }
            }), false
        )
        val host = source.controlCenter()
        for (style in arrayOf<DualStyle>(DualStyle.GROUPED, DualStyle.LAYERED)) {
            Assert.assertEquals(
                10.8f,
                StackedTechnologyStyle.targetSize(
                    style,
                    AppearanceScale.technologyTextSize(source.technologyScale()) * 0.2f,
                    AppearanceScale.technologyTextSize(host.technologyScale()) * 0.2f
                ),
                EPSILON
            )
        }
    }

    @Test
    fun capsuleKeepsItsSeparatelyMeasuredTextSizing() {
        Assert.assertEquals(
            6.84f,
            StackedTechnologyStyle.targetSize(DualStyle.CAPSULE, 10.8f, 6.84f),
            EPSILON
        )
    }

    @Test
    fun duoUsesTheFittedStatusReadoutAtTheCollapsedEndpoint() {
        Assert.assertEquals(
            10.8f,
            StackedTechnologyStyle.targetSize(DualStyle.DUO, 10.8f, 6.84f),
            EPSILON
        )
    }

    companion object {
        private const val EPSILON = 0.0001f
    }
}
