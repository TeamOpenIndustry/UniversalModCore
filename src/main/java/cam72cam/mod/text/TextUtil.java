package cam72cam.mod.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.util.text.translation.I18n;

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
        LanguageManager manager = Minecraft.getMinecraft().getLanguageManager();
        Language lang = manager.getCurrentLanguage();
        String l = lang == null ? "en-US" : lang.getLanguageCode().replace('_', '-');
        return Locale.forLanguageTag(l);
    }
}
