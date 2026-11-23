package com.cyzah.circular.core.graphics.geometry;

public final class SignalSpacingTest {
    private SignalSpacingTest() {
    }

    public static void main(String[] args) {
        thickerDotsKeepTheirEdgeGap();
        maximumThicknessFitsTheAvailableWidth();
        pairedRowsStaySeparatedAsDotsGrow();
        signalRowsStayOnTheirRingsAtEveryThickness();
        onlyTheLayeredOuterRowUsesTheFiveUnitDrop();
        doubleLayerSpacingStopsAtDefaultThickness();
        doubleLayerGapIsFixed();
        minimumThicknessMovesTheLayerThreeMoreUnits();
        groupedRowsUseTheSameGapInsideAndBetweenGroups();
        groupedRowsRemainOnTheRingAfterSpacingCompression();
        previewUsesTheSameSignalThicknessAsTheDevice();
        System.out.println("SignalSpacingTest passed");
    }

    private static void thickerDotsKeepTheirEdgeGap() {
        float[] base = SignalSpacing.rowCentres(4, 60.0f, 1.2f, 5.7f, 84.0f);
        float[] thick = SignalSpacing.rowCentres(4, 60.0f, 1.5f, 5.7f, 84.0f);
        assertEquals(33.81f, base[0], "default first point matches the existing geometry");
        assertEquals(51.27f, base[1], "default second point matches the existing geometry");
        assertEquals(20.4f, thick[1] - thick[0], "thicker dots keep the same visible edge gap");
        assertEquals(60.0f, (thick[0] + thick[3]) / 2.0f, "adaptive rows stay centred");
    }

    private static void maximumThicknessFitsTheAvailableWidth() {
        float[] maximum = SignalSpacing.rowCentres(4, 60.0f, 1.8f, 5.7f, 84.0f);
        float radius = SignalSpacing.radius(1.8f);
        assertEquals(84.0f, (maximum[3] - maximum[0]) + (2.0f * radius),
                "the row compresses only when a fixed gap would overflow the ring");
    }

    private static void pairedRowsStaySeparatedAsDotsGrow() {
        float[] base = SignalSpacing.pairCentres(98.8f, 1.2f, 3.6f);
        float[] thick = SignalSpacing.pairCentres(98.8f, 1.5f, 3.6f);
        assertTrue(base[1] - base[0] < thick[1] - thick[0],
                "the two rows move apart as dot radius grows");
        assertEquals(3.6f, (thick[1] - thick[0]) - (2.0f * SignalSpacing.radius(1.5f)),
                "the two visible rows retain the target edge gap");
    }

    private static void signalRowsStayOnTheirRingsAtEveryThickness() {
        float[] weights = {1.2f, 1.5f, 1.8f};
        for (float weight : weights) {
            float[] outer = SignalSpacing.outerRow(weight);
            float[] inner = SignalSpacing.innerRow(weight);
            for (int i = 0; i < outer.length / 2; i++) {
                float centreY = SignalSpacing.outerCentreY(weight);
                assertEquals(50.0f, distanceFrom(outer[i * 2], outer[i * 2 + 1], 60.0f, centreY),
                        "outer points keep the ring radius on their shifted circle");
            }
            for (int i = 0; i < inner.length / 2; i++) {
                assertEquals(38.0f, distanceFrom(inner[i * 2], inner[i * 2 + 1], 60.0f, 54.5f),
                        "inner points keep their radius after the latest vertical calibration");
            }
        }
    }

    private static void onlyTheLayeredOuterRowUsesTheFiveUnitDrop() {
        assertEquals(50.0f, distanceFrom(
                        SignalSpacing.outerX(0, 1.2f),
                        SignalSpacing.ringY(0, 1.2f),
                        60.0f,
                        58.0f),
                "grouped and single-card rows stay on the battery ring centre");
        assertTrue(SignalSpacing.outerCentreY(1.2f) > 58.0f,
                "layered outer row uses a shifted-down circle");
    }

    private static void doubleLayerSpacingStopsAtDefaultThickness() {
        assertEquals(SignalSpacing.outerX(0, 1.2f), SignalSpacing.outerX(0, 1.5f),
                "outer spacing stops expanding at default thickness");
        assertEquals(SignalSpacing.outerX(3, 1.2f), SignalSpacing.outerX(3, 1.5f),
                "outer row span stops expanding at default thickness");
        assertEquals(SignalSpacing.innerX(0, 1.2f), SignalSpacing.innerX(0, 1.5f),
                "inner spacing stops in sync with the outer row");
        assertEquals(SignalSpacing.innerX(2, 1.2f), SignalSpacing.innerX(2, 1.5f),
                "inner row span stops in sync with the outer row");
    }

    private static void doubleLayerGapIsFixed() {
        float[] weights = {0.96f, 1.2f, 1.5f, 1.8f};
        for (float weight : weights) {
            assertEquals(17.086f, averageOuterY(weight) - averageInnerY(weight),
                    "double-layer centreline gap stays fixed after the two-unit reduction");
        }
    }

    private static void minimumThicknessMovesTheLayerThreeMoreUnits() {
        assertEquals(90.412f, averageInnerY(0.96f),
                "the whole double-layer basis moves 1.5 units back down");
    }

    private static void groupedRowsUseTheSameGapInsideAndBetweenGroups() {
        float[] grouped = SignalSpacing.groupedRow(1.5f);
        float firstGap = grouped[2] - grouped[0];
        float groupGap = grouped[4] - grouped[2];
        float secondGap = grouped[6] - grouped[4];
        assertEquals(firstGap, groupGap, "the gap between groups matches the first in-group gap");
        assertEquals(firstGap, secondGap, "the gap between groups matches the second in-group gap");
    }

    private static void groupedRowsRemainOnTheRingAfterSpacingCompression() {
        assertEquals(SignalSpacing.ringX(0, 1.8f), SignalSpacing.groupedX(0, 1.8f),
                "grouped x uses the same width-compressed row as the ring");
        assertEquals(SignalSpacing.ringX(3, 1.8f), SignalSpacing.groupedX(3, 1.8f),
                "all grouped points stay on the same row at high thickness");
        float[] points = SignalSpacing.groupedRow(1.8f);
        for (int i = 0; i < 4; i++) {
            assertEquals(50.0f, distanceFrom(points[i * 2], points[i * 2 + 1], 60.0f, 58.0f),
                    "grouped point remains on the battery-ring radius");
        }
    }

    private static void previewUsesTheSameSignalThicknessAsTheDevice() {
        assertEquals(1.2f, SignalSpacing.scaleFromPercent(100),
                "100 percent uses the device signal weight scale");
        assertEquals(1.62f, SignalSpacing.scaleFromPercent(135),
                "the preview scale keeps the slider's actual ratio");
        assertEquals(5.88f, SignalSpacing.dotRadiusFromPercent(100),
                "100 percent produces the device's 5.88-unit dot radius");
        assertEquals(10.584f, SignalSpacing.dotRadiusFromPercent(180),
                "the maximum slider value produces the actual thick dot radius");
    }

    private static float averageOuterY(float weight) {
        float sum = 0.0f;
        for (int i = 0; i < 4; i++) {
            sum += SignalSpacing.outerY(i, weight);
        }
        return sum / 4.0f;
    }

    private static float averageInnerY(float weight) {
        float sum = 0.0f;
        for (int i = 0; i < 3; i++) {
            sum += SignalSpacing.innerY(i, weight);
        }
        return sum / 3.0f;
    }

    private static float distanceFrom(float x, float y, float centreX, float centreY) {
        float dx = x - centreX;
        float dy = y - centreY;
        return (float) Math.sqrt((dx * dx) + (dy * dy));
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (!(Math.abs(expected - actual) <= 0.001f)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
