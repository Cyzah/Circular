package com.cyzah.circular.hook.runtime;

import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import com.cyzah.circular.core.graphics.animation.StartupReveal;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.hook.platform.OemStatusAdapter;
import java.util.List;
import java.util.WeakHashMap;

/** Runs the five actual inner draw scopes with a deterministic Canvas and injected primary error. */
public final class InnerDrawFailureTest {
    private interface Action { void run() throws Throwable; }
    private static final class DrawFixture {
        final Canvas canvas = new Canvas();
        final RuntimeException primary = new IllegalStateException("native draw");
        final RuntimeException cleanup = new IllegalArgumentException("canvas restoration");
        final int saveCount = canvas.getSaveCount();
        final String before = canvas.snapshot();
        int calls;
        DrawFixture(int restoreCall) { canvas.restoreFailure = cleanup; canvas.restoreFailureOnCall = restoreCall; }
        void verify(Action action) throws Throwable {
            Throwable actual = null;
            try { action.run(); } catch (Throwable error) { actual = error; }
            OemHostAttachTest.check(actual == primary, "primary failure must survive the inner scope, got " + actual);
            OemHostAttachTest.check(actual.getSuppressed().length == 1 && actual.getSuppressed()[0] == cleanup,
                    "cleanup failure must remain suppressed");
            OemHostAttachTest.check(saveCount == canvas.getSaveCount() && before.equals(canvas.snapshot()), "Canvas state leaked");
            OemHostAttachTest.check(calls == 1, "native draw must execute once");
        }
    }

    public static void main(String[] args) throws Throwable {
        int failures = 0;
        for (int scope = 0; scope < 5; scope++) {
            try { exercise(scope); System.out.println("PASS actual inner scope " + scope); }
            catch (Throwable failure) { failures++; System.out.println("FAIL actual inner scope " + scope + ": " + failure); }
        }
        if (failures != 0) throw new AssertionError(failures + " inner draw checks failed");
    }

    private static void exercise(int scope) throws Throwable {
        if (scope == 3 || scope == 4) {
            oem(scope == 4);
            return;
        }
        ModuleMain module = OemHostAttachTest.allocate(ModuleMain.class);
        View battery = new View();
        IconSession session = OemHostAttachTest.allocate(IconSession.class);
        OemHostAttachTest.set(session, "module", module);
        OemHostAttachTest.set(session, "battery", battery);
        DrawFixture f = new DrawFixture(1);
        if (scope == 2) {
            module.nativeBatteryTints = new WeakHashMap<>();
            module.nativeBatteryTints.put(battery, battery.tint);
            StartupReveal reveal = new StartupReveal();
            reveal.alpha(920, true); // The same fixture clock reads 1000 inside paint.
            OemHostAttachTest.set(session, "startupReveal", reveal);
            OemHostAttachTest.set(session, "renderer", new IconSessionPainter());
            f.verify(() -> session.paint(f.canvas, () -> { f.calls++; throw f.primary; }));
        } else if (scope == 0) {
            f.verify(() -> NativeSessionDraw.battery(session, f.canvas, () -> {
                OemHostAttachTest.check(module.bypass, "legacy battery must run under bypass");
                f.calls++;
                throw f.primary;
            }, 0.5f, 2, 24, 24));
        } else {
            View child = new View();
            child.drawing = () -> {
                OemHostAttachTest.check(module.bypass, "legacy child must run under bypass");
                f.calls++;
                throw f.primary;
            };
            f.verify(() -> NativeSessionDraw.view(session, f.canvas, child, 1, 1, 2, 24, 24));
            OemHostAttachTest.check(child.draws == 1, "legacy native View drawn once");
        }
        OemHostAttachTest.check(!module.bypass, "legacy bypass leaked");
    }

    private static void oem(boolean childFailure) throws Throwable {
        OemHostAttachTest.Fixture host = new OemHostAttachTest.Fixture(new OemHostAttachTest.Phone());
        OemStatusAdapter adapter = host.adapter;
        adapter.motionReady = true;
        adapter.batteryTint = View.class.getField("tint");
        adapter.wifiType = View.class;
        host.module.state = new IconState();
        OemIconSession session = new OemIconSession(adapter, host.battery, new ViewGroup(), host.root, false, true);
        session.progress = 1.0f;
        DrawFixture f = new DrawFixture(childFailure ? 2 : 1);
        View child = new View();
        int[] batteryCalls = {0};
        if (childFailure) {
            child.drawing = () -> {
                OemHostAttachTest.check(adapter.bypass, "OEM child must run under bypass");
                f.calls++;
                throw f.primary;
            };
            ((List<View>) OemHostAttachTest.get(session, "parts")).add(child);
        }
        f.verify(() -> session.paint(f.canvas, () -> {
            OemHostAttachTest.check(adapter.bypass, "OEM battery must run under bypass");
            batteryCalls[0]++;
            if (childFailure) return null;
            f.calls++;
            throw f.primary;
        }));
        OemHostAttachTest.check(batteryCalls[0] == 1 && child.draws == (childFailure ? 1 : 0), "OEM native draws must remain single");
        OemHostAttachTest.check(!adapter.bypass, "OEM bypass leaked");
    }
}
