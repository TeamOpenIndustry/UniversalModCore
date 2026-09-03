package cam72cam.mod.item;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE,
    AXE,
    SHOVEL,
    ;

    public String toString() {
        return super.toString().toLowerCase();
    }
}
