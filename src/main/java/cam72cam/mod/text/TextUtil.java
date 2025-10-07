package cam72cam.mod.text;


import net.minecraft.util.text.LanguageMap;
import net.minecraftforge.client.MinecraftForgeClient;

import java.util.Locale;

/** Client side translation utility */
public class TextUtil {
    //TODO this breaks server side ...
    public static String translate(String name) {
        return translate(name, new Object[0]);
    }

    public static String translate(String name, Object[] objects) {
        return String.format(LanguageMap.getInstance().getOrDefault(name), objects);
    }

    public static Locale getClientLocal(){
        return MinecraftForgeClient.getLocale();
    }
}
