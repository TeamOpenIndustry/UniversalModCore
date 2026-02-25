package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterRecipeEvent;
import cam72cam.mod.util.RegistryUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.ModLoader;
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
public abstract class MixinRecipeManager extends SimpleJsonResourceReloadListener {
    private MixinRecipeManager(Gson p_10768_, String p_10769_) {
        super(p_10768_, p_10769_);
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/crafting/RecipeManager;makeConditionalOps()Lnet/neoforged/neoforge/common/conditions/ConditionalOps;"))
    public void captureBuilder(Map<ResourceLocation, JsonElement> p_44037_, ResourceManager p_44038_, ProfilerFiller p_44039_, CallbackInfo ci,
                               @Local ImmutableMultimap.Builder<ResourceLocation, RecipeHolder<?>> builder,
                               @Local ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder1,
                               @Local RegistryOps<JsonElement> ops) {
        RegistryUtil.recipeBuildingContext(this.makeConditionalOps().context);
        RegisterRecipeEvent event = new RegisterRecipeEvent(builder, builder1);
        ModLoader.postEvent(event);
        RegistryUtil.recipeBuildingContext(null);
    }
}
