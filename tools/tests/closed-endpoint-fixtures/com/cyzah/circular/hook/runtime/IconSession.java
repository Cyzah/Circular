package com.cyzah.circular.hook.runtime;

import android.view.View;
import com.cyzah.circular.core.graphics.BatteryTransferRingRenderer;
import com.cyzah.circular.core.graphics.animation.BatteryTransferProjection;

/** Only process/UI state used by the actual private endpoint draw method. */
public final class IconSession {
    public ModuleMain module;
    public View battery = new View();
    public int from = 6, painterMask = 255;
    public float driftX = 2.5f, driftY = -1.5f, stackedAlpha = .75f;
    public boolean stackedDrawing = true;
    public final BatteryTransferProjection batteryTransferProjection = new BatteryTransferProjection();
    public final SwappedWifiTransfer swappedWifiTransfer = new SwappedWifiTransfer();
    public final BatteryTransferRingRenderer batteryRingRenderer = new BatteryTransferRingRenderer();
    public int dp(int value) { return value * 2; }
}
