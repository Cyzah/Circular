package com.cyzah.circular.core.graphics.animation;

import com.cyzah.circular.core.graphics.geometry.RingGeometry;
import com.cyzah.circular.hook.runtime.IconSession;

/** Projection boundary: supply an unequal X/Y transform, or fail as a missing host would. */
public final class BatteryTransferProjection {
    public boolean result = true;
    public int calls;
    public float ringLocalX = 17.25f, ringLocalY = -8.75f;
    public float ringUnitX = .625f, ringUnitY = .8f;
    public IconSession source, target;
    public RingGeometry geometry;
    public float canvasShift;
    public boolean project(IconSession source, RingGeometry geometry,
            IconSession target, float canvasShift) {
        calls++; this.source = source; this.target = target;
        this.geometry = geometry; this.canvasShift = canvasShift;
        return result && source != null;
    }
}
