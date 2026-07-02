package cam72cam.mod.util;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.conditions.ConditionContext;
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

    public static HolderSet<Item> getRecipeItemTags(@Nonnull Fuzzy fuzzy) {
        if (!(CONTEXT instanceof ConditionContext con) || !CONTEXT.isTagLoaded(fuzzy.getTag())) return HolderSet.empty();
        return (HolderSet<Item>) con.pendingTags.get(fuzzy.getTag().registry()).get((TagKey) fuzzy.getTag()).get();
    }

    public static List<ItemStack> resolveTagsRecipePhase(@Nonnull Fuzzy fuzzy) {
        if (CONTEXT == null || !CONTEXT.isTagLoaded(fuzzy.getTag())) return new ArrayList<>();
        HolderSet.Named<Item> taggedItems = CONTEXT.registryAccess().lookupOrThrow(Registries.ITEM).get(
                fuzzy.getTag()).orElseThrow();
        return taggedItems.stream()
                      .filter(Objects::nonNull)
                      .filter(Holder::isBound)
                      .map(holder -> new ItemStack(new net.minecraft.world.item.ItemStack(holder.value())))
                      .toList();
    }

    public static RegistryAccess getRegistry() {
        try {
            if (EffectiveSide.get().isClient()) {
                //Logical client
                return Minecraft.getInstance().getConnection().registryAccess();
            } else {
                //Integrated or dedicated server
                return ServerLifecycleHooks.getCurrentServer().registryAccess();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FuelValues getFuelValues() {
        try {
            if (EffectiveSide.get().isClient()) {
                return Minecraft.getInstance().getConnection().fuelValues();
            } else {
                return ServerLifecycleHooks.getCurrentServer().fuelValues();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
