package cam72cam.mod.text;


import net.minecraft.util.text.LanguageMap;

/** Client side translation utility */
public class TextUtil {
    //TODO this breaks server side ...
    public static String translate(String name) {
        return translate(name, new Object[0]);
    }

    public static String translate(String name, Object[] objects) {
        return String.format(LanguageMap.getInstance().func_230503_a_(name), objects);
    }
}
