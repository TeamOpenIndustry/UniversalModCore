package cam72cam.mod.render;

import net.minecraft.item.EnumDyeColor;

/** Wrapper for MC's Colors */
public enum Color {
    WHITE(EnumDyeColor.WHITE),
    ORANGE(EnumDyeColor.ORANGE),
    MAGENTA(EnumDyeColor.MAGENTA),
    LIGHT_BLUE(EnumDyeColor.LIGHT_BLUE),
    YELLOW(EnumDyeColor.YELLOW),
    LIME(EnumDyeColor.LIME),
    PINK(EnumDyeColor.PINK),
    GRAY(EnumDyeColor.GRAY),
    SILVER(EnumDyeColor.SILVER),
    CYAN(EnumDyeColor.CYAN),
    PURPLE(EnumDyeColor.PURPLE),
    BLUE(EnumDyeColor.BLUE),
    BROWN(EnumDyeColor.BROWN),
    GREEN(EnumDyeColor.GREEN),
    RED(EnumDyeColor.RED),
    BLACK(EnumDyeColor.BLACK),
    ;

    /** Internal, do not use */
    public final EnumDyeColor internal;

    Color(EnumDyeColor internal) {
        this.internal = internal;
    }
}