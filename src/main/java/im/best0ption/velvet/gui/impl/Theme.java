package im.best0ption.velvet.gui.impl;

import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.settings.ColorSetting;
import im.best0ption.velvet.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Theme {

    public final String name;
    public final ColorSetting gradient;
    public boolean custom;

    public static final List<Theme> all = new ArrayList<>();
    public static Theme current;
    static int customCount;

    static {
        all.add(new Theme("Фиолетовый", RenderUtils.rgb(157, 120, 255), RenderUtils.rgb(92, 158, 255)));
        all.add(new Theme("Желтый", RenderUtils.rgb(255, 122, 89), RenderUtils.rgb(255, 61, 113)));
        all.add(new Theme("Крутой", RenderUtils.rgb(127, 90, 240), RenderUtils.rgb(44, 181, 232)));
        all.add(new Theme("Еще круче", RenderUtils.rgb(0, 245, 160), RenderUtils.rgb(0, 217, 245)));
        all.add(new Theme("Прикольный", RenderUtils.rgb(79, 227, 193), RenderUtils.rgb(58, 168, 255)));
        all.add(new Theme("Какой то", RenderUtils.rgb(255, 179, 71), RenderUtils.rgb(255, 94, 98)));
        all.add(new Theme("Нормальный", RenderUtils.rgb(255, 111, 181), RenderUtils.rgb(161, 123, 255)));
        all.add(new Theme("Золотистый", RenderUtils.rgb(247, 208, 112), RenderUtils.rgb(245, 175, 25)));
        all.add(new Theme("Обычный", RenderUtils.rgb(252, 70, 107), RenderUtils.rgb(63, 94, 251)));
        all.add(new Theme("Синийк", RenderUtils.rgb(150, 200, 255), RenderUtils.rgb(90, 150, 255)));
        current = all.get(0);
    }

    public Theme(String name, int first, int second) {
        this.name = name;
        this.gradient = new ColorSetting("Gradient", first, second);
    }

    public int first() {
        return gradient.getFirst();
    }

    public int second() {
        return gradient.getSecond();
    }

    public List<Setting> settings() {
        return List.<Setting>of(gradient);
    }

    public String hex() {
        return String.format("#%06X", first() & 0xFFFFFF) + "  >  " + String.format("#%06X", second() & 0xFFFFFF);
    }

    public static Theme add() {
        customCount++;
        Theme t = new Theme("Custom " + customCount, current.first(), current.second());
        t.custom = true;
        all.add(t);
        current = t;
        return t;
    }

    public static List<Theme> visible(String search) {
        if (search.isEmpty()) return all;
        String q = search.toLowerCase(Locale.ROOT);
        return all.stream().filter(t -> t.name.toLowerCase(Locale.ROOT).contains(q)).toList();
    }
}
