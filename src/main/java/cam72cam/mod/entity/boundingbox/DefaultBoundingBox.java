package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;

/** Default implementation of IBoundingBox, do not use directly! */
public class DefaultBoundingBox implements IBoundingBox {
    protected final AxisAlignedBB internal;
    private Vec3d minCached;
    private Vec3d maxCached;

    public DefaultBoundingBox(AxisAlignedBB internal) {
        this.internal = internal;
    }

    @Override
    public Vec3d min() {
        if (minCached == null) {
            minCached = new Vec3d(internal.minX, internal.minY, internal.minZ);
        }
        return minCached;
    }

    @Override
    public Vec3d max() {
        if (maxCached == null) {
            maxCached = new Vec3d(internal.maxX, internal.maxY, internal.maxZ);
        }
        return maxCached;
    }

    @Override
    public IBoundingBox expand(Vec3d centered) {
        return IBoundingBox.from(internal.expand(centered.x, centered.y, centered.z));
    }

    @Override
    public IBoundingBox contract(Vec3d centered) {
        return IBoundingBox.from(internal.contract(centered.x, centered.y, centered.z));
    }

    @Override
    public IBoundingBox grow(Vec3d val) {
        return IBoundingBox.from(internal.grow(val.x, val.y, val.z));
    }

    @Override
    public IBoundingBox offset(Vec3d vec3d) {
        return IBoundingBox.from(internal.offset(vec3d.internal()));
    }

    @Override
    public double calculateXOffset(IBoundingBox other, double offsetX) {
        return internal.calculateXOffset(BoundingBox.from(other), offsetX);
    }

    @Override
    public double calculateYOffset(IBoundingBox other, double offsetY) {
        return internal.calculateYOffset(BoundingBox.from(other), offsetY);
    }

    @Override
    public double calculateZOffset(IBoundingBox other, double offsetZ) {
        return internal.calculateZOffset(BoundingBox.from(other), offsetZ);
    }

    @Override
    public boolean intersects(Vec3d min, Vec3d max) {
        return internal.intersects(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    @Override
    public IBoundingBox expandToFit(IBoundingBox other) {
        Vec3d min = min();
        Vec3d max = max();

        min = min != null ? min.min(other.min()) : other.min();
        max = max != null ? max.max(other.max()) : other.max();

        return IBoundingBox.from(min, max);
    }

    @Override
    public Vec3d getCenter() {
        return new Vec3d(internal.getCenter());
    }

    @Override
    public boolean intersectsSegment(Vec3d startVec, Vec3d endVec) {
        double tmin = 0.0;
        double tmax = 1.0;

        for (int i = 0; i < 2; i++) {
            double start = i == 0 ? startVec.x : startVec.z;
            double end = i == 0 ? endVec.x : endVec.z;
            double boxMin = i == 0 ? internal.minX : internal.minZ;
            double boxMax = i == 0 ? internal.maxX : internal.maxZ;

            if (endVec.y < internal.minY || endVec.y > internal.maxY) return false;

            double direction = end - start;
            if (Math.abs(direction) < 1e-8) {
                if (start < boxMin || start > boxMax) return false;
            } else {
                double t1 = (boxMin - start) / direction;
                double t2 = (boxMax - start) / direction;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);

                if (tmin > tmax) return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Vec3d vec) {
        return internal.contains(vec.internal());
    }
}
