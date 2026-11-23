package com.cyzah.circular.core.graphics.geometry;

public final class DuoLayoutTest {
    private DuoLayoutTest() {
    }

    public static void main(String[] args) {
        signalRowsDockAtTheBottomWhenWifiConnects();
        thickerDuoDotsKeepAdaptiveSpacing();
        centreContentUsesStableVerticalSlots();
        batteryNumberUsesLayerReadoutRange();
        duoBatteryDigitsRespectTheVisibilitySwitch();
        dataSwitchOffMovesBatteryBelowTheRing();
        wifiRestoresTheConnectedDuoLayout();
        upperAndLowerGapsFollowTheirOwnContent();
        wifiAndTechnologyCrossFadeAtTheEndpoints();
        signalDotsRevealStrengthWithFourSteps();
        System.out.println("DuoLayoutTest passed");
    }

    private static void signalRowsDockAtTheBottomWhenWifiConnects() {
        assertEquals(48.92f, DuoLayout.dotY(0, 0.0f), "card one starts one unit above the ring centre");
        assertEquals(65.08f, DuoLayout.dotY(1, 0.0f), "card two starts one unit above the ring centre");
        assertEquals(86.42f, DuoLayout.dotY(0, 1.0f), "card one docks one unit higher");
        assertEquals(102.58f, DuoLayout.dotY(1, 1.0f), "card two docks one unit higher");
        assertEquals(67.67f, DuoLayout.dotY(0, 0.5f), "card one moves with progress from the raised layout");
        assertEquals(83.83f, DuoLayout.dotY(1, 0.5f), "card two moves with progress from the raised layout");

        assertEquals(35.76f, DuoLayout.dotX(0), "first dot follows the shared adaptive row geometry");
        assertEquals(84.24f, DuoLayout.dotX(3), "last dot follows the shared adaptive row geometry");
    }

    private static void thickerDuoDotsKeepAdaptiveSpacing() {
        float baseStep = DuoLayout.dotX(1, 1.2f) - DuoLayout.dotX(0, 1.2f);
        float thickStep = DuoLayout.dotX(1, 1.5f) - DuoLayout.dotX(0, 1.5f);
        assertTrue(thickStep > baseStep, "horizontal spacing grows with dot thickness");
        assertEquals(19.1f, thickStep, "DUO removes another 0.3 coordinates between dot edges");
        assertTrue(DuoLayout.dotY(1, 0.0f, 1.5f) - DuoLayout.dotY(0, 0.0f, 1.5f)
                        > DuoLayout.dotY(1, 0.0f) - DuoLayout.dotY(0, 0.0f),
                "row separation grows with dot thickness");
    }

    private static void wifiAndTechnologyCrossFadeAtTheEndpoints() {
        assertEquals(0.0f, DuoLayout.wifiAlpha(0.0f), "wifi is hidden without a connection");
        assertEquals(1.0f, DuoLayout.wifiAlpha(1.0f), "wifi is visible when connected");
        assertEquals(1.0f, DuoLayout.technologyAlpha(0.0f), "technology is visible without wifi");
        assertEquals(0.0f, DuoLayout.technologyAlpha(1.0f), "technology is hidden when wifi is connected");
        assertEquals(0.5f, DuoLayout.technologyAlpha(0.5f), "technology fades across the transition");
    }

    private static void centreContentUsesStableVerticalSlots() {
        assertEquals(14.0f, DuoLayout.batteryCentreY(), "battery readout occupies the upper ring opening");
        assertEquals(55.0f, DuoLayout.wifiCentreY(), "wifi sits three coordinates above the ring centre");
        assertEquals(112.0f, DuoLayout.technologyBaseline(), "technology stays in the bottom gap");
    }

    private static void batteryNumberUsesLayerReadoutRange() {
        assertEquals(1.0f, DuoLayout.fitBatteryReadout(40.0f, 30.0f, 82.0f),
                "ordinary DUO numerals keep the layered-style natural size");
        assertEquals(0.82f, DuoLayout.fitBatteryReadout(100.0f, 30.0f, 82.0f),
                "wide DUO numerals use the layered-style width cap");
        assertEquals(44.0f / 50.0f, DuoLayout.fitBatteryReadout(40.0f, 50.0f, 82.0f),
                "tall DUO numerals use the layered-style height cap");
    }

    private static void duoBatteryDigitsRespectTheVisibilitySwitch() {
        assertTrue(DuoLayout.showBatteryDigits(true, true), "DUO shows digits when enabled");
        assertTrue(!DuoLayout.showBatteryDigits(false, true), "DUO hides digits when disabled");
        assertTrue(!DuoLayout.showBatteryDigits(true, false), "DUO hides unknown battery values");
    }

    private static void dataSwitchOffMovesBatteryBelowTheRing() {
        assertTrue(DuoLayout.batteryMovesBelow(false, false),
                "DUO moves the battery below when both data and Wi-Fi are off");
        assertEquals(102.0f, DuoLayout.batteryCentreY(true),
                "the disconnected DUO readout occupies the lower opening");
        assertEquals(0.0f, DuoLayout.technologyAlpha(0.0f, false),
                "no cached network technology remains visible with data switched off");
    }

    private static void wifiRestoresTheConnectedDuoLayout() {
        assertTrue(!DuoLayout.batteryMovesBelow(true, false),
                "connected Wi-Fi keeps the battery in the upper opening");
        assertEquals(14.0f, DuoLayout.batteryCentreY(false),
                "the Wi-Fi layout restores the upper battery slot");
        assertEquals(1.0f, DuoLayout.technologyAlpha(0.0f, true),
                "technology remains visible when cellular data is enabled");
        assertEquals(0.0f, DuoLayout.technologyAlpha(1.0f, true),
                "technology yields to Wi-Fi after connection");
    }

    private static void upperAndLowerGapsFollowTheirOwnContent() {
        assertTrue(DuoLayout.gapHalfAngle(48.0f, 1.2f) > DuoLayout.gapHalfAngle(24.0f, 1.2f),
                "wider battery readouts widen the upper gap");
        assertEquals(27.0f, DuoLayout.bottomContentWidth(18.0f, 72.0f, 0.0f),
                "the technology opening gains the requested nine-unit base width");
        assertEquals(81.0f, DuoLayout.bottomContentWidth(18.0f, 72.0f, 1.0f),
                "the lower gap follows docked signal points when Wi-Fi is connected");
        assertEquals(54.0f, DuoLayout.bottomContentWidth(18.0f, 72.0f, 0.5f),
                "the lower gap follows the visible bottom content during the transition");
        assertEquals(112.0f, DuoLayout.sideArcSweep(28.0f, 40.0f),
                "the two side arcs share the space left outside both gaps");
    }

    private static void signalDotsRevealStrengthWithFourSteps() {
        assertEquals(255, DuoLayout.dotAlpha(true, 2, 0), "a lit dot is fully opaque");
        assertEquals(255, DuoLayout.dotAlpha(true, 2, 1), "the second lit dot is fully opaque");
        assertEquals(48, DuoLayout.dotAlpha(true, 2, 2), "dots above the signal level stay dim");
        assertEquals(48, DuoLayout.dotAlpha(false, 4, 0), "out-of-service rows stay dim");
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (!(Math.abs(expected - actual) <= 0.001f)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
