package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterRecipeEvent;
import cam72cam.mod.util.RegistryUtil;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedMap;

/**
 * Used for posting <code>RegisterRecipeEvent</code>
 * @see RegisterRecipeEvent
 */
@Mixin(RecipeManager.class)
public abstract class MixinRecipeManager extends SimplePreparableReloadListener<RecipeMap> {
    private MixinRecipeManager() {
        super();
    }

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
            at = @At(value = "INVOKE", target = "Ljava/util/SortedMap;forEach(Ljava/util/function/BiConsumer;)V"))
    public void captureBuilder(ResourceManager p_379845_, ProfilerFiller p_380058_, CallbackInfoReturnable<RecipeMap> cir, @Local SortedMap<ResourceLocation, Recipe<?>> map) {
        RegistryUtil.recipeBuildingContext(this.makeConditionalOps().context);
        RegisterRecipeEvent event = new RegisterRecipeEvent(map);
        ModLoader.postEvent(event);
        RegistryUtil.recipeBuildingContext(null);
    }
}
