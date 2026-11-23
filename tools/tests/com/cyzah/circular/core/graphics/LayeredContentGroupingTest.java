package com.cyzah.circular.core.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.cyzah.circular.core.graphics.geometry.IconGeometry;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.domain.settings.BatteryNumberPlacement;
import com.cyzah.circular.domain.settings.ContentOffsets;
import com.cyzah.circular.domain.settings.DualStyle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import sun.misc.Unsafe;

/** Runs real painter/overlay geometry on the host, without pushing files to a device. */
public final class LayeredContentGroupingTest {
    private static final float UNIT = .5f, WEIGHT = 1.2f;
    private static final Unsafe UNSAFE = unsafe();
    private interface Check { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Check[] checks = {LayeredContentGroupingTest::staticDotsShareTheBottomOffset,
                LayeredContentGroupingTest::noServiceCrossesStayWithTheirRows,
                LayeredContentGroupingTest::openingBoundsContainBothRowsAsOneBottomGroup,
                LayeredContentGroupingTest::animationTargetsUseTheSameBottomGroup};
        int failures = 0;
        for (Check check : checks) {
            try { check.run(); }
            catch (Throwable error) { failures++; error.printStackTrace(System.out); }
        }
        if (failures > 0) throw new AssertionError(failures + " layered grouping checks failed");
        System.out.println("PASS layered static dots, opening bounds and animation targets share the bottom group");
    }

    private static void noServiceCrossesStayWithTheirRows() throws Exception {
        IconPainter painter = painter(new ContentOffsets(0, 0, 12, -10, -7, 9));
        IconState state = state();
        state.inService = state.secondaryInService = false;
        Canvas canvas = new Canvas();
        painter.drawPartMasked(canvas, 0, 0, 120 * UNIT, state, -1, false, 4, 1);
        if (canvas.pathCenters.size() != 2) throw new AssertionError("Expected both no-service crosses");
        close(60 - 7, canvas.pathCenters.get(0)[0], "Upper-row no-service cross X");
        close(IconGeometry.innerDotY(1, 0, WEIGHT) + 9, canvas.pathCenters.get(0)[1], "Upper-row no-service cross Y");
        close(60 - 7, canvas.pathCenters.get(1)[0], "Lower-row no-service cross X");
        close(IconGeometry.outerDotY(1, WEIGHT) + 9, canvas.pathCenters.get(1)[1], "Lower-row no-service cross Y");
    }

    private static void staticDotsShareTheBottomOffset() throws Exception {
        for (int bottom : new int[] {0, 1, -1, 48, -48}) {
            IconPainter painter = painter(new ContentOffsets(0, 0, 12, -10, bottom, -bottom));
            Canvas canvas = new Canvas();
            painter.drawPartMasked(canvas, 0, 0, 120 * UNIT, state(), -1, false, 4, 1);
            if (canvas.circles.size() != 7) throw new AssertionError("Expected both rows of seven dots");
            for (int index = 0; index < 7; index++) {
                boolean inner = index >= 4;
                int dot = inner ? index - 4 : index;
                float x = inner ? IconGeometry.innerDotX(dot, 0, WEIGHT) : IconGeometry.outerDotX(dot, WEIGHT);
                float y = inner ? IconGeometry.innerDotY(dot, 0, WEIGHT) : IconGeometry.outerDotY(dot, WEIGHT);
                close(x + bottom, canvas.circles.get(index)[0], "Static layered X index " + index);
                close(y - bottom, canvas.circles.get(index)[1], "Static layered Y index " + index);
            }
        }
    }

    private static void openingBoundsContainBothRowsAsOneBottomGroup() throws Exception {
        for (int bottom : new int[] {0, 1, -1, 48, -48}) {
            IconPainter painter = painter(new ContentOffsets(0, 0, 12, -10, bottom, -bottom));
            Method method = IconPainter.class.getDeclaredMethod("flatOpeningBounds", IconState.class);
            method.setAccessible(true);
            method.invoke(painter, state());
            RectF actual = (RectF) field(painter, "bottomOpeningBounds");
            RectF center = (RectF) field(painter, "centerOpeningBounds");
            RectF expected = new RectF();
            float radius = 4.9f * WEIGHT;
            for (int index = 0; index < 7; index++) {
                boolean inner = index >= 4;
                int dot = inner ? index - 4 : index;
                float x = (inner ? IconGeometry.innerDotX(dot, 0, WEIGHT) : IconGeometry.outerDotX(dot, WEIGHT)) + bottom;
                float y = (inner ? IconGeometry.innerDotY(dot, 0, WEIGHT) : IconGeometry.outerDotY(dot, WEIGHT)) - bottom;
                expected.union(x - radius, y - radius, x + radius, y + radius);
            }
            close(expected.left, actual.left, "Bottom group bounds left");
            close(expected.top, actual.top, "Bottom group must include the upper three dots");
            close(expected.right, actual.right, "Bottom group bounds right");
            close(expected.bottom, actual.bottom, "Bottom group bounds bottom");
            if (!center.isEmpty()) throw new AssertionError("Signal dots must not occupy center content bounds");
        }
    }

    private static void animationTargetsUseTheSameBottomGroup() throws Exception {
        for (int bottom : new int[] {0, 1, -1, 48, -48}) {
            IconPainter endpoint = painter(new ContentOffsets(0, 0, 12, -10, bottom, -bottom));
            IconPainter host = painter(ContentOffsets.DEFAULT);
            StackedSignals signals = (StackedSignals) UNSAFE.allocateInstance(StackedSignals.class);
            set(signals, "barPaint", new Paint());
            set(signals, "rect", new RectF());
            StackedSignals.Target target = new StackedSignals.Target();
            StackedOverlayRenderer overlay = new StackedOverlayRenderer();
            overlay.setLabelsEnabled(false);
            overlay.frame(100, 220, 75, 0, 0, 20, 30, UNIT, UNIT, 1, 0, 0, 0,
                    -1, 100, false, false, true, DualStyle.LAYERED, BatteryNumberPlacement.INSIDE);
            overlay.draw(new Canvas(), host, endpoint, signals, target, state(), null, null);
            for (int element = 0; element < StackedSignals.ELEMENTS; element++) {
                boolean inner = element < 4;
                int dot = inner ? Math.min(2, element) : element - 4;
                float x = inner ? IconGeometry.innerDotX(dot, 0, WEIGHT) : IconGeometry.outerDotX(dot, WEIGHT);
                float y = inner ? IconGeometry.innerDotY(dot, 0, WEIGHT) : IconGeometry.outerDotY(dot, WEIGHT);
                close(20 + (x + bottom) * UNIT, target.x[element], "Animation layered X element " + element);
                close(30 + (y - bottom) * UNIT, target.y[element], "Animation layered Y element " + element);
            }
        }
    }

    private static IconPainter painter(ContentOffsets offsets) throws Exception {
        IconPainter painter = (IconPainter) UNSAFE.allocateInstance(IconPainter.class);
        set(painter, "duoRenderer", UNSAFE.allocateInstance(DuoStyleRenderer.class));
        set(painter, "p", new Paint());
        set(painter, "duoSignals", new DuoSignalRenderer());
        set(painter, "networkPaint", new Paint());
        set(painter, "textPaint", new StatusNumberPaint());
        set(painter, "textMetrics", new Paint.FontMetrics());
        set(painter, "numberBounds", new Rect());
        set(painter, "batteryReadoutInk", new Rect());
        for (String name : new String[] {"openingContentBounds", "topOpeningBounds", "bottomOpeningBounds", "centerOpeningBounds"}) {
            set(painter, name, new RectF());
        }
        set(painter, "weight", WEIGHT);
        set(painter, "technologyPercent", 100);
        set(painter, "percentScale", 100);
        painter.setContentOffsets(offsets);
        painter.setContentPixelUnit(UNIT);
        return painter;
    }

    private static IconState state() {
        IconState state = new IconState();
        state.simPresent = state.secondarySimPresent = true;
        state.mobileKnown = state.secondaryKnown = true;
        state.inService = state.secondaryInService = true;
        state.mobileLevel = state.secondaryLevel = 4;
        state.mobileSlot = 0; state.secondarySlot = 1;
        return state;
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void close(float expected, float actual, String label) {
        if (!(Math.abs(expected - actual) <= .0001f)) {
            throw new AssertionError(label + "; expected=" + expected + " actual=" + actual);
        }
    }
}
