package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;

public interface IClickable {
    IClickable NOP = (player, hand) -> ClickResult.PASS;

    static IClickable get(Object o) {
        if (o instanceof IClickable) {
            return (IClickable) o;
        }
        return NOP;
    }

    /** Called when entity is interacted with */
    ClickResult onClick(Player player, Player.Hand hand);
}
