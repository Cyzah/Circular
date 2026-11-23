package com.cyzah.circular.hook.platform

import com.cyzah.circular.hook.platform.Compatibility

object OemExpansion {
    const val FAKE: String = "com.oplus.systemui.seamless.widget.StatusBarFakeFrame"
    const val OPLUS_HEADER: String = "com.oplus.systemui.qs.OplusQuickStatusBarHeader"
    const val OPLUS_STATE: String = "com.oplus.systemui.statusbar.SysUiStateControllerImpl"
    const val PANEL: String = "com.android.systemui.shade.NotificationPanelViewController"
    const val SHADE: String = "com.android.systemui.shade.ShadeHeaderController"
    @JvmField val COMMON: Array<String?> = arrayOf<String?>(
        "motion|field|com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView|binding|Lcom/android/systemui/statusbar/pipeline/shared/ui/binder/ModernStatusBarViewBinding;",
        "motion|method|com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding|getShouldIconBeVisible|()Z",
        "motion|method|com.android.systemui.statusbar.StatusIconDisplayable|isIconBlocked|()Z",
        "motion|method|com.android.systemui.statusbar.StatusIconDisplayable|getSlot|()Ljava/lang/String;",
        "motion|field|com.android.systemui.statusbar.phone.StatusIconContainer|mIgnoredSlots|Ljava/util/ArrayList;"
    )
    @JvmField val SAMSUNG: Array<String?> = arrayOf<String?>(
        "motion|method|com.android.systemui.battery.BatteryMeterView|onLayout|(ZIIII)V",
        "motion|field|com.android.systemui.shade.NotificationPanelViewController|mExpandedFraction|F",
        "motion|method|com.android.systemui.shade.NotificationPanelViewController|updateExpansionAndVisibility|()V",
        "motion|method|com.android.systemui.qs.QSImpl|setQsExpansion|(FFFF)V",
        "motion|field|com.android.systemui.statusbar.phone.StatusIconContainer|mIconSpacing|I",
        "motion|field|com.android.systemui.shade.ShadeHeaderController|batteryIcon|Lcom/android/systemui/battery/BatteryMeterView;",
        "motion|field|com.android.systemui.shade.ShadeHeaderController|iconContainer|Lcom/android/systemui/statusbar/phone/StatusIconContainer;",
        "motion|field-subtype|com.android.systemui.shade.ShadeHeaderController|header|android.view.ViewGroup",
        "motion|method|com.android.systemui.shade.ShadeHeaderController|onViewAttached|()V"
    )
    @JvmField val OPLUS: Array<String?> = arrayOf<String?>(
        "motion|method|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|onLayout|(ZIIII)V",
        "motion|method|com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView|getTransXForCoord|()F",
        "motion|method|com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarMobileView|getTransXForCoord|()F",
        "motion|method|com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarWifiView|getTransXForCoord|()F",
        "motion|field|com.oplus.systemui.statusbar.SysUiStateControllerImpl|panelExpandFraction|F",
        "motion|method|com.oplus.systemui.statusbar.SysUiStateControllerImpl|notifyExpandFractionChange|(FI)V",
        "motion|method|com.oplus.systemui.qs.OplusQuickStatusBarHeader|onFinishInflate|()V",
        "motion|field|com.oplus.systemui.qs.OplusQuickStatusBarHeader|mBatteryView|Lcom/oplus/systemui/statusbar/pipeline/battery/ui/view/StatBatteryMeterView;",
        "motion|field|com.oplus.systemui.qs.OplusQuickStatusBarHeader|mIconContainer|Lcom/android/systemui/statusbar/phone/StatusIconContainer;",
        "motion|subtype|com.oplus.systemui.qs.OplusQuickStatusBarHeader||android.view.ViewGroup"
    )
    @JvmField val FAKE_MEMBERS: Array<String?> = arrayOf<String?>(
        "motion|field|com.oplus.systemui.seamless.widget.StatusBarFakeFrame|mView|Landroid/view/View;",
        "motion|field|com.oplus.systemui.seamless.widget.StatusBarFakeFrame|type|Lcom/oplus/systemui/seamless/FakeType;",
        "motion|method|com.oplus.systemui.seamless.widget.StatusBarFakeFrame|setFakeView|(Landroid/view/View;Lcom/oplus/systemui/seamless/FakeType;)V",
        "motion|method|com.oplus.systemui.seamless.widget.StatusBarFakeFrame|dispatchDraw|(Landroid/graphics/Canvas;)V",
        "motion|enum|com.oplus.systemui.seamless.FakeType|FAKE_SYSTEM_ICON|",
        "motion|enum|com.oplus.systemui.seamless.FakeType|KEYGUARD_FAKE_SYSTEM_ICON|"
    )

    @JvmStatic fun matches(inspector: Compatibility.Inspector, vararg strArr: Array<String?>?): Boolean {
        try {
            for (strArr2 in strArr) {
                for (str in strArr2!!) {
                    inspector.check(Compatibility.Requirement(requireNotNull(str)))
                }
            }
            return true
        } catch (e: Exception) {
            return false
        } catch (e: LinkageError) {
            return false
        }
    }
}
