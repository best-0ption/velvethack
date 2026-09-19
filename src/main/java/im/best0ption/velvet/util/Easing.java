package im.best0ption.velvet.util;

public enum Easing {
    LINEAR {
        public float ease(float t) {
            return t;
        }
    },
    CUBIC_IN {
        public float ease(float t) {
            return t * t * t;
        }
    },
    CUBIC_OUT {
        public float ease(float t) {
            float f = 1.0F - t;
            return 1.0F - f * f * f;
        }
    },
    CUBIC_IN_OUT {
        public float ease(float t) {
            if (t < 0.5F) {
                return 4.0F * t * t * t;
            }
            float f = -2.0F * t + 2.0F;
            return 1.0F - f * f * f / 2.0F;
        }
    };

    public abstract float ease(float t);
}
