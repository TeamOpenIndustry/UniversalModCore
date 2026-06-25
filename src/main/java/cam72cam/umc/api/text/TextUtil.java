package cam72cam.umc.api.text;

import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.MinecraftForgeClient;

import java.util.Locale;

/** Client side translation utility */
@SuppressWarnings("deprecation")
public class TextUtil {
    public static String translate(String name) {
        return I18n.translateToLocal(name);
    }

    public static String translate(String name, Object... objects) {
        return I18n.translateToLocalFormatted(name, objects);
    }

    public static Locale getClientLocal(){
        return MinecraftForgeClient.getLocale();
    }
}
