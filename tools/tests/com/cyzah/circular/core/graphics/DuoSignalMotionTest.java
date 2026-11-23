package com.cyzah.circular.core.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.cyzah.circular.core.graphics.animation.OpeningBatteryMorph;
import com.cyzah.circular.core.graphics.geometry.RingGeometry;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.domain.settings.BatteryNumberPlacement;
import com.cyzah.circular.domain.settings.ContentOffsets;
import com.cyzah.circular.domain.settings.DualStyle;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** Checks actual animation draw bounds, rather than just the final target coordinates. */
public final class DuoSignalMotionTest {
    private static final Unsafe UNSAFE = unsafe();

    public static void main(String[] args) throws Exception {
        openingRingPreservesTheInitialSignalRows();
        openingMotionKeepsDrawBoundsContinuous();
        for (boolean rtl : new boolean[] {false, true}) {
            for (float wifi : new float[] {0, 1}) {
                for (int direction : new int[] {-1, 1}) {
                    for (int frame = 0; frame <= 100; frame++) {
                        float progress = (direction > 0 ? frame : 100 - frame) / 100f;
                        Canvas canvas = render(progress, wifi, rtl);
                        for (int row = 0; row < 2; row++) {
                            for (int dot = 0; dot < 4; dot++) {
                                float[] bounds = canvas.roundRects.get(row * 4 + dot);
                                requireFiniteBounds(bounds, progress, row, dot, wifi, rtl);
                                if ((progress == 0.0f || progress == 1.0f)
                                        && (bounds[2] <= bounds[0] || bounds[3] <= bounds[1])) {
                                    throw new AssertionError("DUO endpoint dot lost its size: progress=" + progress
                                            + " row=" + row + " dot=" + dot + " wifi=" + wifi
                                            + " rtl=" + rtl);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("PASS DUO keeps all eight signal elements throughout split and merge");
    }

    private static void openingRingPreservesTheInitialSignalRows() throws Exception {
        for (boolean rtl : new boolean[] {false, true}) {
            for (float wifi : new float[] {0, 1}) {
                for (float progress : new float[] {0, .001f, .01f}) {
                    Canvas unobstructed = render(progress, wifi, rtl);
                    Canvas opening = render(progress, wifi, rtl, true);
                    for (int element = 0; element < 8; element++) {
                        float[] expected = unobstructed.roundRects.get(element);
                        float[] actual = opening.roundRects.get(element);
                        for (int edge = 0; edge < 4; edge++) {
                            if (!(Math.abs(expected[edge] - actual[edge]) <= .001f)) {
                                throw new AssertionError("Hollow opening ring displaced an interior DUO dot:"
                                        + " progress=" + progress + " wifi=" + wifi + " rtl=" + rtl
                                        + " element=" + element + " edge=" + edge
                                        + " expected=" + expected[edge] + " actual=" + actual[edge]);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void openingMotionKeepsDrawBoundsContinuous() throws Exception {
        for (boolean rtl : new boolean[] {false, true}) {
            for (float wifi : new float[] {0, 1}) {
                for (int direction : new int[] {-1, 1}) {
                    Canvas previous = null;
                    for (int frame = 0; frame <= 5000; frame++) {
                        float progress = (direction > 0 ? frame : 5000 - frame) / 5000f;
                        Canvas current = render(progress, wifi, rtl, true);
                        for (int element = 0; element < 8; element++) {
                            float[] bounds = current.roundRects.get(element);
                            requireFiniteBounds(bounds, progress, element / 4, element % 4, wifi, rtl);
                            if (previous == null) continue;
                            float[] before = previous.roundRects.get(element);
                            if (bounds[2] <= bounds[0] || bounds[3] <= bounds[1]
                                    || before[2] <= before[0] || before[3] <= before[1]) continue;
                            float deltaX = Math.abs((bounds[0] + bounds[2] - before[0] - before[2]) / 2);
                            if (!(deltaX <= 1.0f)) {
                                throw new AssertionError("Opening ring caused a DUO signal jump:"
                                        + " progress=" + progress + " wifi=" + wifi + " rtl=" + rtl
                                        + " element=" + element + " deltaX=" + deltaX);
                            }
                        }
                        previous = current;
                    }
                }
            }
        }
    }

    private static void requireFiniteBounds(
            float[] bounds,
            float progress,
            int row,
            int dot,
            float wifi,
            boolean rtl) {
        for (float value : bounds) {
            if (!Float.isFinite(value)) {
                throw new AssertionError("DUO dot has non-finite bounds: progress=" + progress
                        + " row=" + row + " dot=" + dot + " wifi=" + wifi
                        + " rtl=" + rtl);
            }
        }
        if (bounds[2] < bounds[0] || bounds[3] < bounds[1]) {
            throw new AssertionError("DUO dot bounds reversed: progress=" + progress
                    + " row=" + row + " dot=" + dot + " wifi=" + wifi
                    + " rtl=" + rtl);
        }
    }

    private static Canvas render(float progress, float wifi, boolean rtl) throws Exception {
        return render(progress, wifi, rtl, false);
    }

    private static Canvas render(float progress, float wifi, boolean rtl, boolean opening) throws Exception {
        IconPainter painter = (IconPainter) UNSAFE.allocateInstance(IconPainter.class);
        set(painter, "duoRenderer", UNSAFE.allocateInstance(DuoStyleRenderer.class));
        set(painter, "networkPaint", new Paint());
        set(painter, "textMetrics", new Paint.FontMetrics());
        set(painter, "technologyPercent", 100);
        painter.setContentOffsets(ContentOffsets.DEFAULT);
        StackedSignals signals = (StackedSignals) UNSAFE.allocateInstance(StackedSignals.class);
        set(signals, "barPaint", new Paint());
        set(signals, "rect", new RectF());
        StackedOverlayRenderer overlay = new StackedOverlayRenderer();
        overlay.setLabelsEnabled(false);
        overlay.frame(100, 220, 75, progress, progress, 20, 30, .5f, .5f, 1,
                progress, wifi, 0, -1, 100, rtl, false, true,
                DualStyle.DUO, BatteryNumberPlacement.INSIDE);
        if (opening) {
            RingGeometry ring = new RingGeometry();
            ring.setCircular(DualStyle.DUO, 10, 8, 110, 108, 18, 55, 9);
            OpeningBatteryMorph battery = new OpeningBatteryMorph();
            battery.update(ring, 20, 30, .5f, .5f, 110, 61, 42, 20, progress);
            if (!battery.valid) throw new AssertionError("Invalid opening ring fixture");
            float endpointX = 20 + ring.startX * .5f;
            float endpointY = 30 + ring.startY * .5f;
            overlay.setBatteryObstacle(new RectF(battery.left, battery.top, battery.right, battery.bottom),
                    new RectF(endpointX - 2.25f, endpointY - 2.25f, endpointX + 2.25f, endpointY + 2.25f));
            overlay.setOpeningBatteryObstacle(battery, new RectF(110, 61, 152, 81));
        }
        IconState state = new IconState();
        state.simPresent = state.secondarySimPresent = true;
        state.mobileKnown = state.secondaryKnown = true;
        state.inService = state.secondaryInService = true;
        state.mobileLevel = state.secondaryLevel = 4;
        state.mobileSlot = 0;
        state.secondarySlot = 1;
        Canvas canvas = new Canvas();
        overlay.draw(canvas, painter, painter, signals, new StackedSignals.Target(), state, null, null);
        if (canvas.roundRects.size() != 8) throw new AssertionError("Expected eight DUO signal elements");
        return canvas;
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
}
