package im.best0ption.velvet.settings;

/**
 * Setting that cycles through a list of modes.
 */
public class ModeSetting extends Setting {

    private final String[] modes;
    private int index;

    public ModeSetting(String name, String[] modes) {
        this(name, modes, 0);
    }

    public ModeSetting(String name, String[] modes, int defaultIndex) {
        super(name);
        this.modes = modes;
        this.index = Math.max(0, Math.min(modes.length - 1, defaultIndex));
    }

    public String[] getModes() {
        return modes;
    }

    public String getValue() {
        if (modes.length == 0) return "";
        return modes[Math.max(0, Math.min(index, modes.length - 1))];
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.max(0, Math.min(modes.length - 1, index));
    }

    public void cycle() {
        if (modes.length == 0) return;
        index = (index + 1) % modes.length;
    }
}