package android.view;

/** Only endpoint-frame dimensions and layout direction cross this boundary. */
public final class View {
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public int width = 160, height = 48, direction;
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLayoutDirection() { return direction; }
}
