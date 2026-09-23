package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.settings.ModeSetting;
import im.best0ption.velvet.settings.Setting;
import im.best0ption.velvet.util.MouseUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModeSettingRender extends SettingRender {

    public ModeSettingRender(ClickGUIScreen gui) {
        super(gui);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float k, float a, float mx, float my, float dt) {
        ModeSetting mode = (ModeSetting) s;

        FontManager.sfmedium.drawDefaultString(g, mode.getName(), x + 7 * k,
                y + (ROW_H * k - FontManager.sfmedium.getHeight(8 * k)) / 2f, 8 * k,
                RenderUtils.rgba(235, 235, 240, (int) (255 * a)));

        float cw = FontManager.sfmedium.getWidth(mode.getValue(), 7.5f * k) + 12 * k;
        float ch = 13 * k;
        float cx = x + w - 7 * k - cw;
        float cy = y + (ROW_H * k - ch) / 2f;
        float hov = gui.animValue("mode" + mode.getName(), MouseUtility.isHovered(mx, my, cx, cy, cw, ch), dt);
        RenderUtils.drawRoundedRectangle(g, cx, cy, cw, ch / 2f, ch,
                ClickGUIScreen.fade(ClickGUIScreen.mix(RenderUtils.rgb(38, 38, 46),
                        ClickGUIScreen.mix(RenderUtils.rgb(52, 42, 84), ClickGUIScreen.accent, 0.35f), hov), a));
        FontManager.sfmedium.drawDefaultString(g, mode.getValue(), cx + 6 * k,
                cy + (ch - FontManager.sfmedium.getHeight(7.5f * k)) / 2f, 7.5f * k,
                RenderUtils.rgba(255, 255, 255, (int) (255 * a)));
    }

    @Override
    public void click(Setting s, float mx, float my, float x, float y, float w, float k) {
        ((ModeSetting) s).cycle();
    }
}
