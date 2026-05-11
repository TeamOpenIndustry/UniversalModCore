package cam72cam.mod.gui_v2.core.actions;

import cam72cam.mod.entity.Player;

/**
 * Represents
 */
public interface IClickable {
    /**
     * PRIMARY for left click, otherwise SECONDARY
     */
    boolean onClick(Player.Hand hand, float x, float y);
}
