package com.cyzah.circular.core.graphics;

import android.graphics.Canvas;
import com.cyzah.circular.core.graphics.animation.BatteryTransferState;
import com.cyzah.circular.core.graphics.geometry.RingGeometry;

/** Transfer draw boundary: the real method must preserve waiting points and user ring geometry. */
public final class BatteryTransferRingRenderer {
    public int calls, battery, plainTint, batteryTint;
    public float x, y, unit;
    public RingGeometry geometry;
    public BatteryTransferState state;
    public void draw(Canvas canvas, RingGeometry geometry, BatteryTransferState state,
            int battery, int plainTint, int batteryTint, float x, float y, float unit) {
        calls++; this.geometry = geometry; this.state = state; this.battery = battery;
        this.plainTint = plainTint; this.batteryTint = batteryTint;
        this.x = x; this.y = y; this.unit = unit;
    }
}
