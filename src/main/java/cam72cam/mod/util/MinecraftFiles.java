package cam72cam.mod.util;

import cam72cam.mod.world.World;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

public class MinecraftFiles {
    /** Get base game (.minecraft) directory */
    public static File getMinecraftDir() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    /** Get config directory */
    public static File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    /**
     * Get given world's save folder
     * @return Null on Client, the world's folder on server
     */
    public static File getSaveDir(World world) {
        if (world.internal.getServer() != null) {
            return world.internal.getServer().getDataDirectory();
        }
        return null;
    }
}
