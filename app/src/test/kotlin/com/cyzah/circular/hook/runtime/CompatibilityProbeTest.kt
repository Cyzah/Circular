package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.platform.Compatibility
import com.cyzah.circular.hook.runtime.CompatibilityProbeTest
import java.util.Arrays
import java.util.HashSet
import org.junit.Assert
import org.junit.Test

class CompatibilityProbeTest {
    @Test
    fun completeModernContractEnablesCoreCapabilities() {
        val result = Compatibility.probe(Compatibility.Inspector { requirement: Compatibility.Requirement? -> })

        Assert.assertTrue(result.isAvailable(Compatibility.Capability.CORE))
        Assert.assertTrue(result.isAvailable(Compatibility.Capability.MOTION))
        Assert.assertTrue(result.isAvailable(Compatibility.Capability.KEYGUARD))
        Assert.assertFalse(result.legacyKeyguard)
        Assert.assertFalse(result.legacyAod)
        Assert.assertEquals(Compatibility.PROFILE, result.profile())
    }

    @Test
    fun unavailableOptionalMemberKeepsCoreAvailable() {
        val available: MutableSet<String?> = HashSet<String?>(Arrays.asList<String?>(*Compatibility.CONTRACT))
        available.remove(HANDOFF_METHOD)

        val result = Compatibility.probe(
            Compatibility.Inspector { requirement: Compatibility.Requirement? -> require(available.contains(requirement!!.raw())) })

        Assert.assertTrue(result.isAvailable(Compatibility.Capability.CORE))
        Assert.assertFalse(result.isAvailable(Compatibility.Capability.HANDOFF))
        Assert.assertEquals(
            Compatibility.Availability.UNAVAILABLE,
            result.state(Compatibility.Capability.HANDOFF).status()
        )
        Assert.assertTrue(result.details().contains("handoff"))
    }

    @Test
    fun legacyKeyguardAndAodContractSelectsLegacyProfile() {
        val legacyMembers: MutableSet<String?> = HashSet<String?>()
        legacyMembers.addAll(Arrays.asList<String?>(*Compatibility.LEGACY_KEYGUARD))
        legacyMembers.addAll(Arrays.asList<String?>(*Compatibility.LEGACY_AOD))

        val result = Compatibility.probe(Compatibility.Inspector { requirement: Compatibility.Requirement? ->
            val keyguardGroup = "keyguard" == requirement!!.group()
                    || "keyguardEvents" == requirement.group()
                    || "aod" == requirement.group()
            if (keyguardGroup && !legacyMembers.contains(requirement.raw())) {
                throw NoSuchFieldException(requirement.raw())
            }
        })

        Assert.assertTrue(result.isAvailable(Compatibility.Capability.CORE))
        Assert.assertTrue(result.isAvailable(Compatibility.Capability.KEYGUARD))
        Assert.assertTrue(result.isAvailable(Compatibility.Capability.AOD_EVENTS))
        Assert.assertTrue(result.legacyKeyguard)
        Assert.assertTrue(result.legacyAod)
        Assert.assertEquals("miui-modern-a16-v2", result.profile())
    }

    companion object {
        private const val HANDOFF_METHOD =
            "handoff|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|enableAnimation|()Z"

        @Throws(Exception::class)
        private fun require(condition: Boolean) {
            if (!condition) {
                throw NoSuchMethodException("fixture member unavailable")
            }
        }
    }
}
