package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.NativeIconPackingTest
import java.util.Arrays
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class NativeIconPackingTest {
    private class Icon(@JvmField val slot: String?, @JvmField val width: Float, @JvmField var right: Float, @JvmField var visible: Boolean) {
        @JvmField var correction: Float = 0f
        @JvmField var suppressed: Boolean = false
        @JvmField var opacity: Float = 1f
    }

    @Test
    fun firstAssemblyFrameClosesTheBlockedNfcSlotInBothCopies() {
        val speed5 = Icon("network_speed", 64f, 826f, true)
        val nfc5 = Icon("nfc", 0f, 0f, false)
        val speed6 = Icon("network_speed", 64f, 767f, true)
        val nfc6 = Icon("nfc", 59f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(speed5, nfc5), Arrays.asList<Icon?>(speed6, nfc6), 826f, 0f)
        Assert.assertEquals(826f, speed5.right + speed5.correction, 0f)
        Assert.assertEquals(826f, speed6.right + speed6.correction, 0f)
        Assert.assertTrue(nfc6.suppressed)
        packing.release()
        Assert.assertEquals(767f, speed6.right + speed6.correction, 0f)
        Assert.assertFalse(nfc6.suppressed)
    }

    @Test
    fun remainingIconsKeepOrderWithoutGapsEvenWhenSpeedIsDisabled() {
        val mute = Icon("volume", 20f, 600f, true)
        val removed = Icon("nfc", 59f, 659f, false)
        val alarm = Icon("alarm_clock", 25f, 700f, true)
        val bluetooth = Icon("bluetooth", 18f, 750f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(
            Arrays.asList<Icon?>(mute, removed, alarm, bluetooth),
            mutableListOf<Icon?>(), 826f, 0f
        )
        Assert.assertEquals(826f, bluetooth.right + bluetooth.correction, 0f)
        Assert.assertEquals(808f, alarm.right + alarm.correction, 0f)
        Assert.assertEquals(783f, mute.right + mute.correction, 0f)
        Assert.assertEquals(0f, removed.correction, 0f)
    }

    @Test
    fun repeatedFramesUseNewBoundaryWithoutAccumulatingOffsets() {
        val left = Icon("volume", 20f, 600f, true)
        val right = Icon("alarm_clock", 25f, 700f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(left, right), mutableListOf<Icon?>(), 826f, 2f)
        packing.apply(Arrays.asList<Icon?>(left, right), mutableListOf<Icon?>(), 900f, 2f)
        Assert.assertEquals(900f, right.right + right.correction, 0f)
        Assert.assertEquals(873f, left.right + left.correction, 0f)
        right.visible = false
        packing.apply(Arrays.asList<Icon?>(left, right), mutableListOf<Icon?>(), 900f, 2f)
        Assert.assertEquals(900f, left.right + left.correction, 0f)
        Assert.assertEquals(0f, right.correction, 0f)
    }

    @Test
    fun replacedCopyReleasesOldTranslationsAndVisibility() {
        val target = Icon("volume", 20f, 600f, true)
        val copy = Icon("volume", 20f, 650f, true)
        val extra = Icon("nfc", 59f, 709f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(target), Arrays.asList<Icon?>(copy, extra), 826f, 0f)
        packing.apply(Arrays.asList<Icon?>(target), mutableListOf<Icon?>(), 850f, 0f)
        Assert.assertEquals(0f, copy.correction, 0f)
        Assert.assertFalse(extra.suppressed)
    }

    @Test
    fun largerCopyDoesNotOverlapItsLeftNeighbour() {
        val left = Icon("volume", 20f, 600f, true)
        val right = Icon("alarm_clock", 25f, 700f, true)
        val wideRight = Icon("alarm_clock", 30f, 750f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(left, right), Arrays.asList<Icon?>(wideRight), 826f, 0f)
        Assert.assertEquals(796f, left.right + left.correction, 0f)
        Assert.assertEquals(826f, wideRight.right + wideRight.correction, 0f)
    }

    @Test
    fun expandedOnlyIconAppearsAfterItsFullWidthIsReserved() {
        val speed = Icon("network_speed", 64f, 826f, true)
        val copy = Icon("network_speed", 64f, 767f, true)
        val nfc = Icon("nfc", 59f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(speed), Arrays.asList<Icon?>(copy, nfc), 826f, 0f, 0.5f, 0f)
        Assert.assertEquals(796.5f, speed.right + speed.correction, 0f)
        Assert.assertEquals(0f, nfc.opacity, 0f)
        packing.apply(Arrays.asList<Icon?>(speed), Arrays.asList<Icon?>(copy, nfc), 826f, 0f, 1f, 0.5f)
        Assert.assertEquals(767f, speed.right + speed.correction, 0f)
        Assert.assertEquals(826f, nfc.right + nfc.correction, 0f)
        Assert.assertEquals(0.5f, nfc.opacity, 0f)
        packing.release()
        Assert.assertEquals(767f, copy.right + copy.correction, 0f)
        Assert.assertEquals(1f, nfc.opacity, 0f)
    }

    @Test
    fun newlySharedIconRestoresOpacityAndKeepsItsNativeOrder() {
        val speed = Icon("network_speed", 64f, 826f, true)
        val nfc = Icon("nfc", 59f, 826f, false)
        val copy = Icon("network_speed", 64f, 767f, true)
        val nfcCopy = Icon("nfc", 59f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(speed, nfc), Arrays.asList<Icon?>(copy, nfcCopy), 826f, 0f)
        Assert.assertEquals(0f, nfcCopy.opacity, 0f)
        nfc.visible = true
        packing.apply(Arrays.asList<Icon?>(speed, nfc), Arrays.asList<Icon?>(copy, nfcCopy), 826f, 0f)
        Assert.assertEquals(1f, nfcCopy.opacity, 0f)
        Assert.assertEquals(826f, nfc.right + nfc.correction, 0f)
        Assert.assertEquals(767f, speed.right + speed.correction, 0f)
    }

    @Test
    fun disappearingAllRetainedIconsDoesNotLeaveSourceOnlyIconsVisible() {
        val nfc = Icon("nfc", 59f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(mutableListOf<Icon?>(), Arrays.asList<Icon?>(nfc), 826f, 0f)
        Assert.assertEquals(0f, nfc.opacity, 0f)
        packing.release()
        Assert.assertEquals(1f, nfc.opacity, 0f)
    }

    @Test
    fun collapsedOnlySlotDoesNotLeaveAHoleAtTheExpandedHandoff() {
        val speed = Icon("network_speed", 64f, 806f, true)
        val alarm = Icon("alarm_clock", 20f, 826f, true)
        val copy = Icon("network_speed", 64f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(speed, alarm), Arrays.asList<Icon?>(copy), 826f, 0f, 1f, 1f)
        Assert.assertEquals(826f, copy.right + copy.correction, 0f)
        Assert.assertEquals(0f, alarm.opacity, 0f)
    }

    @Test
    fun collapsedOnlyIconFadesBeforeItsSpaceShrinks() {
        val speed = Icon("network_speed", 64f, 806f, true)
        val alarm = Icon("alarm_clock", 20f, 826f, true)
        val copy = Icon("network_speed", 64f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        packing.apply(Arrays.asList<Icon?>(speed, alarm), Arrays.asList<Icon?>(copy), 826f, 0f, 0.1f, 0f)
        Assert.assertTrue(alarm.opacity > 0 && alarm.opacity < 1)
        Assert.assertEquals(806f, copy.right + copy.correction, 0f)
        packing.apply(Arrays.asList<Icon?>(speed, alarm), Arrays.asList<Icon?>(copy), 826f, 0f, 0.2f, 0f)
        Assert.assertEquals(0f, alarm.opacity, 0f)
        Assert.assertEquals(806f, copy.right + copy.correction, 0f)
        packing.apply(Arrays.asList<Icon?>(speed, alarm), Arrays.asList<Icon?>(copy), 826f, 0f, 0.6f, 0f)
        Assert.assertEquals(0f, alarm.opacity, 0f)
        Assert.assertEquals(816f, copy.right + copy.correction, 0.0001f)
    }

    @Test
    fun collapsedOnlyOpacityHasNoFirstFrameJump() {
        val alarm = Icon("alarm_clock", 20f, 826f, true)
        val packing = NativeIconPacking<Icon?>(NODES)
        var previous = 1f
        for (frame in 0..100) {
            packing.apply(
                Arrays.asList<Icon?>(alarm), mutableListOf<Icon?>(), 826f, 0f,
                frame / 1000.0f, 0f
            )
            Assert.assertTrue(previous - alarm.opacity < 0.02f)
            Assert.assertTrue(alarm.opacity <= previous)
            previous = alarm.opacity
        }
    }

    companion object {
        private val NODES: NativeIconPacking.Nodes<Icon?> = object : NativeIconPacking.Nodes<Icon?> {
            override fun slot(icon: Icon?): String? {
                return icon!!.slot
            }

            override fun visible(icon: Icon?): Boolean {
                return icon!!.visible
            }

            override fun width(icon: Icon?): Float {
                return icon!!.width
            }

            override fun place(icon: Icon?, right: Float) {
                icon!!.correction = right - icon.right
            }

            override fun opacity(icon: Icon?, value: Float) {
                icon!!.opacity = value
                icon.suppressed = value == 0f
            }

            override fun release(icon: Icon?) {
                icon!!.correction = 0f
                icon.suppressed = false
                icon.opacity = 1f
            }
        }
    }
}
