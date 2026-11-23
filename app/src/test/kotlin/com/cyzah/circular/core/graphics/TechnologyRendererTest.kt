package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class TechnologyRendererTest {
    @Test
    fun fiveGaKeepsItsNaturalBaseWidthByDefault() {
        Assert.assertEquals("5G", TechnologyRenderer.normalBase("5GA"))
        Assert.assertEquals("", TechnologyRenderer.condensedBase("5GA"))
        Assert.assertEquals("A", TechnologyRenderer.Companion.suffix("5GA"))
    }

    @Test
    fun fiveGaLetterWidthsStayNaturalUnlessCompressionIsRequested() {
        Assert.assertEquals(1.0f, TechnologyText.baseWidthScale("5G", "A"), 0.0f)
        Assert.assertEquals(1.0f, TechnologyText.suffixWidthScale("5G", "A"), 0.0f)
    }

    @Test
    fun explicitCompressionPreservesTheFiveGaLetterTreatment() {
        Assert.assertEquals("5", TechnologyRenderer.Companion.normalBase("5GA", true))
        Assert.assertEquals("G", TechnologyRenderer.Companion.condensedBase("5GA", true))
        Assert.assertEquals(0.8f, TechnologyText.Companion.baseWidthScale("5G", "A", true), 0.0f)
        Assert.assertEquals(0.8f, TechnologyText.Companion.suffixWidthScale("5G", "A", true), 0.0f)
    }

    @Test
    fun disablingCompressionRestoresTheFullFiveGaBase() {
        Assert.assertEquals("5G", TechnologyRenderer.Companion.normalBase("5GA", false))
        Assert.assertEquals("", TechnologyRenderer.Companion.condensedBase("5GA", false))
        Assert.assertEquals(1.0f, TechnologyText.Companion.baseWidthScale("5G", "A", false), 0.0f)
        Assert.assertEquals(1.0f, TechnologyText.Companion.suffixWidthScale("5G", "A", false), 0.0f)
    }

    @Test
    fun explicitCompressionDoesNotCondenseOtherLabels() {
        val examples = arrayOf<Array<String?>?>(
            arrayOf<String?>("5G+", "5G"), arrayOf<String?>("4GA", "4G"), arrayOf<String?>("5Ga", "5G"),
            arrayOf<String?>("5GAA", "5G"), arrayOf<String?>("LTE", "LTE")
        )
        for (example in examples) {
            Assert.assertEquals(example!![1], TechnologyRenderer.Companion.normalBase(example[0], true))
            Assert.assertEquals("", TechnologyRenderer.Companion.condensedBase(example[0], true))
        }
        Assert.assertEquals(1.0f, TechnologyText.Companion.baseWidthScale("5G", "+", true), 0.0f)
        Assert.assertEquals(1.0f, TechnologyText.Companion.suffixWidthScale("5G", "+", true), 0.0f)
        Assert.assertEquals(1.0f, TechnologyText.Companion.baseWidthScale("4G", "A", true), 0.0f)
        Assert.assertEquals(1.0f, TechnologyText.Companion.suffixWidthScale("4G", "A", true), 0.0f)
    }

    @Test
    fun ordinarySuffixKeepsFullBase() {
        Assert.assertEquals("5G", TechnologyRenderer.normalBase("5G+"))
        Assert.assertEquals("", TechnologyRenderer.condensedBase("5G+"))
        Assert.assertEquals("+", TechnologyRenderer.Companion.suffix("5G+"))
    }

    @Test
    fun longSuffixIsNotSeparated() {
        Assert.assertEquals("5GXXX", TechnologyRenderer.normalBase("5GXXX"))
        Assert.assertEquals("", TechnologyRenderer.condensedBase("5GXXX"))
        Assert.assertEquals("", TechnologyRenderer.Companion.suffix("5GXXX"))
    }
}
