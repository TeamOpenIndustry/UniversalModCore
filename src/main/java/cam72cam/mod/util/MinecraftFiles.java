package cam72cam.mod.util;

import cam72cam.mod.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

import java.io.File;

public class MinecraftFiles {
    /** Get base game (.minecraft) directory */
    public static File getMinecraftDir() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory().getAbsoluteFile();
    }

    /** Get config directory */
    public static File getConfigDir() {
        File configDir;
        try {
            configDir = Loader.instance().getConfigDir();
        } catch (ClassCastException ex) {
            configDir = null;
        }
        if (configDir == null) {
            configDir = new File(System.getProperty("java.io.tmpdir"), "minecraft");
        }
        return configDir;
    }

    /**
     * Get given world's save folder
     * @return Null on Client, the world's folder on server
     */
    public static File getSaveDir(World world) {
        return world.internal.getSaveHandler().getWorldDirectory().getAbsoluteFile();
    }
}
