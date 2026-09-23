package im.best0ption.velvet.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import im.best0ption.velvet.event.EventTick;
import im.best0ption.velvet.modules.Module;
import im.best0ption.velvet.modules.ModuleRegister;
import im.best0ption.velvet.modules.Type;
import im.best0ption.velvet.settings.BooleanSetting;
import im.best0ption.velvet.settings.ModeSetting;
import im.best0ption.velvet.settings.SliderSetting;

@ModuleRegister(name = "AutoSprint", type = Type.Player, desc = "Просто бегает за тебя")
public class AutoSprint extends Module {

    public AutoSprint() {
    }

    @Subscribe
    public void onUpdate(EventTick eventTick) {
        if (mc.player == null) return;

        mc.options.keySprint.setDown(true);

    }
}