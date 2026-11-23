package com.cyzah.circular.hook.runtime;
import android.graphics.Canvas;
import io.github.libxposed.api.XposedInterface;
public class IconSession {
    public boolean draw = true;
    public int from;
    public float stackedAlpha;
    public int scenario, failures, hiddenSignalCount = 2;
    public Throwable lastFailure;
    public final DrawCallStack nativeDrawStack = new DrawCallStack(4);
    public final NativeDraw nativeDrawAdapter = new NativeDraw() {
        public Object run() throws Throwable { return ((XposedInterface.Chain) nativeDrawStack.current()).proceed(); }
    };
    public void paint(Canvas canvas, NativeDraw nativeDraw) throws Throwable {
        canvas.translate(13, 9);
        canvas.clipRect(2, 3, 20, 30);
        if (scenario > 0) canvas.ink++;
        if (scenario > 1) nativeDraw.run();
        if (scenario < 3) throw new IllegalStateException("draw-" + scenario);
    }
    public void fail(String phase, Throwable failure) {
        draw = false; hiddenSignalCount = 0; failures++; lastFailure = failure;
    }
}
