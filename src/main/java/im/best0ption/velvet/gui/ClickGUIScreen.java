package im.best0ption.velvet.gui;

import com.mojang.blaze3d.platform.InputConstants;
import im.best0ption.velvet.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class ClickGUIScreen extends Screen {
    public ClickGUIScreen() {
        super(Component.nullToEmpty("ffdsdffddfsfsdfsddfsfdsdsfdfsdsfdsfsdfsdfdsfsddsf"));
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        return;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRoundedRectangle(graphics,10,10,100,10,100,RenderUtils.rgb(255,255,255));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RSHIFT || event.key() == InputConstants.KEY_ESCAPE) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }
}
