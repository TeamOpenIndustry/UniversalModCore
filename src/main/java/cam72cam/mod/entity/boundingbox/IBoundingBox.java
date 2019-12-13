package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.Box;

public interface IBoundingBox {
    static IBoundingBox from(Box internal) {
        if (internal == null) {
            return null;
        }
        return new IBoundingBox() {
            @Override
            public Vec3d min() {
                return new Vec3d(internal.x1, internal.y1, internal.z1);
            }

            @Override
            public Vec3d max() {
                return new Vec3d(internal.x2, internal.y2, internal.z2);
            }

            @Override
            public IBoundingBox expand(Vec3d centered) {
                return from(internal.stretch(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox contract(Vec3d centered) {
                return from(internal.shrink(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox grow(Vec3d val) {
                return from(internal.expand(val.x, val.y, val.z));
            }

            @Override
            public IBoundingBox offset(Vec3d vec3d) {
                return from(internal.offset(vec3d.internal));
            }

            @Override
            public double calculateXOffset(IBoundingBox other, double offsetX) {
                return 0;//internal.calculateXOffset(new Box(other.min().internal, other.max().internal), offsetX);
            }

            @Override
            public double calculateYOffset(IBoundingBox other, double offsetY) {
                return 0;//internal.calculateYOffset(new Box(other.min().internal, other.max().internal), offsetY);
            }

            @Override
            public double calculateZOffset(IBoundingBox other, double offsetZ) {
                return 0;//internal.calculateZOffset(new Box(other.min().internal, other.max().internal), offsetZ);
            }

            @Override
            public boolean intersects(Vec3d a, Vec3d b) {
                Vec3d min = a.min(b);
                Vec3d max = a.max(b);
                return internal.intersects(min.x, min.y, min.z, max.x, max.y, max.z);
            }

            @Override
            public boolean contains(Vec3d vec) {
                return internal.contains(vec.internal);
            }
        };
    }

    static IBoundingBox from(Vec3i pos) {
        return from(new Box(pos.internal));
    }

    Vec3d min();

    Vec3d max();

    IBoundingBox expand(Vec3d val);

    IBoundingBox contract(Vec3d val);

    IBoundingBox grow(Vec3d val);

    IBoundingBox offset(Vec3d vec3d);

    double calculateXOffset(IBoundingBox other, double offsetX);

    double calculateYOffset(IBoundingBox other, double offsetY);

    double calculateZOffset(IBoundingBox other, double offsetZ);

    boolean intersects(Vec3d min, Vec3d max);

    boolean contains(Vec3d vec);

    default boolean intersects(IBoundingBox bounds) {
        return this.intersects(bounds.min(), bounds.max());
    }
}
