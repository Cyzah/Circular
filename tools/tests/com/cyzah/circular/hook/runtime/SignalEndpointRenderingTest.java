package com.cyzah.circular.hook.runtime;
import com.cyzah.circular.domain.settings.DualStyle;
import com.cyzah.circular.domain.settings.BatteryNumberPlacement;
import com.cyzah.circular.domain.settings.AppearanceScale;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.core.graphics.geometry.IconGeometry;
import com.cyzah.circular.core.graphics.geometry.DuoLayout;
import com.cyzah.circular.core.graphics.StackedSignals;
import com.cyzah.circular.core.graphics.StackedOverlayRenderer;
import com.cyzah.circular.core.graphics.IconPainter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/** Real overlay samples checked numerically; no screenshots or image files. */
public final class SignalEndpointRenderingTest {
    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method fonts = android.graphics.Typeface.class
                .getDeclaredMethod("loadPreinstalledSystemFontMap");
        fonts.setAccessible(true);
        fonts.invoke(null);
        try {
            configuredDotGeometrySurvivesTheCollapsedEndpoint();
            singleSimKeepsFourDistinctOuterArcTargets();
            expandedBarsDoNotInheritStatusSizeOrWeight();
            duoTechnologyUsesTheSameFittedSizeAsTheStaticIcon();
        } catch (Throwable failure) {
            failure.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("Signal endpoint rendering checks passed (4)");
    }

    private static void singleSimKeepsFourDistinctOuterArcTargets() {
        Fixture fixture = new Fixture(DualStyle.LAYERED, 150, 100);
        fixture.state.secondarySimPresent = false;
        Bitmap image = fixture.render(0, 0.3f, 150, false);
        try {
            float scale = 1.8f;
            for (int dot = 0; dot < 4; dot++) {
                close(20 + IconGeometry.singleDotX(dot, scale) * 0.3f,
                        fixture.target.x[dot], "Single SIM dot x " + dot);
                close(20 + IconGeometry.singleDotY(dot, scale) * 0.3f,
                        fixture.target.y[dot], "Single SIM dot y " + dot);
            }
            if (!(fixture.target.x[3] > fixture.target.x[2])) {
                throw new AssertionError("Fourth single-SIM dot collapsed into the third inner dot");
            }
        } finally {
            image.recycle();
        }
    }

    private static void configuredDotGeometrySurvivesTheCollapsedEndpoint() {
        for (DualStyle style : new DualStyle[] {DualStyle.LAYERED, DualStyle.GROUPED, DualStyle.DUO}) {
            for (int weight : new int[] {80, 180}) {
                for (float unit : new float[] {0.15f, 0.3f}) {
                    Fixture fixture = new Fixture(style, weight, 100);
                    Bitmap image = fixture.render(0, unit, weight, false);
                    try {
                        float scale = weight * 0.012f;
                        close(4.9f * scale * unit, fixture.target.radius[0], "Configured dot radius");
                        float expectedX, expectedY;
                        if (style == DualStyle.DUO) {
                            expectedX = DuoLayout.dotX(0, scale);
                            expectedY = DuoLayout.dotY(0, 0, scale);
                        } else if (style == DualStyle.GROUPED) {
                            expectedX = IconGeometry.groupedDotX(0, 0, scale);
                            expectedY = IconGeometry.groupedDotY(0, scale);
                        } else {
                            expectedX = IconGeometry.innerDotX(0, 0, scale);
                            expectedY = IconGeometry.innerDotY(0, 0, scale);
                        }
                        close(20 + expectedX * unit, fixture.target.x[0], "Configured dot x");
                        close(20 + expectedY * unit, fixture.target.y[0], "Configured dot y");
                    } finally {
                        image.recycle();
                    }
                }
            }
        }
    }

    private static void expandedBarsDoNotInheritStatusSizeOrWeight() {
        Bitmap small = new Fixture(DualStyle.LAYERED, 80, 100).render(1, 0.15f, 80, false);
        Bitmap large = new Fixture(DualStyle.LAYERED, 180, 100).render(1, 0.3f, 180, false);
        try {
            for (int y = 0; y < small.getHeight(); y++) {
                for (int x = 0; x < small.getWidth(); x++) {
                    if (small.getPixel(x, y) != large.getPixel(x, y)) {
                        throw new AssertionError("Expanded control-center bars changed with status settings");
                    }
                }
            }
        } finally {
            small.recycle();
            large.recycle();
        }
    }

    private static void duoTechnologyUsesTheSameFittedSizeAsTheStaticIcon() {
        for (int percent : new int[] {80, 180}) {
            Fixture fixture = new Fixture(DualStyle.DUO, 150, percent);
            IconPainter actualStatic = painter(DualStyle.DUO, 150, percent);
            Bitmap staticImage = Bitmap.createBitmap(200, 160, Bitmap.Config.ARGB_8888);
            Bitmap moving = fixture.render(0, 0.3f, 150, true);
            try {
                if (!"5GA".equals(fixture.state.centreTechnology()) || fixture.target.centreRow != 0) {
                    throw new AssertionError("DUO fixture must expose the primary 5GA endpoint");
                }
                actualStatic.drawPartMasked(new Canvas(staticImage), 0, 0, 120,
                        fixture.state, Color.WHITE, false,
                        1 | IconPainter.MASK_HIDE_BATTERY | IconPainter.MASK_HIDE_SIGNALS
                                | IconPainter.MASK_HIDE_WIFI, 1);
                int ink = 0;
                for (int y = 0; y < staticImage.getHeight(); y++) {
                    for (int x = 0; x < staticImage.getWidth(); x++) {
                        if (Color.alpha(staticImage.getPixel(x, y)) > 0) ink++;
                    }
                }
                if (ink == 0) throw new AssertionError("Static DUO label fixture did not draw glyphs");
                float staticSize = actualStatic.centreTextSize();
                close(staticSize * 0.3f, fixture.target.centreSize,
                        "DUO fitted technology size at user setting " + percent);
                close(20 + 112 * 0.3f, fixture.target.centreY, "DUO technology baseline");
                if (percent == 180 && !(staticSize < AppearanceScale.technologyTextSize(percent))) {
                    throw new AssertionError("Large DUO fixture must exercise actual text fitting");
                }
            } finally {
                staticImage.recycle();
                moving.recycle();
            }
        }
    }

    private static IconPainter painter(DualStyle style, int weight, int textScale) {
        IconPainter painter = new IconPainter();
        painter.setWeightPercent(weight);
        painter.setGroupedDots(style == DualStyle.GROUPED);
        painter.setDuoDual(style == DualStyle.DUO);
        painter.setDuoWifiProgress(0);
        painter.setCentreSizes(100, textScale);
        painter.setShowPercent(true);
        return painter;
    }

    private static final class Fixture {
        final StackedOverlayRenderer overlay = new StackedOverlayRenderer();
        final StackedSignals.Target target = new StackedSignals.Target();
        final StackedSignals signals = new StackedSignals();
        final IconPainter endpoint;
        final IconPainter host = painter(DualStyle.LAYERED, 100, 95);
        final IconState state = new IconState();
        final DualStyle style;

        Fixture(DualStyle style, int weight, int textScale) {
            this.style = style;
            endpoint = painter(style, weight, textScale);
            state.battery = 88;
            state.wifiKnown = true;
            state.simPresent = state.secondarySimPresent = true;
            state.mobileKnown = state.secondaryKnown = true;
            state.inService = state.secondaryInService = true;
            state.mobileLevel = state.secondaryLevel = 4;
            state.mobileSlot = 0;
            state.secondarySlot = 1;
            state.mobileTechnology = "5GA";
        }

        Bitmap render(float progress, float unit, int weight, boolean labels) {
            Bitmap image = Bitmap.createBitmap(240, 180, Bitmap.Config.ARGB_8888);
            overlay.frame(100, 220, 75, progress, progress, 20, 20, unit, unit,
                    1, progress, 0, 0, Color.WHITE, weight, false, false, true,
                    style, BatteryNumberPlacement.INSIDE);
            overlay.setTechnologyEndpointUnit(unit);
            overlay.setLabelsEnabled(labels);
            overlay.draw(new Canvas(image), host, endpoint, signals, target, state,
                    labels ? "5GA" : null, labels ? "5GA" : null);
            return image;
        }
    }

    private static void close(float expected, float actual, String message) {
        if (!(Math.abs(expected - actual) <= 0.0001f)) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }
}
