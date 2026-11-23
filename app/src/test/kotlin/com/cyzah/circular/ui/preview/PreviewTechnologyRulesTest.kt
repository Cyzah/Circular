package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.network.IconState
import com.cyzah.circular.ui.preview.PreviewTechnologyRulesTest
import org.junit.Assert
import org.junit.Test

class PreviewTechnologyRulesTest {
    @Test
    fun renderInputAppliesRulesWithoutChangingThePersistedScene() {
        val raw: IconState = scene()
        val rules = PreviewTechnologyRules()
        val rendered = rules.update(raw, 1, "5G=5G+")
        Assert.assertEquals("5G+", rendered.mobileTechnology)
        Assert.assertEquals("5G", raw.mobileTechnology)
        Assert.assertNotSame(raw, rendered)
        Assert.assertEquals(91, rendered.battery.toLong())
        Assert.assertEquals(4, rendered.mobileLevel.toLong())
        Assert.assertEquals(2, rendered.secondaryLevel.toLong())
        Assert.assertTrue(rendered.dualSim())
        Assert.assertTrue(rendered.dataEnabled)
    }

    @Test
    fun changedRulesAreAlwaysAppliedToTheOriginalLabel() {
        val raw: IconState = scene()
        val rules = PreviewTechnologyRules()
        val rendered = rules.update(raw, 1, "5G=5G+;5G+=wrong")
        Assert.assertEquals("5G+", rendered.mobileTechnology)
        Assert.assertSame(rendered, rules.update(raw, 1, "5G=4G;5G+=wrong"))
        Assert.assertEquals("4G", rendered.mobileTechnology)
        Assert.assertEquals("5G", raw.mobileTechnology)
        Assert.assertSame(rendered, rules.update(raw, 1, ""))
        Assert.assertEquals("5G", rendered.mobileTechnology)
    }

    @Test
    fun drawFramesReuseTheProjectionUntilSceneVersionChanges() {
        val raw: IconState = scene()
        val rules = PreviewTechnologyRules()
        val rendered = rules.update(raw, 10, "5G=5G+")
        raw.battery = 17
        raw.mobileTechnology = "LTE"
        Assert.assertSame(rendered, rules.update(raw, 10, "5G=5G+"))
        Assert.assertEquals(91, rendered.battery.toLong())
        Assert.assertEquals("5G+", rendered.mobileTechnology)
        Assert.assertSame(rendered, rules.update(raw, 11, "5G=5G+"))
        Assert.assertEquals(17, rendered.battery.toLong())
        Assert.assertEquals("LTE", rendered.mobileTechnology)
    }

    @Test
    fun ruleChangeRefreshesTheProjectionWithoutASceneVersionChange() {
        val raw: IconState = scene()
        val rules = PreviewTechnologyRules()
        val rendered = rules.update(raw, 2, "5G=one")
        Assert.assertEquals("one", rendered.mobileTechnology)
        rules.update(raw, 2, "5G=two")
        Assert.assertEquals("two", rendered.mobileTechnology)
        Assert.assertEquals("5G", raw.mobileTechnology)
    }

    @Test
    fun explicitEmptyReplacementReachesTheRenderInput() {
        val raw: IconState = scene()
        val rendered = PreviewTechnologyRules().update(raw, 0, "5G=")
        Assert.assertEquals("", rendered.mobileTechnology)
        Assert.assertEquals("5G", raw.mobileTechnology)
    }

    companion object {
        private fun scene(): IconState {
            val raw = IconState()
            raw.battery = 91
            raw.mobileTechnology = "5G"
            raw.mobileKnown = true
            raw.simPresent = true
            raw.inService = true
            raw.mobileLevel = 4
            raw.mobileSlot = 1
            raw.secondaryKnown = true
            raw.secondarySimPresent = true
            raw.secondaryInService = true
            raw.secondaryLevel = 2
            raw.secondarySlot = 0
            return raw
        }
    }
}
