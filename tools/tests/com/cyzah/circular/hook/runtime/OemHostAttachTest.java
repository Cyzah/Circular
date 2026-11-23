package com.cyzah.circular.hook.runtime;
import android.view.View;
import android.view.ViewGroup;
import com.cyzah.circular.hook.platform.OemStatusAdapter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.lang.ref.WeakReference;

/** Executes the real OEM attach adapter; doubles only its Android/session boundaries. */
public final class OemHostAttachTest {
    public static class Phone extends ViewGroup { }
    public static class Keyguard extends ViewGroup { }
    public static class Signals extends ViewGroup { }
    public static void main(String[] args) throws Exception {
        int failed = 0;
        for (String name : new String[]{"phoneRootIsRetained", "keyguardRoleIsRetained", "panelRootHasPriority", "unmatchedRootStaysNative", "duplicateAttachDoesNotRegisterAgain"}) {
            try { OemHostAttachTest.class.getDeclaredMethod(name).invoke(null); System.out.println("PASS " + name); }
            catch (java.lang.reflect.InvocationTargetException error) { failed++; System.out.println("FAIL " + name + ": " + error.getCause()); }
        }
        if (failed != 0) throw new AssertionError(failed + " OEM attach behavior checks failed");
    }
    private static void phoneRootIsRetained() throws Exception {
        Fixture f = new Fixture(new Phone()); f.adapter.registerBatteryHost(f.battery);
        OemIconSession session = f.session(); check(session != null, "PHONE ancestor must attach");
        check(session.root == f.root && !session.keyguard && !session.panel, "PHONE root/role must survive selection");
    }
    private static void keyguardRoleIsRetained() throws Exception {
        Fixture f = new Fixture(new Keyguard()); f.adapter.registerBatteryHost(f.battery);
        OemIconSession session = f.session(); check(session != null, "KEYGUARD ancestor must attach");
        check(session.root == f.root && session.keyguard && !session.panel, "KEYGUARD role must survive selection");
    }
    private static void panelRootHasPriority() throws Exception {
        Fixture f = new Fixture(new Keyguard()); View panel = new View();
        ((Map<View, WeakReference<View>>) get(f.adapter, "panelRoots")).put(f.battery, new WeakReference<>(panel));
        f.adapter.registerBatteryHost(f.battery); OemIconSession session = f.session();
        check(session != null && session.root == panel && session.panel && !session.keyguard, "explicit panel must take priority");
    }
    private static void unmatchedRootStaysNative() throws Exception {
        Fixture f = new Fixture(new ViewGroup()); f.adapter.registerBatteryHost(f.battery);
        check(f.session() == null && f.module.contextCalls == 0 && f.module.hostChangeCalls == 0, "unsupported ancestry must stay native");
    }
    private static void duplicateAttachDoesNotRegisterAgain() throws Exception {
        Fixture f = new Fixture(new Phone()); f.adapter.registerBatteryHost(f.battery); OemIconSession first = f.session();
        check(first != null, "first PHONE attach must create a session");
        f.adapter.registerBatteryHost(f.battery);
        check(f.session() == first && first.observations == 1 && f.module.contextCalls == 1 && f.module.hostChangeCalls == 1, "duplicate attach must preserve one registration");
    }
    static final class Fixture {
        final ModuleMain module = allocate(ModuleMain.class);
        final OemStatusAdapter adapter;
        final View battery = new View();
        final ViewGroup root;
        Fixture(ViewGroup root) throws Exception {
            this.root = root; ViewGroup row = new ViewGroup(); root.addView(row); row.addView(battery); row.addView(new Signals());
            Constructor<OemStatusAdapter> constructor = OemStatusAdapter.class.getDeclaredConstructor(ModuleMain.class, ClassLoader.class, String.class);
            constructor.setAccessible(true);
            try { adapter = constructor.newInstance(module, OemHostAttachTest.class.getClassLoader(), "fixture"); }
            catch (java.lang.reflect.InvocationTargetException error) { throw new AssertionError("OEM fixture constructor: " + error.getCause(), error.getCause()); }
            adapter.installed = true; set(adapter, "iconsType", Signals.class); set(adapter, "phoneType", Phone.class); set(adapter, "keyguardType", Keyguard.class);
            adapter.mobileType = Signals.class; adapter.wifiType = Signals.class;
        }
        OemIconSession session() throws Exception { return ((Map<View, OemIconSession>) get(adapter, "batteries")).get(battery); }
    }
    static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe"); Field field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
    static Object get(Object target, String name) throws Exception { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
    static void set(Object target, String name, Object value) throws Exception { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
