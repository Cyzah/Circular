package com.cyzah.circular.core.graphics;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import com.cyzah.circular.core.network.IconState;
import java.util.Arrays;

/** Numerical Canvas state checks only: no screenshot, Bitmap output or pixel comparison. */
public final class CanvasAndroidStateTest {
    public static void main(String[] args) {
        for (int stage = 0; stage < 3; stage++) balancedScope(stage);
        System.out.println("Android Canvas numerical state checks passed (3); no image was exported");
    }
    private static void balancedScope(int stage) {
        Bitmap target = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        final RuntimeException expected = new IllegalStateException("injected draw");
        try {
            Canvas canvas = new Canvas(target) {
                @Override public void scale(float x, float y) {
                    super.scale(x, y);
                    if (stage == 2) return;
                    clipRect(1, 2, 8, 9);
                    if (stage == 1) drawRect(2, 3, 5, 6, new Paint());
                    throw expected;
                }
            };
            canvas.translate(2, 4); canvas.clipRect(0, 0, 52, 48);
            int count = canvas.getSaveCount(); float[] before = matrix(canvas); Rect clip = new Rect(); canvas.getClipBounds(clip);
            Throwable failure = null;
            // This scope-only path uses primitive fields and mask=0. Full renderer
            // initialization is covered by the installed-app startup checks instead.
            try { scopeOnlyPainter().drawPartMasked(canvas, 9, 7, 24, new IconState(), 0xffeeeeee, false, 0, 1); }
            catch (RuntimeException error) { failure = error; }
            Rect afterClip = new Rect(); canvas.getClipBounds(afterClip);
            if (count != canvas.getSaveCount() || !Arrays.equals(before, matrix(canvas)) || !clip.equals(afterClip)) {
                throw new AssertionError("Canvas state leaked at stage " + stage);
            }
            if (failure != (stage == 2 ? null : expected)) throw new AssertionError("original failure was swallowed at stage " + stage);
        } finally { target.recycle(); }
    }
    private static IconPainter scopeOnlyPainter() {
        try {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && field.getType() == type) {
                    field.setAccessible(true);
                    Object unsafe = field.get(null);
                    return (IconPainter) type.getMethod("allocateInstance", Class.class)
                            .invoke(unsafe, IconPainter.class);
                }
            }
            throw new AssertionError("Runtime allocator unavailable");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Cannot prepare primitive-only painter scope", error);
        }
    }
    @SuppressWarnings("deprecation")
    private static float[] matrix(Canvas canvas) {
        Matrix matrix = new Matrix(); canvas.getMatrix(matrix); float[] values = new float[9]; matrix.getValues(values); return values;
    }
}
