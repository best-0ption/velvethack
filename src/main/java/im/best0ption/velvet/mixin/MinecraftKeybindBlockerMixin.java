package im.best0ption.velvet.mixin;

import im.best0ption.velvet.VelvetHack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftKeybindBlockerMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void saddasasdsdaasdasdsdasddsaadsdszxcZXCzXC(CallbackInfo ci) {
        if (VelvetHack.getInstance().getClickGUIScreen().isOpened()) {
            KeyMapping.releaseAll();
            ci.cancel();
        }
    }
}