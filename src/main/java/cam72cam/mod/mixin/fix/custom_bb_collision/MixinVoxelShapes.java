package cam72cam.mod.mixin.fix.custom_bb_collision;

import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.CustomVoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inject our own VoxelShape so custom BB collision data won't get lost
 * @see CustomVoxelShape
 */
@Mixin(Shapes.class)
public class MixinVoxelShapes {
    @Inject(method = "create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private static void inj(AABB aabb, CallbackInfoReturnable<VoxelShape> cir) {
        if (aabb instanceof BoundingBox) {
            cir.setReturnValue(new CustomVoxelShape((BoundingBox) aabb));
            cir.cancel();
        }
    }
}
