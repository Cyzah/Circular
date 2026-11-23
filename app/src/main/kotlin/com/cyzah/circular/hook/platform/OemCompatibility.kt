package com.cyzah.circular.hook.platform

import com.cyzah.circular.hook.platform.Compatibility

object OemCompatibility {
    const val ICONS: String = "com.android.systemui.statusbar.phone.StatusIconContainer"
    const val KEYGUARD: String = "com.android.systemui.statusbar.phone.KeyguardStatusBarView"
    const val MOBILE: String = "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
    const val MODERN: String = "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    const val OPLUS: String = "oplus-a16-v1"
    const val OPLUS_BATTERY: String = "com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView"
    const val PHONE: String = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    const val SAMSUNG: String = "samsung-a16-v1"
    const val SAMSUNG_BATTERY: String = "com.android.systemui.battery.BatteryMeterView"
    const val WIFI: String = "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
    @JvmField val COMMON: Array<String> = arrayOf<String>(
        "core|subtype|com.android.systemui.statusbar.phone.StatusIconContainer||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.phone.PhoneStatusBarView||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.phone.KeyguardStatusBarView||android.view.ViewGroup",
        "core|subtype|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView||android.view.ViewGroup",
        "core|method|com.android.systemui.statusbar.phone.StatusIconContainer|onMeasure|(II)V",
        "core|method|com.android.systemui.statusbar.phone.StatusIconContainer|onLayout|(ZIIII)V",
        "core|method|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|isIconVisible|()Z",
        "core|method|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|setVisibleState|(IZ)V",
        "core|subtype|com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView||com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView",
        "core|subtype|com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView||com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView",
        "core|field|com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView|subId|I",
        "core|field|com.android.systemui.statusbar.StatusBarStateControllerImpl|mIsDozing|Z",
        "core|field|com.android.systemui.statusbar.StatusBarStateControllerImpl|mState|I",
        "core|method|com.android.systemui.statusbar.StatusBarStateControllerImpl|setDozeAmountInternal|(F)V",
        "core|method|com.android.systemui.statusbar.StatusBarStateControllerImpl|setState|(IZ)Z"
    )
    @JvmField val SAMSUNG_MEMBERS: Array<String> = arrayOf<String>(
        "core|subtype|com.android.systemui.battery.BatteryMeterView||android.widget.LinearLayout",
        "core|field|com.android.systemui.battery.BatteryMeterView|mSamsungDrawable|Lcom/android/systemui/battery/SamsungBatteryMeterDrawable;",
        "core|field|com.android.systemui.battery.BatteryMeterView|mTextColor|I",
        "core|field|com.android.systemui.battery.BatteryMeterView|mIsOnAodBattery|Z",
        "core|method|com.android.systemui.battery.BatteryMeterView|updateColors|(III)V"
    )
    @JvmField val OPLUS_MEMBERS: Array<String> = arrayOf<String>(
        "core|subtype|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView||android.widget.LinearLayout",
        "core|field|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|batteryIcon|Lcom/oplus/systemui/statusbar/pipeline/battery/ui/model/StatBatteryIcon;",
        "core|field|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|hidden|Z",
        "core|field|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|disable|Z",
        "core|field|com.oplus.systemui.statusbar.pipeline.battery.ui.model.StatBatteryIcon|tintColor|I",
        "core|method|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|setBatteryIcon|(Lcom/oplus/systemui/statusbar/pipeline/battery/ui/model/StatBatteryIcon;)V",
        "core|subtype|com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarMobileView||com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView",
        "core|subtype|com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarWifiView||com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
    )

    @JvmStatic fun probe(inspector: Compatibility.Inspector): String {
        try {
            check(inspector, COMMON)
            try {
                check(inspector, OPLUS_MEMBERS)
                return OPLUS
            } catch (e: Exception) {
                try {
                    check(inspector, SAMSUNG_MEMBERS)
                    return SAMSUNG
                } catch (e2: Exception) {
                    return ""
                } catch (e2: LinkageError) {
                    return ""
                }
            } catch (e: LinkageError) {
                try {
                    check(inspector, SAMSUNG_MEMBERS)
                    return SAMSUNG
                } catch (e2: Exception) {
                    return ""
                } catch (e2: LinkageError) {
                    return ""
                }
            }
        } catch (e3: Exception) {
            return ""
        } catch (e3: LinkageError) {
            return ""
        }
    }

    @Throws(Exception::class)
    private fun check(inspector: Compatibility.Inspector, strArr: Array<String>) {
        for (str in strArr) {
            inspector.check(Compatibility.Requirement(str))
        }
    }

    @JvmStatic fun probe(classLoader: ClassLoader?): String {
        return probe(object : Compatibility.Inspector {
            @Throws(Exception::class)  // com.cyzah.circular.hook.platform.Compatibility.Inspector
            override fun check(requirement: Compatibility.Requirement) {
                Compatibility.check(classLoader, requirement)
            }
        })
    }

    @JvmStatic fun normalizedLevel(i: Int, i2: Int): Int {
        if (i2 < 2 || i2 > 6 || i < 0 || i >= i2) {
            return -1
        }
        return Math.round((i * 4.0f) / (i2 - 1))
    }

    @JvmStatic fun eligible(
        z: Boolean,
        z2: Boolean,
        z3: Boolean,
        z4: Boolean,
        z5: Boolean,
        z6: Boolean,
        z7: Boolean,
        z8: Boolean,
        i: Int
    ): Boolean {
        if (!z || !z2 || !z3 || !z4 || !z5 || z6) {
            return false
        }
        if (z7) {
            if (!z8) {
                return false
            }
            if (i != 1 && i != 2) {
                return false
            }
        } else if (i != 0) {
            return false
        }
        return true
    }
}
