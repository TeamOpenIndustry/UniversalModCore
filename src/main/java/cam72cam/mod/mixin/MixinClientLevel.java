package cam72cam.mod.mixin;

import cam72cam.mod.block.tile.TileEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    @Shadow @Final private BlockStatePredictionHandler blockStatePredictionHandler;

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    public void inject(BlockPos pos, BlockState state, int flag, CallbackInfo ci) {
        ClientLevel self = (ClientLevel) (Object) this;
        if (self.getBlockEntity(pos) instanceof TileEntity) {
            this.blockStatePredictionHandler.updateKnownServerState(pos, state);
            self.setBlock(pos, state, flag, 512);
        }
    }
}
