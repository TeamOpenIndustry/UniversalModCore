package cam72cam.mod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class RegistryUtil {
    private static ThreadLocal<HolderLookup.Provider> registries = new ThreadLocal<>();

    public static void update(HolderLookup.Provider provider) {
        registries.set(provider);
    }

    public static HolderLookup.Provider defaultRegistry() {
        if(FMLLoader.getDist().isClient()) {
            if (Minecraft.getInstance().getConnection() != null) {
                return Minecraft.getInstance().getConnection().registryAccess();
            }
            return registries.get();
        }
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }
}
