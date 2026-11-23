package com.cyzah.circular.core.graphics;
import com.cyzah.circular.domain.settings.BatteryNumberPlacement;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.core.graphics.geometry.BatteryShapeGeometry;
import com.cyzah.circular.core.graphics.IconPainter;
import com.cyzah.circular.core.graphics.BatteryShapeRenderer;

import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.lang.reflect.InvocationTargetException;

/** Numeric-only software Canvas checks. Run on Android; never writes image files. */
public final class BatteryNumberRenderingTest {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 140;
    private static final int MARGIN = 20;
    private static final int BACKGROUND = 0xFF123456;
    private static final int OUTSIDE_LEFT = 224;

    public static void main(String[] args) throws Exception {
        // app_process lacks ActivityThread's usual font-map initialization.
        java.lang.reflect.Method fonts = android.graphics.Typeface.class
                .getDeclaredMethod("loadPreinstalledSystemFontMap");
        fonts.setAccessible(true);
        fonts.invoke(null);
        String[] tests = {
                "insideGlyphsRevealExistingBackgroundWithoutARectangularHole",
                "insideGlyphsAreTransparentOnAnEmptyCanvas",
                "outsideDigitsAreWhiteWhenBatteryFillIsWhite",
                "outsideDigitsFollowTheChargedFillTint",
                "translucentBatteryStillHasFullyTransparentGlyphs",
                "cutoutDoesNotLeakIntoSharedPaintOrSubsequentDrawing",
                "compressedBatteryHasStraightSidesAndTheConfiguredStrokeWidth",
                "lastCompressedFrameMeetsTheRoundPointWithoutAnEdgeJump",
                "compressedEndsMatchTheTwoPointSemicircles",
                "nearNarrowCapsKeepTheSameAntialiasCoverageAtEveryChargeLevel"
        };
        int failures = 0;
        for (String test : tests) {
            try {
                BatteryNumberRenderingTest.class.getDeclaredMethod(test).invoke(null);
                System.out.println("PASS " + test);
            } catch (InvocationTargetException failure) {
                failures++;
                System.out.println("FAIL " + test + ": " + failure.getCause());
            }
        }
        if (failures != 0) {
            throw new AssertionError(failures + " of " + tests.length + " rendering checks failed");
        }
        System.out.println("Battery number rendering checks passed (" + tests.length + ")");
    }

    private static void insideGlyphsRevealExistingBackgroundWithoutARectangularHole() {
        Bitmap hidden = render(BatteryNumberPlacement.HIDDEN, BACKGROUND, Color.WHITE, 1);
        Bitmap inside = render(BatteryNumberPlacement.INSIDE, BACKGROUND, Color.WHITE, 1);
        try {
            int holes = 0;
            int minX = WIDTH, minY = HEIGHT, maxX = -1, maxY = -1;
            for (int y = MARGIN; y < 114; y++) {
                for (int x = MARGIN; x < 201; x++) {
                    if (hidden.getPixel(x, y) != BACKGROUND
                            && inside.getPixel(x, y) == BACKGROUND) {
                        holes++;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            require(holes > 100, "Inside digits must reveal the background; hole pixels=" + holes);
            int retained = 0;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (inside.getPixel(x, y) == hidden.getPixel(x, y)
                            && inside.getPixel(x, y) != BACKGROUND) retained++;
                }
            }
            require(retained > 50, "Digit counters/gaps must retain battery fill, not a rectangle");
            equal(hidden.getPixel(30, 67), inside.getPixel(30, 67), "Left body outside glyphs");
            equal(hidden.getPixel(190, 67), inside.getPixel(190, 67), "Right body outside glyphs");
            equal(BACKGROUND, inside.getPixel(5, 5), "Background outside layer");
        } finally {
            hidden.recycle();
            inside.recycle();
        }
    }

    private static void insideGlyphsAreTransparentOnAnEmptyCanvas() {
        Bitmap hidden = render(BatteryNumberPlacement.HIDDEN, Color.TRANSPARENT, Color.WHITE, 1);
        Bitmap inside = render(BatteryNumberPlacement.INSIDE, Color.TRANSPARENT, Color.WHITE, 1);
        try {
            int holes = 0;
            int remainingBody = 0;
            for (int y = MARGIN; y < 114; y++) {
                for (int x = MARGIN; x < 201; x++) {
                    if (Color.alpha(hidden.getPixel(x, y)) != 255) continue;
                    int alpha = Color.alpha(inside.getPixel(x, y));
                    if (alpha == 0) holes++;
                    if (alpha == 255) remainingBody++;
                }
            }
            require(holes > 100, "Transparent-canvas glyph pixels=" + holes);
            require(remainingBody > 500, "The cutout must preserve the surrounding body");
        } finally {
            hidden.recycle();
            inside.recycle();
        }
    }

    private static void outsideDigitsAreWhiteWhenBatteryFillIsWhite() {
        Bitmap outside = render(BatteryNumberPlacement.OUTSIDE, BACKGROUND, Color.WHITE, 1);
        try {
            int white = count(outside, Color.WHITE, OUTSIDE_LEFT, WIDTH);
            require(white > 100, "Outside white digit pixels=" + white);
            equal(0, count(outside, Color.BLACK, OUTSIDE_LEFT, WIDTH),
                    "Outside digits must not inherit the supplied black plain tint");
        } finally {
            outside.recycle();
        }
    }

    private static void outsideDigitsFollowTheChargedFillTint() {
        int tint = 0xFF19CDAA;
        Bitmap outside = render(BatteryNumberPlacement.OUTSIDE, BACKGROUND, tint, 1);
        try {
            equal(tint, outside.getPixel(30, 67), "Charged body fixture");
            int tinted = count(outside, tint, OUTSIDE_LEFT, WIDTH);
            require(tinted > 100, "Outside digits must use charged fill tint; pixels=" + tinted);
        } finally {
            outside.recycle();
        }
    }

    private static void translucentBatteryStillHasFullyTransparentGlyphs() {
        Bitmap hidden = render(BatteryNumberPlacement.HIDDEN, BACKGROUND, Color.WHITE, 0.5f);
        Bitmap inside = render(BatteryNumberPlacement.INSIDE, BACKGROUND, Color.WHITE, 0.5f);
        try {
            int holes = 0;
            for (int y = MARGIN; y < 114; y++) {
                for (int x = MARGIN; x < 201; x++) {
                    if (hidden.getPixel(x, y) != BACKGROUND
                            && inside.getPixel(x, y) == BACKGROUND) holes++;
                }
            }
            require(holes > 100, "Whole-body alpha must not weaken cutout strength; pixels=" + holes);
        } finally {
            hidden.recycle();
            inside.recycle();
        }
    }

    private static void cutoutDoesNotLeakIntoSharedPaintOrSubsequentDrawing() {
        IconPainter painter = painter();
        BatteryShapeRenderer renderer = new BatteryShapeRenderer();
        BlendMode before = painter.textPaint.getBlendMode();
        Bitmap inside = render(renderer, painter, BatteryNumberPlacement.INSIDE,
                BACKGROUND, Color.WHITE, 1);
        try {
            require(painter.textPaint.getBlendMode() == before,
                    "Shared text paint blend mode must be restored");
            Bitmap outside = render(renderer, painter, BatteryNumberPlacement.OUTSIDE,
                    BACKGROUND, Color.WHITE, 1);
            try {
                require(count(outside, Color.WHITE, OUTSIDE_LEFT, WIDTH) > 100,
                        "A later outside readout must remain visible");
            } finally {
                outside.recycle();
            }
            Bitmap text = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            try {
                text.eraseColor(BACKGROUND);
                Canvas canvas = new Canvas(text);
                painter.textPaint.setColor(Color.WHITE);
                painter.textPaint.setAlpha(255);
                painter.textPaint.setTextSize(60);
                painter.textPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("88", 20, 90, painter.textPaint);
                require(count(text, Color.WHITE, 0, WIDTH) > 100,
                        "Later shared-paint glyphs must paint rather than erase");
            } finally {
                text.recycle();
            }
        } finally {
            inside.recycle();
        }
    }

    private static Bitmap render(BatteryNumberPlacement placement, int background,
                                 int batteryTint, float alpha) {
        return render(new BatteryShapeRenderer(), painter(), placement, background, batteryTint, alpha);
    }

    private static void compressedBatteryHasStraightSidesAndTheConfiguredStrokeWidth() {
        for (float stroke : new float[] {6, 12, 20}) {
            BatteryShapeGeometry geometry = collapseGeometry(0.65f, stroke);
            Bitmap bitmap = renderCollapse(geometry);
            try {
                int left = Math.round((geometry.centreX - geometry.width / 2) * 4);
                int top = Math.round((geometry.centreY - geometry.height / 2) * 4);
                int bottom = Math.round((geometry.centreY + geometry.height / 2) * 4);
                int right = left + Math.round(stroke * 4);
                int y = top + Math.round(geometry.cornerRadius * 4) + 3;
                require(Color.alpha(bitmap.getPixel(left + 1, y)) == 255,
                        "Compressed left edge must be straight below the semicircle");
                require(Color.alpha(bitmap.getPixel(right - 2, y)) == 255,
                        "Rectangle width must equal the configured ring stroke");
                require(Color.alpha(bitmap.getPixel(left + 1, bottom - (y - top))) == 255,
                        "Compressed lower left edge must be straight above the corner");
                require(Color.alpha(bitmap.getPixel(left, top)) == 0,
                        "The rectangle must retain a rounded corner");
            } finally {
                bitmap.recycle();
            }
        }
    }

    private static void lastCompressedFrameMeetsTheRoundPointWithoutAnEdgeJump() {
        Bitmap before = renderCollapse(collapseGeometry(0.99999f, 12));
        Bitmap point = renderCollapse(collapseGeometry(1, 12));
        try {
            int substantialDifferences = 0;
            for (int y = 0; y < before.getHeight(); y++) {
                for (int x = 0; x < before.getWidth(); x++) {
                    if (Math.abs(Color.alpha(before.getPixel(x, y))
                            - Color.alpha(point.getPixel(x, y))) > 24) substantialDifferences++;
                }
            }
            require(substantialDifferences < 8,
                    "Point handoff changed the actual contour: pixels=" + substantialDifferences);
        } finally {
            before.recycle();
            point.recycle();
        }
    }

    private static void compressedEndsMatchTheTwoPointSemicircles() {
        for (float stroke : new float[] {6, 12, 20}) {
            for (float progress : new float[] {0.65f, 0.8f, 0.95f}) {
                BatteryShapeGeometry geometry = collapseGeometry(progress, stroke);
                Bitmap actual = renderCollapse(geometry);
                Bitmap expected = Bitmap.createBitmap(500, 420, Bitmap.Config.ARGB_8888);
                try {
                    float radius = stroke * 2; // Four pixels per geometry unit.
                    float centreX = geometry.centreX * 4;
                    float top = (geometry.centreY - geometry.height / 2) * 4;
                    float bottom = (geometry.centreY + geometry.height / 2) * 4;
                    Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
                    mask.setColor(Color.WHITE);
                    Canvas canvas = new Canvas(expected);
                    canvas.drawCircle(centreX, top + radius, radius, mask);
                    canvas.drawCircle(centreX, bottom - radius, radius, mask);
                    int differences = 0;
                    for (int y = (int) Math.floor(top); y < Math.ceil(bottom); y++) {
                        if (y + 0.5f >= top + radius && y + 0.5f <= bottom - radius) continue;
                        for (int x = (int) (centreX - radius) - 1; x <= centreX + radius; x++) {
                            if (Math.abs(Color.alpha(actual.getPixel(x, y))
                                    - Color.alpha(expected.getPixel(x, y))) > 24) differences++;
                        }
                    }
                    require(differences < 8, "End caps differ from point semicircles: stroke="
                            + stroke + " progress=" + progress + " pixels=" + differences);
                } finally {
                    actual.recycle();
                    expected.recycle();
                }
            }
        }
    }

    private static BatteryShapeGeometry collapseGeometry(float collapse, float stroke) {
        BatteryShapeGeometry geometry = new BatteryShapeGeometry();
        geometry.update(collapse, 60, 50, 112, 53.1667f, 60, 50, stroke, stroke);
        return geometry;
    }

    private static void nearNarrowCapsKeepTheSameAntialiasCoverageAtEveryChargeLevel() {
        for (float progress : new float[] {0.60f, 0.63f, 0.64999f, 0.65f, 0.7f}) {
            for (float stroke : new float[] {6, 12, 20}) {
                BatteryShapeGeometry geometry = collapseGeometry(progress, stroke);
                Bitmap empty = renderCollapse(geometry, 0, 1);
                try {
                    for (int battery : new int[] {1, 20, 50, 88, 100}) {
                        Bitmap charged = renderCollapse(geometry, battery, 1);
                        try {
                            int maxAlphaDifference = 0;
                            for (int y = 0; y < empty.getHeight(); y++) {
                                for (int x = 0; x < empty.getWidth(); x++) {
                                    maxAlphaDifference = Math.max(maxAlphaDifference,
                                            Math.abs(Color.alpha(empty.getPixel(x, y))
                                                    - Color.alpha(charged.getPixel(x, y))));
                                }
                            }
                            require(maxAlphaDifference <= 2,
                                    "Charge overlay changes cap edge coverage: progress=" + progress
                                            + " stroke=" + stroke + " charge=" + battery
                                            + " alpha difference=" + maxAlphaDifference);
                        } finally {
                            charged.recycle();
                        }
                    }
                } finally {
                    empty.recycle();
                }
            }
        }
    }

    private static Bitmap renderCollapse(BatteryShapeGeometry geometry) {
        return renderCollapse(geometry, 100, 4);
    }

    private static Bitmap renderCollapse(BatteryShapeGeometry geometry, int battery, float scale) {
        Bitmap bitmap = Bitmap.createBitmap(500, 420, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(scale, scale);
        IconState state = new IconState();
        state.battery = battery;
        new BatteryShapeRenderer().draw(canvas, painter(), state, geometry,
                BatteryNumberPlacement.HIDDEN, Color.WHITE, Color.WHITE, 1);
        return bitmap;
    }

    private static Bitmap render(BatteryShapeRenderer renderer, IconPainter painter,
                                 BatteryNumberPlacement placement, int background,
                                 int batteryTint, float alpha) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(background);
        Canvas canvas = new Canvas(bitmap);
        BatteryShapeGeometry geometry = new BatteryShapeGeometry();
        geometry.width = 198.44f;
        geometry.height = 94.2f;
        geometry.centreX = MARGIN + geometry.width / 2;
        geometry.centreY = MARGIN + geometry.height / 2;
        geometry.stage = BatteryShapeGeometry.Stage.COMPLETE;
        geometry.batteryContentAlpha = 1;
        IconState state = new IconState();
        state.battery = 88;
        renderer.draw(canvas, painter, state, geometry, placement,
                batteryTint, Color.BLACK, alpha);
        return bitmap;
    }

    private static IconPainter painter() {
        IconPainter painter = new IconPainter();
        painter.setBatteryWeightPercent(145);
        return painter;
    }

    private static int count(Bitmap bitmap, int color, int left, int right) {
        int count = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = left; x < right; x++) {
                if (bitmap.getPixel(x, y) == color) count++;
            }
        }
        return count;
    }

    private static void equal(int expected, int actual, String description) {
        require(expected == actual, description + ": expected=0x"
                + Integer.toHexString(expected) + " actual=0x" + Integer.toHexString(actual));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
