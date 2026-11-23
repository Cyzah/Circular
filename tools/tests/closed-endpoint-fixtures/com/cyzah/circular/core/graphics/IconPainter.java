package com.cyzah.circular.core.graphics;

import android.graphics.Canvas;
import com.cyzah.circular.core.network.IconState;
import com.cyzah.circular.hook.runtime.SessionSettings;

/** Endpoint draw boundary: record masks, user appearance and physical-pixel reference. */
public final class IconPainter {
    public static final int MASK_HIDE_BATTERY_RING = 256;
    public SessionSettings settings;
    public float pixelUnit = .375f, motion = .67f;
    public int calls, mask, tint, motionCalls;
    public float x, y, size, alpha, reference;
    public boolean coloured;
    public IconState state;
    public RuntimeException drawFailure;
    public float contentPixelUnit() { return pixelUnit; }
    public void setMotion(float value) { motionCalls++; motion = value; }
    public void drawPartMaskedAtReferenceUnit(Canvas canvas, float x, float y, float size,
            IconState state, int tint, boolean coloured, int mask, float alpha, float reference) {
        calls++; this.x = x; this.y = y; this.size = size; this.state = state;
        this.tint = tint; this.coloured = coloured; this.mask = mask;
        this.alpha = alpha; this.reference = reference;
        if (motion != 0.0f) throw new AssertionError("Endpoint inherited nonzero animation motion");
        if (drawFailure != null) throw drawFailure;
    }
}
