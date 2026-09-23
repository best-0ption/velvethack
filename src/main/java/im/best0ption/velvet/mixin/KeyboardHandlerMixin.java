package im.best0ption.velvet.mixin;

import im.best0ption.velvet.VelvetHack;
import im.best0ption.velvet.event.EventChar;
import im.best0ption.velvet.event.EventKey;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void velvetchar(long window, CharacterEvent event, CallbackInfo ci) {
        if (VelvetHack.getInstance().getClickGUIScreen().isOpened()) {
            VelvetHack.getInstance().getEventBus().post(new EventChar(event.codepointAsString()));
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void velvetkey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (VelvetHack.getInstance().getClickGUIScreen().isOpened()) {
            VelvetHack.getInstance().getEventBus().post(new EventKey(event.key(), action));
        }
    }
}
