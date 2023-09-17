package cam72cam.mod.text;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeHooks;

/** A message that can be sent to a player */
public class PlayerMessage {
    public final Component internal;

    private PlayerMessage(Component component) {
        internal = component;
    }

    /** Untranslated */
    public static PlayerMessage direct(String msg) {
        return new PlayerMessage(Component.literal(msg));
    }

    /** Translated */
    public static PlayerMessage translate(String msg, Object... objects) {
        return new PlayerMessage(Component.translatable(msg, objects));
    }

    /** URL Formatted (clickable) */
    public static PlayerMessage url(String url) {
        return new PlayerMessage(ForgeHooks.newChatWithLinks(url));
    }
}
