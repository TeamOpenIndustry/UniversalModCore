package cam72cam.mod.input;

import net.minecraft.client.Minecraft;

/**
 * Client utility class to interact with the system clipboard.
 */
public class Clipboard {
    /**
     * Retrieves the current text content of the system clipboard.
     *
     * @return the clipboard string, or {@code ""} if the clipboard is empty or inaccessible
     */
    public static String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    /**
     * Sets the system clipboard to the given text.
     *
     * @param newText the text to place on the clipboard
     */
    public static void setClipboard(String newText) {
        Minecraft.getInstance().keyboardHandler.setClipboard(newText);
    }
}