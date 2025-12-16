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
        try {
            if (FMLLoader.getDist().isClient()) {
                return Minecraft.getInstance().getConnection().registryAccess();
            }
            return ServerLifecycleHooks.getCurrentServer().registryAccess();
        } catch (Throwable e) {
            return registries.get();
        }
    }
}
