package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterRecipeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Used for posting <code>RegisterRecipeEvent</code>
 * @see RegisterRecipeEvent
 */
@Mixin(RecipeManager.class)
public class MixinRecipeManager {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/ImmutableMap;builder()Lcom/google/common/collect/ImmutableMap$Builder;"))
    public void captureBuilder(Map<ResourceLocation, JsonElement> p_44037_, ResourceManager p_44038_, ProfilerFiller p_44039_, CallbackInfo ci,
                               @Local ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder,
                               @Share("builder") LocalRef<ImmutableMap.Builder<ResourceLocation, Recipe<?>>> builderLocalRef) {
        builderLocalRef.set(builder);
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 1))
    public void postRecipeReload(Map<ResourceLocation, JsonElement> p_44037_, ResourceManager p_44038_, ProfilerFiller p_44039_, CallbackInfo ci,
                                 @Local(ordinal = 1) Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> mapLocalRef,
                                 @Share("builder") LocalRef<ImmutableMap.Builder<ResourceLocation, Recipe<?>>> builderLocalRef) {
        RegisterRecipeEvent event = new RegisterRecipeEvent(mapLocalRef, builderLocalRef.get());
        ModLoader.get().postEvent(event);
    }
}
