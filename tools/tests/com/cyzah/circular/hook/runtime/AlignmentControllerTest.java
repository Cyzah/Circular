package com.cyzah.circular.hook.runtime;
import com.cyzah.circular.hook.platform.Compatibility;


import io.github.libxposed.api.XposedInterface;
import android.view.Display;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;


/** Exercises native reflection and exception cleanup without constructing Android views. */
public final class AlignmentControllerTest {
    public static void main(String[] args) throws Throwable {
        AlignmentControllerTest tests = new AlignmentControllerTest();
        tests.nativeUpdateSeesMaskedIslandAndOriginalValueIsRestored();
        tests.nativeFailureStillRestoresIslandAndPreservesFailure();
        tests.nativeIslandBatteryIsNeverMasked();
        tests.callbackCaptureAndOffsetRestoreUseTheOriginalController();
        tests.acceptedZeroSkipsHostLookupAndScreenCoordinates();
        tests.onlyDetachOfACalibratedHostResetsSampling();
        System.out.println("Alignment controller behavior checks passed (6)");
    }

    private static void assertTrue(boolean actual) {
        if (!actual) throw new AssertionError("Expected true");
    }

    private static void assertFalse(boolean actual) {
        if (actual) throw new AssertionError("Expected false");
    }

    private static void assertSame(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError("Original value must be preserved");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static boolean nativeBatteryIsland;

    public static final class NativeController {
        public boolean island = true;
        public Object realIcons = new Object();
        public int offset = 23;
    }

    public static final class NativeCallback {
        public NativeController controller;
    }

    public static final class SamplingView extends View {
        SamplingView() { super(null); }
        @Override public Display getDisplay() { return null; }
        @Override public int getLayoutDirection() { return View.LAYOUT_DIRECTION_LTR; }
        @Override public void getLocationOnScreen(int[] location) {
            throw new AssertionError("A calibrated row must not sample screen coordinates");
        }
    }

    public void nativeUpdateSeesMaskedIslandAndOriginalValueIsRestored() throws Throwable {
        Fixture fixture = new Fixture();
        Object expected = new Object();
        assertSame(expected, fixture.update(expected, null));
        assertTrue(fixture.nativeController.island);
        assertSame(fixture.nativeController, fixture.alignment.controller());
    }

    public void nativeFailureStillRestoresIslandAndPreservesFailure() throws Throwable {
        Fixture fixture = new Fixture();
        Throwable expected = new IllegalStateException("native update");
        try {
            fixture.update(null, expected);
            throw new AssertionError("Expected native failure");
        } catch (Throwable actual) {
            assertSame(expected, actual);
        }
        assertTrue(fixture.nativeController.island);
    }

    public void nativeIslandBatteryIsNeverMasked() throws Throwable {
        Fixture fixture = new Fixture();
        nativeBatteryIsland = true;
        fixture.expectMask = false;
        fixture.update(null, null);
        assertTrue(fixture.nativeController.island);
    }

    public void callbackCaptureAndOffsetRestoreUseTheOriginalController() throws Exception {
        Fixture fixture = new Fixture();
        NativeCallback callback = new NativeCallback();
        callback.controller = fixture.nativeController;
        fixture.alignment.captureController(callback);
        assertSame(fixture.nativeController, fixture.alignment.controller());
        NativeController replacement = new NativeController();
        replacement.offset = 99;
        callback.controller = replacement;
        fixture.alignment.captureController(callback);
        fixture.nativeController.offset = -7;
        fixture.alignment.restoreOffset(fixture.nativeController, 23);
        assertEquals(23, fixture.nativeController.offset);
        assertEquals(99, replacement.offset);
    }

    public void acceptedZeroSkipsHostLookupAndScreenCoordinates() throws Exception {
        AlignmentController alignment = allocate(AlignmentController.class);
        RowAlignmentCalibration calibration = new RowAlignmentCalibration();
        calibration.sample(100, 100);
        calibration.sample(100, 100);
        SamplingView panel = allocate(SamplingView.class);
        set(alignment, "rowCalibration", calibration);
        set(alignment, "panelHost", panel);
        set(alignment, "rowDisplayId", -1);
        set(alignment, "rowLayoutDirection", View.LAYOUT_DIRECTION_LTR);
        // No ModuleMain fixture: reaching a host lookup would fail this test too.
        alignment.sampleRowAlign(panel);
        assertTrue(calibration.isCalibrated());
    }

    public void onlyDetachOfACalibratedHostResetsSampling() throws Exception {
        AlignmentController alignment = allocate(AlignmentController.class);
        RowAlignmentCalibration calibration = new RowAlignmentCalibration();
        calibration.sample(100, 162);
        calibration.sample(100, 162);
        SamplingView panel = allocate(SamplingView.class);
        SamplingView status = allocate(SamplingView.class);
        set(alignment, "rowCalibration", calibration);
        set(alignment, "panelHost", panel);
        set(alignment, "statusHost", status);
        alignment.onHostDetached(allocate(SamplingView.class));
        assertTrue(calibration.isCalibrated());
        alignment.onHostDetached(status);
        assertFalse(calibration.isCalibrated());
        assertTrue(alignment.rowAlignHostShift() == 0.0f);
        assertFalse(calibration.sample(100, 162));
        assertTrue(calibration.sample(100, 162));
    }

    private static final class Fixture {
        final ModuleMain module = allocate(ModuleMain.class);
        final AlignmentController alignment = allocate(AlignmentController.class);
        final NativeController nativeController = new NativeController();
        boolean expectMask = true;

        Fixture() throws Exception {
            nativeBatteryIsland = false;
            module.compatibility = new Compatibility.Result();
            set(module, "containers", new HashMap<>());
            IconSession session = allocate(IconSession.class);
            session.merge = true;
            // The battery field is static so no Android View is needed for this reflection path.
            module.batteryIsland = AlignmentControllerTest.class.getField("nativeBatteryIsland");
            ((java.util.Map) module.containers).put(nativeController.realIcons, session);
            set(alignment, "module", module);
            set(alignment, "alignmentReal", NativeController.class.getField("realIcons"));
            set(alignment, "alignmentIsland", NativeController.class.getField("island"));
            set(alignment, "alignmentDiff", NativeController.class.getField("offset"));
            set(alignment, "alignmentOuter", NativeCallback.class.getField("controller"));
        }

        Object update(final Object result, final Throwable failure) throws Throwable {
            XposedInterface.Chain chain = (XposedInterface.Chain) Proxy.newProxyInstance(
                    XposedInterface.Chain.class.getClassLoader(),
                    new Class<?>[]{XposedInterface.Chain.class},
                    (proxy, method, arguments) -> {
                        if ("getThisObject".equals(method.getName())) {
                            return nativeController;
                        }
                        if ("proceed".equals(method.getName())) {
                            if (expectMask) {
                                assertFalse(nativeController.island);
                            } else {
                                assertTrue(nativeController.island);
                            }
                            if (failure != null) {
                                throw failure;
                            }
                            return result;
                        }
                        throw new AssertionError(method.getName());
                    });
            return this.alignment.handleAlignmentProgress(chain);
        }
    }

    private static <T> T allocate(Class<T> type) {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field field = unsafeType.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return type.cast(unsafeType.getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, type));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
