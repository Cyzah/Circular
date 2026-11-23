package com.cyzah.circular.domain.settings;

public final class NetworkTechnologyOptionsTest {
    private NetworkTechnologyOptionsTest() {
    }

    public static void main(String[] args) {
        supportedFamiliesProducePreviewLabels();
        unknownCapabilitiesFallBackToAllLabels();
        previousSelectionRemainsAvailable();
        System.out.println("NetworkTechnologyOptionsTest passed");
    }

    private static void supportedFamiliesProducePreviewLabels() {
        assertArrayEquals(
                new String[]{"5GA", "5G+", "5G", "4G+", "4G"},
                NetworkTechnologyOptions.supported(true, true, true, true),
                "all supported radio families produce their preview labels");
        assertArrayEquals(
                new String[]{"5GA", "5G+", "5G"},
                NetworkTechnologyOptions.supported(false, false, false, true),
                "NR support exposes the 5G display variants");
    }

    private static void unknownCapabilitiesFallBackToAllLabels() {
        assertArrayEquals(
                new String[]{"5GA", "5G+", "5G", "4G+", "4G"},
                NetworkTechnologyOptions.supported(false, false, false, false),
                "unreadable radio capabilities keep every preview label available");
    }

    private static void previousSelectionRemainsAvailable() {
        assertArrayEquals(
                new String[]{"LTE", "5GA", "5G+", "5G"},
                NetworkTechnologyOptions.withSelected(
                        new String[]{"5GA", "5G+", "5G"}, "LTE"),
                "a previously selected custom label remains selectable");
    }

    private static void assertArrayEquals(
            String[] expected, String[] actual, String message) {
        if (expected.length != actual.length) {
            throw new AssertionError(message + ": length " + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(actual[i])) {
                throw new AssertionError(
                        message + ": index " + i + " expected " + expected[i] + ", got " + actual[i]);
            }
        }
    }
}
