package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterRecipeEvent;
import cam72cam.mod.util.RegistryUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    @Shadow
    @Final
    private ICondition.IContext context;

    private MixinRecipeManager(Gson p_10768_, String p_10769_) {
        super(p_10768_, p_10769_);
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/resources/RegistryOps;withContext(Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Object;)Ljava/lang/Object;"))
    public void captureBuilder(Map<ResourceLocation, JsonElement> p_44037_, ResourceManager p_44038_, ProfilerFiller p_44039_, CallbackInfo ci,
                               @Local(name = "builder") ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> builder,
                               @Local(name = "builder1") ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder1) {
        RegistryUtil.recipeBuildingContext(context);
        RegisterRecipeEvent event = new RegisterRecipeEvent(builder, builder1);
        ModLoader.get().postEvent(event);
        RegistryUtil.recipeBuildingContext(null);
    }
}
