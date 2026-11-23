package android.graphics;

/** Records the actual endpoint method's outer transform; does not rasterize. */
public final class Canvas {
    private int count = 1;
    public int translations, scales, restores;
    public float translateX, translateY, scaleX, scaleY;
    public int save() { return count++; }
    public int getSaveCount() { return count; }
    public void translate(float x, float y) { translations++; translateX = x; translateY = y; }
    public void scale(float x, float y) { scales++; scaleX = x; scaleY = y; }
    public void restoreToCount(int value) { restores++; count = value; }
}
