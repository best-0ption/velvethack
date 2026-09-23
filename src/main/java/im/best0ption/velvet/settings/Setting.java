package im.best0ption.velvet.settings;

/**
 * Base class for every module setting.
 */
public abstract class Setting {

    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}