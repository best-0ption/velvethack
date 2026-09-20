package im.best0ption.velvet.gui;

import com.google.common.eventbus.Subscribe;
import im.best0ption.velvet.event.EventRender2D;
import im.best0ption.velvet.event.EventTick;
import im.best0ption.velvet.render.RenderUtils;
import im.best0ption.velvet.util.AnimationValue;
import im.best0ption.velvet.util.Easing;
import im.best0ption.velvet.util.IMinecraft;
import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;

@Getter
//FUCK YOUR MOJANG!!! IM DONT USE YOUR FUCKING SCREEN.CLASS!!!!
public class ClickGUIScreen implements IMinecraft {
    public boolean opened;
    private boolean closing;
    private final AnimationValue anim = new AnimationValue(0.5f, Easing.CUBIC_IN_OUT);

    public ClickGUIScreen() {}

    public void open() {
        anim.reset();
        anim.setReverse(false);
        anim.start();
        closing = false;
        opened = true;
    }@Subscribe
    public void onTick(EventTick eventTick){


    }
    @Subscribe
    public void onRender(EventRender2D eventRender2D) {
        if (!opened) return;

        anim.update(mc.getDeltaTracker().getRealtimeDeltaTicks() / 20f);

        if (anim.get() <= 0f) {
            opened = false;
            closing = false;
            return;
        }

        float width = Math.min(500, mc.getWindow().getGuiScaledWidth() - 20) * anim.get();
        float height = Math.min(350, mc.getWindow().getGuiScaledHeight() - 20) * anim.get();
        float x = mc.getWindow().getGuiScaledWidth() / 2f - width / 2;
        float y = mc.getWindow().getGuiScaledHeight() / 2f - height / 2;

        RenderUtils.drawRoundedRectangle(
                eventRender2D.getGraphics(),
                x, y, width,
                15 * Math.min(1f, anim.get()),
                height,
                RenderUtils.rgb(255, 255, 255)
        );
    }

    public void close() {
        if (!opened || closing) return;
        closing = true;
        anim.setReverse(true);
    }
}