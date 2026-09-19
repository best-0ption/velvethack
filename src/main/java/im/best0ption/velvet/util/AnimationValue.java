package im.best0ption.velvet.util;

public class AnimationValue {

    private final float duration;
    private final Easing easing;
    private float time;
    private boolean running;

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

    public void update(float delta) {
        if (running && time < duration) {
            time = Math.min(time + delta, duration);
        }
    }

    public float get() {
        return easing.ease(Math.min(time / duration, 1.0F));
    }
}
