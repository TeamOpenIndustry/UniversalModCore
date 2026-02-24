package cam72cam.mod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.server.ServerLifecycleHooks;

public class RegistryUtil {
    public static RegistryAccess getRegistry() {
        try {
            if (FMLLoader.getDist().isClient()) {
                return Minecraft.getInstance().getConnection().registryAccess();
            } else {
                return ServerLifecycleHooks.getCurrentServer().registryAccess();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
