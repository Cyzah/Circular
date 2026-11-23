package com.cyzah.circular.hook.platform

import com.cyzah.circular.core.diagnostics.HookOrderRecorder
import java.util.Arrays

object OptionalHookInstallers {
    @JvmStatic fun all(
        bootstrap: SystemUiHookBootstrap,
        classLoader: ClassLoader?
    ): MutableList<HookInstallUnit> {
        return Arrays.asList<HookInstallUnit>(
            unit(
                HookOrderRecorder.Stage.POWER,
                Compatibility.Capability.BATTERY_POWER,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installPower()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.PERFORMANCE,
                Compatibility.Capability.BATTERY_POWER,
                true,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installPerformance()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.PRIVACY,
                Compatibility.Capability.PRIVACY,
                true,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installPrivacy()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.MOTION,
                Compatibility.Capability.MOTION,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installMotion(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.ALIGNMENT,
                Compatibility.Capability.MOTION_ALIGNMENT,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installAlignment(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.NATIVE_SIGNAL,
                Compatibility.Capability.NATIVE_SIGNAL,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installNativeSignal(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.SIGNAL_RESOURCES,
                Compatibility.Capability.NATIVE_SIGNAL,
                true,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installSignalResources(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.SIGNAL_SNAPSHOT,
                Compatibility.Capability.NATIVE_SIGNAL,
                false,
                object : Action {
                    override fun run() {
                        bootstrap.installSignalSnapshot(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.VIEW_SIGNAL,
                Compatibility.Capability.NATIVE_SIGNAL,
                false,
                object : Action {
                    override fun run() {
                        bootstrap.installViewSignal()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.HANDOFF,
                Compatibility.Capability.HANDOFF,
                true,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installHandoff()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.CHARGING_ISLAND,
                Compatibility.Capability.CHARGING_ISLAND,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installChargingIsland()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.KEYGUARD,
                Compatibility.Capability.KEYGUARD,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installKeyguard(classLoader)
                    }
                }),
            unit(
                HookOrderRecorder.Stage.HOME_PRIVACY,
                Compatibility.Capability.PRIVACY,
                false,
                object : Action {
                    @Throws(Throwable::class)
                    override fun run() {
                        bootstrap.installHomePrivacy()
                    }
                }),
            unit(
                HookOrderRecorder.Stage.DEOPTIMIZE,
                Compatibility.Capability.CORE,
                true,
                object : Action {
                    @Throws(Exception::class)
                    override fun run() {
                        bootstrap.installDeoptimize()
                    }
                })
        )
    }

    private fun unit(
        stage: HookOrderRecorder.Stage,
        capability: Compatibility.Capability,
        fatal: Boolean,
        action: Action
    ): HookInstallUnit {
        return object : HookInstallUnit {
            override fun stage(): HookOrderRecorder.Stage {
                return stage
            }

            override fun capability(): Compatibility.Capability {
                return capability
            }

            override fun fatalOnFailure(): Boolean {
                return fatal
            }

            @Throws(Throwable::class)
            override fun install(context: HookInstallContext?) {
                action.run()
            }
        }
    }

    private interface Action {
        @Throws(Throwable::class)
        fun run()
    }
}
