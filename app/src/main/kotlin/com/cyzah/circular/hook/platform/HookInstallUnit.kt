package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder

interface HookInstallUnit {
    fun stage(): HookOrderRecorder.Stage?

    fun capability(): Compatibility.Capability?

    fun fatalOnFailure(): Boolean

    @Throws(Throwable::class)
    fun install(context: HookInstallContext?)
}
