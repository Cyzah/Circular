package android.graphics;

public final class Path {
    public final java.util.List<float[]> points = new java.util.ArrayList<>();
    public Path() { }
    public void reset() { points.clear(); }
    public void moveTo(float x, float y) { points.add(new float[] {x, y}); }
    public void lineTo(float x, float y) { points.add(new float[] {x, y}); }
}
