package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.Box;

public interface IBoundingBox {
    IBoundingBox INFINITE = new DefaultBoundingBox(new Box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    IBoundingBox ORIGIN = new DefaultBoundingBox(new Box(0,0,0,0,0,0));
    IBoundingBox BLOCK = new DefaultBoundingBox(new Box(0,0,0,1,1,1));

    static IBoundingBox from(Box internal) {
        if (internal == null) {
            return null;
        }
        if (internal instanceof BoundingBox) {
            return ((BoundingBox) internal).internal;
        }
        if (internal == ((DefaultBoundingBox)BLOCK).internal ||
                internal.x1 == 0 && internal.y1 == 0 && internal.z1 == 0 &&
                internal.x2 == 1 && internal.y2 == 1 && internal.z2 == 1
        ) {
            return BLOCK;
        }
        return new DefaultBoundingBox(internal);
    }

    /** Create a new 0 size BB at pos */
    static IBoundingBox from(Vec3i pos) {
        return from(new Box(pos.internal()));
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
