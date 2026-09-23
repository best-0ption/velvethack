package im.best0ption.velvet.settings;

import im.best0ption.velvet.render.RenderUtils;

public class ColorSetting extends Setting {

    private int first;
    private int second;
    private int active;

    public ColorSetting(String name, int first, int second) {
        super(name);
        this.first = first;
        this.second = second;
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    public int getColor(int slot) {
        return slot == 0 ? first : second;
    }

    public void setColor(int slot, int color) {
        if (slot == 0) first = color;
        else second = color;
    }

    public int getValue() {
        return getColor(active);
    }

    public void setValue(int color) {
        setColor(active, color);
    }

    public int getActive() {
        return active;
    }

    public void setActive(int slot) {
        active = slot;
    }

    public static int alpha(int color, int a) {
        return RenderUtils.rgba((color >> 16) & 255, (color >> 8) & 255, color & 255, a);
    }

    public static float[] hsv(int color) {
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0.0001f) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h *= 60f;
            if (h < 0) h += 360f;
        }
        float s = max <= 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    public static int fromHsv(float h, float s, float v) {
        h = h % 360f;
        if (h < 0) h += 360f;
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r = 0, g = 0, b = 0;
        if (h < 60) {
            r = c;
            g = x;
        } else if (h < 120) {
            r = x;
            g = c;
        } else if (h < 180) {
            g = c;
            b = x;
        } else if (h < 240) {
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return RenderUtils.rgba(Math.round((r + m) * 255f), Math.round((g + m) * 255f), Math.round((b + m) * 255f), 255);
    }
}
