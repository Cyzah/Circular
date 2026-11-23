package android.view;
import java.util.ArrayList;
public class ViewGroup extends View implements ViewParent {
    private final ArrayList<View> children = new ArrayList<>();
    public void addView(View child) { children.add(child); child.parent(this); }
    public int getChildCount() { return children.size(); }
    public View getChildAt(int index) { return children.get(index); }
}
