package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.util.CalculateUtility;
import im.best0ption.velvet.util.MouseUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class ThemeRender {

    static final float CARD_H = 34;
    static final float SW_W = 30;
    static final float SW_H = 16;

    final ClickGUIScreen gui;
    float scroll, scrollTarget;

    public ThemeRender(ClickGUIScreen gui) {
        this.gui = gui;
    }

    public void reset() {
        scroll = 0;
        scrollTarget = 0;
    }

    public void onScroll(float dy) {
        scrollTarget = Math.max(0, scrollTarget - dy);
    }

    public void render(GuiGraphicsExtractor g, float x, float y, float w, float h, float k, float a, float mx, float my, float dt, float appearT, String search) {
        List<Theme> list = Theme.visible(search);
        int cols = w > 150 * k ? 2 : 1;
        float gap = 6 * k;
        float cardW = (w - gap * (cols - 1)) / cols;
        float cardH = CARD_H * k;
        int items = list.size() + 1;
        int rows = (items + cols - 1) / cols;
        float maxScroll = Math.max(0, rows * (cardH + gap) - gap - h);
        scrollTarget = CalculateUtility.clamp(scrollTarget, 0, maxScroll);
        scroll = CalculateUtility.follow(scroll, scrollTarget, dt);

        g.enableScissor(Math.round(x - 1), Math.round(y - 1), Math.round(x + w + 1), Math.round(y + h + 1));
        for (int i = 0; i < items; i++) {
            float ap = ClickGUIScreen.appear(appearT, i);
            if (ap <= 0f) continue;
            int row = i / cols;
            int col = i % cols;
            float px = x + col * (cardW + gap);
            float py = y + row * (cardH + gap) - scroll;
            if (py > y + h || py + cardH < y) continue;
            drawCard(g, i < list.size() ? list.get(i) : null, px, py + (1f - ap) * 8 * k, cardW, cardH, k, a, mx, my, dt, ap);
        }
        g.disableScissor();
    }

    void drawCard(GuiGraphicsExtractor g, Theme t, float x, float y, float w, float h, float k, float a, float mx, float my, float dt, float ap) {
        boolean hovered = MouseUtility.isHovered(mx, my, x, y, w, h);
        String key = t == null ? "addTheme" : "theme" + t.name;
        float hov = gui.animValue(key, hovered, dt);
        float on = t == null ? 0f : gui.animValue("on" + key, t == Theme.current, dt);
        y -= hov * 1.5f * k;

        float sx = x + w - 8 * k - SW_W * k;
        float sy = y + (h - SW_H * k) / 2f;

        if (t == null) {
            int bg = ClickGUIScreen.mix(RenderUtils.rgb(23, 23, 28), RenderUtils.rgb(31, 31, 38), hov);
            RenderUtils.drawRoundedRectangle(g, x, y, w, 5 * k, h, ClickGUIScreen.fade(bg, a * ap));
            RenderUtils.drawFlowBorder(g, x, y, w, h, 5 * k, 1f,
                    ClickGUIScreen.alpha(ClickGUIScreen.accent, (45 + 95 * hov) * a * ap),
                    ClickGUIScreen.alpha(ClickGUIScreen.accent2, (45 + 95 * hov) * a * ap));
            FontManager.sfbold.drawDefaultString(g, "New theme", x + 8 * k, y + 6.5f * k, 9 * k,
                    RenderUtils.rgba(245, 245, 250, (int) (255 * a * ap)));
            FontManager.sfmedium.drawDefaultString(g, "свой градиент", x + 8 * k, y + 19 * k, 6.5f * k,
                    RenderUtils.rgba(255, 255, 255, (int) ((95 + 40 * hov) * a * ap)));
            int plus = ClickGUIScreen.mix(ClickGUIScreen.accent, ClickGUIScreen.accent2, 0.5f);
            FontManager.sfbold.drawDefaultString(g, "+", sx + (SW_W * k - FontManager.sfbold.getWidth("+", 13 * k)) / 2f,
                    sy + (SW_H * k - FontManager.sfbold.getHeight(13 * k)) / 2f, 13 * k,
                    ClickGUIScreen.fade(plus, (0.55f + 0.45f * hov) * a * ap));
            return;
        }

        int onBg = ClickGUIScreen.mix(RenderUtils.rgb(43, 34, 72), t.first(), 0.35f);
        int bg = ClickGUIScreen.mix(RenderUtils.rgb(23, 23, 28), onBg, on);
        bg = ClickGUIScreen.mix(bg, RenderUtils.rgb(31, 31, 38), hov * (1f - on * 0.6f));
        RenderUtils.drawRoundedRectangle(g, x, y, w, 5 * k, h, ClickGUIScreen.fade(bg, a * ap));

        if (on > 0.02f) {
            float ba = (85 + 110 * hov) * on * a * ap;
            RenderUtils.drawFlowBorder(g, x, y, w, h, 5 * k, 1f,
                    ClickGUIScreen.alpha(t.first(), ba), ClickGUIScreen.alpha(t.second(), ba));
        } else {
            RenderUtils.drawRoundedBorder(g, x, y, w, h, 5 * k, 1f,
                    RenderUtils.rgba(255, 255, 255, (int) ((11 + 25 * hov) * a * ap)));
        }

        int name = ClickGUIScreen.mix(RenderUtils.rgb(245, 245, 250), t.first(), on);
        FontManager.sfbold.drawDefaultString(g, t.name, x + 8 * k, y + 6.5f * k, 9 * k,
                ClickGUIScreen.fade(name, a * ap));
        FontManager.sfmedium.drawDefaultString(g, t.hex(), x + 8 * k, y + 19 * k, 6.5f * k,
                RenderUtils.rgba(255, 255, 255, (int) ((95 + 40 * hov) * a * ap)));

        RenderUtils.drawFlowRectangle(g, sx, sy, SW_W * k, 4 * k, SW_H * k, t.first(), t.second());
        RenderUtils.drawRoundedBorder(g, sx, sy, SW_W * k, SW_H * k, 4 * k, 1f,
                RenderUtils.rgba(255, 255, 255, (int) ((25 + 45 * hov) * a * ap)));

        if (t.custom) {
            float gxx = sx - 4 * k - 9 * k;
            float gv = gui.animValue("gear" + key, MouseUtility.isHovered(mx, my, gxx, sy - 2 * k, 9 * k, SW_H * k + 4 * k), dt);
            FontManager.icons.drawDefaultString(g, "0", gxx + 1 * k, sy + 3.5f * k, 9 * k,
                    RenderUtils.rgba(255, 255, 255, (int) ((60 + 140 * gv) * a * ap)));
        }
    }

    public void click(float mx, float my, float x, float y, float w, float h, float k, int button, String search) {
        if (!MouseUtility.isHovered(mx, my, x, y, w, h)) return;

        List<Theme> list = Theme.visible(search);
        int cols = w > 150 * k ? 2 : 1;
        float gap = 6 * k;
        float cardW = (w - gap * (cols - 1)) / cols;
        float cardH = CARD_H * k;

        for (int i = 0; i <= list.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            float px = x + col * (cardW + gap);
            float py = y + row * (cardH + gap) - scroll;
            if (!MouseUtility.isHovered(mx, my, px, py, cardW, cardH)) continue;

            if (i == list.size()) {
                if (button == 1) {
                    Theme t = Theme.add();
                    gui.openSettings(t.name, t.settings());
                }
                return;
            }

            Theme t = list.get(i);
            if (t.custom) {
                float sx = px + cardW - 8 * k - SW_W * k;
                float sy = py + (cardH - SW_H * k) / 2f;
                float gxx = sx - 4 * k - 9 * k;
                boolean gear = MouseUtility.isHovered(mx, my, gxx, sy - 2 * k, 9 * k, SW_H * k + 4 * k);
                if (button == 2|| (gear && button == 1)) {
                    gui.openSettings(t.name, t.settings());
                    return;
                }
            }
            if (button == 1) {
                Theme.current = t;
            }
            return;
        }
    }
}
