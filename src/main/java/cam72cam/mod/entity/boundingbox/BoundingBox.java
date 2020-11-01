package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

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
        return new double[]{min.x, min.y, min.z, max.x, max.y, max.z};
    }

    /* NOP */
    /* Removed 1.7.10
    @Override
    public BoundingBox setMaxY(double y) {
        // Used by blockwall
        return this;
    }
    */

    @Override
    public BoundingBox union(AxisAlignedBB other) {
        // Used by piston
        // Used by entityliving for BB stuff
        return this;
    }

    /* Modifiers */

    @Override
    public AxisAlignedBB addCoord(double x, double y, double z) {
        return new BoundingBox(internal.expand(new Vec3d(x, y, z)));
    }

    @Override
    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(internal.grow(new Vec3d(x, y, z)));
    }

    @Override
    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(internal.offset(new Vec3d(x, y, z)));
    }

    /* Interactions */
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

    @Override
    public boolean intersectsWith(AxisAlignedBB other) {
        return super.intersectsWith(other) && // Fast check
            internal.intersects(new Vec3d(other.minX, other.minY, other.minZ), new Vec3d(other.maxX, other.maxY, other.maxZ)); // Slow check
    }

    @Override
    public boolean isVecInside(Vec3 vec) {
        return internal.contains(new Vec3d(vec));
    }

    @Override
    public MovingObjectPosition calculateIntercept(Vec3 vecA, Vec3 vecB) {
        int steps = 10;
        double xDist = vecB.xCoord - vecA.xCoord;
        double yDist = vecB.yCoord - vecA.yCoord;
        double zDist = vecB.zCoord - vecA.zCoord;
        double xDelta = xDist / steps;
        double yDelta = yDist / steps;
        double zDelta = zDist / steps;
        for (int step = 0; step < steps; step++) {
            Vec3d stepPos = new Vec3d(vecA.xCoord + xDelta * step, vecA.yCoord + yDelta * step, vecA.zCoord + zDelta * step);
            if (internal.contains(stepPos)) {
                return new MovingObjectPosition(0, 0, 0, EnumFacing.UP.ordinal(), stepPos.internal());
            }
        }
        return null;
    }
}
