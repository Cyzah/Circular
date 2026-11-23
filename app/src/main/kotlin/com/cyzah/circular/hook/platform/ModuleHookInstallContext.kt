package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder
import com.cyzah.circular.hook.runtime.ModuleMain

class ModuleHookInstallContext(private val module: ModuleMain) : HookInstallContext {
    override fun mark(): Int {
        return module.installed.size
    }

    override fun rollbackTo(installedCount: Int) {
        module.rollback(installedCount)
    }

    override fun failed(
        capability: Compatibility.Capability?,
        stage: HookOrderRecorder.Stage?,
        error: Throwable?
    ) {
        checkNotNull(module.compatibility).markFailed(
            checkNotNull(capability),
            "install-" + checkNotNull(stage).name.lowercase(),
            if (error == null) "unavailable" else error.toString()
        )
    }

    override fun recordFailure(stage: HookOrderRecorder.Stage?, error: Throwable?) {
        module.failures.record(
            "hook",
            checkNotNull(stage).name.lowercase(),
            error
        )
    }
}
