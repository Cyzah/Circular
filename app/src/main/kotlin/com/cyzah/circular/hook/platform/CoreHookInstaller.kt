package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder

class CoreHookInstaller(private val bootstrap: SystemUiHookBootstrap, private val classLoader: ClassLoader?) :
    HookInstallUnit {
    override fun stage(): HookOrderRecorder.Stage {
        return HookOrderRecorder.Stage.CORE_VIEWS
    }

    override fun capability(): Compatibility.Capability {
        return Compatibility.Capability.CORE
    }

    override fun fatalOnFailure(): Boolean {
        return true
    }

    @Throws(Throwable::class)
    override fun install(context: HookInstallContext?) {
        bootstrap.installCoreViews(classLoader)
    }
}
