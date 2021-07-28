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
        return IBoundingBox.from(internal.addCoord(centered.x, centered.y, centered.z));
    }

    @Override
    public IBoundingBox contract(Vec3d centered) {
        return IBoundingBox.from(internal.addCoord(-centered.x, -centered.y, -centered.z));
    }

    @Override
    public IBoundingBox grow(Vec3d val) {
        return IBoundingBox.from(internal.expand(val.x, val.y, val.z));
    }

    @Override
    public IBoundingBox offset(Vec3d vec3d) {
        return IBoundingBox.from(internal.offset(vec3d.x, vec3d.y, vec3d.z));
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
    public boolean contains(Vec3d vec) {
        return internal.isVecInside(vec.internal());
    }
}
