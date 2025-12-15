package cam72cam.mod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Util {
    public static RegistryAccess getDefaultRegistry() {
        if(FMLLoader.getDist().isClient()) {
            return Minecraft.getInstance().getConnection().registryAccess();
        }
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }
}
