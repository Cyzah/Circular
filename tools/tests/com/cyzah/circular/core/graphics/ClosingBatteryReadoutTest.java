package com.cyzah.circular.core.graphics;
import com.cyzah.circular.domain.settings.BatteryNumberPlacement;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import java.lang.reflect.InvocationTargetException;

/** Actual Android Canvas assertions; never captures the screen or writes images. */
public final class ClosingBatteryReadoutTest {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 180;
    private static final int BACKGROUND = 0xFF123456;
    private static final float RING_X = 300.0f;
    private static final float RING_Y = 20.0f;
    private static final int READOUT_MASK = 1 | IconPainter.MASK_HIDE_CENTRE
            | IconPainter.MASK_HIDE_SIGNALS | IconPainter.MASK_HIDE_WIFI
            | IconPainter.MASK_HIDE_TECHNOLOGY | IconPainter.MASK_HIDE_BATTERY_RING;

    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method fonts = android.graphics.Typeface.class
                .getDeclaredMethod("loadPreinstalledSystemFontMap");
        fonts.setAccessible(true);
        fonts.invoke(null);
        String[] tests = {
                "outsideStartsAtTheOriginalPanelGlyphsWithoutADuplicateTarget",
                "outsideFinishesAtTheOriginalStatusGlyphsWithoutASourceGhost",
                "hiddenPanelNumberFadesOnlyAtTheDestinationAndHasNoFinalJump",
                "insideStartsWithTheSameIsolatedGlyphCutoutAsTheStaticBattery",
                "reversedGestureRetracesIdenticalReadoutPixels"
        };
        int failures = 0;
        for (String test : tests) {
            try {
                ClosingBatteryReadoutTest.class.getDeclaredMethod(test).invoke(null);
                System.out.println("PASS " + test);
            } catch (InvocationTargetException failure) {
                failures++;
                System.out.println("FAIL " + test + ": " + failure.getCause());
            }
        }
        if (failures > 0) {
            throw new AssertionError(failures + " of " + tests.length + " readout checks failed");
        }
        System.out.println("Closing battery readout checks passed (" + tests.length + ")");
    }

    private static void outsideStartsAtTheOriginalPanelGlyphsWithoutADuplicateTarget() {
        Fixture fixture = new Fixture();
        Bitmap expected = blank(Color.TRANSPARENT);
        Bitmap actual = fixture.readout(BatteryNumberPlacement.OUTSIDE, 1);
        try {
            Canvas canvas = new Canvas(expected);
            canvas.translate(20, 20);
            fixture.renderer.drawReadoutGlyph(canvas, fixture.host, 88,
                    BatteryNumberPlacement.OUTSIDE, Color.WHITE, 1, false);
            require(alphaSum(expected) > 10000, "Original outside number fixture must have ink");
            samePixels(expected, actual, "Expanded readout endpoint");
        } finally {
            expected.recycle();
            actual.recycle();
        }
    }

    private static void outsideFinishesAtTheOriginalStatusGlyphsWithoutASourceGhost() {
        Fixture fixture = new Fixture();
        Bitmap expected = fixture.statusReadout();
        Bitmap actual = fixture.readout(BatteryNumberPlacement.OUTSIDE, 0);
        try {
            require(alphaSum(expected) > 10000, "Status number fixture must have ink");
            samePixels(expected, actual, "Collapsed readout endpoint");
        } finally {
            expected.recycle();
            actual.recycle();
        }
    }

    private static void hiddenPanelNumberFadesOnlyAtTheDestinationAndHasNoFinalJump() {
        Fixture fixture = new Fixture();
        Bitmap status = fixture.statusReadout();
        Bitmap expanded = fixture.readout(BatteryNumberPlacement.HIDDEN, 1);
        try {
            require(alphaSum(expanded) == 0, "Hidden panel number must not invent a source glyph");
            Rect allowed = inkBounds(status);
            require(!allowed.isEmpty(), "Destination number must be enabled");
            allowed.inset(-1, -1);
            long previous = 0;
            long full = alphaSum(status);
            for (float progress : new float[] {0.9f, 0.5f, 0.1f}) {
                Bitmap moving = fixture.readout(BatteryNumberPlacement.HIDDEN, progress);
                try {
                    long sum = alphaSum(moving);
                    require(sum > previous && sum < full,
                            "Hidden readout must fade continuously at p=" + progress);
                    require(allowed.contains(inkBounds(moving)),
                            "Hidden readout moved away from its status anchor at p=" + progress);
                    previous = sum;
                } finally {
                    moving.recycle();
                }
            }
            Bitmap beforeHandoff = fixture.readout(BatteryNumberPlacement.HIDDEN, 0.002f);
            Bitmap afterHandoff = fixture.readout(BatteryNumberPlacement.HIDDEN, 0);
            try {
                samePixels(status, beforeHandoff, "Readout must reach full opacity before handoff");
                samePixels(beforeHandoff, afterHandoff, "Final handoff must not flash");
            } finally {
                beforeHandoff.recycle();
                afterHandoff.recycle();
            }
        } finally {
            status.recycle();
            expanded.recycle();
        }
    }

    private static void insideStartsWithTheSameIsolatedGlyphCutoutAsTheStaticBattery() {
        Fixture fixture = new Fixture();
        Bitmap expected = blank(BACKGROUND);
        Bitmap actual = blank(BACKGROUND);
        Bitmap solid = blank(BACKGROUND);
        try {
            fixture.renderer.draw(new Canvas(expected), fixture.host, fixture.state,
                    fixture.geometry, BatteryNumberPlacement.INSIDE, Color.WHITE, Color.WHITE, 1);
            fixture.renderer.draw(new Canvas(solid), fixture.host, fixture.state,
                    fixture.geometry, BatteryNumberPlacement.HIDDEN, Color.WHITE, Color.WHITE, 1);
            fixture.prepare(BatteryNumberPlacement.INSIDE, 1);
            require(fixture.closing.cutout(), "Inside readout must own a cutout layer");
            Canvas canvas = new Canvas(actual);
            int layer = canvas.saveLayer(null, null);
            fixture.renderer.draw(canvas, fixture.host, fixture.state,
                    fixture.geometry, BatteryNumberPlacement.HIDDEN, Color.WHITE, Color.WHITE, 1);
            fixture.closing.drawSource(canvas, Color.WHITE);
            canvas.restoreToCount(layer);
            fixture.closing.drawTarget(canvas, fixture.state, Color.WHITE, false);
            int holes = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (solid.getPixel(x, y) != BACKGROUND
                            && actual.getPixel(x, y) == BACKGROUND) holes++;
                }
            }
            require(holes > 100, "Inside handoff must preserve real transparent glyph holes");
            samePixels(expected, actual, "Inside handoff must preserve static glyph geometry");
        } finally {
            expected.recycle();
            actual.recycle();
            solid.recycle();
        }
    }

    private static void reversedGestureRetracesIdenticalReadoutPixels() {
        Fixture fixture = new Fixture();
        Bitmap forward = fixture.readout(BatteryNumberPlacement.OUTSIDE, 0.65f);
        Bitmap later = fixture.readout(BatteryNumberPlacement.OUTSIDE, 0.1f);
        Bitmap reversed = fixture.readout(BatteryNumberPlacement.OUTSIDE, 0.65f);
        try {
            require(alphaSum(forward) > 10000, "Intermediate readout must contain real ink");
            samePixels(forward, reversed, "Gesture reversal must retrace the same frame");
        } finally {
            forward.recycle();
            later.recycle();
            reversed.recycle();
        }
    }

    private static final class Fixture {
        final IconPainter endpoint = new IconPainter();
        final IconPainter host = new IconPainter();
        final IconState state = new IconState();
        final BatteryShapeRenderer renderer = new BatteryShapeRenderer();
        final ClosingBatteryReadout closing = new ClosingBatteryReadout();
        final BatteryShapeGeometry geometry = new BatteryShapeGeometry();
        final RectF fullBattery = new RectF(20, 20, 218.44f, 114.2f);

        Fixture() {
            state.battery = 88;
            state.simPresent = true;
            state.mobileKnown = true;
            state.inService = true;
            state.mobileLevel = 4;
            endpoint.setShowPercent(true);
            endpoint.setBatteryWeightPercent(100);
            host.setBatteryWeightPercent(145);
            geometry.width = 198.44f;
            geometry.height = 94.2f;
            geometry.centreX = 20 + geometry.width / 2;
            geometry.centreY = 20 + geometry.height / 2;
            geometry.stage = BatteryShapeGeometry.Stage.COMPLETE;
            geometry.batteryContentAlpha = 1;
        }

        void prepare(BatteryNumberPlacement placement, float progress) {
            require(closing.prepare(endpoint, host, renderer, state, 88, placement,
                    RING_X, RING_Y, 1, fullBattery, progress), "Readout preparation failed");
        }

        Bitmap readout(BatteryNumberPlacement placement, float progress) {
            prepare(placement, progress);
            Bitmap bitmap = blank(Color.TRANSPARENT);
            Canvas canvas = new Canvas(bitmap);
            closing.drawSource(canvas, Color.WHITE);
            closing.drawTarget(canvas, state, Color.WHITE, false);
            return bitmap;
        }

        Bitmap statusReadout() {
            Bitmap bitmap = blank(Color.TRANSPARENT);
            endpoint.drawPartMasked(new Canvas(bitmap), RING_X, RING_Y, 120,
                    state, Color.WHITE, false, READOUT_MASK, 1);
            return bitmap;
        }
    }

    private static Bitmap blank(int color) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }

    private static long alphaSum(Bitmap bitmap) {
        long sum = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) sum += Color.alpha(bitmap.getPixel(x, y));
        }
        return sum;
    }

    private static Rect inkBounds(Bitmap bitmap) {
        Rect bounds = new Rect();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) bounds.union(x, y, x + 1, y + 1);
            }
        }
        return bounds;
    }

    private static void samePixels(Bitmap expected, Bitmap actual, String description) {
        int differences = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (expected.getPixel(x, y) != actual.getPixel(x, y)) differences++;
            }
        }
        require(differences == 0, description + ": differing pixels=" + differences);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
