package com.cyzah.circular.hook.runtime;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import com.cyzah.circular.hook.platform.OemStatusAdapter;
import io.github.libxposed.api.XposedInterface;
public class OemIconSession {
    public interface NativeDraw { Object run() throws Throwable; }
    public boolean active;
    public final View battery, root;
    public final ViewGroup icons;
    public final boolean keyguard, panel;
    public int observations, scenario, failures, hiddenSignalCount = 2;
    public Throwable lastFailure;
    public final DrawCallStack nativeDrawStack = new DrawCallStack(4);
    public final NativeDraw nativeDrawAdapter = new NativeDraw() {
        public Object run() throws Throwable { return ((XposedInterface.Chain) nativeDrawStack.current()).proceed(); }
    };
    public OemIconSession(OemStatusAdapter host, View battery, ViewGroup icons, View root, boolean keyguard, boolean panel) {
        this.battery = battery; this.icons = icons; this.root = root; this.keyguard = keyguard; this.panel = panel;
    }
    public void observe() { observations++; }
    public void reconcile() { }
    public void paint(Canvas canvas, NativeDraw nativeDraw) throws Throwable {
        canvas.translate(13, 9); canvas.clipRect(2, 3, 20, 30);
        if (scenario > 0) canvas.ink++;
        if (scenario > 1) nativeDraw.run();
        if (scenario < 3) throw new IllegalStateException("draw-" + scenario);
    }
    public void fail(Throwable error) { active = false; hiddenSignalCount = 0; failures++; lastFailure = error; }
}
