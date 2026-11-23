package com.cyzah.circular.domain.settings;

public final class DualStyleTest {
    private DualStyleTest() {
    }

    public static void main(String[] args) {
        duoWinsOverStaleLegacyFlags();
        existingStylePriorityIsPreserved();
        capsuleHidesCapsuleSpecificControls();
        System.out.println("DualStyleTest passed");
    }

    private static void duoWinsOverStaleLegacyFlags() {
        assertEquals(DualStyle.DUO, DualStyle.from(true, true, true),
                "stale grouped and capsule flags cannot hide the selected DUO style");
        assertEquals(DualStyle.DUO, DualStyle.from(false, false, true),
                "DUO is selected from its persisted flag");
    }

    private static void existingStylePriorityIsPreserved() {
        assertEquals(DualStyle.LAYERED, DualStyle.from(false, false, false),
                "layered is the default");
        assertEquals(DualStyle.GROUPED, DualStyle.from(true, false, false),
                "grouped remains selectable");
        assertEquals(DualStyle.CAPSULE, DualStyle.from(true, true, false),
                "capsule keeps its legacy priority");
    }

    private static void capsuleHidesCapsuleSpecificControls() {
        assertTrue(!DualStyle.CAPSULE.allowsShowPercent(), "capsule hides the percent visibility setting");
        assertTrue(!DualStyle.CAPSULE.allowsSwapContents(), "capsule hides swap contents");
        assertTrue(DualStyle.DUO.allowsShowPercent(), "DUO keeps the percent visibility setting");
        assertTrue(DualStyle.DUO.allowsSwapContents() == false, "DUO hides swap contents");
        assertTrue(DualStyle.GROUPED.allowsShowPercent(), "grouped keeps percent visibility");
        assertTrue(DualStyle.GROUPED.allowsSwapContents(), "grouped keeps swap contents");
        assertTrue(DualStyle.LAYERED.allowsShowPercent(), "layered keeps percent visibility");
        assertTrue(DualStyle.LAYERED.allowsSwapContents(), "layered keeps swap contents");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(DualStyle expected, DualStyle actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
