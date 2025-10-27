package cam72cam.mod.mixin.fix.block_replacement;

import cam72cam.mod.block.BlockType;
import cam72cam.mod.math.Vec3i;
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

/**
 * Fixes client-side BlockEntity updates when replacing UMC BlockEntities.
 * <p>
 * In Minecraft 1.19, Mojang added client-side BlockEntity update validation that prevents
 * {@link cam72cam.mod.world.World#setBlock(Vec3i, BlockType)} from properly replacing one UMC BlockEntity
 * with another on the client, despite working correctly on the server.
 * <p>
 * This mixin overrides the validation logic for UMC BlockEntity replacement scenarios.
 */
@Mixin(BlockStatePredictionHandler.class)
public class MixinBlockStatePredictionHandler {
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates;

    @Inject(method = "updateKnownServerState", at = @At("HEAD") ,cancellable = true)
    public void mixinStateCheck(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
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
