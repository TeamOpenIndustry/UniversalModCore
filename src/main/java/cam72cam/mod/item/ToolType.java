package cam72cam.mod.item;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE(ItemAbilities.PICKAXE_DIG),
    AXE(ItemAbilities.AXE_DIG),
    SHOVEL(ItemAbilities.SHOVEL_DIG),
    ;
    public final net.neoforged.neoforge.common.ItemAbility internal;

    ToolType(ItemAbility internal) {
        this.internal = internal;
    }

    public String toString() {
        return this.internal.name();
    }
}
