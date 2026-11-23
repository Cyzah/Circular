package com.cyzah.circular.hook.platform

import org.junit.Assert
import org.junit.Test

class ReflectionSupportTest {
    @Test
    fun nullOptionalFieldIsUnavailable() {
        Assert.assertFalse(ReflectionSupport.usableBooleanField(null))
    }

    @Test
    @Throws(Exception::class)
    fun instanceBooleanFieldIsUsable() {
        val field = Holder::class.java.getDeclaredField("enabled")
        Assert.assertTrue(ReflectionSupport.usableBooleanField(field))
    }

    @Test
    @Throws(Exception::class)
    fun staticOrWrongTypeFieldIsNotUsable() {
        Assert.assertFalse(
            ReflectionSupport.usableBooleanField(
                Holder::class.java.getDeclaredField("staticEnabled")
            )
        )
        Assert.assertFalse(
            ReflectionSupport.usableBooleanField(
                Holder::class.java.getDeclaredField("level")
            )
        )
    }

    private class Holder {
        private val enabled = false
        private val level = 0

        companion object {
            private const val staticEnabled = false
        }
    }
}
