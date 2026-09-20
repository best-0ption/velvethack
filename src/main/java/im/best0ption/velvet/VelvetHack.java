package im.best0ption.velvet;

import com.google.common.eventbus.EventBus;
import com.mojang.blaze3d.platform.InputConstants;
import im.best0ption.velvet.event.EventRender2D;
import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.font.FontManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class VelvetHack implements ModInitializer {
    @Getter
    private static VelvetHack instance;
    private EventBus eventBus = new EventBus();
    private boolean rshiftWasDown;
    private boolean closing;
    private ClickGUIScreen clickGUIScreen;

    @Override
    public void onInitialize() {
        instance = this;

        clickGUIScreen = new ClickGUIScreen();
        eventBus.register(clickGUIScreen);
        eventBus.register(this);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FontManager.init();

            if (client.getWindow() == null) return;

            boolean down = InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
            if (down && !rshiftWasDown) {
                if (clickGUIScreen.isOpened()) {
                    clickGUIScreen.close();
                } else {
                    clickGUIScreen.open();
                }
            }
            rshiftWasDown = down;

            boolean close = InputConstants.isKeyDown(InputConstants.KEY_ESCAPE);
            if (close && !closing) {
                if (clickGUIScreen.isOpened()) {
                    clickGUIScreen.close();
                }
            }
            closing = close;
        });

        HudElementRegistry.addLast(Identifier.parse("velvet:eventrender2d"), (graphics, deltaTracker) ->
                eventBus.post(new EventRender2D(graphics, deltaTracker)));
    }

}