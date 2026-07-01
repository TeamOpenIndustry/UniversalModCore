package cam72cam.mod.gui_v2.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//TODO Container support
public class GuiManager {
    @SideOnly(Side.CLIENT)
    public static void openScreen(ClientScreen screen) {
        openScreen(screen, false);
    }

    @SideOnly(Side.CLIENT)
    public static void openScreen(ClientScreen screen, boolean pausesGame) {
        ScreenWrapper sa = new ScreenWrapper(screen, pausesGame);
        Minecraft.getMinecraft().displayGuiScreen(sa);
    }

    @SideOnly(Side.CLIENT)
    public static void closeCurrentScreen() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof ScreenWrapper) {
            ScreenWrapper wrapper = (ScreenWrapper) minecraft.currentScreen;
            minecraft.displayGuiScreen(null);
            if (minecraft.currentScreen == null) {
                minecraft.setIngameFocus();
            }
            wrapper.onGuiClosed();
        }
    }
}