package cam72cam.mod.input;

import net.minecraft.client.gui.GuiScreen;

public class Clipboard {
    /**
     * Get text from system clipboard
     */
    public static String getClipboard() {
        return GuiScreen.getClipboardString();
    }

    /**
     * Set text of system clipboard
     */
    public static void setClipboard(String text) {
        GuiScreen.setClipboardString(text);
    }
}
