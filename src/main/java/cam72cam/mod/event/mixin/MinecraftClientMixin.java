package cam72cam.mod.event.mixin;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "joinWorld(Lnet/minecraft/client/world/ClientWorld;)V", at=@At("HEAD"))
    public void joinWorld(ClientWorld clientWorld_1, CallbackInfo info) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            CommonEvents.World.UNLOAD.execute(c -> c.accept(world));
        }
    }

    @Inject(method = "initializeSearchableContainers", at=@At("HEAD"))
    public void init(CallbackInfo info) {
        ClientEvents.TEXTURE_STITCH.execute(Runnable::run);
        ModCore.instance.postInit();
    }
}
