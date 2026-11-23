package com.cyzah.circular.hook.runtime;
import com.cyzah.circular.hook.platform.SystemUiHookBootstrap;
import com.cyzah.circular.hook.platform.Compatibility;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/** Host regression test; run with the compiled app, Android and libxposed jars. */
public final class ChargingIslandHookTest {
    public static boolean requested;
    public static boolean nativeIsland;
    public static String wrongType;
    private static Fixture reconciling;
    private static Throwable reconciliationFailure;

    public static void main(String[] args) throws Throwable {
        normalReturnRestoresRequestedState();
        nativeFailureBalancesDepth();
        nestedNativeFailurePreservesOuterDepth();
        restorationFailureBalancesDepth();
        restorationFailureDoesNotReplaceNativeFailure();
        reconciliationFailureBalancesBothScopes();
        System.out.println("Charging island hook behavior checks passed (6)");
    }

    private static void normalReturnRestoresRequestedState() throws Throwable {
        Fixture fixture = new Fixture(0, false);
        requested = false;
        Object result = new Object();
        same(result, fixture.invoke(result, null));
        equal(0, fixture.session.islandDepth);
        check(requested, "requested state must survive native updates");
    }

    private static void nativeFailureBalancesDepth() throws Throwable {
        Fixture fixture = new Fixture(0, false);
        requested = false;
        Throwable nativeFailure = new IllegalStateException("native");
        same(nativeFailure, fixture.failure(nativeFailure));
        equal(0, fixture.session.islandDepth);
        check(requested, "requested state must be restored after native failure");
    }

    private static void nestedNativeFailurePreservesOuterDepth() throws Throwable {
        Fixture fixture = new Fixture(1, false);
        Throwable nativeFailure = new IllegalStateException("nested native");
        same(nativeFailure, fixture.failure(nativeFailure));
        equal(1, fixture.session.islandDepth);
    }

    private static void restorationFailureBalancesDepth() throws Throwable {
        Fixture fixture = new Fixture(0, true);
        check(fixture.failure(null) instanceof IllegalArgumentException,
                "invalid stored field must report its restoration failure");
        equal(0, fixture.session.islandDepth);
    }

    private static void restorationFailureDoesNotReplaceNativeFailure() throws Throwable {
        Fixture fixture = new Fixture(1, true);
        Throwable nativeFailure = new IllegalStateException("native with restoration failure");
        same(nativeFailure, fixture.failure(nativeFailure));
        equal(1, fixture.session.islandDepth);
        equal(1, nativeFailure.getSuppressed().length);
        check(nativeFailure.getSuppressed()[0] instanceof IllegalArgumentException,
                "restoration failure must remain available as suppressed detail");
    }

    private static void reconciliationFailureBalancesBothScopes() throws Throwable {
        Fixture fixture = new Fixture(0, false);
        fixture.module.compatibility = new Compatibility.Result();
        fixture.module.batteryIsland = ChargingIslandHookTest.class.getField("nativeIsland");
        fixture.module.updateBatteryIsland = ChargingIslandHookTest.class.getMethod(
                "updateIsland", boolean.class, boolean.class);
        requested = true;
        nativeIsland = false;
        reconciling = fixture;
        reconciliationFailure = new IllegalStateException("native reconciliation");
        try {
            fixture.session.reconcileIsland();
            throw new AssertionError("Expected native reconciliation failure");
        } catch (InvocationTargetException error) {
            same(reconciliationFailure, error.getCause());
        } finally {
            reconciling = null;
            reconciliationFailure = null;
        }
        equal(0, fixture.session.islandDepth);
        check(requested, "requested state must survive nested reconciliation failure");
    }

    public static void updateIsland(boolean requestedState, boolean animate) throws Throwable {
        equal(1, reconciling.session.islandDepth);
        reconciling.invoke(null, reconciliationFailure);
    }

    private static final class Fixture {
        final IconSession session = allocate(IconSession.class);
        final ModuleMain module = allocate(ModuleMain.class);
        final SystemUiHookBootstrap hooks;
        final Class<?> chainClass = Class.forName(
                "io.github.libxposed.api.XposedInterface$Chain");

        Fixture(int depth, boolean brokenStoredField) throws Exception {
            set(module, "batteries", new HashMap<>());
            module.batteries.put(null, session);
            module.storedBatteryIsland = ChargingIslandHookTest.class.getField(
                    brokenStoredField ? "wrongType" : "requested");
            // Keep lifecycle notification inert; exercise the real hook and its reflection.
            session.closed = true;
            session.islandDepth = depth;
            set(session, "module", module);
            hooks = new SystemUiHookBootstrap(module);
        }

        Object invoke(Object result, Throwable nativeFailure) throws Throwable {
            Object chain = Proxy.newProxyInstance(chainClass.getClassLoader(),
                    new Class<?>[]{chainClass}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "getThisObject": return null;
                            case "getArg": return Boolean.TRUE;
                            case "proceed":
                                requested = false;
                                if (nativeFailure != null) throw nativeFailure;
                                return result;
                            default: throw new AssertionError(method.getName());
                        }
                    });
            Method hook = SystemUiHookBootstrap.class.getDeclaredMethod(
                    "onChargingIslandChanged", chainClass);
            hook.setAccessible(true);
            try {
                return hook.invoke(hooks, chain);
            } catch (InvocationTargetException error) {
                throw error.getCause();
            }
        }

        Throwable failure(Throwable nativeFailure) throws Throwable {
            try {
                invoke(null, nativeFailure);
            } catch (Throwable error) {
                return error;
            }
            throw new AssertionError("Expected hook failure");
        }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, type));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void equal(int expected, int actual) {
        check(expected == actual, "expected " + expected + ", got " + actual);
    }

    private static void same(Object expected, Object actual) {
        check(expected == actual, "original result or failure must be preserved");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
