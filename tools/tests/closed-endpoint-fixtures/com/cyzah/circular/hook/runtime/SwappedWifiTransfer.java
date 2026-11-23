package com.cyzah.circular.hook.runtime;

/** A closed static endpoint must discard the moving Wi-Fi state. */
public final class SwappedWifiTransfer {
    public int resets;
    public void reset() { resets++; }
}
