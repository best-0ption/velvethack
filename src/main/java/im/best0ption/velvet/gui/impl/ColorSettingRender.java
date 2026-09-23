package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.settings.ColorSetting;
import im.best0ption.velvet.settings.Setting;
import im.best0ption.velvet.util.CalculateUtility;
import im.best0ption.velvet.util.MouseUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ColorSettingRender extends SettingRender {

    static final float OPEN_H = 84;

    ColorSetting open;
    String part = "";

    float k0;
    float svX, svY, svW, svH;
    float hueX, hueY, hueW, hueH;
    float chipX, chipY, chipW, chipH;
    float preX, preY, preW, preH;

    public ColorSettingRender(ClickGUIScreen gui) {
        super(gui);
    }

    @Override
    public float height(Setting s, float k) {
        return (s == open ? OPEN_H : ROW_H) * k;
    }

    void layout(float x, float y, float w, float k) {
        k0 = k;
        svX = x + 7 * k;
        svW = w - 14 * k;
        svY = y + 24 * k;
        svH = 28 * k;
        hueX = svX;
        hueW = svW;
        hueY = y + 56 * k;
        hueH = 8 * k;
        chipX = svX;
        chipW = 14 * k;
        chipY = y + 69 * k;
        chipH = 11 * k;
        preX = x + w - 7 * k - 34 * k;
        preW = 34 * k;
        preY = y + 5 * k;
        preH = 10 * k;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float k, float a, float mx, float my, float dt) {
        ColorSetting c = (ColorSetting) s;
        layout(x, y, w, k);

        FontManager.sfmedium.drawDefaultString(g, c.getName(), x + 7 * k,
                y + (ROW_H * k - FontManager.sfmedium.getHeight(8 * k)) / 2f, 8 * k,
                RenderUtils.rgba(235, 235, 240, (int) (255 * a)));

        RenderUtils.drawFlowRectangle(g, preX, preY, preW, 3 * k, preH, c.getFirst(), c.getSecond());
        RenderUtils.drawRoundedBorder(g, preX, preY, preW, preH, 3 * k, 1f,
                RenderUtils.rgba(255, 255, 255, (int) (40 * a)));

        if (open != c) return;

        float[] hsv = ColorSetting.hsv(c.getValue());

        int hue = ColorSetting.fromHsv(hsv[0], 1f, 1f);
        RenderUtils.drawGradientRectangle(g, svX, svY, svW, 4 * k, svH, RenderUtils.rgb(255, 255, 255), hue);
        RenderUtils.drawGradientRectangleV(g, svX, svY, svW, 4 * k, svH,
                RenderUtils.rgba(0, 0, 0, 0), RenderUtils.rgba(0, 0, 0, 255));

        float cx = svX + hsv[1] * svW;
        float cy = svY + (1f - hsv[2]) * svH;
        RenderUtils.drawRoundedRectangle(g, cx - 4 * k, cy - 4 * k, 8 * k, 4 * k, 8 * k, RenderUtils.rgb(255, 255, 255));
        RenderUtils.drawRoundedRectangle(g, cx - 3 * k, cy - 3 * k, 6 * k, 3 * k, 6 * k, c.getValue());

        float seg = hueW / 6f;
        for (int i = 0; i < 6; i++) {
            RenderUtils.drawGradientRectangle(g, hueX + i * seg, hueY, seg, 0, hueH,
                    ColorSetting.fromHsv(i * 60, 1f, 1f), ColorSetting.fromHsv((i + 1) * 60f, 1f, 1f));
        }
        float hx = hueX + hsv[0] / 360f * hueW;
        RenderUtils.drawRoundedRectangle(g, hx - 1.5f * k, hueY - 1.5f * k, 3 * k, 1.5f * k, hueH + 3 * k,
                RenderUtils.rgb(255, 255, 255));

        for (int i = 0; i < 2; i++) {
            boolean act = c.getActive() == i;
            RenderUtils.drawRoundedRectangle(g, chipX + i * 16 * k, chipY, chipW, 3 * k, chipH, c.getColor(i));
            RenderUtils.drawRoundedBorder(g, chipX + i * 16 * k, chipY, chipW, chipH, 3 * k, 1f,
                    RenderUtils.rgba(255, 255, 255, (int) ((act ? 220 : 45) * a)));
        }

        String hex = String.format("#%06X", c.getValue() & 0xFFFFFF);
        FontManager.sfbold.drawDefaultString(g, hex, x + w - 7 * k - FontManager.sfbold.getWidth(hex, 7.5f * k),
                chipY + (chipH - FontManager.sfbold.getHeight(7.5f * k)) / 2f, 7.5f * k,
                RenderUtils.rgba(255, 255, 255, (int) (165 * a)));
    }

    @Override
    public void click(Setting s, float mx, float my, float x, float y, float w, float k) {
        ColorSetting c = (ColorSetting) s;
        layout(x, y, w, k);

        if (my >= y && my <= y + ROW_H * k) {
            open = open == c ? null : c;
            part = "";
            return;
        }
        if (open != c) return;

        if (MouseUtility.isHovered(mx, my, svX, svY, svW, svH)) {
            part = "sv";
            apply(mx, my);
        } else if (MouseUtility.isHovered(mx, my, hueX, hueY, hueW, hueH)) {
            part = "hue";
            apply(mx, my);
        } else if (MouseUtility.isHovered(mx, my, chipX, chipY, chipW, chipH)) {
            c.setActive(0);
        } else if (MouseUtility.isHovered(mx, my, chipX + 16 * k0, chipY, chipW, chipH)) {
            c.setActive(1);
        }
    }

    void apply(float mx, float my) {
        if (open == null || part.isEmpty()) return;
        float[] hsv = ColorSetting.hsv(open.getValue());
        if (part.equals("sv")) {
            float sn = CalculateUtility.clamp((mx - svX) / svW, 0, 1);
            float vn = CalculateUtility.clamp(1f - (my - svY) / svH, 0, 1);
            open.setValue(ColorSetting.fromHsv(hsv[0], sn, vn));
        } else {
            float hn = CalculateUtility.clamp((mx - hueX) / hueW, 0, 1) * 360f;
            open.setValue(ColorSetting.fromHsv(hn, hsv[1], hsv[2]));
        }
    }

    public void drag(float mx, float my) {
        apply(mx, my);
    }

    public void stop() {
        part = "";
    }

    public void reset() {
        open = null;
        part = "";
    }
}
