package android.graphics;

public final class RectF {
    public float left, top, right, bottom;
    public RectF() { }
    public RectF(float left, float top, float right, float bottom) { set(left, top, right, bottom); }
    public void set(float left, float top, float right, float bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }
    public void set(RectF source) { set(source.left, source.top, source.right, source.bottom); }
    public void setEmpty() { set(0, 0, 0, 0); }
    public boolean isEmpty() { return left >= right || top >= bottom; }
    public float width() { return right - left; }
    public float height() { return bottom - top; }
    public float centerX() { return (left + right) * .5f; }
    public float centerY() { return (top + bottom) * .5f; }
    public void offset(float dx, float dy) { left += dx; right += dx; top += dy; bottom += dy; }
    public void union(float left, float top, float right, float bottom) {
        if (!(left < right && top < bottom)) return;
        if (isEmpty()) set(left, top, right, bottom);
        else set(Math.min(this.left, left), Math.min(this.top, top),
                Math.max(this.right, right), Math.max(this.bottom, bottom));
    }
    public void union(RectF source) { union(source.left, source.top, source.right, source.bottom); }
}
