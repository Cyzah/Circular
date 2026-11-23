package com.cyzah.circular.core.graphics;

public final class SignalCrossPolicyTest {
    private SignalCrossPolicyTest() {
    }

    public static void main(String[] args) {
        centralDotsAreRemovedWhileOuterDotsRemain();
        System.out.println("SignalCrossPolicyTest passed");
    }

    private static void centralDotsAreRemovedWhileOuterDotsRemain() {
        float[] dotX = {33.81f, 51.27f, 68.73f, 86.19f};
        boolean[] expectedOverlap = {false, true, true, false};
        for (int index = 0; index < dotX.length; index++) {
            boolean overlaps = SignalCrossPolicy.coversDot(dotX[index], 107.23f, 60f, 107.23f, 5.88f, 1.2f);
            assertEquals(expectedOverlap[index], overlaps, "cross overlap for dot " + index);
        }
    }

    private static void assertEquals(boolean expected, boolean actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
