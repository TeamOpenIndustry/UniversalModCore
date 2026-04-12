package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.LoadDatapackEvent;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Datapack loading callback
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "configurePackRepository", at = @At("HEAD"))
    private static void callback(ResourcePackList repo, DatapackCodec codec, boolean forceModData, CallbackInfoReturnable<DatapackCodec> cir) {
        LoadDatapackEvent event = new LoadDatapackEvent(repo);
        ModLoader.get().postEvent(event);
    }
}
