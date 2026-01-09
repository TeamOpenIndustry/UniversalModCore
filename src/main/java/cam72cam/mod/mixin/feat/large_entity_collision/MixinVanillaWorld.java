package cam72cam.mod.mixin.feat.large_entity_collision;

import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.world.ChunkPos;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hook into <code>World</code> to inject large UMC entities into search result
 */
@Mixin(World.class)
public abstract class MixinVanillaWorld {
    @Shadow
    protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Inject(method = "getEntitiesInAABBexcluding", at = @At("RETURN"))
    public void injectEntitySearch0(Entity entityIn, AxisAlignedBB aabb, Predicate<? super Entity> filter,
                                    CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> result = cir.getReturnValue();
        cam72cam.mod.world.World world = cam72cam.mod.world.World.get((World) (Object) this);
        Set<Long> collection = world.tracker.queryPotentialPackedChunkPos(
                ChunkPos.asLong(new Vec3d((aabb.minX + aabb.maxX) / 2, (aabb.minY + aabb.maxY) / 2, (aabb.minZ + aabb.maxZ) / 2)));
        if (!collection.isEmpty()) {
            for (long packed : collection) {
                int x = ChunkPos.x(packed);
                int z = ChunkPos.z(packed);
                if (this.isChunkLoaded(x, z, true)) {
                    //Search entities on our own instead of by Chunk class
                    result.addAll(world.tracker.queryEntities(packed)
                                               .stream()
                                               .filter(e -> e.getEntityBoundingBox() != null
                                                       && e != entityIn
                                                       && (filter == null || filter.apply(e))
                                                       && e.getEntityBoundingBox().intersects(aabb))
                                               .collect(Collectors.toList()));
                }
            }
        }
    }

    @Inject(method = "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;", at = @At("RETURN"))
    public void injectEntitySearch1(Class<? extends Entity> clazz, AxisAlignedBB aabb, Predicate<? super Entity> filter,
                                    CallbackInfoReturnable<List<Entity>> cir) {
        if (!ModdedEntity.class.isAssignableFrom(clazz)) {
            //Target is not a UMC entity, nothing needed to do here
            return;
        }

        List<Entity> result = cir.getReturnValue();
        cam72cam.mod.world.World world = cam72cam.mod.world.World.get((World) (Object) this);
        Set<Long> collection = world.tracker.queryPotentialPackedChunkPos(
                ChunkPos.asLong(new Vec3d((aabb.minX + aabb.maxX) / 2, (aabb.minY + aabb.maxY) / 2, (aabb.minZ + aabb.maxZ) / 2)));
        if (!collection.isEmpty()) {
            for (long packed : collection) {
                int x = ChunkPos.x(packed);
                int z = ChunkPos.z(packed);
                if (this.isChunkLoaded(x, z, true)) {
                    //Search entities on our own instead of by Chunk class
                    result.addAll(world.tracker.queryEntities(packed)
                                               .stream()
                                               .filter(e -> e.getCollisionBoundingBox() != null
                                                       && (filter == null || filter.apply(e))
                                                       && e.getEntityBoundingBox().intersects(aabb))
                                               .collect(Collectors.toList()));
                }
            }
        }
    }
}
