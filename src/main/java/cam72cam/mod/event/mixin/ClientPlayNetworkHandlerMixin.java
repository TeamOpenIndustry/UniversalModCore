package cam72cam.mod.event.mixin;

import cam72cam.mod.block.tile.TileEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow
    private MinecraftClient client;

    @Inject(method = "onBlockEntityUpdate", at=@At(value="TAIL"))
    public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo info) {
        if (this.client.world.isBlockLoaded(packet.getPos())) {
            BlockEntity blockEntity = this.client.world.getBlockEntity(packet.getPos());

            if (blockEntity instanceof TileEntity) {
                blockEntity.fromTag(packet.getCompoundTag());
            }
        }
    }
}
