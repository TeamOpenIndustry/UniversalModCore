package cam72cam.mod.mixin;

import cam72cam.mod.block.BlockType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStatePredictionHandler.class)
public class MixinBlockStatePredictionHandler {
    @Shadow @Final private Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates;

    @Inject(method = "updateKnownServerState", at = @At("HEAD") ,cancellable = true)
    public void inj(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        BlockStatePredictionHandler.ServerVerifiedState serverVerifiedState = this.serverVerifiedStates.get(pos.asLong());
        if (serverVerifiedState == null) {
            return;
        }
        if (state.getBlock() instanceof BlockType.BlockInternal) {
            serverVerifiedState.setBlockState(state);
            cir.setReturnValue(false);
        }
    }
}
