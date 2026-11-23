package com.cyzah.circular.framework.device

import com.cyzah.circular.framework.device.RootShellTest
import com.cyzah.circular.framework.device.RootShellTest.FakeProcess
import com.cyzah.circular.framework.device.RootShellTest.TrackedInputStream
import com.cyzah.circular.framework.device.RootShellTest.TrackedOutputStream
import com.cyzah.circular.framework.device.RootShellTest.WaitMode
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.HashSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import org.junit.Assert
import org.junit.Test

class RootShellTest {
    @Test
    @Throws(Exception::class)
    fun launchIoFailureIsClassifiedAsUnavailableSu() {
        val cause = IOException("su missing")
        val shell = RootShell(object : RootShell.Launcher {
            @Throws(IOException::class)
            override fun start(command: String?): Process {
                Assert.assertEquals("read fixture", command)
                throw cause
            }
        })
        val error: IOException = failure(shell)
        Assert.assertTrue(error.message!!.startsWith("无法调用 su"))
        Assert.assertSame(cause, error.cause)
    }

    @Test
    @Throws(Exception::class)
    fun launchSecurityFailureIsClassifiedAsUnavailableSu() {
        val cause = SecurityException("launch denied")
        val shell = RootShell(object : RootShell.Launcher {
            override fun start(command: String?): Process {
                throw cause
            }
        })
        val error: IOException = failure(shell)
        Assert.assertTrue(error.message!!.startsWith("无法调用 su"))
        Assert.assertSame(cause, error.cause)
    }

    @Test
    @Throws(Exception::class)
    fun successfulCopyReturnsOutputAndReleasesOwnedResources() {
        val process = FakeProcess("fixture", "", 0, WaitMode.EXIT)
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val destination = ByteArrayOutputStream()
            shell(process).copy("read fixture", destination, 100, 1)
            Assert.assertEquals("fixture", destination.toString(StandardCharsets.UTF_8))
            assertCleanup(process, previousWorkers)
            Assert.assertEquals(1, process.destroyCalls.get().toLong())
            Assert.assertEquals(2, process.stdin.closeCalls.get().toLong())
            Assert.assertEquals(2, process.stdout.closeCalls.get().toLong())
            Assert.assertEquals(2, process.stderr.closeCalls.get().toLong())
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun nonzeroExitRetainsExecutionFailureAndStderr() {
        val process = FakeProcess("", "permission denied", 7, WaitMode.EXIT)
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val error: IOException = failure(shell(process))
            Assert.assertTrue(error.message, error.message!!.startsWith("Root 命令失败（7）"))
            Assert.assertTrue(error.message!!.contains("permission denied"))
            assertCleanup(process, previousWorkers)
            Assert.assertEquals(1, process.destroyCalls.get().toLong())
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun stdoutFailureRetainsReadCause() {
        val process = FakeProcess("", "", 0, WaitMode.EXIT)
        val cause = IOException("stdout lost")
        process.stdout.failure = cause
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val error: IOException = failure(shell(process))
            Assert.assertTrue(error.message, error.message!!.startsWith("文件读取失败：stdout lost"))
            Assert.assertSame(cause, error.cause)
            assertCleanup(process, previousWorkers)
            Assert.assertEquals(2, process.destroyCalls.get().toLong())
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun stderrFailureRetainsReadCause() {
        val process = FakeProcess("", "", 0, WaitMode.EXIT)
        val cause = IOException("stderr lost")
        process.stderr.failure = cause
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val error: IOException = failure(shell(process))
            Assert.assertTrue(error.message, error.message!!.startsWith("文件读取失败：stderr lost"))
            Assert.assertSame(cause, error.cause)
            assertCleanup(process, previousWorkers)
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun stdinFailureRetainsOriginalStreamException() {
        val process = FakeProcess("", "", 0, WaitMode.EXIT)
        val cause = IOException("stdin close failed")
        process.stdin.failure = cause
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            Assert.assertSame(cause, failure(shell(process)))
            assertCleanup(process, previousWorkers)
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun timeoutRetainsTimeoutCategoryAndCancelsBothPipeReaders() {
        val process = FakeProcess("", "", 0, WaitMode.TIMEOUT)
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val error: IOException = failure(shell(process))
            Assert.assertTrue(error.message, error.message!!.startsWith("Root 授权或文件读取超时"))
            Assert.assertTrue(error.cause is TimeoutException)
            assertCleanup(process, previousWorkers)
            Assert.assertEquals(1, process.stdout.interruptCalls.get().toLong())
            Assert.assertEquals(1, process.stderr.interruptCalls.get().toLong())
        } finally {
            process.releaseReaders()
        }
    }

    @Test
    @Throws(Exception::class)
    fun interruptionRetainsInterruptStatusAndCancelsBothPipeReaders() {
        val process = FakeProcess("", "", 0, WaitMode.INTERRUPT)
        val previousWorkers: MutableSet<Long?> = workerIds()
        try {
            val error: IOException = failure(shell(process))
            Assert.assertTrue(error.message, error.message!!.startsWith("导出已中断"))
            Assert.assertTrue(error.cause is InterruptedException)
            Assert.assertTrue(Thread.currentThread().isInterrupted())
            // Clear only after checking the caller-visible flag, so test cleanup can await workers.
            Thread.interrupted()
            assertCleanup(process, previousWorkers)
            Assert.assertEquals(1, process.stdout.interruptCalls.get().toLong())
            Assert.assertEquals(1, process.stderr.interruptCalls.get().toLong())
        } finally {
            Thread.interrupted()
            process.releaseReaders()
        }
    }

    private enum class WaitMode {
        EXIT, TIMEOUT, INTERRUPT
    }

    private class FakeProcess(output: String, error: String, @JvmField val exitCode: Int, @JvmField val mode: WaitMode?) : Process() {
        @JvmField val readersStarted: CountDownLatch = CountDownLatch(2)
        @JvmField val release: CountDownLatch = CountDownLatch(1)
        @JvmField val stdout: TrackedInputStream
        @JvmField val stderr: TrackedInputStream
        @JvmField val stdin: TrackedOutputStream = TrackedOutputStream()
        @JvmField val destroyCalls: AtomicInteger = AtomicInteger()

        init {
            this.stdout = TrackedInputStream(output, this)
            this.stderr = TrackedInputStream(error, this)
        }

        override fun getOutputStream(): OutputStream {
            return stdin
        }

        override fun getInputStream(): InputStream {
            return stdout
        }

        override fun getErrorStream(): InputStream {
            return stderr
        }

        @Throws(InterruptedException::class)
        override fun waitFor(): Int {
            return exitCode
        }

        @Throws(InterruptedException::class)
        override fun waitFor(timeout: Long, unit: TimeUnit?): Boolean {
            if (mode != WaitMode.EXIT) {
                if (!readersStarted.await(3L, TimeUnit.SECONDS)) {
                    throw AssertionError("both pipe readers must start")
                }
                if (mode == WaitMode.INTERRUPT) {
                    throw InterruptedException("interrupted fixture")
                }
                return false
            }
            return true
        }

        override fun exitValue(): Int {
            return exitCode
        }

        override fun destroy() {
            destroyCalls.incrementAndGet()
        }

        override fun destroyForcibly(): Process {
            destroy()
            return this
        }

        fun releaseReaders() {
            release.countDown()
        }
    }

    private class TrackedInputStream(value: String, @JvmField val process: FakeProcess) : InputStream() {
        @JvmField val closeCalls: AtomicInteger = AtomicInteger()
        @JvmField val interruptCalls: AtomicInteger = AtomicInteger()
        @JvmField val data: ByteArray
        @JvmField var position: Int = 0
        @JvmField var failure: IOException? = null

        init {
            this.data = value.toByteArray(StandardCharsets.UTF_8)
        }

        @Throws(IOException::class)
        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 255
        }

        @Throws(IOException::class)
        override fun read(output: ByteArray, offset: Int, count: Int): Int {
            process.readersStarted.countDown()
            if (process.mode != WaitMode.EXIT) {
                try {
                    process.release.await()
                } catch (error: InterruptedException) {
                    interruptCalls.incrementAndGet()
                    Thread.currentThread().interrupt()
                    throw IOException("pipe cancelled", error)
                }
            }
            val pendingFailure = failure
            if (pendingFailure != null) throw pendingFailure
            if (position == data.size) return -1
            val available = min(count, data.size - position)
            System.arraycopy(data, position, output, offset, available)
            position += available
            return available
        }

        override fun close() {
            closeCalls.incrementAndGet()
        }
    }

    private class TrackedOutputStream : OutputStream() {
        @JvmField val closeCalls: AtomicInteger = AtomicInteger()
        @JvmField var failure: IOException? = null
        override fun write(value: Int) {}

        @Throws(IOException::class)
        override fun close() {
            closeCalls.incrementAndGet()
            val pendingFailure = failure
            if (pendingFailure != null) throw pendingFailure
        }
    }

    companion object {
        private fun shell(process: FakeProcess): RootShell {
            return RootShell(object : RootShell.Launcher {
                override fun start(command: String?): Process {
                    Assert.assertEquals("read fixture", command)
                    return process
                }
            })
        }

        @Throws(Exception::class)
        private fun failure(shell: RootShell): IOException {
            try {
                shell.copy("read fixture", ByteArrayOutputStream(), 100, 1)
                Assert.fail("expected IOException")
                throw AssertionError()
            } catch (error: IOException) {
                return error
            }
        }

        @Throws(Exception::class)
        private fun assertCleanup(process: FakeProcess, previousWorkers: MutableSet<Long?>) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (hasNewWorkers(previousWorkers) && System.nanoTime() < deadline) {
                Thread.sleep(1L)
            }
            Assert.assertFalse("pipe executor workers must terminate", hasNewWorkers(previousWorkers))
            Assert.assertTrue("process must be destroyed", process.destroyCalls.get() > 0)
            Assert.assertTrue("stdin must be closed", process.stdin.closeCalls.get() > 0)
            Assert.assertTrue("stdout must be closed", process.stdout.closeCalls.get() > 0)
            Assert.assertTrue("stderr must be closed", process.stderr.closeCalls.get() > 0)
        }

        private fun hasNewWorkers(previousWorkers: MutableSet<Long?>): Boolean {
            val current: MutableSet<Long?> = workerIds()
            current.removeAll(previousWorkers)
            return !current.isEmpty()
        }

        private fun workerIds(): MutableSet<Long?> {
            val ids: MutableSet<Long?> = HashSet<Long?>()
            for (thread in Thread.getAllStackTraces().keys) {
                if (thread.isAlive() && "systemui-export-pipe" == thread.getName()) {
                    ids.add(thread.getId())
                }
            }
            return ids
        }
    }
}
