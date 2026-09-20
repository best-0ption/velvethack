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
    @Override
    public void onInitialize() {
        instance = this;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null && InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)) {
                FontManager.init();
                client.setScreenAndShow(new ClickGUIScreen());

            }
        });

        HudElementRegistry.addLast(Identifier.parse("velvet:eventrender2d"), (graphics, deltaTracker) ->
                eventBus.post(new EventRender2D(graphics, deltaTracker)));

        eventBus.register(this);
    }
}
