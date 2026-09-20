package im.best0ption.velvet.mixin;

import im.best0ption.velvet.VelvetHack;
import im.best0ption.velvet.event.EventTick;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class EventTickHook
{
    @Inject(method = "tick",at = @At("HEAD"))
    private void hook(CallbackInfo ci){
        VelvetHack.getInstance().getEventBus().post(new EventTick());
    }
}
