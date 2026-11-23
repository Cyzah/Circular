package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder

interface HookInstallContext {
    fun mark(): Int

    fun rollbackTo(installedCount: Int)

    fun failed(
        capability: Compatibility.Capability?,
        stage: HookOrderRecorder.Stage?,
        error: Throwable?
    )

    fun recordFailure(stage: HookOrderRecorder.Stage?, error: Throwable?)
}
