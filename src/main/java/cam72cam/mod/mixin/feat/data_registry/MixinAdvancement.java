package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterAdvancementEvent;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Used for posting <code>RegisterAdvancementEvent</code>
 * @see RegisterAdvancementEvent
 */
@Mixin(AdvancementManager.class)
public class MixinAdvancement {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V",
            at = @At(value = "NEW", target = "()Lnet/minecraft/advancements/AdvancementList;"))
    public void postAdvancementReload(Map<ResourceLocation, JsonObject> p_212853_1_, IResourceManager p_212853_2_,
                                      IProfiler p_212853_3_, CallbackInfo ci, @Local(ordinal = 1) Map<ResourceLocation, Advancement.Builder> map) {
        RegisterAdvancementEvent event = new RegisterAdvancementEvent(map);
        ModLoader.get().postEvent(event);
    }
}
