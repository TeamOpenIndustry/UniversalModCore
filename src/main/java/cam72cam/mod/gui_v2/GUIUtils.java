package cam72cam.mod.gui_v2;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.gui_v2.wrapper.ScreenBuilder;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

public class GUIUtils {
    public static float mouseX;
    public static float mouseY;
    public static ScreenBuilder currentBuilder;

    public static float getMouseX() {
        return mouseX;
    }

    public static float getMouseY() {
        return mouseY;
    }

    /** Try to open an external link in player's browser */
    public void openLink(String url){
        ITextComponent component = new TextComponentString("");
        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        if (currentBuilder != null) {
            currentBuilder.handleComponentClick(component);
        } else {
            ModCore.error("Trying to open a link outside a screen: %s", url);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.url(url));
            }
        }
    }

    /** Try to open an external link in player's browser */
    public static void openFile(String path){
        ITextComponent component = new TextComponentString("");
        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path));
        if (currentBuilder != null) {
            currentBuilder.handleComponentClick(component);
        } else {
            ModCore.error("Trying to open a file outside a screen: %s", path);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.direct("Please check this location on your computer: " + path));
            }
        }
    }
}
