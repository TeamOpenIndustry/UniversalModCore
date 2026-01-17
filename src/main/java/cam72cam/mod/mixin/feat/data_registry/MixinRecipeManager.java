package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterCraftingRecipeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;


@Mixin(RecipeManager.class)
public class MixinRecipeManager {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 1))
    public void postRecipeReload(Map<ResourceLocation, JsonObject> splashList, IResourceManager resourceManagerIn,
                                 IProfiler profilerIn, CallbackInfo ci,
                                 @Local(ordinal = 1) Map<IRecipeType<?>, ImmutableMap.Builder<ResourceLocation, IRecipe<?>>> mapLocalRef) {
        RegisterCraftingRecipeEvent event = new RegisterCraftingRecipeEvent(mapLocalRef);
        ModLoader.get().postEvent(event);
    }
}
