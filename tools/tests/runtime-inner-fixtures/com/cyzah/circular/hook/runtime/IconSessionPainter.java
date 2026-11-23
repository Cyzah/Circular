package com.cyzah.circular.hook.runtime;
import android.graphics.Canvas;
/** Drives the actual IconSession alpha wrapper through its existing renderer boundary. */
public class IconSessionPainter {
    public void paint(IconSession owner, Canvas canvas, NativeDraw draw) throws Throwable { draw.run(); }
}
