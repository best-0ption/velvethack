package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class SettingRender {

    static final float ROW_H = 20;

    final ClickGUIScreen gui;

    SettingRender(ClickGUIScreen gui) {
        this.gui = gui;
    }

    public float height(Setting s, float k) {
        return ROW_H * k;
    }

    public abstract void render(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float k, float a, float mx, float my, float dt);

    public abstract void click(Setting s, float mx, float my, float x, float y, float w, float k);
}
