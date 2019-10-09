package cam72cam.mod.event.mixin;

import cam72cam.mod.entity.ModdedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerInteractEntityC2SPacket;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow
    private MinecraftServer server;


    @Inject(method = "onPlayerInteractEntity", at=@At(value="HEAD"), cancellable = true)
    public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo info) {
        NetworkThreadUtils.forceMainThread(playerInteractEntityC2SPacket_1, (ServerPlayNetworkHandler)(Object)this, (ServerWorld)this.player.getServerWorld());

        ServerWorld serverWorld_1 = this.server.getWorld(this.player.dimension);
        Entity target = playerInteractEntityC2SPacket_1.getEntity(serverWorld_1);

        if (target instanceof ModdedEntity) {
            switch (playerInteractEntityC2SPacket_1.getType()) {
                case INTERACT:
                    player.interact(target, playerInteractEntityC2SPacket_1.getHand());
                    info.cancel();
                    break;
                case ATTACK:
                    player.attack(target);
                    info.cancel();
                    break;
                case INTERACT_AT:
                    break;
            }
        }
    }
}
