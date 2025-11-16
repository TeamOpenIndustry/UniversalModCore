package cam72cam.mod.item;

import net.neoforged.neoforge.common.ToolActions;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE(ToolActions.PICKAXE_DIG),
    AXE(ToolActions.AXE_DIG),
    SHOVEL(ToolActions.SHOVEL_DIG),
    ;
    public final net.neoforged.neoforge.common.ToolAction internal;

    ToolType(net.neoforged.neoforge.common.ToolAction internal) {
        this.internal = internal;
    }

    public String toString() {
        return this.internal.name();
    }
}
