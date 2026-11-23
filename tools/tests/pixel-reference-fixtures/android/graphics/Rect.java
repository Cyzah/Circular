package android.graphics;

public final class Rect {
    public int left, top, right, bottom;
    public Rect() { }
    public void set(int left, int top, int right, int bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }
    public void setEmpty() { set(0, 0, 0, 0); }
    public boolean isEmpty() { return left >= right || top >= bottom; }
    public int width() { return right - left; }
    public int height() { return bottom - top; }
    public void inset(int dx, int dy) { left += dx; right -= dx; top += dy; bottom -= dy; }
}
