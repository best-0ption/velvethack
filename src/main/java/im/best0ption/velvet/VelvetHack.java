package im.best0ption.velvet;

import com.mojang.blaze3d.platform.InputConstants;
import im.best0ption.velvet.gui.ClickGUIScreen;
import im.best0ption.velvet.render.font.FontManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class VelvetHack implements ModInitializer {

    @Override
    public void onInitialize() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null && InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)) {
                client.setScreenAndShow(new ClickGUIScreen());
                FontManager.init();
            }
        });


    }
}
