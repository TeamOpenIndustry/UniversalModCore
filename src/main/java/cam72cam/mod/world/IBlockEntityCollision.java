package cam72cam.mod.world;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.math.Vec3i;

public interface IBlockEntityCollision {
    boolean canCollide(World world, Vec3i pos, Entity entity);
}
