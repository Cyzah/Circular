package com.cyzah.circular.framework.device

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.min

class RootShell @JvmOverloads constructor(
    private val launcher: Launcher = object : Launcher {
        @Throws(IOException::class)  // com.cyzah.circular.framework.device.RootShell.Launcher
        override fun start(str: String?): Process {
            return `lambda$0`(str)
        }
    }
) {
    interface Launcher {
        @Throws(IOException::class)
        fun start(str: String?): Process
    }

    @Throws(IOException::class)
    fun text(str: String?): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        copy(str, byteArrayOutputStream, 4194304L, 45)
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    fun copy(str: String?, outputStream: OutputStream, j: Long, i: Int) {
        val processStart: Process
        try {
            processStart = this.launcher.start(str)
        } catch (error: IOException) {
            throw IOException("无法调用 su，请检查 Root 管理器及本应用授权。", error)
        } catch (error: SecurityException) {
            throw IOException("无法调用 su，请检查 Root 管理器及本应用授权。", error)
        }
        var executorServiceNewFixedThreadPool: ExecutorService? = null
        var futureSubmit: Future<Long?>? = null
        var futureSubmit2: Future<String>? = null
        try {
            executorServiceNewFixedThreadPool = Executors.newFixedThreadPool(2, object : ThreadFactory {
                // java.util.concurrent.ThreadFactory
                override fun newThread(runnable: Runnable?): Thread {
                    return `lambda$1`(runnable)
                }
            })
            val jNanoTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(i.toLong())
            futureSubmit = executorServiceNewFixedThreadPool.submit<Long?>(object : Callable<Long?> {
                @Throws(Exception::class)  // java.util.concurrent.Callable
                override fun call(): Long {
                    return `lambda$2`(processStart, j, outputStream)
                }
            })
            futureSubmit2 = executorServiceNewFixedThreadPool.submit<String>(object : Callable<String> {
                @Throws(Exception::class)  // java.util.concurrent.Callable
                override fun call(): String {
                    return `lambda$3`(processStart)
                }
            })
            processStart.getOutputStream().close()
            if (!processStart.waitFor(max(1L, jNanoTime - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                throw TimeoutException()
            }
            futureSubmit.get(max(1L, jNanoTime - System.nanoTime()), TimeUnit.NANOSECONDS)
            val str2 = futureSubmit2.get(max(1L, jNanoTime - System.nanoTime()), TimeUnit.NANOSECONDS)
            if (processStart.exitValue() != 0) {
                throw IOException("Root 命令失败（" + processStart.exitValue() + "），请检查授权或文件读取权限。" + (if (str2.isEmpty()) "" else " " + str2))
            }
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("导出已中断。", error)
        } catch (error: ExecutionException) {
            throw IOException("文件读取失败：" + error.cause!!.message, error.cause)
        } catch (error: TimeoutException) {
            throw IOException("Root 授权或文件读取超时，请授权后重试。", error)
        } finally {
            processStart.destroyForcibly()
            close(processStart.getOutputStream())
            close(processStart.getInputStream())
            close(processStart.getErrorStream())
            if (futureSubmit != null) futureSubmit.cancel(true)
            if (futureSubmit2 != null) futureSubmit2.cancel(true)
            if (executorServiceNewFixedThreadPool != null) {
                executorServiceNewFixedThreadPool.shutdownNow()
                try {
                    executorServiceNewFixedThreadPool.awaitTermination(2L, TimeUnit.SECONDS)
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    companion object {
        @JvmStatic @Throws(IOException::class)
        fun start(str: String?, z: Boolean): Process {
            return ProcessBuilder("su", "-c", str).redirectErrorStream(z).start()
        }

        @JvmStatic @Throws(IOException::class)
        fun `lambda$0`(str: String?): Process {
            return start(str, false)
        }

        @JvmStatic fun `lambda$1`(runnable: Runnable?): Thread {
            val thread = Thread(runnable, "systemui-export-pipe")
            thread.setDaemon(true)
            return thread
        }

        /**
         * 读取子进程 stdout 并写入目标流，超过上限则中断并销毁进程。
         */
        @JvmStatic @Throws(Exception::class)
        fun `lambda$2`(process: Process, j: Long, outputStream: OutputStream): Long {
            val bArr = ByteArray(65536)
            var total: Long = 0
            try {
                process.getInputStream().use { inputStream ->
                    while (true) {
                        val read = inputStream.read(bArr)
                        if (read == -1) {
                            return total
                        }
                        total += read.toLong()
                        if (total > j) {
                            throw IOException("读取数据超过导出大小限制。")
                        }
                        outputStream.write(bArr, 0, read)
                    }
                }
            } catch (e: Exception) {
                process.destroyForcibly()
                throw e
            } catch (e: Error) {
                process.destroyForcibly()
                throw e
            }
        }

        @JvmStatic @Throws(Exception::class)
        fun `lambda$3`(process: Process): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val bArr = ByteArray(4096)
            try {
                val errorStream = process.getErrorStream()
                while (true) {
                    try {
                        val i = errorStream.read(bArr)
                        if (i == -1) {
                            break
                        }
                        val iMin = min(i, 4096 - byteArrayOutputStream.size())
                        if (iMin > 0) {
                            byteArrayOutputStream.write(bArr, 0, iMin)
                        }
                    } finally {
                    }
                }
                if (errorStream != null) {
                    errorStream.close()
                }
                return byteArrayOutputStream.toString(StandardCharsets.UTF_8).trim { it <= ' ' }
            } finally {
            }
        }

        private fun close(closeable: Closeable) {
            try {
                closeable.close()
            } catch (e: IOException) {
            }
        }
    }
}
