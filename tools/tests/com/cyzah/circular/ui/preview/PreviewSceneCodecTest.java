package com.cyzah.circular.ui.preview;
import com.cyzah.circular.core.network.IconState;

public final class PreviewSceneCodecTest {
    private PreviewSceneCodecTest() {
    }

    public static void main(String[] args) {
        roundTripKeepsDataSwitchAndDerivesWifiConnection();
        legacySceneMigratesWifiToSwitchOnly();
        System.out.println("PreviewSceneCodecTest passed");
    }

    private static void roundTripKeepsDataSwitchAndDerivesWifiConnection() {
        IconState source = new IconState();
        source.battery = 87;
        source.charging = true;
        source.powerSave = false;
        source.performanceMode = true;
        source.wifiEnabled = true;
        source.wifiConnected = false;
        source.wifiLevel = 2;
        source.dataEnabled = false;
        source.mobileLevel = 3;
        source.secondaryLevel = 4;
        source.simPresent = true;
        source.secondarySimPresent = true;
        source.inService = true;
        source.secondaryInService = true;
        source.mobileTechnology = "5GA";

        String encoded = PreviewSceneCodec.encode(source, 0.42f, true);
        IconState restored = new IconState();
        PreviewSceneCodec.Result result = PreviewSceneCodec.decode(encoded, restored);
        assertTrue(result != null, "the current preview scene format decodes");
        assertTrue(restored.wifiEnabled,
                "the Wi-Fi switch survives a preview scene round trip");
        assertTrue(PreviewWifiPolicy.connected(restored.wifiEnabled)
                        == restored.wifiConnected,
                "preview Wi-Fi connection is derived from the switch");
        assertTrue(!restored.dataEnabled,
                "the cellular data switch survives a preview scene round trip");
        assertEquals("5GA", restored.mobileTechnology,
                "the selected data technology survives a preview scene round trip");
        assertTrue(result.expanded, "the scene control expansion state survives");
        assertEquals(0.42f, result.expansion, "the preview expansion survives");
    }

    private static void legacySceneMigratesWifiToSwitchOnly() {
        String legacy = "91,0,0,0,1,0,3,4,2,1,1,1,1,0,5G,500,1";
        IconState restored = new IconState();
        assertTrue(PreviewSceneCodec.decode(legacy, restored) != null,
                "legacy preview scenes remain readable");
        assertTrue(restored.wifiEnabled,
                "legacy Wi-Fi enabled state is retained");
        assertTrue(restored.wifiConnected,
                "legacy Wi-Fi connection state migrates to the enabled switch");
        assertTrue(restored.dataEnabled,
                "legacy scenes default cellular data to enabled");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (!(Math.abs(expected - actual) <= 0.001f)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
