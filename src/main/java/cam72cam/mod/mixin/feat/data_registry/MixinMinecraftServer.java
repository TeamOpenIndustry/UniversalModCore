package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.LoadDatapackEvent;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Datapack loading callback
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Shadow
    @Final
    private ResourcePackList<ResourcePackInfo> resourcePacks;

    @Inject(method = "loadDataPacks(Ljava/io/File;Lnet/minecraft/world/storage/WorldInfo;)V", at = @At("HEAD"))
    public void callback(File p_195560_1_, WorldInfo p_195560_2_, CallbackInfo ci) {
        LoadDatapackEvent event = new LoadDatapackEvent(this.resourcePacks);
        ModLoader.get().postEvent(event);
    }
}
