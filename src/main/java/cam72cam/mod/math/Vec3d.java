package cam72cam.mod.math;

import net.minecraft.world.phys.Vec3;

/** Custom Vec3d that is equivalent to MC's Vec3d */
public class Vec3d {
    public static final Vec3d ZERO = new Vec3d(Vec3.ZERO);
    public final double x;
    public final double y;
    public final double z;
    private Vec3 internal = null;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d(Vec3 internal) {
        this(internal.x, internal.y, internal.z);
        this.internal = internal;
    }

    public Vec3d(Vec3i pos) {
        this(pos.x, pos.y, pos.z);
    }

    public Vec3d add(double x, double y, double z) {
        return new Vec3d(this.x + x, this.y + y, this.z + z);
    }

    public Vec3d add(Vec3i offset) {
        return add(offset.x, offset.y, offset.z);
    }

    public Vec3d add(Vec3d other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3d subtract(Vec3d other) {
        return subtract(other.x, other.y, other.z);
    }

    public Vec3d subtract(Vec3i offset) {
        return subtract(offset.x, offset.y, offset.z);
    }

    public Vec3d subtract(double x, double y, double z) {
        return new Vec3d(this.x - x, this.y - y, this.z - z);
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double length() {
        return length(x, y, z);
    }

    public double distanceTo(Vec3d other) {
        // optimized by inlining the subtract (removes 1 allocation)
        //return this.subtract(other).length();
        return length(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vec3d scale(double scale) {
        return new Vec3d(x * scale, y * scale, z * scale);
    }

    public Vec3d normalize() {
        double length = length();
        return length < 1.0E-4D ? ZERO : scale(1/length);
    }

    public Vec3d min(Vec3d other) {
        return new Vec3d(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vec3d max(Vec3d other) {
        return new Vec3d(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", this.x, this.y, this.z);
    }

    public Vec3d rotateMinecraftYaw(float angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3d(cos * -x + sin * z, y, sin * x + cos * z);
    }

    public Vec3d rotateYaw(float angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3d(cos * x + sin * z, y, sin * x + cos * z);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Vec3d) {
            Vec3d ov = (Vec3d) other;
            return ov.x == this.x && ov.y == this.y && ov.z == this.z;
        }
        return false;
    }

    public Vec3 internal() {
        if (internal == null) {
            internal = new Vec3(x, y, z);
        }
        return internal;
    }
}
