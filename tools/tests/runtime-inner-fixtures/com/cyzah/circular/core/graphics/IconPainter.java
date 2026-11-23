package com.cyzah.circular.core.graphics;
import android.graphics.Canvas;
import com.cyzah.circular.core.network.IconState;
/** The OEM session's sibling module renderer, outside the scope under test. */
public class IconPainter {
    public void setMotion(float progress) { }
    public float anchorY(int part, IconState state) { return 60.0f; }
    public void draw(Canvas c,float x,float y,float size,IconState state,int tint,boolean colored) { }
    public void drawPart(Canvas c,float x,float y,float size,IconState state,int tint,boolean colored,int part,float alpha) { }
}
