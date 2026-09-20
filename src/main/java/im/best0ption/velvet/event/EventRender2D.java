package im.best0ption.velvet.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Data
@AllArgsConstructor
public class EventRender2D {
    private GuiGraphicsExtractor graphics;
    private DeltaTracker deltaTracker;
}
