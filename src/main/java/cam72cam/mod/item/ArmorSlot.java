package cam72cam.mod.item;

public enum ArmorSlot {
    MAINHAND(-2),
    OFFHAND(-1),
    FEET(3),
    LEGS(2),
    CHEST(1),
    HEAD(0);
    public final int internal;

    ArmorSlot(int slot) {
        this.internal = slot;
    }

    public static ArmorSlot from(int i) {
        for (ArmorSlot value : values()) {
            if (value.internal == i) {
                return value;
            }
        }
        return null;
    }
}
