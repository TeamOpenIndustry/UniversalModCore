package cam72cam.mod.gui_v2.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//TODO Container
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
}