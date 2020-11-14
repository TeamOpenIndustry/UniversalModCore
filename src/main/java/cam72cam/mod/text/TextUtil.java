package cam72cam.mod.text;

import net.minecraft.util.Language;

/** Client side translation utility */
@SuppressWarnings("deprecation")
public class TextUtil {
    public static String translate(String name) {
        return Language.getInstance().translate(name);
    }

    public static String translate(String name, Object[] objects) {
        return String.format(Language.getInstance().translate(name), objects);
    }
}
