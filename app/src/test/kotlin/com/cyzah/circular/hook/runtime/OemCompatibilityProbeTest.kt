package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.platform.Compatibility
import com.cyzah.circular.hook.platform.OemCompatibility
import com.cyzah.circular.hook.runtime.OemCompatibilityProbeTest
import java.util.HashSet
import org.junit.Assert
import org.junit.Test

class OemCompatibilityProbeTest {
    @Test
    fun detectsOplusContract() {
        val available: MutableSet<String> = members(
            OemCompatibility.COMMON,
            OemCompatibility.OPLUS_MEMBERS
        )

        Assert.assertEquals(
            OemCompatibility.OPLUS,
            OemCompatibility.probe(Compatibility.Inspector { requirement: Compatibility.Requirement? ->
                require(
                    available.contains(requirement!!.raw())
                )
            })
        )
    }

    @Test
    fun fallsBackToSamsungContract() {
        val available: MutableSet<String> = members(
            OemCompatibility.COMMON,
            OemCompatibility.SAMSUNG_MEMBERS
        )

        Assert.assertEquals(
            OemCompatibility.SAMSUNG,
            OemCompatibility.probe(Compatibility.Inspector { requirement: Compatibility.Requirement? ->
                require(
                    available.contains(requirement!!.raw())
                )
            })
        )
    }

    @Test
    fun rejectsMissingCommonContract() {
        val available: MutableSet<String> = members(OemCompatibility.OPLUS_MEMBERS)

        Assert.assertEquals(
            "",
            OemCompatibility.probe(Compatibility.Inspector { requirement: Compatibility.Requirement? ->
                require(
                    available.contains(requirement!!.raw())
                )
            })
        )
    }

    companion object {
        private fun members(vararg groups: Array<String>): MutableSet<String> {
            val result: MutableSet<String> = HashSet()
            for (group in groups) {
                result.addAll(group.asList())
            }
            return result
        }

        @Throws(Exception::class)
        private fun require(condition: Boolean) {
            if (!condition) {
                throw NoSuchFieldException("fixture member unavailable")
            }
        }
    }
}
