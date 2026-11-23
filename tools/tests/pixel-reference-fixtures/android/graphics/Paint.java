package android.graphics;

/** Minimal host metrics for geometry tests; does not claim to rasterize fonts. */
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1, SUBPIXEL_TEXT_FLAG = 128;
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }
    public static final class FontMetrics {
        public float top, ascent, descent, bottom, leading;
    }
    private float textSize = 12, strokeWidth;
    private Align align = Align.LEFT;
    public Paint() { }
    public Paint(int flags) { }
    public void setColor(int color) { }
    public void setAlpha(int alpha) { }
    public void setStyle(Style style) { }
    public void setStrokeCap(Cap cap) { }
    public void setStrokeJoin(Join join) { }
    public void setStrokeWidth(float width) { strokeWidth = width; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setTextSize(float size) { textSize = size; }
    public float getTextSize() { return textSize; }
    public void setTextAlign(Align align) { this.align = align; }
    public Align getTextAlign() { return align; }
    public Typeface setTypeface(Typeface face) { return face; }
    public float getFontMetrics(FontMetrics out) {
        out.ascent = out.top = -textSize * .8f;
        out.descent = out.bottom = textSize * .2f;
        return textSize;
    }
    public float measureText(String text) { return text.length() * textSize * .5f; }
    public void getTextBounds(String text, int start, int end, Rect out) {
        if (start == end) out.setEmpty();
        else out.set(0, -(int) textSize, (int) ((end - start) * textSize * .5f), 0);
    }
}
