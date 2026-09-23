package im.best0ption.velvet.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventMouseButton {

    private int button;

    private int action;
    private float mouseX;
    private float mouseY;
}