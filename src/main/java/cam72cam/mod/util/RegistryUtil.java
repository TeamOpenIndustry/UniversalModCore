package cam72cam.mod.util;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RegistryUtil {
    private static ICondition.IContext CONTEXT = null;

    public static void recipeBuildingContext(ICondition.IContext context) {
        CONTEXT = context;
    }

    public static List<ItemStack> resolveTagsRecipePhase(@Nonnull Fuzzy fuzzy) {
        if (CONTEXT == null) return new ArrayList<>();
        return CONTEXT.getTag(fuzzy.getTag()).stream()
                      .filter(Objects::nonNull)
                      .filter(Holder::isBound)
                      .map(holder -> new ItemStack(new net.minecraft.world.item.ItemStack(holder.value())))
                      .toList();
    }

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
