package com.cyzah.circular.hook.runtime;
import android.content.Context;
import android.view.View;
import java.util.IdentityHashMap;
import java.util.Map;
import io.github.libxposed.api.XposedModule;
/** Only the Android/process boundary used by the real adapters under test. */
public class ModuleMain extends XposedModule {
    public boolean bypass;
    public java.util.WeakHashMap<View, Integer> nativeBatteryTints;
    public com.cyzah.circular.core.network.IconState state;
    public int oemSize() { return 24; }
    public int oemWeight() { return 100; }
    public boolean oemColoured() { return false; }
    public com.cyzah.circular.core.network.IconState oemState() { return state; }
    public void oemPainter(com.cyzah.circular.core.graphics.IconPainter painter) { }
    public Map<View, IconSession> batteries = new IdentityHashMap<>();
    public NativeStatusIconHold nativeStatusIconHold = new NativeStatusIconHold();
    public int contextCalls, hostChangeCalls, restores, anchorRefreshes;
    public Object controller;
    public RuntimeException restorationFailure;
    public static class Offset { public int value; public Offset(int value) { this.value = value; } }
    public void oemContext(Context context) { contextCalls++; }
    public void oemHostsChanged() { hostChangeCalls++; }
    public boolean oemEnabled() { return false; }
    public float rowAlignHostShift() { return 0.0f; }
    public IconSession owner(Object object) { return null; }
    public void releaseClosedAnchors() { }
    public boolean alignmentSyncing() { return true; }
    public void captureAlignmentController(Object callback) { }
    public Object alignmentController() { return controller; }
    public Integer applyAlignment() { Offset offset = (Offset) controller; int old = offset.value; offset.value = -7; return old; }
    public void restoreAlignment(Object captured, int value) {
        restores++;
        ((Offset) captured).value = value;
        if (restorationFailure != null) throw restorationFailure;
    }
    public void refreshClosedAnchors() { anchorRefreshes++; }
    public void disableAlignment(Throwable error) { }
}
