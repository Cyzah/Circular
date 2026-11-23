package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class TechnologyTextRulesTest {
    @Test
    fun appearanceExampleSeparatesBothFullWidthSemicolonRules() {
        val rules: TechnologyText = TechnologyText.Companion.parse("5G=5G+；LTE=4G")
        Assert.assertEquals("5G+", rules.apply("5G"))
        Assert.assertEquals("4G", rules.apply("LTE"))
    }

    @Test
    fun allDocumentedSeparatorsCanBeMixed() {
        val rules: TechnologyText = TechnologyText.Companion.parse("5G=A;LTE=B，NR=C,4G=D；3G=E\n2G=F\rEDGE=G")
        val examples = arrayOf<Array<String?>?>(
            arrayOf<String?>("5G", "A"),
            arrayOf<String?>("LTE", "B"),
            arrayOf<String?>("NR", "C"),
            arrayOf<String?>("4G", "D"),
            arrayOf<String?>("3G", "E"),
            arrayOf<String?>("2G", "F"),
            arrayOf<String?>("EDGE", "G")
        )
        for (example in examples) {
            Assert.assertEquals(example!![1], rules.apply(example[0]))
        }
    }

    @Test
    fun exactMatchStillWinsOverAnEarlierWildcard() {
        val rules: TechnologyText = TechnologyText.Companion.parse("*=fallback; 5g = exact ;5G=later")
        Assert.assertEquals("exact", rules.apply("5G"))
        Assert.assertEquals("exact", rules.apply("5g"))
        Assert.assertEquals("fallback", rules.apply("LTE"))
    }

    @Test
    fun firstWildcardStillWinsAndBareLabelStillMeansWildcard() {
        Assert.assertEquals("first", TechnologyText.Companion.parse("*=first;*=second").apply("LTE"))
        Assert.assertEquals("fixed", TechnologyText.Companion.parse(" fixed ").apply("LTE"))
    }

    @Test
    fun emptyReplacementStillHidesOnlyItsMatchingLabel() {
        val rules: TechnologyText = TechnologyText.Companion.parse("5G=")
        Assert.assertEquals("", rules.apply("5G"))
        Assert.assertEquals("LTE", rules.apply("LTE"))
        Assert.assertFalse(rules.isEmpty)
    }

    @Test
    fun emptyAndMalformedRulesKeepUnmatchedLabels() {
        Assert.assertTrue(TechnologyText.Companion.parse(null).isEmpty)
        Assert.assertTrue(TechnologyText.Companion.parse(" ; ,\n\r").isEmpty)
        Assert.assertEquals("5G", TechnologyText.Companion.parse("=ignored").apply("5G"))
        Assert.assertEquals("5G", TechnologyText.Companion.parse("").apply("5G"))
        Assert.assertEquals("", TechnologyText.Companion.parse("*=fixed").apply(null))
    }

    @Test
    fun replacementKeepsAdditionalEqualsCharacters() {
        Assert.assertEquals("A=B", TechnologyText.Companion.parse("5G=A=B").apply("5G"))
    }

    @Test
    fun sanitizingPreservesStoredSeparatorsAndTheExistingLengthLimit() {
        Assert.assertEquals("5G=5G+；LTE=4G", TechnologyText.Companion.sanitize(" 5G=5G+；LTE=4G "))
        Assert.assertEquals("", TechnologyText.Companion.sanitize(null))
        val value = StringBuilder("5G=")
        while (value.length < 401) value.append('X')
        val sanitized: String = TechnologyText.Companion.sanitize(value.toString())
        Assert.assertEquals(400, sanitized.length.toLong())
        Assert.assertEquals(397, checkNotNull(TechnologyText.Companion.parse(sanitized).apply("5G")).length.toLong())
    }
}
