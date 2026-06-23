package cam72cam.umc.api.world;

import cam72cam.umc.api.entity.Entity;
import cam72cam.umc.api.math.Vec3i;

public interface IBlockEntityCollision {
    boolean canCollide(World world, Vec3i pos, Entity entity);
}
