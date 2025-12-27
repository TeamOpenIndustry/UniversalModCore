package cam72cam.mod.util;

import cam72cam.mod.world.World;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftFiles {
    /** Get base game (.minecraft) directory */
    public static File getMinecraftDir() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    /** Get config directory */
    public static File getConfigDir() {
        Path path = FMLPaths.CONFIGDIR.get();
        if (path == null) {
            path = Paths.get(System.getProperty("java.io.tmpdir"), "minecraft");
        }
        return path.toFile();
    }

    /**
     * Get given world's save folder
     * @return Null on Client, the world's folder on server
     */
    public static File getSaveDir(World world) {
        if (world.internal.getServer() != null) {
            return world.internal.getServer().getServerDirectory().toFile();
        }
        return null;
    }
}
