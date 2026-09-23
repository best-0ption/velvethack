package im.best0ption.velvet.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import im.best0ption.velvet.event.EventTick;
import im.best0ption.velvet.modules.Module;
import im.best0ption.velvet.modules.ModuleRegister;
import im.best0ption.velvet.modules.Type;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@ModuleRegister(name = "FullBright", type = Type.Render, desc = "Да будет свет!")
public class FullBright extends Module {

    @Subscribe
    public void onUpdate(EventTick eventTick) {
        if (mc.player == null) return;
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10000, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}
