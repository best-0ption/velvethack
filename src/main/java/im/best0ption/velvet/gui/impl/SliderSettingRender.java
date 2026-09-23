package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.settings.Setting;
import im.best0ption.velvet.settings.SliderSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

public class SliderSettingRender extends SettingRender {

    SliderSetting dragging;
    float[] bar;

    public SliderSettingRender(ClickGUIScreen gui) {
        super(gui);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float k, float a, float mx, float my, float dt) {
        SliderSetting slider = (SliderSetting) s;

        FontManager.sfmedium.drawDefaultString(g, slider.getName(), x + 7 * k, y + 3 * k, 8 * k,
                RenderUtils.rgba(235, 235, 240, (int) (255 * a)));
        String val = String.format(Locale.US, "%.1f", slider.getValue());
        FontManager.sfbold.drawDefaultString(g, val, x + w - 7 * k - FontManager.sfbold.getWidth(val, 8 * k), y + 3 * k, 8 * k,
                ClickGUIScreen.alpha(ClickGUIScreen.accent, 255 * a));

        float bx = x + 7 * k;
        float bw = w - 14 * k;
        float by = y + 14 * k;
        float prog = gui.animValue("prog" + slider.getName(), slider.getProgress(), dt);
        float drag = gui.animValue("drag" + slider.getName(), dragging == slider, dt);
        RenderUtils.drawRoundedRectangle(g, bx, by, bw, 1.5f * k, 3 * k,
                ClickGUIScreen.fade(RenderUtils.rgb(45, 45, 53), a));
        if (prog > 0.01f) {
            RenderUtils.drawFlowRectangle(g, bx, by, bw * prog, 1.5f * k, 3 * k,
                    ClickGUIScreen.fade(ClickGUIScreen.accent, a), ClickGUIScreen.fade(ClickGUIScreen.accent2, a));
        }
        float knob = 6 * k * (1f + drag * 0.35f);
        RenderUtils.drawRoundedRectangle(g, bx + bw * prog - knob / 2f, by + 1.5f * k - knob / 2f,
                knob, knob / 2f, knob, RenderUtils.rgba(255, 255, 255, (int) (255 * a)));

        if (dragging == slider) {
            bar = new float[]{bx, bw};
        }
    }

    @Override
    public void click(Setting s, float mx, float my, float x, float y, float w, float k) {
        SliderSetting slider = (SliderSetting) s;
        float bx = x + 7 * k;
        float bw = w - 14 * k;
        if (bw <= 0) return;
        slider.setProgress((mx - bx) / bw);
        dragging = slider;
        bar = new float[]{bx, bw};
    }

    public void drag(float mx) {
        if (dragging == null || bar == null || bar[1] <= 0) return;
        dragging.setProgress((mx - bar[0]) / bar[1]);
    }

    public void stop() {
        dragging = null;
        bar = null;
    }
}
