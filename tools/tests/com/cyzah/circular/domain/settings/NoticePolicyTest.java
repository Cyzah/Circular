package com.cyzah.circular.domain.settings;

public final class NoticePolicyTest {
    private NoticePolicyTest() {
    }

    public static void main(String[] args) {
        sameInstallationCanStayDismissed();
        updateOrReinstallInvalidatesDismissal();
        legacyDismissalShowsOnceAfterUpgrade();
        backgroundRecreationNeverShowsTheNotice();
        System.out.println("NoticePolicyTest passed");
    }

    private static void sameInstallationCanStayDismissed() {
        assertFalse(NoticePolicy.shouldShow(true, true, "22:1000", "22:1000"),
                "an unchanged installation respects the previous permanent dismissal");
    }

    private static void updateOrReinstallInvalidatesDismissal() {
        assertTrue(NoticePolicy.shouldShow(true, true, "22:1000", "23:2000"),
                "a new build shows the notice after an update");
        assertTrue(NoticePolicy.shouldShow(true, true, "22:1000", "22:2000"),
                "a reinstall of the same version also shows the notice");
    }

    private static void legacyDismissalShowsOnceAfterUpgrade() {
        assertTrue(NoticePolicy.shouldShow(true, true, "", "23:2000"),
                "an old boolean-only dismissal cannot suppress the first versioned launch");
    }

    private static void backgroundRecreationNeverShowsTheNotice() {
        assertFalse(NoticePolicy.shouldShow(false, false, "", "23:2000"),
                "activity recreation is not a new installation");
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
