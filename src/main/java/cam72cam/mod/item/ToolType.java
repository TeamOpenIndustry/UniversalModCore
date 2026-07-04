package cam72cam.mod.item;

import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.ToolAction;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE(ToolActions.PICKAXE_DIG),
    AXE(ToolActions.AXE_DIG),
    SHOVEL(ToolActions.SHOVEL_DIG),
    ;
    public final ToolAction internal;

    ToolType(ToolAction internal) {
        this.internal = internal;
    }

    public String toString() {
        return this.internal.name();
    }
}
