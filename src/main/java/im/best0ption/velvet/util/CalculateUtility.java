package im.best0ption.velvet.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CalculateUtility {

    public static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public float follow(float cur, float target, float dt) {
        return cur + (target - cur) * Math.min(1f, dt * 12f);
    }

}
