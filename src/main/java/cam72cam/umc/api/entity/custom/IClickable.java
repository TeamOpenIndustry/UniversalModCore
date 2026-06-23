package cam72cam.umc.api.entity.custom;

import cam72cam.umc.api.entity.Player;
import cam72cam.umc.api.item.ClickResult;

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
