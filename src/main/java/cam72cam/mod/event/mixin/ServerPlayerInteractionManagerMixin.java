package cam72cam.mod.event.mixin;

import cam72cam.mod.event.CommonEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    public ServerWorld world;
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at=@At("HEAD"))
    public void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        if (!CommonEvents.Block.BROKEN.executeCancellable(h -> h.onBroken(world, pos, player))) {
            info.setReturnValue(false);
        }
    }
}
