package cam72cam.mod.gui_v2;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.gui_v2.core.ScreenWrapper;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

public class GuiUtils {
    public static int TEXT_HEIGHT = 8;

    public static int mouseX;
    public static int mouseY;
    private static ScreenWrapper current;

    public static int getMouseX() {
        return mouseX;
    }

    public static int getMouseY() {
        return mouseY;
    }

    public static void setCurrent(ScreenWrapper current) {
        //TODO Ridiculous NPE
        GuiUtils.current = current;
    }

    public static int getTextWidth(PlayerMessage text) {
        return getTextWidth(text.internal.getFormattedText());
    }

    public static int getTextWidth(String text) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
    }

    public static int getScreenWidth() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
    }

    public static int getScreenHeight() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
    }

    /** Try to open an external link in player's browser */
    public void openLink(String url){
        ITextComponent component = new TextComponentString("");
        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        if (current != null) {
            current.handleComponentClick(component);
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
        if (current != null) {
            current.handleComponentClick(component);
        } else {
            ModCore.error("Trying to open a file outside a screen: %s", path);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.direct("Please check this location on your computer: " + path));
            }
        }
    }
}
