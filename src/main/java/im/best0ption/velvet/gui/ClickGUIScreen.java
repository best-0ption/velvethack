package im.best0ption.velvet.gui;

import com.google.common.eventbus.Subscribe;
import im.best0ption.velvet.VelvetHack;
import im.best0ption.velvet.event.EventChar;
import im.best0ption.velvet.event.EventKey;
import im.best0ption.velvet.event.EventMouseButton;
import im.best0ption.velvet.event.EventMouseScroll;
import im.best0ption.velvet.event.EventRender2D;
import im.best0ption.velvet.gui.impl.*;
import im.best0ption.velvet.modules.Module;
import im.best0ption.velvet.modules.Type;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.render.font.FontManager;
import im.best0ption.velvet.settings.BooleanSetting;
import im.best0ption.velvet.settings.ColorSetting;
import im.best0ption.velvet.settings.ModeSetting;
import im.best0ption.velvet.settings.Setting;
import im.best0ption.velvet.settings.SliderSetting;
import im.best0ption.velvet.util.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class ClickGUIScreen implements IMinecraft {

    public static int accent = RenderUtils.rgb(157, 120, 255);
    public static int accent2 = RenderUtils.rgb(92, 158, 255);



    static final Type[] CATS = {Type.Combat, Type.Movement, Type.Render, Type.Player, Type.Misc, Type.Themes};
    static final float side = 92;

    boolean opened;
    boolean closing;
    final AnimationValue anim = new AnimationValue(0.35f, Easing.CUBIC_OUT);

    Type selectedType = Type.Combat;
    String search = "";
    boolean searchFocused;

    float appearT;
    float scroll, scrollTarget;
    float catIndY = -1;

    String expandedName;
    List<Setting> expandedSettings;
    final AnimationValue expandAnim = new AnimationValue(0.3f, Easing.CUBIC_OUT);
    float setOffX, setOffY;
    float setScroll, setScrollTarget;
    boolean dragWindow;
    float dragAnchorX, dragAnchorY;

    boolean leftHeld;

    final SliderSettingRender sliderRender = new SliderSettingRender(this);
    final ColorSettingRender colorRender = new ColorSettingRender(this);
    final ThemeRender themeRender = new ThemeRender(this);
    final HashMap<Class<?>, SettingRender> renderers = new HashMap<>();

    final HashMap<Object, Float> anims = new HashMap<>();

    public ClickGUIScreen() {
        renderers.put(BooleanSetting.class, new BooleanSettingRender(this));
        renderers.put(SliderSetting.class, sliderRender);
        renderers.put(ModeSetting.class, new ModeSettingRender(this));
        renderers.put(ColorSetting.class, colorRender);
    }

    void setSearchFocused(boolean focused) {
        if (searchFocused == focused) return;
        searchFocused = focused;
        if (focused) {
            mc.textInputManager().startTextInput(this);
        } else {
            mc.textInputManager().stopTextInput(this);
        }
    }

    public void open() {
        if (opened && !closing) return;
        anim.reset();
        anim.run(true);
        appearT = 0;
        catIndY = -1;
        setSearchFocused(false);
        closing = false;
        opened = true;
    }

    public void close() {
        if (!opened || closing) return;
        closing = true;
        setSearchFocused(false);
        leftHeld = false;
        sliderRender.stop();
        colorRender.reset();
        dragWindow = false;
        anim.run(false);
        if (expandedSettings != null) {
            expandAnim.run(false);
        }
    }

    @Subscribe
    public void onRender(EventRender2D event) {
        if (!opened) return;

        if (mc.gui.screen() != null) {
            if (!closing) {
                close();
            }
            return;
        }

        mc.mouseHandler.releaseMouse();
        float dt = mc.getDeltaTracker().getRealtimeDeltaTicks() / 20f;
        anim.update(dt);
        expandAnim.update(dt);
        accent = Theme.current.first();
        accent2 = Theme.current.second();

        if (anim.get() <= 0f) {
            opened = false;
            closing = false;
            anims.clear();
            if (mc.gui.screen() == null) {
                mc.mouseHandler.grabMouse();
            }
            return;
        }
        if (expandAnim.isReverse() && expandAnim.get() <= 0f) {
            expandedSettings = null;
            expandedName = null;
            colorRender.reset();
        }

        float mx = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float my = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (leftHeld && dragWindow) {
            setOffX = mx - dragAnchorX;
            setOffY = my - dragAnchorY;
        }
        if (leftHeld) {
            sliderRender.drag(mx);
            colorRender.drag(mx, my);
        }

        appearT += dt;
        scroll = CalculateUtility.follow(scroll, scrollTarget, dt);
        setScroll = CalculateUtility.follow(setScroll, setScrollTarget, dt);

        float a = anim.get();
        float k = scale();
        float[] win = winRect();
        float x = win[0], y = win[1], w = win[2], h = win[3];

        GuiGraphicsExtractor g = event.getGraphics();

        RenderUtils.drawBlur(g, x, y, w, h, 10 * k, RenderUtils.rgba(24, 18, 38, (int) (110 * a)));
        RenderUtils.drawRoundedRectangle(g, x, y, w, 10 * k, h, RenderUtils.rgba(13, 13, 16, (int) (242 * a)));
        RenderUtils.drawRoundedBorder(g, x, y, w, h, 10 * k, 1f, RenderUtils.rgba(255, 255, 255, (int) (15 * a)));

        drawSidebar(g, x, y, h, k, a, mx, my, dt);
        drawContent(g, x, y, w, h, k, a, mx, my, dt);
        if (expandedSettings != null) {
            drawSettings(g, win, k, a, mx, my, dt);
        }
    }

    private void drawSidebar(GuiGraphicsExtractor g, float x, float y, float h, float k, float a, float mx, float my, float dt) {
        float sw = side * k;

        RenderUtils.drawRoundedRectangle(g, x, y, sw, 10 * k, h, RenderUtils.rgba(17, 17, 21, (int) (255 * a)));
        RenderUtils.drawRoundedRectangle(g, x + sw / 2f, y, sw / 2f, 0, h, RenderUtils.rgba(17, 17, 21, (int) (255 * a)));

        RenderUtils.drawTexture(g, Identifier.fromNamespaceAndPath("velvet", "textures/icon"),
                x + 10 * k, y + 9 * k, 15 * k, 15 * k, 4 * k, RenderUtils.rgba(255, 255, 255, (int) (235 * a)));
        FontManager.sfbold.drawDefaultString(g, "Velvet", x + 29 * k,
                y + 9 * k + (15 * k - FontManager.sfbold.getHeight(11 * k)) / 2f, 11 * k,
                RenderUtils.rgba(240, 240, 245, (int) (255 * a)));

        RenderUtils.drawRoundedRectangle(g, x + 8 * k, y + 32 * k, sw - 16 * k, 0.5f * k, 1 * k,
                RenderUtils.rgba(255, 255, 255, (int) (12 * a)));

        float rowH = 23 * k;
        float rowGap = 3 * k;
        float cy0 = y + 40 * k;

        float indTarget = cy0 + catIndex(selectedType) * (rowH + rowGap);
        catIndY = catIndY < 0 ? indTarget : CalculateUtility.follow(catIndY, indTarget, dt);
        RenderUtils.drawFlowRectangleV(g, x + 3.5f * k, catIndY + 4.5f * k, 2.5f * k, 1.2f * k, rowH - 9 * k,
                alpha(accent, 255 * a), alpha(accent2, 255 * a));

        for (int i = 0; i < CATS.length; i++) {
            Type t = CATS[i];
            float bx = x + 7 * k;
            float by = cy0 + i * (rowH + rowGap);
            float bw = sw - 14 * k;
            boolean sel = t == selectedType;
            float hov = animValue(t, MouseUtility.isHovered(mx, my, bx, by, bw, rowH), dt);
            float selv = animValue("sel" + t.name(), sel, dt);

            int bg = mix(RenderUtils.rgb(20, 20, 25), RenderUtils.rgb(29, 29, 36), hov);
            RenderUtils.drawRoundedRectangle(g, bx, by, bw, 5 * k, rowH, fade(bg, a));
            float pill = Math.max(selv, hov * 0.3f);
            if (pill > 0.02f) {
                RenderUtils.drawFlowRectangle(g, bx, by, bw, 5 * k, rowH,
                        alpha(accent, 255 * a * pill), alpha(accent2, 255 * a * pill));
            }

            int iconCol = RenderUtils.rgba(255, 255, 255, (int) ((110 + 70 * hov + 75 * selv) * a));
            drawCatIcon(g, t, bx + 8 * k, by + (rowH - 9 * k) / 2f, k, iconCol);

            String name = t.name();
            int textCol = RenderUtils.rgba(255, 255, 255,
                    (int) ((120 + 55 * hov + 80 * selv) * a));
            FontManager.sfmedium.drawDefaultString(g, name, bx + 23 * k,
                    by + (rowH - FontManager.sfmedium.getHeight(8.5f * k)) / 2f, 8.5f * k, textCol);
        }

        float uy = y + h - 22 * k;
        RenderUtils.drawRoundedRectangle(g, x + 8 * k, uy - 7 * k, sw - 16 * k, 0.5f * k, 1 * k,
                RenderUtils.rgba(255, 255, 255, (int) (12 * a)));
    }

    private void drawCatIcon(GuiGraphicsExtractor g, Type t, float x, float y, float k, int color) {
        switch (t) {
            case Combat -> FontManager.icons.drawDefaultString(g, "C", x, y - 0.5F, 10 * k, color);
            case Movement -> FontManager.icons.drawDefaultString(g, "B", x + 0.5F, y, 9 * k, color);
            case Render -> FontManager.icons.drawDefaultString(g, "4", x, y - 0.5F, 10 * k, color);
            case Player -> FontManager.icons.drawDefaultString(g, "K", x, y - 0.5F, 10 * k, color);
            case Themes -> FontManager.icons.drawDefaultString(g, "E", x, y - 0.5F, 10 * k, color);
            default -> FontManager.icons.drawDefaultString(g, "v", x, y - 0.5F, 10 * k, color);
        }
    }

    private void drawContent(GuiGraphicsExtractor g, float x, float y, float w, float h, float k, float a, float mx, float my, float dt) {
        float sx = x + (side + 10) * k;
        float sy = y + 10 * k;
        float sw = w - (side + 20) * k;
        float sh = 20 * k;

        float shv = animValue("search", searchFocused || MouseUtility.isHovered(mx, my, sx, sy, sw, sh), dt);
        RenderUtils.drawRoundedRectangle(g, sx, sy, sw, 6 * k, sh, RenderUtils.rgba(21, 21, 26, (int) (255 * a)));
        RenderUtils.drawFlowBorder(g, sx, sy, sw, sh, 6 * k, 1f,
                alpha(accent, (16 + 130 * shv) * a), alpha(accent2, (16 + 130 * shv) * a));

        FontManager.icons.drawDefaultString(g, "R", sx + 5, sy + 5, 9 * k, RenderUtils.rgba(255, 255, 255, (int) ((90 + 60 * shv) * a)));
        if (search.isEmpty() && !searchFocused) {
            FontManager.sfmedium.drawDefaultString(g, "Поиск", sx + 19 * k,
                    sy + (sh - FontManager.sfmedium.getHeight(8 * k)) / 2f, 8 * k,
                    RenderUtils.rgba(255, 255, 255, (int) (70 * a)));
        } else {
            FontManager.sfmedium.drawDefaultString(g, search, sx + 19 * k,
                    sy + (sh - FontManager.sfmedium.getHeight(8 * k)) / 2f, 8 * k,
                    RenderUtils.rgba(240, 240, 245, (int) (255 * a)));
        }
        if (searchFocused && (System.currentTimeMillis() / 450) % 2 == 0) {
            float caretX = sx + 19 * k + FontManager.sfmedium.getWidth(search, 8 * k) + 1.5f * k;
            RenderUtils.drawFlowRectangleV(g, caretX, sy + 5 * k, 1.2f * k, 0.6f * k, sh - 10 * k,
                    alpha(accent, 220 * a), alpha(accent2, 220 * a));
        }

        float gx = sx;
        float gy = sy + sh + 8 * k;
        float gwd = sw;
        float ghd = y + h - gy - 10 * k;

        if (selectedType == Type.Themes) {
            themeRender.render(g, gx, gy, gwd, ghd, k, a, mx, my, dt, appearT, search);
            return;
        }

        List<Module> modules = visibleModules();

        if (!search.isEmpty() && modules.isEmpty()) {
            drawNoModules(g, gx, gy, gwd, ghd, k, a);
            return;
        }

        int cols = gwd > 150 * k ? 2 : 1;
        float gap = 6 * k;
        float cardW = (gwd - gap * (cols - 1)) / cols;
        float cardH = 30 * k;
        int rows = (modules.size() + cols - 1) / cols;
        float maxScroll = Math.max(0, rows * (cardH + gap) - gap - ghd);
        scrollTarget = CalculateUtility.clamp(scrollTarget, 0, maxScroll);

        g.enableScissor(Math.round(gx - 1), Math.round(gy - 1),
                Math.round(gx + gwd + 1), Math.round(gy + ghd + 1));

        for (int i = 0; i < modules.size(); i++) {
            float ap = appear(appearT, i);
            if (ap <= 0f) continue;
            int row = i / cols;
            int col = i % cols;
            float px = gx + col * (cardW + gap);
            float py = gy + row * (cardH + gap) - scroll;
            if (py > gy + ghd || py + cardH < gy) continue;

            drawCard(g, modules.get(i), px, py + (1f - ap) * 8 * k, cardW, cardH, k, a, mx, my, dt, ap);
        }
        g.disableScissor();
    }

    private void drawCard(GuiGraphicsExtractor g, Module mod, float x, float y, float w, float h, float k, float a, float mx, float my, float dt, float ap) {
        boolean hovered = MouseUtility.isHovered(mx, my, x, y, w, h);
        float hov = animValue(mod, hovered, dt);
        float on = animValue("on" + mod.getName(), mod.isState(), dt);

        y -= hov * 1.5f * k;

        int bg = mix(RenderUtils.rgb(23, 23, 28), mix(RenderUtils.rgb(43, 34, 72), accent, 0.35f), on);
        bg = mix(bg, RenderUtils.rgb(31, 31, 38), hov * (1f - on * 0.6f));
        RenderUtils.drawRoundedRectangle(g, x, y, w, 5 * k, h, fade(bg, a * ap));

        if (on > 0.02f) {
            float ba = (85 + 110 * hov) * on * a * ap;
            RenderUtils.drawFlowBorder(g, x, y, w, h, 5 * k, 1f, alpha(accent, ba), alpha(accent2, ba));
        } else {
            RenderUtils.drawRoundedBorder(g, x, y, w, h, 5 * k, 1f,
                    RenderUtils.rgba(255, 255, 255, (int) ((11 + 25 * hov) * a * ap)));
        }

        FontManager.sfbold.drawDefaultString(g, mod.getName(), x + 8 * k, y + 6.5f * k, 9 * k,
                RenderUtils.rgba(245, 245, 250, (int) (255 * a * ap)));

        if (!search.isEmpty()) {
            String tag = mod.getType().name();
            float tagSize = 5.5f * k;
            float chipX = x + 8 * k + FontManager.sfbold.getWidth(mod.getName(), 9 * k) + 5 * k;
            float chipH = 10 * k;
            float chipW = FontManager.sfmedium.getWidth(tag, tagSize) + 6 * k;
            RenderUtils.drawRoundedRectangle(g, chipX, y + 6 * k, chipW, 3 * k, chipH,
                    fade(mix(RenderUtils.rgb(45, 36, 72), accent, 0.35f), a * ap));
            FontManager.sfmedium.drawDefaultString(g, tag, chipX + 3 * k,
                    y + 6 * k + (chipH - FontManager.sfmedium.getHeight(tagSize)) / 2f, tagSize,
                    fade(mix(RenderUtils.rgb(190, 170, 235), accent, 0.45f), a * ap));
        }

        String desc = mod.getDesc().isEmpty() ? mod.getType().name() : mod.getDesc();
        FontManager.sfmedium.drawDefaultString(g, desc, x + 8 * k, y + 16.5f * k, 6.5f * k,
                RenderUtils.rgba(255, 255, 255, (int) ((95 + 40 * hov) * a * ap)));

        float tw = 22 * k;
        float th = 11 * k;
        float tx = x + w - 8 * k - tw;
        float ty = y + (h - th) / 2f;
        float tog = animValue("tg" + mod.getName(), mod.isState(), dt);
        int togOff = RenderUtils.rgb(46, 46, 54);
        RenderUtils.drawFlowRectangle(g, tx, ty, tw, th / 2f, th,
                fade(mix(togOff, accent, tog), a * ap), fade(mix(togOff, accent2, tog), a * ap));
        float knob = 9 * k;
        float kx = tx + 1 * k + (tw - 2 * k - knob) * tog;
        RenderUtils.drawRoundedRectangle(g, kx, ty + (th - knob) / 2f, knob, knob / 2f, knob,
                RenderUtils.rgba(255, 255, 255, (int) (255 * a * ap)));

        float gr = 9 * k;
        float gxx = tx - 4 * k - gr;
        boolean gHov = MouseUtility.isHovered(mx, my, gxx, ty - 1 * k, gr, gr + 2 * k);
        float gv = animValue("gear" + mod.getName(), gHov, dt);

        FontManager.icons.drawDefaultString(g, "0", gxx + 1, ty + 1, 9, RenderUtils.rgba(255, 255, 255, (int) ((85 + 120 * gv) * a * ap)));
    }

    private void drawSettings(GuiGraphicsExtractor g, float[] win, float k, float a, float mx, float my, float dt) {
        float e = expandAnim.get();
        float[] r = settingsRect(win, k);
        float sw = r[2];
        float sh = r[3];

        float cx = r[0] + sw / 2f;
        float cy = r[1] + sh / 2f;
        float sc = 0.88f + 0.12f * e;
        float x0 = cx + (r[0] - cx) * sc - (1f - e) * 10 * k;
        float y0 = cy + (r[1] - cy) * sc;
        float w0 = sw * sc;
        float h0 = sh * sc;

        RenderUtils.drawBlur(g, x0, y0, w0, h0, 9 * k, RenderUtils.rgba(24, 18, 38, (int) (110 * e * a)));
        RenderUtils.drawRoundedRectangle(g, x0, y0, w0, 9 * k, h0, RenderUtils.rgba(17, 17, 21, (int) (246 * e * a)));
        RenderUtils.drawRoundedBorder(g, x0, y0, w0, h0, 9 * k, 1f, RenderUtils.rgba(255, 255, 255, (int) (15 * e * a)));

        float xb = x0 + w0 - 21 * k;
        float yb = y0 + 5 * k;
        float xbw = 16 * k;
        float xbh = 14 * k;
        float xv = animValue("closex", MouseUtility.isHovered(mx, my, xb, yb, xbw, xbh), dt);
        RenderUtils.drawRoundedRectangle(g, xb, yb, xbw, 4 * k, xbh,
                fade(mix(RenderUtils.rgb(34, 34, 40), RenderUtils.rgb(122, 42, 50), xv), e * a));
        FontManager.sfbold.drawDefaultString(g, "X", xb + (xbw - FontManager.sfbold.getWidth("X", 8 * k)) / 2f,
                yb + (xbh - FontManager.sfbold.getHeight(8 * k)) / 2f, 8 * k,
                fade(mix(RenderUtils.rgb(175, 175, 182), RenderUtils.rgb(255, 95, 105), xv), e * a));
        FontManager.sfbold.drawDefaultString(g, expandedName, x0 + 12 * k,
                y0 + (24 * k - FontManager.sfbold.getHeight(9.5f * k)) / 2f, 9.5f * k,
                RenderUtils.rgba(245, 245, 250, (int) (255 * e * a)));

        List<Setting> settings = expandedSettings;
        float rowsTop = y0 + 28 * k;
        float rowsH = h0 - 28 * k - 6 * k;
        float rx = x0 + 5 * k;
        float rw = w0 - 10 * k;
        float total = 0;
        for (Setting s : settings) {
            total += rowHeight(s, k);
        }
        float maxScroll = Math.max(0, total - rowsH);
        setScrollTarget = CalculateUtility.clamp(setScrollTarget, 0, maxScroll);

        g.enableScissor(Math.round(x0 + 2), Math.round(rowsTop), Math.round(x0 + w0 - 2), Math.round(rowsTop + rowsH));
        float yy = rowsTop - setScroll;
        for (int j = 0; j < settings.size(); j++) {
            Setting s = settings.get(j);
            float rh = rowHeight(s, k);
            SettingRender render = renderers.get(s.getClass());
            if (render != null) {
                float stag = Easing.CUBIC_OUT.ease(CalculateUtility.clamp((e * 1.3f - j * 0.09f) / 0.5f));
                float ry = yy + (1f - stag) * 5 * k;
                if (ry + rh >= rowsTop && ry <= rowsTop + rowsH) {
                    float hov = animValue(s, MouseUtility.isHovered(mx, my, rx, ry, rw, rh), dt);
                    RenderUtils.drawRoundedRectangle(g, rx, ry, rw, 3.5f * k, rh,
                            fade(mix(RenderUtils.rgb(20, 20, 25), RenderUtils.rgb(28, 28, 34), hov), e * a * stag));
                    render.render(g, s, rx, ry, rw, k, e * a * stag, mx, my, dt);
                }
            }
            yy += rh;
        }
        if (settings.isEmpty()) {
            drawNoSettings(g, rx, rowsTop, rw, rowsH, k, e * a);
        }
        g.disableScissor();
    }

    private void drawNoSettings(GuiGraphicsExtractor g, float x, float y, float w, float h, float k, float a) {
        float size = 7.5f * k;
        List<String> lines = wrap("Настроек для этого модуля не нашлось:(", size, w);
        float lineH = FontManager.sfmedium.getHeight(size) + 3 * k;
        float blockH = lines.size() * lineH;
        float ly = y + Math.max(0, (h - blockH) / 2f);
        for (String line : lines) {
            RenderUtils.drawGif(g, Identifier.fromNamespaceAndPath("velvet", "textures/animations/improved/notfound"), x + 27.5f, y + 25, 100, 100, 0, fade(RenderUtils.rgb(255, 255, 255), 1 * a));
            FontManager.sfmedium.drawDefaultString(g, line,
                    x + (w - FontManager.sfmedium.getWidth(line, size)) / 2f, ly + 50, size,
                    fade(RenderUtils.rgb(255, 255, 255), 0.5f * a));
            ly += lineH;
        }
    }

    private void drawNoModules(GuiGraphicsExtractor g, float x, float y, float w, float h, float k, float a) {
        float size = 7.5f * k;
        List<String> lines = wrap("Модулей по вашему запросу не найдено:(", size, w);
        float lineH = FontManager.sfmedium.getHeight(size) + 3 * k;
        float blockH = lines.size() * lineH;
        float ly = y + Math.max(0, (h - blockH) / 2f);

        for (String line : lines) {
            RenderUtils.drawGif(g, Identifier.fromNamespaceAndPath("velvet", "textures/animations/improved/notfound"),
                    x + (w - 100 * k) / 2f, ly - 55 * k, 100 * k, 100 * k, 0,
                    fade(RenderUtils.rgb(255, 255, 255), 1 * a));

            FontManager.sfmedium.drawDefaultString(g, line,
                    x + (w - FontManager.sfmedium.getWidth(line, size)) / 2f, ly + 25 * k, size,
                    fade(RenderUtils.rgb(255, 255, 255), 0.5f * a));
            ly += lineH;
        }
    }

    private List<String> wrap(String text, float size, float maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (FontManager.sfmedium.getWidth(candidate, size) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    @Subscribe
    public void onMouseButton(EventMouseButton event) {
        if (!opened) return;

        float mx = event.getMouseX();
        float my = event.getMouseY();

        if (event.getAction() == 0) {
            if (event.getButton() == 1) {
                leftHeld = false;
            }
            sliderRender.stop();
            colorRender.stop();
            dragWindow = false;
            return;
        }

        if (event.getButton() == 1) {
            leftHeld = true;
            sliderRender.stop();
            colorRender.stop();
        }

        if (event.getButton() != 1 && event.getButton() != 2 && event.getButton() != 3) {
            return;
        }

        float k = scale();
        float[] win = winRect();

        if (expandedSettings != null) {
            float[] r = settingsRect(win, k);
            if (MouseUtility.isHovered(mx, my, r[0], r[1], r[2], r[3])) {
                float xb = r[0] + r[2] - 21 * k;
                float yb = r[1] + 5 * k;
                if (MouseUtility.isHovered(mx, my, xb, yb, 16 * k, 14 * k)) {
                    expandAnim.run(false);
                    return;
                }
                if (my < r[1] + 26 * k && event.getButton() == 1) {
                    dragWindow = true;
                    dragAnchorX = mx - setOffX;
                    dragAnchorY = my - setOffY;
                    return;
                }

                List<Setting> settings = expandedSettings;
                float rowsTop = r[1] + 28 * k;
                float rowsH = r[3] - 28 * k - 6 * k;
                float rx = r[0] + 5 * k;
                float rw = r[2] - 10 * k;

                if (mx >= rx && mx <= rx + rw && my >= rowsTop && my <= rowsTop + rowsH) {
                    float cy = rowsTop - setScroll;
                    for (int j = 0; j < settings.size(); j++) {
                        Setting s = settings.get(j);
                        float rh = rowHeight(s, k);
                        SettingRender render = renderers.get(s.getClass());
                        if (render != null && event.getButton() == 1 && MouseUtility.isHovered(mx, my, rx, cy, rw, rh)) {
                            render.click(s, mx, my, rx, cy, rw, k);
                        }
                        cy += rh;
                    }
                }
                return;
            }
        }

        if (!MouseUtility.isHovered(mx, my, win[0], win[1], win[2], win[3])) {
            setSearchFocused(false);
            return;
        }

        float sw = side * k;
        float rowH = 23 * k;
        float cy0 = win[1] + 40 * k;
        for (int i = 0; i < CATS.length; i++) {
            float bx = win[0] + 7 * k;
            float by = cy0 + i * (rowH + 3 * k);
            if (MouseUtility.isHovered(mx, my, bx, by, sw - 14 * k, rowH)) {
                if (selectedType != CATS[i] && event.getButton() == 1) {
                    selectCategory(CATS[i]);
                }
                return;
            }
        }

        float scx = win[0] + (side + 10) * k;
        float scy = win[1] + 10 * k;
        float scw = win[2] - (side + 20) * k;
        if (MouseUtility.isHovered(mx, my, scx, scy, scw, 20 * k)) {
            if (event.getButton() == 1) {
                setSearchFocused(true);
            }
            return;
        }
        setSearchFocused(false);

        float gx = scx;
        float gy = scy + 20 * k + 8 * k;
        float gwd = scw;
        float ghd = win[1] + win[3] - gy - 10 * k;

        if (selectedType == Type.Themes) {
            themeRender.click(mx, my, gx, gy, gwd, ghd, k, event.getButton(), search);
            return;
        }

        List<Module> modules = visibleModules();
        if (!search.isEmpty() && modules.isEmpty()) {
            return;
        }

        int cols = gwd > 150 * k ? 2 : 1;
        float gap = 6 * k;
        float cardW = (gwd - gap * (cols - 1)) / cols;
        float cardH = 30 * k;

        if (mx >= gx && mx <= gx + gwd && my >= gy && my <= gy + ghd) {
            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int row = i / cols;
                int col = i % cols;
                float px = gx + col * (cardW + gap);
                float py = gy + row * (cardH + gap) - scroll;
                if (!MouseUtility.isHovered(mx, my, px, py, cardW, cardH)) continue;

                float tw = 22 * k;
                float th = 11 * k;
                float tx = px + cardW - 8 * k - tw;
                float ty = py + (cardH - th) / 2f;
                float gr = 9 * k;
                float gxx = tx - 4 * k - gr;

                if (MouseUtility.isHovered(mx, my, gxx, ty - 1 * k, gr, gr + 2 * k) || event.getButton() == 3) {
                    openSettings(mod.getName(), mod.getSettings());
                } else if (event.getButton() == 1) {
                    mod.touch();
                }
                return;
            }
        }
    }

    @Subscribe
    public void onMouseScroll(EventMouseScroll event) {
        if (!opened) return;
        float dy = (float) event.getScrollY() * 16f;

        float mx = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float my = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (expandedSettings != null) {
            float[] r = settingsRect(winRect(), scale());
            if (MouseUtility.isHovered(mx, my, r[0], r[1], r[2], r[3])) {
                setScrollTarget = CalculateUtility.clamp(setScrollTarget - dy, 0, Float.MAX_VALUE);
                return;
            }
        }
        if (selectedType == Type.Themes) {
            themeRender.onScroll(dy);
            return;
        }
        scrollTarget = CalculateUtility.clamp(scrollTarget - dy, 0, Float.MAX_VALUE);
    }

    @Subscribe
    public void onChar(EventChar event) {
        if (!opened || !searchFocused) return;
        String c = event.getCharacter();
        if (!c.isEmpty() && c.charAt(0) >= 32 && search.length() < 24) {
            search += c;
            onSearchChanged();
        }
    }

    @Subscribe
    public void onKey(EventKey event) {
        if (!opened || !searchFocused) return;
        if (event.getKey() == InputConstants.KEY_BACKSPACE && event.getAction() != 0 && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            onSearchChanged();
        }
    }

    private void selectCategory(Type t) {
        selectedType = t;
        search = "";
        setSearchFocused(false);
        appearT = 0;
        scroll = 0;
        scrollTarget = 0;
        themeRender.reset();
        if (expandedSettings != null) {
            expandAnim.run(false);
        }
    }

    public void openSettings(String name, List<Setting> settings) {
        if (name.equals(expandedName)) return;
        expandedName = name;
        expandedSettings = settings;
        setOffX = 0;
        setOffY = 0;
        setScroll = 0;
        setScrollTarget = 0;
        sliderRender.stop();
        colorRender.reset();
        expandAnim.reset();
        expandAnim.run(true);
    }

    private void onSearchChanged() {
        appearT = 0;
        scroll = 0;
        scrollTarget = 0;
        themeRender.reset();
    }

    private List<Module> visibleModules() {
        List<Module> all = VelvetHack.getInstance().getModuleInitializator().getModules();
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            return all.stream().filter(m ->
                    m.getName().toLowerCase(Locale.ROOT).contains(q)
                            || m.getDesc().toLowerCase(Locale.ROOT).contains(q)
                            || m.getType().name().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
        return all.stream().filter(m -> m.getType() == selectedType).toList();
    }

    float rowHeight(Setting s, float k) {
        SettingRender render = renderers.get(s.getClass());
        return render == null ? 20 * k : render.height(s, k);
    }

    private float scale() {
        return 0.92f + 0.08f * anim.get();
    }

    private float[] winRect() {
        float gw = mc.getWindow().getGuiScaledWidth();
        float gh = mc.getWindow().getGuiScaledHeight();
        float k = scale();
        float w = Math.min(470, gw - 30) * k;
        float h = Math.min(312, gh - 30) * k;
        return new float[]{gw / 2f - w / 2f, gh / 2f - h / 2f + (1f - anim.get()) * 12f, w, h};
    }

    private float[] settingsRect(float[] win, float k) {
        float gh = mc.getWindow().getGuiScaledHeight();
        float sw = 168 * k;
        float sh = Math.min(252, gh - 40) * k;
        float sx = win[0] - sw - 8 * k;
        if (sx < 4) sx = win[0] + win[2] + 8 * k;
        return new float[]{sx + setOffX, win[1] + setOffY, sw, sh};
    }

    public static float appear(float t, int i) {
        return Easing.CUBIC_OUT.ease(CalculateUtility.clamp((t - Math.min(i, 12) * 0.045f) / 0.3f));
    }

    private static int catIndex(Type t) {
        for (int i = 0; i < CATS.length; i++) {
            if (CATS[i] == t) return i;
        }
        return 0;
    }

    public float animValue(Object key, float target, float dt) {
        float v = anims.getOrDefault(key, 0f);
        v += (target - v) * Math.min(1f, dt * 14f);
        if (Math.abs(v - target) < 0.002f) v = target;
        anims.put(key, v);
        return v;
    }

    public float animValue(Object key, boolean on, float dt) {
        return animValue(key, on ? 1f : 0f, dt);
    }

    public static int mix(int from, int to, float t) {
        float f = 1f - t;
        return RenderUtils.rgba(
                (int) (((from >> 16) & 255) * f + ((to >> 16) & 255) * t),
                (int) (((from >> 8) & 255) * f + ((to >> 8) & 255) * t),
                (int) ((from & 255) * f + (to & 255) * t),
                (int) (((from >> 24) & 255) * f + ((to >> 24) & 255) * t));
    }

    public static int fade(int color, float f) {
        return RenderUtils.rgba((color >> 16) & 255, (color >> 8) & 255, color & 255,
                (int) (((color >> 24) & 255) * CalculateUtility.clamp(f)));
    }

    public static int alpha(int color, float a) {
        return RenderUtils.rgba((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) a);
    }
}