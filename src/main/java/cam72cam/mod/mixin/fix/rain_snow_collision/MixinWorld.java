package cam72cam.mod.mixin.fix.rain_snow_collision;

import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.world.ChunkPos;
import cam72cam.mod.world.WorldEntityTracker;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "getPrecipitationHeight", at = @At("HEAD"), cancellable = true)
    public void checkEntityCollision(BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        World self = (World) (Object) this;
        WorldEntityTracker tracker = cam72cam.mod.world.World.get(self).tracker;
        Set<ModdedEntity> moddedEntities = tracker.queryPotentialPackedChunkPos(ChunkPos.asLong(pos))
                                                  .stream()
                                                  .map(tracker::queryEntities)
                                                  .flatMap(Set::stream)
                                                  .collect(Collectors.toSet());
        BlockPos target = self.getChunk(pos).getPrecipitationHeight(pos);
        if (moddedEntities != null && !moddedEntities.isEmpty()) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(target);
            for (ModdedEntity moddedEntity : moddedEntities) {
                AxisAlignedBB collisionBoundingBox = moddedEntity.getCollisionBoundingBox();
                if (collisionBoundingBox == null) continue;

                if (mutable.getY() < collisionBoundingBox.minY) {
                    mutable.setY((int) collisionBoundingBox.minY);
                }
                while (collisionBoundingBox.intersects(new AxisAlignedBB(mutable))) {
                    mutable.setY(mutable.getY() + 1);
                }
            }
            cir.setReturnValue(mutable.toImmutable());
        }
    }
}
