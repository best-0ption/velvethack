package im.best0ption.velvet.util;

public class AnimationValue {

    private final float duration;
    private final Easing easing;
    private float time;
    private boolean running;
    private boolean reverse;

    public AnimationValue(float duration, Easing easing) {
        this.duration = duration;
        this.easing = easing;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        time = 0.0F;
        running = false;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /** continue the animation in the given direction from wherever it is now */
    public void run(boolean forward) {
        this.reverse = !forward;
        this.running = true;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void update(float delta) {
        if (!running) return;
        if (reverse) {
            time = Math.max(time - delta, 0.0F);
            if (time <= 0.0F) {
                running = false;
            }
        } else if (time < duration) {
            time = Math.min(time + delta, duration);
        }
    }

    public float get() {
        return easing.ease(Math.min(Math.max(time / duration, 0.0F), 1.0F));
    }
}
