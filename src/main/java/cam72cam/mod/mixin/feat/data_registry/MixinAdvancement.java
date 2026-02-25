package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterAdvancementEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Used for posting <code>RegisterAdvancementEvent</code>
 * @see RegisterAdvancementEvent
 */
@Mixin(ServerAdvancementManager.class)
public class MixinAdvancement {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap$Builder;buildOrThrow()Lcom/google/common/collect/ImmutableMap;"))
    public void postAdvancementReload(Map<ResourceLocation, JsonElement> p_136034_, ResourceManager p_136035_, ProfilerFiller p_136036_, CallbackInfo ci,
                                      @Local ImmutableMap.Builder<ResourceLocation, AdvancementHolder> map) {
        RegisterAdvancementEvent event = new RegisterAdvancementEvent(map);
        ModLoader.postEvent(event);
    }
}
