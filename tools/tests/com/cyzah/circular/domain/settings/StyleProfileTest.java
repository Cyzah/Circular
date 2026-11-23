package com.cyzah.circular.domain.settings;

public final class StyleProfileTest {
    private StyleProfileTest() {
    }

    public static void main(String[] args) {
        capsuleDefaultsMatchReference();
        duoDefaultsMatchReference();
        truncatedLegacyProfileIsRejected();
        System.out.println("StyleProfileTest passed");
    }

    private static void capsuleDefaultsMatchReference() {
        StyleProfile profile = StyleProfile.defaults(DualStyle.CAPSULE);
        assertEquals(22, profile.size, "capsule size");
        assertEquals(135, profile.weight, "capsule ring weight");
        assertEquals(120, profile.wifiScale, "capsule wifi scale");
        assertEquals(110, profile.wifiWeight, "capsule wifi weight");
        assertEquals(110, profile.technologyScale, "capsule technology scale");
        assertEquals(110, profile.technologyWeight, "capsule technology weight");
        assertFalse(profile.showPercent, "capsule show percent");
        assertEquals(80, profile.percentScale, "capsule percent scale");
        assertEquals(145, profile.percentWeight, "capsule percent weight");
        assertFalse(profile.swapContents, "capsule swap layout");
    }

    private static void duoDefaultsMatchReference() {
        StyleProfile profile = StyleProfile.defaults(DualStyle.DUO);
        assertEquals(26, profile.size, "DUO size");
        assertEquals(100, profile.weight, "DUO ring weight");
        assertEquals(95, profile.wifiScale, "DUO wifi scale");
        assertEquals(110, profile.wifiWeight, "DUO wifi weight");
        assertEquals(95, profile.technologyScale, "DUO technology scale");
        assertEquals(110, profile.technologyWeight, "DUO technology weight");
        assertTrue(profile.showPercent, "DUO show percent");
        assertEquals(100, profile.percentScale, "DUO percent scale");
        assertEquals(140, profile.percentWeight, "DUO percent weight");
        assertFalse(profile.swapContents, "DUO swap layout");
    }

    private static void truncatedLegacyProfileIsRejected() {
        assertTrue(StyleProfile.decode("v1|22|135") == null, "truncated v1 profile is ignored");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
