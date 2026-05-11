package cam72cam.mod.gui_v2.core.actions;

import cam72cam.mod.entity.Player;

public interface IDraggable {
    boolean onDrag(Player.Hand hand, int mouseX, int mouseY);
    boolean onRelease(Player.Hand hand, int mouseX, int mouseY);
}
