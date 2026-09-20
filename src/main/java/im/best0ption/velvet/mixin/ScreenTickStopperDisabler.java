package im.best0ption.velvet.mixin;

import im.best0ption.velvet.VelvetHack;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenTickStopperDisabler {

    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void isableScreenTickStop(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }


}
