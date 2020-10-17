package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.AxisAlignedBB;

public interface IBoundingBox {
    IBoundingBox INFINITE = from(TileEntity.INFINITE_EXTENT_AABB);
    IBoundingBox ORIGIN = from(new AxisAlignedBB(0,0,0,0,0,0));

    static IBoundingBox from(AxisAlignedBB internal) {
        if (internal == null) {
            return null;
        }
        // TODO consider caching min/max objects?
        return new IBoundingBox() {
            @Override
            public Vec3d min() {
                return new Vec3d(internal.minX, internal.minY, internal.minZ);
            }

            @Override
            public Vec3d max() {
                return new Vec3d(internal.maxX, internal.maxY, internal.maxZ);
            }

            @Override
            public IBoundingBox expand(Vec3d centered) {
                return from(internal.expand(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox contract(Vec3d centered) {
                return from(internal.contract(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox grow(Vec3d val) {
                return from(internal.grow(val.x, val.y, val.z));
            }

            @Override
            public IBoundingBox offset(Vec3d vec3d) {
                return from(internal.offset(vec3d.internal()));
            }

            @Override
            public double calculateXOffset(IBoundingBox other, double offsetX) {
                return internal.calculateXOffset(new BoundingBox(other), offsetX);
            }

            @Override
            public double calculateYOffset(IBoundingBox other, double offsetY) {
                return internal.calculateYOffset(new BoundingBox(other), offsetY);
            }

            @Override
            public double calculateZOffset(IBoundingBox other, double offsetZ) {
                return internal.calculateZOffset(new BoundingBox(other), offsetZ);
            }

            @Override
            public boolean intersects(Vec3d min, Vec3d max) {
                return internal.intersects(min.x, min.y, min.z, max.x, max.y, max.z);
            }

            @Override
            public boolean contains(Vec3d vec) {
                return internal.contains(vec.internal());
            }
        };
    }

    /** Create a new 0 size BB at pos */
    static IBoundingBox from(Vec3i pos) {
        return from(new AxisAlignedBB(pos.internal()));
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
