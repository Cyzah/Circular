package com.cyzah.circular.core.graphics;

import android.graphics.Canvas;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.domain.settings.ContentOffsets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import sun.misc.Unsafe;

/** Exercises the real IconPainter entrypoint without Android native rasterization. */
public final class ContentPixelReferenceTest {
    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Unsafe unsafe = (Unsafe) access.get(null);
        IconPainter painter = (IconPainter) unsafe.allocateInstance(IconPainter.class);
        IconState state = new IconState();
        float referenceUnit = .5f;
        for (int halfPixels : new int[] {1, -1, 8, -18}) {
        painter.setContentOffsets(new ContentOffsets(0, 0, halfPixels, -4, 0, 0));
        for (float outputUnit : new float[] {.5f, .625f, .8f, 1.25f}) {
            // Another endpoint query may reconfigure this shared painter before draw.
            painter.setContentPixelUnit(1.25f);
            Canvas canvas = new Canvas();
            projectedDraw(painter, canvas, state, outputUnit, referenceUnit);
            float hostScale = referenceUnit / outputUnit;
            requireClose(halfPixels * .5f, canvas.lastTranslateX * outputUnit * hostScale,
                    "X offset must meet the measured screen endpoint at outputUnit=" + outputUnit);
            requireClose(-2, canvas.lastTranslateY * outputUnit * hostScale,
                    "Y offset must meet the measured screen endpoint at outputUnit=" + outputUnit);
            requireClose(1.25f, painter.contentPixelUnit(), "A projected draw must restore the shared ruler unit");
        }
        }
        System.out.println("PASS real painter reference-unit positioning at 0.5px precision across sixteen scale/offset cases");
    }

    private static void projectedDraw(IconPainter painter, Canvas canvas, IconState state,
                                      float outputUnit, float referenceUnit) throws Exception {
        try {
            Method method = IconPainter.class.getMethod("drawPartMaskedAtReferenceUnit", Canvas.class,
                    float.class, float.class, float.class, IconState.class, int.class,
                    boolean.class, int.class, float.class, float.class);
            method.invoke(painter, canvas, 100f, 100f, outputUnit * 120, state,
                    -1, false, 0, 1f, referenceUnit);
        } catch (NoSuchMethodException beforeFix) {
            // The legacy production entrypoint is the failing behavior under investigation.
            painter.drawPartMasked(canvas, 100, 100, outputUnit * 120, state, -1, false, 0, 1);
        }
    }

    private static void requireClose(float expected, float actual, String label) {
        if (!(Math.abs(expected - actual) <= .0001f)) {
            throw new AssertionError(label + "; expected=" + expected + " actual=" + actual);
        }
    }
}
