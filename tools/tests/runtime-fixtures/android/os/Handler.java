package android.os;
public class Handler {
    public Handler(Looper looper) { }
    public boolean post(Runnable task) { return true; }
    public boolean postDelayed(Runnable task, long delay) { return true; }
    public void removeCallbacks(Runnable task) { }
}
