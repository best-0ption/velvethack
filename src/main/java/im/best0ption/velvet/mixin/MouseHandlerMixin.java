package im.best0ption.velvet.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import im.best0ption.velvet.VelvetHack;
import im.best0ption.velvet.event.EventMouseButton;
import im.best0ption.velvet.event.EventMouseScroll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void dasdsaasddsadsdsasda(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        boolean opened = VelvetHack.getInstance().getClickGUIScreen().isOpened();
        if (opened) {
            Minecraft mc = Minecraft.getInstance();
            float mx = (float) MouseHandler.getScaledXPos(mc.getWindow(), ((MouseHandler) (Object) this).xpos());
            float my = (float) MouseHandler.getScaledYPos(mc.getWindow(), ((MouseHandler) (Object) this).ypos());
            VelvetHack.getInstance().getEventBus().post(new EventMouseButton(button.button(), action, mx, my));
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void asddsazzxc(long window, double scrollX, double scrollY, CallbackInfo ci) {
        boolean opened = VelvetHack.getInstance().getClickGUIScreen().isOpened();
        if (opened) {
            VelvetHack.getInstance().getEventBus().post(new EventMouseScroll(scrollX, scrollY));
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void asddsadscxz(CallbackInfo ci) {
        if (VelvetHack.getInstance().getClickGUIScreen().isOpened()) {
            ci.cancel();
        }
    }
}