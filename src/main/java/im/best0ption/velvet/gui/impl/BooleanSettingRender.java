package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.settings.BooleanSetting;
import im.best0ption.velvet.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BooleanSettingRender extends SettingRender {

    public BooleanSettingRender(ClickGUIScreen gui) {
        super(gui);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float k, float a, float mx, float my, float dt) {
        BooleanSetting b = (BooleanSetting) s;

        FontManager.sfmedium.drawDefaultString(g, b.getName(), x + 7 * k,
                y + (ROW_H * k - FontManager.sfmedium.getHeight(8 * k)) / 2f, 8 * k,
                RenderUtils.rgba(235, 235, 240, (int) (255 * a)));

        float pw = 20 * k;
        float ph = 10 * k;
        float px = x + w - 7 * k - pw;
        float py = y + (ROW_H * k - ph) / 2f;
        float tog = gui.animValue("set" + b.getName(), b.getValue(), dt);
        int off = RenderUtils.rgb(46, 46, 54);
        RenderUtils.drawFlowRectangle(g, px, py, pw, ph / 2f, ph,
                ClickGUIScreen.fade(ClickGUIScreen.mix(off, ClickGUIScreen.accent, tog), a),
                ClickGUIScreen.fade(ClickGUIScreen.mix(off, ClickGUIScreen.accent2, tog), a));

        float knob = 8 * k;
        RenderUtils.drawRoundedRectangle(g, px + 1 * k + (pw - 2 * k - knob) * tog, py + (ph - knob) / 2f,
                knob, knob / 2f, knob, RenderUtils.rgba(255, 255, 255, (int) (255 * a)));
    }

    @Override
    public void click(Setting s, float mx, float my, float x, float y, float w, float k) {
        ((BooleanSetting) s).toggle();
    }
}
