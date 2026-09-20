package im.best0ption.velvet.mixin;

import im.best0ption.velvet.VelvetHack;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class PauseScreenBlockerMixin {

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void blockPauseScreen(boolean paused, CallbackInfo ci) {
        if (VelvetHack.getInstance().getClickGUIScreen().isOpened()) {
            ci.cancel();
        }
    }
}