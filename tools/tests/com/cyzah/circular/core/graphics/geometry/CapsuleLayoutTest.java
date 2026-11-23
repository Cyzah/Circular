package com.cyzah.circular.core.graphics.geometry;

public final class CapsuleLayoutTest {
    private CapsuleLayoutTest() {
    }

    public static void main(String[] args) {
        centredPreviewUsesTheSameOriginForDrawingAndTargets();
        targetRadiusMatchesTheDrawnCapsuleDot();
        contentBoundsUseOnlyTheUniformBodyScale();
        System.out.println("CapsuleLayoutTest passed");
    }

    private static void centredPreviewUsesTheSameOriginForDrawingAndTargets() {
        assertEquals(0.0f, CapsuleLayout.originX(240.0f, 0.5f, true, 8.0f),
                "a centred preview starts the capsule at the icon-box centre");
        assertEquals(-8.0f, CapsuleLayout.originX(240.0f, 0.5f, false, 8.0f),
                "the device keeps the capsule right-aligned before its fixed side gap");
        assertEquals(25.0f, CapsuleLayout.contentCentre(0.0f, 50.0f, 0.5f),
                "targets use the exact transform used to draw centred capsule content");
    }

    private static void targetRadiusMatchesTheDrawnCapsuleDot() {
        assertEquals(3.198f, CapsuleLayout.dotRadius(4.264f, 1.5f, 0.5f),
                "the morph target uses the capsule dot radius, not the ring-style radius");
    }

    private static void contentBoundsUseOnlyTheUniformBodyScale() {
        float bodyScale = 0.8f;
        float origin = CapsuleLayout.originX(262.4f, bodyScale, false, 8.0f);
        float left = CapsuleLayout.contentCentre(origin, 111.2f, bodyScale);
        float right = CapsuleLayout.contentCentre(origin, 231.2f, bodyScale);
        assertEquals(120.0f * bodyScale, right - left,
                "battery digits retain their measured width after the uniform capsule scale");
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (!(Math.abs(expected - actual) <= 0.001f)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
