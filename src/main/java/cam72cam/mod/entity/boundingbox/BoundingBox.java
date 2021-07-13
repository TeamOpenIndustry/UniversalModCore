package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Optional;

public class BoundingBox extends AxisAlignedBB {
    public final IBoundingBox internal;

    private BoundingBox(IBoundingBox internal, double[] constructorParams) {
        super(constructorParams[0], constructorParams[1], constructorParams[2], constructorParams[3], constructorParams[4], constructorParams[5]);
        this.internal = internal;
    }

    private BoundingBox(IBoundingBox internal) {
        this(internal, hack(internal));
    }

    public static AxisAlignedBB from(IBoundingBox internal) {
        if (internal instanceof DefaultBoundingBox) {
            return ((DefaultBoundingBox) internal).internal;
        }
        return new BoundingBox(internal);
    }

    private static double[] hack(IBoundingBox internal) {
        Vec3d min = internal.min();
        Vec3d max = internal.max();
        return new double[]{max.x, max.y, max.z, min.x, min.y, min.z};
    }

    @Override
    public BoundingBox intersect(AxisAlignedBB p_191500_1_) {
        // Used by piston
        return this;
    }

    @Override
    public BoundingBox minmax(AxisAlignedBB other) {
        // Used by piston
        // Used by entityliving for BB stuff
        return this;
    }

    /* Modifiers */

    @Override
    public BoundingBox expandTowards(double x, double y, double z) {
        return new BoundingBox(internal.expand(new Vec3d(x, y, z)));
    }

    @Override
    public BoundingBox contract(double x, double y, double z) {
        return new BoundingBox(internal.contract(new Vec3d(x, y, z)));
    }

    @Override
    public BoundingBox inflate(double x, double y, double z) {
        return new BoundingBox(internal.grow(new Vec3d(x, y, z)));
    }

    @Override
    public BoundingBox move(double x, double y, double z) {
        return new BoundingBox(internal.offset(new Vec3d(x, y, z)));
    }

    /* Interactions */
    /*
    @Override
    public double calculateXOffset(AxisAlignedBB other, double offsetX) {
        return internal.calculateXOffset(IBoundingBox.from(other), offsetX);
    }

    @Override
    public double calculateYOffset(AxisAlignedBB other, double offsetY) {
        return internal.calculateYOffset(IBoundingBox.from(other), offsetY);
    }

    @Override
    public double calculateZOffset(AxisAlignedBB other, double offsetZ) {
        return internal.calculateZOffset(IBoundingBox.from(other), offsetZ);
    }
    */

    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return super.intersects(minX, minY, minZ, maxX, maxY, maxZ) && // Fast check
                internal.intersects(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ)); // Slow check
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return internal.contains(new Vec3d(x, y, z));
    }

    @Override
    public Optional<Vector3d> clip(Vector3d vecA, Vector3d vecB) {
        int steps = 10;
        double xDist = vecB.x - vecA.x;
        double yDist = vecB.y - vecA.y;
        double zDist = vecB.z - vecA.z;
        double xDelta = xDist / steps;
        double yDelta = yDist / steps;
        double zDelta = zDist / steps;
        for (int step = 0; step < steps; step++) {
            Vec3d stepPos = new Vec3d(vecA.x + xDelta * step, vecA.y + yDelta * step, vecA.z + zDelta * step);
            if (internal.contains(stepPos)) {
                return Optional.of(stepPos.internal());
            }
        }
        return Optional.empty();
    }
}
