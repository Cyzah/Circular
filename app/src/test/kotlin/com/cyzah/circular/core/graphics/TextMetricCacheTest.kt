package com.cyzah.circular.core.graphics

import org.junit.Assert
import org.junit.Test

class TextMetricCacheTest {
    @Test
    fun cacheMatchesOnlyWhenEveryMetricInputIsUnchanged() {
        val typeface = Any()
        val cache = TextMetricCache()

        Assert.assertFalse(cache.matches("5G", 100, 100, typeface))

        cache.store(
            "5G",
            100,
            100,
            typeface,
            42.0f,
            33.0f,
            4,
            34
        )

        Assert.assertTrue(cache.matches("5G", 100, 100, typeface))
        Assert.assertFalse(cache.matches("5GA", 100, 100, typeface))
        Assert.assertFalse(cache.matches("5G", 110, 100, typeface))
        Assert.assertFalse(cache.matches("5G", 100, 120, typeface))
        Assert.assertFalse(cache.matches("5G", 100, 100, Any()))
        Assert.assertEquals(42.0f, cache.width(), 0.0f)
        Assert.assertEquals(33.0f, cache.textSize(), 0.0f)
        Assert.assertEquals(4, cache.top().toLong())
        Assert.assertEquals(34, cache.bottom().toLong())
    }
}
