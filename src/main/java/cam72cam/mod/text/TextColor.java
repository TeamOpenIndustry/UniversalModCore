package cam72cam.mod.text;

import net.minecraft.util.Formatting;

public enum TextColor {
    BLACK(Formatting.BLACK),
    DARK_BLUE(Formatting.DARK_BLUE),
    DARK_GREEN(Formatting.DARK_GREEN),
    DARK_AQUA(Formatting.DARK_AQUA),
    DARK_RED(Formatting.DARK_RED),
    DARK_PURPLE(Formatting.DARK_PURPLE),
    GOLD(Formatting.GOLD),
    GRAY(Formatting.GRAY),
    DARK_GRAY(Formatting.DARK_GRAY),
    BLUE(Formatting.BLUE),
    GREEN(Formatting.GREEN),
    AQUA(Formatting.AQUA),
    RED(Formatting.RED),
    LIGHT_PURPLE(Formatting.LIGHT_PURPLE),
    YELLOW(Formatting.YELLOW),
    WHITE(Formatting.WHITE),
    OBFUSCATED(Formatting.OBFUSCATED),
    BOLD(Formatting.BOLD),
    STRIKETHROUGH(Formatting.STRIKETHROUGH),
    UNDERLINE(Formatting.UNDERLINE),
    ITALIC(Formatting.ITALIC),
    RESET(Formatting.RESET);
    public final Formatting internal;

    TextColor(Formatting color) {
        internal = color;
    }

    public String wrap(String text) {
        return internal + text + TextColor.RESET;
    }

    public String toString() {
        return internal.toString();
    }
}
