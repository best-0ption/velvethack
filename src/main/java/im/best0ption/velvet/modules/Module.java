package im.best0ption.velvet.modules;

import im.best0ption.velvet.VelvetHack;
import im.best0ption.velvet.settings.Setting;
import im.best0ption.velvet.util.IMinecraft;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Module implements IMinecraft {
    String name;
    String desc;
    Type type;
    boolean state;
    private final List<Setting> settings = new ArrayList<Setting>();
    public Module(){
        ModuleRegister reg = getClass().getAnnotation(ModuleRegister.class);
        this.name = reg.name();
        this.type = reg.type();
        this.desc = reg.desc();
    }

    /**
     * Registers settings for this module. Call inside the module constructor.
     */
    public void addSettings(Setting... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }
    public void onEnable(){
        VelvetHack.getInstance().getEventBus().register(this);
    }
    public void onDisable(){
        VelvetHack.getInstance().getEventBus().unregister(this);
    }
    public void touch(){
        this.setState(!state);
    }
    public void setState(boolean newState){
        if (newState != state){
            this.state = newState;
        }
        try {
            if (this.state){
                onEnable();
            }
            else {
                onDisable();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
