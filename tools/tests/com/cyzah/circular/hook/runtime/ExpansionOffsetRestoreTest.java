package com.cyzah.circular.hook.runtime;
import com.cyzah.circular.hook.platform.SystemUiHookBootstrap;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;

/** Runs the real onExpansionChanged adapter with an explicit native-offset boundary. */
public final class ExpansionOffsetRestoreTest {
    public static void main(String[] args) throws Throwable {
        int failed = 0;
        for (int scenario = 0; scenario < 4; scenario++) {
            try { restoresCapturedOffset(scenario); System.out.println("PASS expansion " + scenario); }
            catch (Throwable error) { failed++; System.out.println("FAIL expansion " + scenario + ": " + error); }
        }
        if (failed != 0) throw new AssertionError(failed + " expansion offset behavior checks failed");
    }
    private static void restoresCapturedOffset(int scenario) throws Throwable {
        ModuleMain module = OemHostAttachTest.allocate(ModuleMain.class);
        ModuleMain.Offset original = new ModuleMain.Offset(23), replacement = new ModuleMain.Offset(99); module.controller = original;
        Throwable nativeFailure = scenario == 0 ? null : new IllegalStateException("native");
        if (scenario == 3) module.restorationFailure = new IllegalArgumentException("cleanup");
        final int[] calls = {0}; final Object result = new Object();
        XposedInterface.Chain chain = (XposedInterface.Chain) Proxy.newProxyInstance(XposedInterface.Chain.class.getClassLoader(), new Class<?>[]{XposedInterface.Chain.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getArg": return Float.valueOf(0.5f);
                        case "getThisObject": return new Object();
                        case "proceed":
                            calls[0]++; OemHostAttachTest.check(original.value == -7, "native callback must see temporary alignment");
                            if (scenario >= 2) module.controller = replacement;
                            if (nativeFailure != null) throw nativeFailure;
                            return result;
                        default: throw new AssertionError(method.getName());
                    }
                });
        Throwable thrown = null; Object returned = null;
        Method method = SystemUiHookBootstrap.class.getDeclaredMethod("onExpansionChanged", boolean.class, XposedInterface.Chain.class); method.setAccessible(true);
        try { returned = method.invoke(new SystemUiHookBootstrap(module), true, chain); }
        catch (InvocationTargetException error) { thrown = error.getCause(); }
        OemHostAttachTest.check(original.value == 23, "captured controller must regain original offset; got " + original.value);
        OemHostAttachTest.check(replacement.value == 99 && calls[0] == 1, "replacement controller untouched; original invoked once");
        OemHostAttachTest.check(thrown == nativeFailure, "cleanup must preserve original result/failure");
        if (scenario == 3) OemHostAttachTest.check(nativeFailure.getSuppressed().length == 1 && nativeFailure.getSuppressed()[0] == module.restorationFailure, "cleanup failure must remain available as suppressed detail");
        OemHostAttachTest.check(module.anchorRefreshes == (nativeFailure == null ? 1 : 0), "anchors refresh only after success");
        if (nativeFailure == null) OemHostAttachTest.check(returned == result, "native result must survive cleanup");
    }
}
