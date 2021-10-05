package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;

public interface IBoundingBox {
    IBoundingBox INFINITE = new DefaultBoundingBox(TileEntity.INFINITE_EXTENT_AABB);
    IBoundingBox ORIGIN = new DefaultBoundingBox(new AxisAlignedBB(0,0,0,0,0,0));
    IBoundingBox BLOCK = new DefaultBoundingBox(Block.FULL_BLOCK_AABB);

    static IBoundingBox from(AxisAlignedBB internal) {
        if (internal == null) {
            return null;
        }
        if (internal instanceof BoundingBox) {
            return ((BoundingBox) internal).internal;
        }
        if (internal == Block.FULL_BLOCK_AABB ||
                internal.minX == 0 && internal.minY == 0 && internal.minZ == 0 &&
                internal.maxX == 1 && internal.maxY == 1 && internal.maxZ == 1
        ) {
            return BLOCK;
        }
        return new DefaultBoundingBox(internal);
    }

    /** Create a new 0 size BB at pos */
    static IBoundingBox from(Vec3i pos) {
        return from(new AxisAlignedBB(pos.internal()));
    }

    static IBoundingBox from(Vec3d start, Vec3d end) {
        return from(new AxisAlignedBB(start.x, start.y, start.z, end.x, end.y, end.z));
    }

    /** Smaller corner of the BB */
    Vec3d min();

    /** Larger corner of the BB */
    Vec3d max();

    /** Expands the BB in one direction (positive/negative) */
    IBoundingBox expand(Vec3d val);

    /** Contracts the BB in one direction (positive/negative) */
    IBoundingBox contract(Vec3d val);

    /** Increase the BB's size in all dimensions by value specified (by axis) */
    IBoundingBox grow(Vec3d val);

    /** Move the BB by the given amount */
    IBoundingBox offset(Vec3d vec3d);

    double calculateXOffset(IBoundingBox other, double offsetX);

    double calculateYOffset(IBoundingBox other, double offsetY);

    double calculateZOffset(IBoundingBox other, double offsetZ);

    /** Does the AABB represented by these coords intersect this BB */
    boolean intersects(Vec3d min, Vec3d max);

    /** Is this vector within bounds */
    boolean contains(Vec3d vec);

    default boolean intersects(IBoundingBox bounds) {
        return this.intersects(bounds.min(), bounds.max());
    }

}
