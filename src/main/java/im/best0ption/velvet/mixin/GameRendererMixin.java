package im.best0ption.velvet.mixin;

import im.best0ption.velvet.render.RenderUtils;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void processBlurEffect(CallbackInfo ci) {
        if (RenderUtils.runBlurPasses()) {
            ci.cancel();
        }
    }
}
