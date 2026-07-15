package cam72cam.mod.input;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Utility class to interact with the system clipboard.
 */
@SideOnly(Side.CLIENT)
public class Clipboard {
    /**
     * Retrieves the current text content of the system clipboard.
     *
     * @return the clipboard string, or {@code ""} if the clipboard is empty or inaccessible
     */
    public static String getClipboard() {
        return GuiScreen.getClipboardString();
    }

    /**
     * Sets the system clipboard to the given text.
     *
     * @param newText the text to place on the clipboard
     */
    public static void setClipboard(String newText) {
        GuiScreen.setClipboardString(newText);
    }
}