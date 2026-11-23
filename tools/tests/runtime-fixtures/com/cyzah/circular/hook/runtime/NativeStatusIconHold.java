package com.cyzah.circular.hook.runtime;
public class NativeStatusIconHold {
    public boolean ownsSignals(IconSession owner) { return owner != null && owner.draw; }
}
