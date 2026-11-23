package com.cyzah.circular.hook.runtime;
import android.graphics.Canvas;
import android.view.View;
import com.cyzah.circular.hook.platform.SystemUiHookBootstrap;
import com.cyzah.circular.hook.platform.OemStatusAdapter;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

/** Injects failure at the real dispatch boundary; verifies dispatch ownership. */
public final class DrawFailureOwnershipTest {
    public static void main(String[] args) throws Throwable {
        int failed = 0;
        for (boolean oem : new boolean[]{false, true}) for (int stage = 0; stage < 4; stage++) {
            try { failureDoesNotRedrawThisDispatch(oem, stage, false); System.out.println("PASS draw " + oem + "/" + stage); }
            catch (Throwable error) { failed++; System.out.println("FAIL draw " + oem + "/" + stage + ": " + error); }
        }
        for (boolean oem : new boolean[]{false, true}) {
            try { failureDoesNotRedrawThisDispatch(oem, 2, true); System.out.println("PASS restore-failure " + oem); }
            catch (Throwable error) { failed++; System.out.println("FAIL restore-failure " + oem + ": " + error); }
            try { failureDoesNotRedrawThisDispatch(oem, 3, true); System.out.println("PASS cleanup-only-failure " + oem); }
            catch (Throwable error) { failed++; System.out.println("FAIL cleanup-only-failure " + oem + ": " + error); }
        }
        if (failed != 0) throw new AssertionError(failed + " draw dispatch behavior checks failed");
    }
    private static void failureDoesNotRedrawThisDispatch(boolean oem, int stage, boolean cleanupFailure) throws Throwable {
        ModuleMain module = OemHostAttachTest.allocate(ModuleMain.class);
        module.batteries = new IdentityHashMap<>(); module.nativeStatusIconHold = new NativeStatusIconHold();
        View battery = new View(); Canvas canvas = new Canvas(); int count = canvas.getSaveCount(); String state = canvas.snapshot();
        if (cleanupFailure) canvas.restoreFailure = new IllegalArgumentException("restore");
        final int[] calls = {0}; Object hooks; DrawCallStack stack; IconSession session = null; OemIconSession oemSession = null;
        if (oem) {
            OemHostAttachTest.Fixture fixture = new OemHostAttachTest.Fixture(new OemHostAttachTest.Phone());
            OemStatusAdapter adapter = fixture.adapter; battery = fixture.battery;
            oemSession = new OemIconSession(adapter, battery, new OemHostAttachTest.Signals(), fixture.root, false, false);
            oemSession.active = true; oemSession.scenario = stage;
            ((Map<View, OemIconSession>) OemHostAttachTest.get(adapter, "batteries")).put(battery, oemSession);
            hooks = adapter; stack = oemSession.nativeDrawStack;
        } else {
            session = new IconSession(); session.scenario = stage; module.batteries.put(battery, session);
            hooks = new SystemUiHookBootstrap(module); stack = session.nativeDrawStack;
        }
        final View target = battery;
        XposedInterface.Chain chain = (XposedInterface.Chain) Proxy.newProxyInstance(XposedInterface.Chain.class.getClassLoader(), new Class<?>[]{XposedInterface.Chain.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getThisObject": return target;
                        case "getArg": return canvas;
                        case "proceed": calls[0]++; return null;
                        default: throw new AssertionError(method.getName());
                    }
                });
        invoke(hooks, chain);
        OemHostAttachTest.check(canvas.getSaveCount() == count && state.equals(canvas.snapshot()), "scope must restore matrix, clip and count");
        OemHostAttachTest.check(stack.depth() == 0, "draw stack must pop even after restoration failure");
        OemHostAttachTest.check(calls[0] == (stage >= 2 ? 1 : 0), "entered replacement must not invoke original again; calls=" + calls[0]);
        if (stage == 3 && !cleanupFailure) return;
        Throwable failure = oem ? oemSession.lastFailure : session.lastFailure;
        OemHostAttachTest.check(stage == 3 ? failure == canvas.restoreFailure : failure instanceof IllegalStateException, "original draw failure must remain primary");
        if (cleanupFailure && stage != 3) OemHostAttachTest.check(failure.getSuppressed().length == 1 && failure.getSuppressed()[0] == canvas.restoreFailure, "restoration failure must remain suppressed");
        OemHostAttachTest.check(oem ? !oemSession.active && oemSession.hiddenSignalCount == 0 : !session.draw && session.hiddenSignalCount == 0, "failure must relinquish module signal ownership");
        canvas.restoreFailure = null; int before = calls[0]; invoke(hooks, chain);
        OemHostAttachTest.check(calls[0] == before + 1, "next traversal must draw native exactly once");
    }
    private static Object invoke(Object hooks, XposedInterface.Chain chain) throws Throwable {
        Method method = hooks.getClass().getDeclaredMethod("onStatusBarDispatchDraw", XposedInterface.Chain.class); method.setAccessible(true);
        try { return method.invoke(hooks, chain); } catch (InvocationTargetException error) { throw error.getCause(); }
    }
}
