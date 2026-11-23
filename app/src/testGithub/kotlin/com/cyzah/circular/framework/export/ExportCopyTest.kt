package com.cyzah.circular.framework.export

import com.cyzah.circular.framework.export.ExportCopyTest.FlushTrackingOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable

class ExportCopyTest {
    @Test
    @Throws(Exception::class)
    fun copiesEntireBinaryPayloadAndCountsBytes() {
        val input = ByteArray(200000)
        for (index in input.indices) {
            input[index] = index.toByte()
        }
        val output = ByteArrayOutputStream()

        val copied = ExportCopy.copy(ByteArrayInputStream(input), output)

        Assert.assertEquals(input.size.toLong(), copied)
        Assert.assertArrayEquals(input, output.toByteArray())
    }

    @Test
    @Throws(Exception::class)
    fun emptyInputFlushesWithoutWriting() {
        val output = FlushTrackingOutputStream()

        Assert.assertEquals(0L, ExportCopy.copy(ByteArrayInputStream(ByteArray(0)), output))
        Assert.assertTrue(output.flushed)
    }

    @Test
    fun outputFailurePropagates() {
        val failing: OutputStream = object : OutputStream() {
            @Throws(IOException::class)
            override fun write(value: Int) {
                throw IOException("destination unavailable")
            }
        }

        val error = Assert.assertThrows<IOException>(
            IOException::class.java,
            ThrowingRunnable { ExportCopy.copy(ByteArrayInputStream(byteArrayOf(1)), failing) })
        Assert.assertEquals("destination unavailable", error.message)
    }

    private class FlushTrackingOutputStream : ByteArrayOutputStream() {
        @JvmField var flushed: Boolean = false

        @Throws(IOException::class)
        override fun flush() {
            flushed = true
            super.flush()
        }
    }
}
