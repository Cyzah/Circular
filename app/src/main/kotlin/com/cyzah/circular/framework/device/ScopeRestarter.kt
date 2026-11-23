package com.cyzah.circular.framework.device

import com.cyzah.circular.BuildConfig
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

object ScopeRestarter {
    @JvmField val BUSY: AtomicBoolean = AtomicBoolean()
    private val timeoutMessage = if (BuildConfig.DIAGNOSTIC_REPORTS) {
        "root 授权或执行超时，请检查 KernelSU；若状态栏已重载，可复制诊断确认版本。"
    } else {
        "root 授权或执行超时，请检查 KernelSU。"
    }
    private val interruptedMessage = if (BuildConfig.DIAGNOSTIC_REPORTS) {
        "操作被中断，请复制诊断确认是否已重载。"
    } else {
        "操作被中断，请确认状态栏是否已重载。"
    }
    const val COMMAND: String =
        "if [ \"$(id -u)\" != 0 ]; then exit 10; fi; pids=$(/system/bin/pidof com.android.systemui); [ -n \"\$pids\" ] || exit 11; killed=0; for pid in \$pids; do case \"\$pid\" in ''|*[!0-9]*) continue;; esac; name=$(/system/bin/tr '\\000' '\\n' < /proc/\$pid/cmdline | /system/bin/head -n 1); [ \"\$name\" = com.android.systemui ] || continue; kill -9 \"\$pid\" && killed=1; done; [ \"\$killed\" = 1 ] || exit 12; printf 'CIRCULAR_RESTART_SENT\\n'"

    @JvmStatic @Throws(IOException::class)
    fun `lambda$0`(): Process {
        return RootShell.Companion.start(COMMAND, true)
    }

    @JvmStatic fun restart(): String {
        return run(object : Launcher {
            @Throws(IOException::class)  // com.cyzah.circular.framework.device.ScopeRestarter.Launcher
            override fun start(): Process {
                return `lambda$0`()
            }
        }, 30)
    }

    @JvmStatic fun run(launcher: Launcher, i: Int): String {
        val process: Process? = null
        try {
            try {
                try {
                    val processStart = launcher.start()
                    if (!processStart.waitFor(i.toLong(), TimeUnit.SECONDS)) {
                        processStart.destroyForcibly()
                        if (processStart == null) {
                            return timeoutMessage
                        }
                        if (processStart.isAlive()) {
                            processStart.destroyForcibly()
                        }
                        try {
                            processStart.getInputStream().close()
                            processStart.getErrorStream().close()
                            processStart.getOutputStream().close()
                            return timeoutMessage
                        } catch (e: IOException) {
                            return timeoutMessage
                        }
                    }
                    val iExitValue = processStart.exitValue()
                    val str = String(
                        processStart.getInputStream().readNBytes(min(1024, processStart.getInputStream().available())),
                        StandardCharsets.UTF_8
                    )
                    if (iExitValue == 0 && str.contains("CIRCULAR_RESTART_SENT")) {
                        if (processStart == null) {
                            return ""
                        }
                        if (processStart.isAlive()) {
                            processStart.destroyForcibly()
                        }
                        try {
                            processStart.getInputStream().close()
                            processStart.getErrorStream().close()
                            processStart.getOutputStream().close()
                            return ""
                        } catch (e2: IOException) {
                            return ""
                        }
                    }
                    if (iExitValue == 11) {
                        if (processStart == null) {
                            return "未找到 SystemUI 进程，未执行重启。"
                        }
                        if (processStart.isAlive()) {
                            processStart.destroyForcibly()
                        }
                        try {
                            processStart.getInputStream().close()
                            processStart.getErrorStream().close()
                            processStart.getOutputStream().close()
                            return "未找到 SystemUI 进程，未执行重启。"
                        } catch (e3: IOException) {
                            return "未找到 SystemUI 进程，未执行重启。"
                        }
                    }
                    if (iExitValue == 12) {
                        if (processStart == null) {
                            return "未能重启 SystemUI，请检查 root 权限。"
                        }
                        if (processStart.isAlive()) {
                            processStart.destroyForcibly()
                        }
                        try {
                            processStart.getInputStream().close()
                            processStart.getErrorStream().close()
                            processStart.getOutputStream().close()
                            return "未能重启 SystemUI，请检查 root 权限。"
                        } catch (e4: IOException) {
                            return "未能重启 SystemUI，请检查 root 权限。"
                        }
                    }
                    if (processStart == null) {
                        return "未获得 root 权限或执行失败，请在 KernelSU 授权本应用后重试。"
                    }
                    if (processStart.isAlive()) {
                        processStart.destroyForcibly()
                    }
                    try {
                        processStart.getInputStream().close()
                        processStart.getErrorStream().close()
                        processStart.getOutputStream().close()
                        return "未获得 root 权限或执行失败，请在 KernelSU 授权本应用后重试。"
                    } catch (e5: IOException) {
                        return "未获得 root 权限或执行失败，请在 KernelSU 授权本应用后重试。"
                    }
                } catch (th: Throwable) {
                    if (0 != 0) {
                        if (process!!.isAlive()) {
                            process.destroyForcibly()
                        }
                        try {
                            process.getInputStream().close()
                            process.getErrorStream().close()
                            process.getOutputStream().close()
                        } catch (e6: IOException) {
                        }
                    }
                    throw th
                }
            } catch (e7: InterruptedException) {
                Thread.currentThread().interrupt()
                if (0 == 0) {
                    return interruptedMessage
                }
                if (process!!.isAlive()) {
                    process.destroyForcibly()
                }
                try {
                    process.getInputStream().close()
                    process.getErrorStream().close()
                    process.getOutputStream().close()
                    return interruptedMessage
                } catch (e8: IOException) {
                    return interruptedMessage
                }
            }
        } catch (e9: IOException) {
            if (0 == 0) {
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            }
            if (process!!.isAlive()) {
                process.destroyForcibly()
            }
            try {
                process.getInputStream().close()
                process.getErrorStream().close()
                process.getOutputStream().close()
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            } catch (e10: IOException) {
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            }
        } catch (e9: SecurityException) {
            if (0 == 0) {
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            }
            if (process!!.isAlive()) {
                process.destroyForcibly()
            }
            try {
                process.getInputStream().close()
                process.getErrorStream().close()
                process.getOutputStream().close()
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            } catch (e10: IOException) {
                return "无法调用 su，请确认已安装 root 管理器，并允许本应用申请 root。"
            }
        }
    }

    interface Launcher {
        @Throws(IOException::class)
        fun start(): Process
    }
}
