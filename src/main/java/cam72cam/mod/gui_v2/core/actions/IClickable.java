package cam72cam.mod.gui_v2.core.actions;

import cam72cam.mod.entity.Player;

/**
 * Represents
 */
public interface IClickable {
    /**
     *
     * @param hand PRIMARY for left click, otherwise SECONDARY
     * @param x
     * @param y
     * @return
     */
    boolean consumeClick(Player.Hand hand, float x, float y);
}
