package com.cyzah.circular.hook.runtime;

import com.cyzah.circular.core.graphics.IconPainter;
import com.cyzah.circular.core.graphics.geometry.RingGeometry;
import com.cyzah.circular.core.network.IconState;

/** Keeps real immutable SessionSettings while replacing the Android module boundary. */
public final class ModuleMain {
    public SessionSettings sourceSettings, centerSettings, lastRulerSettings;
    public final IconState state = new IconState();
    public final IconPainter endpoint = new IconPainter();
    public final RingGeometry ring = new RingGeometry();
    public IconSession statusHost;
    public SessionSettings sessionSettings() { return sourceSettings; }
    public SessionSettings controlCenterSettings() { return centerSettings; }
    public RingGeometry transferRingGeometry(IconState ignored) { return ring; }
    public IconSession transferHost(int from) { return statusHost; }
    public IconPainter capsuleRuler(SessionSettings settings) {
        lastRulerSettings = settings; endpoint.settings = settings; return endpoint;
    }
}
