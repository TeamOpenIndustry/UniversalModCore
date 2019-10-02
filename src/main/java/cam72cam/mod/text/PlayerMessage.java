package cam72cam.mod.text;


import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class PlayerMessage {
    public final Text internal;

    private PlayerMessage(Text component) {
        internal = component;
    }

    public static PlayerMessage direct(String msg) {
        return new PlayerMessage(new LiteralText(msg));
    }

    public static PlayerMessage translate(String msg, Object... objects) {
        return new PlayerMessage(new TranslatableText(msg, objects));
    }

    public static PlayerMessage url(String url) {
        LiteralText text = new LiteralText(url);
        text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        return new PlayerMessage(text);
    }
}
