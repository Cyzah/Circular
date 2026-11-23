package android.view;
import android.content.Context;
public class View {
    public static final int VISIBLE = 0, GONE = 8;
    private ViewParent parent;
    private final Context context = new Context();
public int invalidations, layouts, draws, tint = 0xffeeeeee;
    public Runnable drawing;
    public int getWidth() { return 24; }
    public int getHeight() { return 24; }
    public int getMeasuredWidth() { return 12; }
    public int getLayoutDirection() { return 0; }
    public int getPaddingEnd() { return 0; }
    public int getPaddingStart() { return 0; }
    public android.content.res.Resources getResources() { return context.getResources(); }
    public void postInvalidateOnAnimation() { invalidate(); }
    public void draw(android.graphics.Canvas canvas) { draws++; if (drawing != null) drawing.run(); }
    public void parent(ViewParent value) { parent = value; }
    public ViewParent getParent() { return parent; }
    public boolean isAttachedToWindow() { return true; }
    public boolean isShown() { return true; }
    public Context getContext() { return context; }
    public void invalidate() { invalidations++; }
    public void requestLayout() { layouts++; }
}
