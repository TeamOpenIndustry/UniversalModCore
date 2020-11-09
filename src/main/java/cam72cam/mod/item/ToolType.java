package cam72cam.mod.item;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE(net.minecraftforge.common.ToolType.PICKAXE),
    AXE(net.minecraftforge.common.ToolType.AXE),
    SHOVEL(net.minecraftforge.common.ToolType.SHOVEL),
    ;
    final net.minecraftforge.common.ToolType internal;

    ToolType(net.minecraftforge.common.ToolType internal) {
        this.internal = internal;
    }

    public String toString() {
        return this.internal.getName();
    }
}
