package im.best0ption.velvet.util;

import net.minecraft.client.Minecraft;

public interface IMinecraft {
    Minecraft mc = Minecraft.getInstance();
    default void print(String text){
        mc.getConnection().sendChat(text);
    }
}
