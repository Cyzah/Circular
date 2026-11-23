package com.cyzah.circular.domain.settings;

public final class SizePolicyTest {
    private SizePolicyTest() {
    }

    public static void main(String[] args) {
        capsuleKeepsLegacyRange();
        otherDualStylesUseExpandedRange();
        outOfRangeValuesClampToStyleBounds();
        System.out.println("SizePolicyTest passed");
    }

    private static void capsuleKeepsLegacyRange() {
        assertEquals(12, SizePolicy.minimum(DualStyle.CAPSULE), "capsule minimum");
        assertEquals(29, SizePolicy.maximum(DualStyle.CAPSULE), "capsule maximum");
    }

    private static void otherDualStylesUseExpandedRange() {
        for (DualStyle style : new DualStyle[]{DualStyle.LAYERED, DualStyle.GROUPED, DualStyle.DUO}) {
            assertEquals(18, SizePolicy.minimum(style), style + " minimum");
            assertEquals(29, SizePolicy.maximum(style), style + " maximum");
        }
    }

    private static void outOfRangeValuesClampToStyleBounds() {
        assertEquals(18, SizePolicy.clamp(DualStyle.LAYERED, 12), "small value clamps up");
        assertEquals(29, SizePolicy.clamp(DualStyle.DUO, 40), "large value clamps down");
        assertEquals(12, SizePolicy.clamp(DualStyle.CAPSULE, 8), "capsule keeps its lower minimum");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
