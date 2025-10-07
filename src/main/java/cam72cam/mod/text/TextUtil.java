package cam72cam.mod.text;

import cam72cam.mod.ModCore;
import cpw.mods.fml.common.SidedProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;

import java.util.Locale;

/** Client side translation utility */
@SuppressWarnings("deprecation")
public class TextUtil {
    public static String translate(String name) {
        return proxy.translate(name, new Object[0]);
    }

    public static String translate(String name, Object[] objects) {
        return proxy.translate(name, objects);
    }

    @SidedProxy(clientSide = "cam72cam.mod.text.TextUtil$ClientTranslator", serverSide = "cam72cam.mod.text.TextUtil$ServerTranslator", modId = ModCore.MODID)
    public static Translator proxy;

    public static abstract class Translator {
        public abstract String translate(String name, Object[] objects);
    }

    public static class ClientTranslator extends Translator {
        @Override
        public String translate(String name, Object[] objects) {
            return I18n.format(name, objects);
        }
    }

    public static class ServerTranslator extends Translator {
        @Override
        public String translate(String name, Object[] objects) {
            return name; // TODO
        }
    }

    public static Locale getClientLocal(){
        LanguageManager manager = Minecraft.getMinecraft().getLanguageManager();
        Language lang = manager.getCurrentLanguage();
        String l = lang == null ? "en-US" : lang.getLanguageCode().replace('_', '-');
        return Locale.forLanguageTag(l);
    }
}
