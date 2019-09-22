package cam72cam.mod.text;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;

public class PlayerMessage {
    public final IChatComponent internal;

    private PlayerMessage(IChatComponent component) {
        internal = component;
    }

    public static PlayerMessage direct(String msg) {
        return new PlayerMessage(new ChatComponentText(msg));
    }

    public static PlayerMessage translate(String msg, Object... objects) {
        return new PlayerMessage(new ChatComponentTranslation(msg, objects));
    }

    public static PlayerMessage url(String url) {
        return new PlayerMessage(ForgeHooks.newChatWithLinks(url));
    }
}
