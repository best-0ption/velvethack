package im.best0ption.velvet.settings;

/**
 * Float setting with a min/max range, optionally snapped to a step.
 */
public class SliderSetting extends Setting {

    private final float min;
    private final float max;
    private final float step;
    private float value;

    public SliderSetting(String name, float defaultValue, float min, float max, float step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        setValue(defaultValue);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    /** 0.0 .. 1.0 position of the current value inside the range. */
    public float getProgress() {
        if (max - min == 0f) return 0f;
        return (value - min) / (max - min);
    }

    /** Set value from a 0.0 .. 1.0 position inside the range, snapped to the step. */
    public void setProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float raw = min + clamped * (max - min);
        if (step > 0f) {
            raw = Math.round(raw / step) * step;
        }
        setValue(raw);
    }
}