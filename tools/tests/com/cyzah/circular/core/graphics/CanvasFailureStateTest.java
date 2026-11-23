package com.cyzah.circular.core.graphics;
import android.graphics.Canvas;
import com.cyzah.circular.core.network.IconState;
import java.lang.reflect.Field;
/** Host state-double check of the real IconPainter scope. Device Canvas checks are separate. */
public final class CanvasFailureStateTest {
    public static void main(String[] args) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe"); Field unsafe = unsafeType.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        IconPainter painter = (IconPainter) unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe.get(null), IconPainter.class);
        RuntimeException expected = new IllegalStateException("scale after transform and clip");
        Canvas canvas = new Canvas() {
            @Override public void scale(float x, float y) { super.scale(x, y); clipRect(3, 4, 10, 12); throw expected; }
        };
        canvas.translate(2, 5); int beforeCount = canvas.getSaveCount(); String before = canvas.snapshot(); Throwable thrown = null;
        try { painter.drawPartMasked(canvas, 10, 11, 24, new IconState(), 0xffeeeeee, false, 0, 1); }
        catch (RuntimeException error) { thrown = error; }
        if (beforeCount != canvas.getSaveCount() || !before.equals(canvas.snapshot())) throw new AssertionError("shared painter leaked save count, matrix or clip");
        if (thrown != expected) throw new AssertionError("shared painter swallowed original error");
        System.out.println("PASS real IconPainter balanced scope and observable original error (host state double)");
    }
}
