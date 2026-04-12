package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.LoadDatapackEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
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
    private static void callback(PackRepository repo, DataPackConfig config, boolean forceModData, FeatureFlagSet set, CallbackInfoReturnable<WorldDataConfiguration> cir) {
        LoadDatapackEvent event = new LoadDatapackEvent(repo);
        ModLoader.get().postEvent(event);
    }
}
