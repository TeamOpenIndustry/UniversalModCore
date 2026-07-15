package cam72cam.mod.input;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Utility class to interact with the system clipboard.
 */
@OnlyIn(Dist.CLIENT)
public class Clipboard {
    /**
     * Retrieves the current text content of the system clipboard.
     *
     * @return the clipboard string, or {@code ""} if the clipboard is empty or inaccessible
     */
    public static String getClipboard() {
        return Minecraft.getInstance().keyboardListener.getClipboardString();
    }

    /**
     * Sets the system clipboard to the given text.
     *
     * @param newText the text to place on the clipboard
     */
    public static void setClipboard(String newText) {
        Minecraft.getInstance().keyboardListener.setClipboardString(newText);
    }
}