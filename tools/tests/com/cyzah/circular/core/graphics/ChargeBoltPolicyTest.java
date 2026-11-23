package com.cyzah.circular.core.graphics;
import com.cyzah.circular.domain.settings.DualStyle;

public final class ChargeBoltPolicyTest {
    private ChargeBoltPolicyTest() {
    }

    public static void main(String[] args) {
        assertFalse(ChargeBoltPolicy.showBolt(DualStyle.LAYERED), "layered style hides the charging bolt");
        assertFalse(ChargeBoltPolicy.showBolt(DualStyle.GROUPED), "grouped style hides the charging bolt");
        assertFalse(ChargeBoltPolicy.showBolt(DualStyle.DUO), "DUO hides the charging bolt");
        assertTrue(ChargeBoltPolicy.showBolt(DualStyle.CAPSULE), "capsule keeps the charging bolt");
        System.out.println("ChargeBoltPolicyTest passed");
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
}
