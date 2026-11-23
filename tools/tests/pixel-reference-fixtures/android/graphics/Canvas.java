package android.graphics;

/** Records positioning and signal geometry without Android native rasterization. */
public final class Canvas {
    private int saves = 1;
    public float lastTranslateX, lastTranslateY;
    public final java.util.List<float[]> circles = new java.util.ArrayList<>();
    public final java.util.List<float[]> pathCenters = new java.util.ArrayList<>();
    public final java.util.List<float[]> roundRects = new java.util.ArrayList<>();
    public int save() { return saves++; }
    public void translate(float x, float y) { lastTranslateX = x; lastTranslateY = y; }
    public void scale(float x, float y) { }
    public void restoreToCount(int count) { saves = count; }
    public void drawCircle(float x, float y, float radius, Paint paint) {
        circles.add(new float[] {x, y, radius});
    }
    public void drawRoundRect(RectF bounds, float rx, float ry, Paint paint) {
        roundRects.add(new float[] {bounds.left, bounds.top, bounds.right, bounds.bottom});
    }
    public void drawPath(Path path, Paint paint) {
        float x = 0, y = 0;
        for (float[] point : path.points) { x += point[0]; y += point[1]; }
        pathCenters.add(new float[] {x / path.points.size(), y / path.points.size()});
    }
}
