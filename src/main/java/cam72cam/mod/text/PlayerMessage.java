package cam72cam.mod.text;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeHooks;

/** A message that can be sent to a player */
public class PlayerMessage {
    public final ITextComponent internal;

    private PlayerMessage(ITextComponent component) {
        internal = component;
    }

    /** Untranslated */
    public static PlayerMessage direct(String msg) {
        return new PlayerMessage(new StringTextComponent(msg));
    }

    /** Translated */
    public static PlayerMessage translate(String msg, Object... objects) {
        return new PlayerMessage(new TranslationTextComponent(msg, objects));
    }

    /** URL Formatted (clickable) */
    public static PlayerMessage url(String url) {
        return new PlayerMessage(ForgeHooks.newChatWithLinks(url));
    }
}
