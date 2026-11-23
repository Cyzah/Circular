package android.graphics;
import java.util.ArrayList;
import java.util.Arrays;
/** A deterministic state double, never evidence about Android's renderer. */
public class Canvas {
    private final ArrayList<float[]> states = new ArrayList<>();
    private float[] state = {0, 0, 1, 1, 0, 0, 100, 100};
public RuntimeException restoreFailure;
    public int restoreFailureOnCall = 1, restoreCalls;
    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha) { return save(); }
    public int saveLayerAlpha(RectF bounds, int alpha) { return save(); }
    public int ink;
    public int save() { int count = getSaveCount(); states.add(state.clone()); return count; }
    public int getSaveCount() { return states.size() + 1; }
    public void translate(float x, float y) { state[0] += x; state[1] += y; }
    public void scale(float x, float y) { state[2] *= x; state[3] *= y; }
    public boolean clipRect(float l, float t, float r, float b) {
        state[4] = l; state[5] = t; state[6] = r; state[7] = b; return true;
    }
    public void restoreToCount(int count) {
        while (getSaveCount() > count) state = states.remove(states.size() - 1);
if (++restoreCalls == restoreFailureOnCall && restoreFailure != null) throw restoreFailure;
    }
    public String snapshot() { return Arrays.toString(state); }
}
