package cam72cam.mod.mixin.fix.custom_bb_collision;

import cam72cam.mod.entity.boundingbox.CustomVoxelShape;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inject our own VoxelShape so custom BB collision data won't get lost
 * @see CustomVoxelShape
 */
@Mixin(VoxelShapes.class)
public class MixinVoxelShapes {
    @Inject(method = "create(Lnet/minecraft/util/math/AxisAlignedBB;)Lnet/minecraft/util/math/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private static void inj(AxisAlignedBB aabb, CallbackInfoReturnable<VoxelShape> cir) {
        if (aabb instanceof BoundingBox) {
            cir.setReturnValue(new CustomVoxelShape((BoundingBox) aabb));
            cir.cancel();
        }
    }
}
