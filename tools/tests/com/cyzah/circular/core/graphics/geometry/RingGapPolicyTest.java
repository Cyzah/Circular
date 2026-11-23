package com.cyzah.circular.core.graphics.geometry;
import com.cyzah.circular.domain.settings.DualStyle;

public final class RingGapPolicyTest {
    private RingGapPolicyTest() {
    }

    public static void main(String[] args) {
        groupedAndUpperRowsDriveTheLowerGap();
        widerPointsWidenTheLowerGap();
        hiddenBatteryReadoutRemovesTheUpperGap();
        thicknessDoesNotChangeTheUpperOpening();
        upperAndLowerOpeningsUseIndependentContent();
        layeredSideClearanceShrinksThreeUnits();
        groupedTechnologyMovesDownThreeUnits();
        System.out.println("RingGapPolicyTest passed");
    }

    private static void groupedAndUpperRowsDriveTheLowerGap() {
        assertEquals(64.14f, RingGapPolicy.lowerGapWidth(DualStyle.GROUPED, 1.2f, 0.0f),
                "the grouped four-dot row determines its lower opening");
        assertEquals(46.68f, RingGapPolicy.lowerGapWidth(DualStyle.LAYERED, 1.2f, 0.0f),
                "the layered style's upper three-dot row determines its lower opening");
    }

    private static void widerPointsWidenTheLowerGap() {
        assertTrue(RingGapPolicy.lowerGapWidth(DualStyle.GROUPED, 1.5f, 0.0f)
                        > RingGapPolicy.lowerGapWidth(DualStyle.GROUPED, 1.2f, 0.0f),
                "grouped opening tracks point thickness");
        assertTrue(RingGapPolicy.lowerGapWidth(DualStyle.LAYERED, 1.5f, 0.0f)
                        > RingGapPolicy.lowerGapWidth(DualStyle.LAYERED, 1.2f, 0.0f),
                "layered opening tracks point thickness");
    }

    private static void hiddenBatteryReadoutRemovesTheUpperGap() {
        assertEquals(0.0f, RingGapPolicy.topGapWidth(false, false, 42.0f, 70.0f),
                "hiding the battery numeral removes the upper opening");
        assertEquals(42.0f, RingGapPolicy.topGapWidth(true, false, 42.0f, 70.0f),
                "the upper opening follows the battery numeral width");
        assertEquals(70.0f, RingGapPolicy.topGapWidth(false, true, 42.0f, 70.0f),
                "swapping content makes the upper opening follow the network header");
    }

    private static void upperAndLowerOpeningsUseIndependentContent() {
        float groupedBottom = RingGapPolicy.lowerHalfAngle(DualStyle.GROUPED, 1.2f);
        float layeredBottom = RingGapPolicy.lowerHalfAngle(DualStyle.LAYERED, 1.2f);
        assertEquals(55.0f, groupedBottom, "grouped keeps the original initial lower opening");
        assertTrue(layeredBottom >= 49.39f, "layered closes both lower-ring side gaps by three units");
        assertTrue(RingGapPolicy.lowerHalfAngle(DualStyle.GROUPED, 1.5f) > 55.0f,
                "thicker grouped points expand the lower opening");
        assertTrue(RingGapPolicy.lowerHalfAngle(DualStyle.LAYERED, 1.5f) > 49.39f,
                "thicker layered points expand the lower opening");
        assertEquals(62.923f, RingGapPolicy.lowerHalfAngle(DualStyle.GROUPED, 1.5f),
                "grouped lower opening uses half of the point-driven angle delta");
        assertEquals(53.820f, RingGapPolicy.lowerHalfAngle(DualStyle.LAYERED, 1.5f),
                "layered lower opening uses half of the point-driven angle delta");
    }

    private static void thicknessDoesNotChangeTheUpperOpening() {
        assertEquals(36.015f, RingGapPolicy.topHalfAngle(true, false, 42.0f, 70.0f),
                "the upper opening depends on numeral width, not signal thickness");
        assertEquals(0.0f, RingGapPolicy.topHalfAngle(false, false, 42.0f, 70.0f),
                "a hidden numeral leaves no upper half-angle");
    }

    private static void layeredSideClearanceShrinksThreeUnits() {
        float halfAngle = RingGapPolicy.lowerHalfAngle(DualStyle.LAYERED, 0.96f);
        assertEquals(49.39f, halfAngle, "layered minimum opening follows the three-unit side clearance");
    }

    private static void groupedTechnologyMovesDownThreeUnits() {
        assertEquals(60.0f, RingGapPolicy.technologyCentreY(DualStyle.GROUPED, true),
                "grouped technology centre moves down three units");
        assertEquals(58.0f, RingGapPolicy.technologyCentreY(DualStyle.GROUPED, false),
                "grouped technology is centred when digits are hidden");
        assertEquals(55.0f, RingGapPolicy.technologyCentreY(DualStyle.LAYERED, false),
                "layered technology moves up two units when digits are hidden");
        assertEquals(57.0f, RingGapPolicy.technologyCentreY(DualStyle.LAYERED, true),
                "layered technology keeps the normal centre with digits visible");
        assertEquals(58.0f, RingGapPolicy.wifiCentreY(DualStyle.GROUPED, false, 57.0f),
                "grouped wifi is vertically centred when digits are hidden");
        assertEquals(57.0f, RingGapPolicy.wifiCentreY(DualStyle.GROUPED, true, 57.0f),
                "grouped wifi keeps the normal position with digits visible");
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
