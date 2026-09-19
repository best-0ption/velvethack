package im.best0ption.velvet.util;

public class TimerUtility {

    private long lastMS = System.currentTimeMillis();

    public boolean hasTimeElapsed(long ms) {
        return System.currentTimeMillis() - lastMS >= ms;
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }
}
