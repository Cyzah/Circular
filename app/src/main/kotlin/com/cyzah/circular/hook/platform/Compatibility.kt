package com.cyzah.circular.hook.platform

import android.widget.TextView
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayList
import java.util.EnumMap
import java.util.List
import java.util.function.Predicate

object Compatibility {
    const val KNOWN_SHA: String = "c83c9d5a2c6ad2d4bf9c53dc401ae8696d213497833098af59cb00e7b5763ec3"
    const val PROFILE: String = "miui-modern-v1"
    @JvmField val CONTRACT: Array<String> = arrayOf<String>(
        "power|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mPowerSave|Z",
        "power|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onPowerSaveChanged|(Z)V",
        "anchor|subtype|com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons||android.view.ViewGroup",
        "anchor|method|com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons|onFinishInflate|()V",
        "anchor|subtype|com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBarIcon||android.view.ViewGroup",
        "anchor|method|com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBarIcon|onFinishInflate|()V",
        "island|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateIslandChanged|(ZZ)V",
        "island|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mStoreIsAddBatteryIsland|Z",
        "island|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mIsAddBatteryIsland|Z",
        "island|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onBatteryStyleChanged|(I)V",
        "island|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateControlCenterExpandState|(Z)V",
        "island|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateIslandShowing|(ZZZ)V",
        "handoff|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|enableAnimation|()Z",
        "handoffHints|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|isNeedAnimateIsland|()Z",
        "handoffHints|field-subtype|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onLayoutChangeListener|android.view.View\$OnLayoutChangeListener",
        "handoff|method-alias|com.android.systemui.statusbar.views.MiuiStatusIconContainer|enableAnimation$1,enableAnimation|()Z",
        "alignment|method|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController|updateLocation|()V",
        "alignment|field|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback$1|this$0|Lcom/android/systemui/controlcenter/shade/ControlCenterHeaderExpandController;",
        "alignment|field|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController|realSystemIcons|Landroid/view/View;",
        "alignment|field|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController|batteryWidthDiff|I",
        "alignmentIsland|field|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController|isAddBatteryIsland|Z",
        "alignment|field|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController|controlCenterCallback|Lcom/android/systemui/controlcenter/shade/ControlCenterHeaderExpandController\$controlCenterCallback$1;",
        "privacy|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mHomeBlock|Z",
        "privacy|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateHomeBlock|(Z)V",
        "privacy|method-alias|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateVisibility$6,updateVisibility$1$1|()V",
        "privacy|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|START_HOME_TO_DOT|",
        "privacy|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|COMPLETE_HOME_TO_DOT|",
        "keyguard|subtype|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView||android.view.ViewGroup",
        "keyguard|field|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView|mToLockScreen|Z",
        "keyguard|method|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView|onFinishInflate|()V",
        "keyguardEvents|method|com.android.systemui.statusbar.phone.KeyguardStatusBarView|setVisibility|(I)V",
        "keyguardEvents|method|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView|initCallback|(Ljava/lang/String;)V",
        "aod|field|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|keyguardStatusBarView|Lcom/android/systemui/statusbar/phone/MiuiKeyguardStatusBarView;",
        "aod|method|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|animateFullAod|(ZZ)V",
        "aod|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|toggleAodMode|(Z)V",
        "aod|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|setIsAodAnimate|(Z)V",
        "motion|method|com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback$1|onExpansionChanged|(F)V",
        "motion|method|com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback$1|onExpansionChanged|(F)V",
        "core|method|com.android.systemui.statusbar.StatusIconDisplayable|isIconVisible|()Z",
        "core|method|com.android.systemui.statusbar.StatusIconDisplayable|isIconBlocked|()Z",
        "core|method|com.android.systemui.statusbar.StatusIconDisplayable|getSlot|()Ljava/lang/String;",
        "native|field|com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl|subId|I",
        "core|method|com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding|getShouldIconBeVisible|()Z",
        "core|field|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|binding|Lcom/android/systemui/statusbar/pipeline/shared/ui/binder/ModernStatusBarViewBinding;",
        "core|method|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|isIconVisible|()Z",
        "core|method|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|setVisibleState|(IZ)V",
        "core|field|com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView|subId|I",
        "core|method|com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView|setVisibleState|(IZ)V",
        "native|field|com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl\$signalIconId$1$1|this$0|Lcom/android/systemui/statusbar/pipeline/mobile/ui/viewmodel/MiuiMobileIconVMImpl;",
        "native|method|com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl\$signalIconId$1$1|invokeSuspend|(Ljava/lang/Object;)Ljava/lang/Object;",
        "native|field|com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl|lastSignalId|I",
        "native|field|com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl|iconInteractor|Lcom/android/systemui/statusbar/pipeline/mobile/domain/interactor/MiuiMobileIconInteractorImpl;",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mLevel|I",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mCharging|Z",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mFirstLevel|Z",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mBatteryPercentView|Landroid/widget/TextView;",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mStatusBarState|I",
        "core|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mLayoutFromTag|I",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onAttachedToWindow|()V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onDetachedFromWindow|()V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onMeasure|(II)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onLayout|(ZIIII)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateChargeAndText|()V",
        "core|field|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|mStatusIcon|Lcom/android/systemui/statusbar/views/MiuiStatusIconContainer;",
        "core|field|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|mPrivacyState|Lcom/android/systemui/statusbar/phone/MiuiHomePrivacyController\$PrivacyState;",
        "core|field|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|mIsHideBattery|Z",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|onMeasure|(II)V",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|onLayout|(ZIIII)V",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|setPrivacyState|(Lcom/android/systemui/statusbar/phone/MiuiHomePrivacyController\$PrivacyState;)V",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer|setIsHideBattery|(Ljava/lang/Boolean;)V",
        "core|field|com.android.systemui.statusbar.views.MiuiStatusIconContainer|iconSpacing|I",
        "core|field|com.android.systemui.statusbar.views.MiuiStatusIconContainer|ignoredSlots|Ljava/util/List;",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusIconContainer|onMeasure|(II)V",
        "core|method|com.android.systemui.statusbar.views.MiuiStatusIconContainer|onLayout|(ZIIII)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onBatteryLevelChanged|(IZZ)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onChargeStateChanged|(ZZ)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onDarkChangedInternal|()V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|onLightDarkTintChanged|(IIZ)V",
        "core|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|updateState|(I)V",
        "core|subtype|com.android.systemui.statusbar.views.MiuiBatteryMeterView||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.views.MiuiStatusBatteryContainer||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.views.MiuiStatusIconContainer||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView||com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView",
        "core|subtype|com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView||com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView",
        "core|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|START_SHOW_PRIVACY|",
        "core|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|COMPLETE_SHOW_PRIVACY|",
        "core|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|START_HIDE_PRIVACY|",
        "core|enum|com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState|COMPLETE_HIDE_PRIVACY|"
    )
    @JvmField val LEGACY_KEYGUARD: Array<String> = arrayOf<String>(
        "keyguard|subtype|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView||android.view.ViewGroup",
        "keyguard|method|com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView|onFinishInflate|()V",
        "keyguard|field|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|keyguardStatusBarView|Lcom/android/systemui/statusbar/phone/MiuiKeyguardStatusBarView;",
        "keyguard|method|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|animateFullAod|(Z)V",
        "keyguard|field|com.android.systemui.statusbar.views.MiuiBatteryMeterView|mToAod|Z",
        "keyguard|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|setIsAodAnimate|(Z)V"
    )
    @JvmField val LEGACY_AOD: Array<String> = arrayOf<String>(
        "aod|field|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|keyguardStatusBarView|Lcom/android/systemui/statusbar/phone/MiuiKeyguardStatusBarView;",
        "aod|method|com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject|animateFullAod|(Z)V",
        "aod|method|com.android.systemui.statusbar.views.MiuiBatteryMeterView|setIsAodAnimate|(Z)V"
    )

    @JvmStatic fun probe(inspector: Inspector): Result {
        val result = Result()
        for (str in CONTRACT) {
            val requirement = Requirement(str)
            try {
                inspector.check(requirement)
            } catch (e: Exception) {
                result.markUnavailable(
                    Capability.Companion.fromGroup(requirement.group),
                    "probe",
                    requirement.label() + ": " + e.javaClass.getSimpleName() + " " + e.message
                )
            } catch (e: LinkageError) {
                result.markUnavailable(
                    Capability.Companion.fromGroup(requirement.group),
                    "probe",
                    requirement.label() + ": " + e.javaClass.getSimpleName() + " " + e.message
                )
            }
        }
        if (result.isAvailable(Capability.CORE)
            && !result.isAvailable(Capability.KEYGUARD)
        ) {
            try {
                for (str2 in LEGACY_KEYGUARD) {
                    inspector.check(Requirement(str2))
                }
                result.legacyKeyguard = true
                result.legacyAod = true
                result.markAvailable(Capability.KEYGUARD, "legacy-keyguard")
                result.markAvailable(Capability.AOD_EVENTS, "legacy-aod")
                result.removeErrors { it.startsWith("keyguard:") || it.startsWith("aod:") }
            } catch (e2: Exception) {
            } catch (e2: LinkageError) {
            }
        }
        if (result.isAvailable(Capability.CORE)
            && result.isAvailable(Capability.KEYGUARD)
            && !result.isAvailable(Capability.AOD_EVENTS)
        ) {
            try {
                for (str3 in LEGACY_AOD) {
                    inspector.check(Requirement(str3))
                }
                result.legacyAod = true
                result.markAvailable(Capability.AOD_EVENTS, "legacy-aod")
                result.removeErrors { it.startsWith("aod:") }
            } catch (e3: Exception) {
            } catch (e3: LinkageError) {
            }
        }
        return result
    }

    @JvmStatic fun `lambda$0`(str: String): Boolean {
        return str.startsWith("keyguard:") || str.startsWith("aod:")
    }

    @JvmStatic fun `lambda$1`(str: String): Boolean {
        return str.startsWith("aod:")
    }

    @JvmStatic fun probe(classLoader: ClassLoader?): Result {
        return probe(object : Inspector {
            @Throws(Exception::class)  // com.cyzah.circular.hook.platform.Compatibility.Inspector
            override fun check(requirement: Requirement) {
                check(classLoader, requirement)
            }
        })
    }

    @JvmStatic @Throws(Exception::class)
    fun check(classLoader: ClassLoader?, requirement: Requirement) {
        val cls = Class.forName(requirement.owner, false, classLoader)
        if (requirement.kind == "subtype") {
            require(
                Class.forName(requirement.type, false, classLoader).isAssignableFrom(cls)
            ) { "parent type mismatch" }
            return
        }
        if (requirement.kind == "enum") {
            require(!(!cls.isEnum() || !cls.getDeclaredField(requirement.name).isEnumConstant())) { "enum mismatch" }
            return
        }
        if (requirement.kind == "field") {
            val declaredField = cls.getDeclaredField(requirement.name)
            require(!(Modifier.isStatic(declaredField.getModifiers()) || descriptor(declaredField.getType()) != requirement.type)) { "field type/static mismatch" }
            return
        }
        if (requirement.kind == "field-subtype") {
            val declaredField2 = cls.getDeclaredField(requirement.name)
            require(
                !(Modifier.isStatic(declaredField2.getModifiers()) || !Class.forName(
                    requirement.type,
                    false,
                    classLoader
                ).isAssignableFrom(declaredField2.getType()))
            ) { "field parent/static mismatch" }
            return
        }
        if (requirement.kind == "method-alias") {
            selectMethod(cls, requirement.name, requirement.type)
            return
        }
        for (method in cls.getDeclaredMethods()) {
            if (method.getName() == requirement.name && descriptor(method) == requirement.type && !Modifier.isStatic(
                    method.getModifiers()
                )
            ) {
                return
            }
        }
        throw NoSuchMethodException(requirement.name + requirement.type)
    }

    @JvmStatic @Throws(NoSuchMethodException::class)
    fun selectMethod(cls: Class<*>, str: String, str2: String?): Method {
        for (str3 in str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            for (method in cls.getDeclaredMethods()) {
                if (method.getName() == str3 && descriptor(method) == str2 && !Modifier.isStatic(method.getModifiers())) {
                    return method
                }
            }
        }
        throw NoSuchMethodException(str + str2)
    }

    @JvmStatic fun descriptor(method: Method): String {
        val sb = StringBuilder("(")
        for (cls in method.getParameterTypes()) {
            sb.append(descriptor(cls))
        }
        return sb.append(')').append(descriptor(method.getReturnType())).toString()
    }

    @JvmStatic fun descriptor(cls: Class<*>): String {
        if (cls.isArray()) {
            return cls.getName().replace('.', '/')
        }
        if (cls.isPrimitive()) {
            return if (cls == Void.TYPE) "V" else if (cls == Boolean::class.javaPrimitiveType!!) "Z" else if (cls == Int::class.javaPrimitiveType!!) "I" else if (cls == Long::class.javaPrimitiveType!!) "J" else if (cls == Float::class.javaPrimitiveType!!) "F" else if (cls == Double::class.javaPrimitiveType!!) "D" else if (cls == Byte::class.javaPrimitiveType!!) "B" else if (cls == Char::class.javaPrimitiveType!!) "C" else "S"
        }
        return "L" + cls.getName().replace('.', '/') + ";"
    }

    fun interface Inspector {
        @Throws(Exception::class)
        fun check(requirement: Requirement)
    }

    enum class Capability(private val group: String) {
        CORE("core"),
        MOTION("motion"),
        NATIVE_SIGNAL("native"),
        KEYGUARD("keyguard"),
        KEYGUARD_EVENTS("keyguardEvents"),
        AOD_EVENTS("aod"),
        PRIVACY("privacy"),
        MOTION_ALIGNMENT("alignment"),
        ALIGNMENT_ISLAND("alignmentIsland"),
        HANDOFF("handoff"),
        HANDOFF_HINTS("handoffHints"),
        CHARGING_ISLAND("island"),
        CLOSED_ANCHOR("anchor"),
        BATTERY_POWER("power"),
        OEM("oem");

        fun group(): String {
            return group
        }

        companion object {
            @JvmStatic fun fromGroup(group: String?): Capability {
                for (capability in entries) {
                    if (capability.group == group) {
                        return capability
                    }
                }
                throw IllegalArgumentException("Unknown capability group: " + group)
            }
        }
    }

    enum class Availability {
        AVAILABLE,
        UNAVAILABLE,
        FAILED
    }

    class CapabilityState(private val status: Availability?, private val phase: String?, private val reason: String?) {
        fun status(): Availability? {
            return status
        }

        fun phase(): String? {
            return phase
        }

        fun reason(): String? {
            return reason
        }
    }

    class Requirement(str: String) {
        @JvmField val group: String?
        @JvmField val kind: String
        @JvmField val name: String
        @JvmField val owner: String?
        @JvmField val type: String?
        private val raw: String?

        init {
            this.raw = str
            val strArrSplit = str.split("\\|".toRegex()).toTypedArray()
            this.group = strArrSplit[0]
            this.kind = strArrSplit[1]
            this.owner = strArrSplit[2]
            this.name = strArrSplit[3]
            this.type = strArrSplit[4]
        }

        fun label(): String {
            return this.owner + "#" + this.name + " " + this.type
        }

        fun group(): String? {
            return group
        }

        fun raw(): String? {
            return raw
        }
    }

    class Result {
        @JvmField var legacyAod: Boolean = false
        @JvmField var legacyKeyguard: Boolean = false
        private val states = EnumMap<Capability, CapabilityState>(Capability::class.java)
        private val errors = ArrayList<String>()

        fun removeErrors(predicate: (String) -> Boolean) {
            errors.removeAll(predicate)
        }

        init {
            for (capability in Capability.entries) {
                states.put(
                    capability,
                    CapabilityState(Availability.AVAILABLE, "probe", "")
                )
            }
        }

        fun isAvailable(capability: Capability?): Boolean {
            return state(capability).status() == Availability.AVAILABLE
        }

        fun state(capability: Capability?): CapabilityState {
            return requireNotNull(capability?.let(states::get)) { "Unknown capability: " + capability }
        }

        fun markAvailable(capability: Capability, phase: String?) {
            states.put(
                capability,
                CapabilityState(Availability.AVAILABLE, phase, "")
            )
        }

        fun markUnavailable(capability: Capability, phase: String?, reason: String?) {
            mark(capability, Availability.UNAVAILABLE, phase, reason)
        }

        fun markFailed(capability: Capability, phase: String?, reason: String?) {
            mark(capability, Availability.FAILED, phase, reason)
        }

        private fun mark(
            capability: Capability,
            availability: Availability?,
            phase: String?,
            reason: String?
        ) {
            states.put(capability, CapabilityState(availability, phase, reason))
            if (errors.size < 16) {
                errors.add(
                    (capability.group() + ":" + phase + "," + availability + " "
                            + (if (reason == null) "" else reason))
                )
            }
        }

        fun profile(): String {
            return if (this.legacyKeyguard) "miui-modern-a16-v2" else if (this.legacyAod) "miui-modern-a16-v3" else PROFILE
        }

        fun details(): String {
            return if (this.errors.isEmpty()) "Required members matched" else this.errors.joinToString("\n")
        }

        fun classification(str: String?): String {
            return if (!isAvailable(Capability.CORE)) "UNSUPPORTED" else if (KNOWN_SHA == str) "KNOWN_APK" else "STRUCTURAL_MATCH"
        }
    }
}
