package cam72cam.mod.item;

/** What class a tool fits into */
public enum ToolType {
    PICKAXE("pickaxe"),
    AXE("axe"),
    SHOVEL("shovel"),
    ;
    private final String internal;

    ToolType(String internal) {
        this.internal = internal;
    }

    public String toString() {
        return this.internal;
    }
}
