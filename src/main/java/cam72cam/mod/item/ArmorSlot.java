package cam72cam.mod.item;

import net.minecraft.entity.EquipmentSlot;

public enum ArmorSlot {
    MAINHAND(EquipmentSlot.MAINHAND),
    OFFHAND(EquipmentSlot.OFFHAND),
    FEET(EquipmentSlot.FEET),
    LEGS(EquipmentSlot.LEGS),
    CHEST(EquipmentSlot.CHEST),
    HEAD(EquipmentSlot.HEAD);
    public final EquipmentSlot internal;

    ArmorSlot(EquipmentSlot slot) {
        this.internal = slot;
    }

    public static ArmorSlot from(EquipmentSlot armorType) {
        switch (armorType) {
            case MAINHAND:
                return MAINHAND;
            case OFFHAND:
                return OFFHAND;
            case FEET:
                return FEET;
            case LEGS:
                return LEGS;
            case CHEST:
                return CHEST;
            case HEAD:
                return HEAD;
            default:
                return null;
        }
    }
}
