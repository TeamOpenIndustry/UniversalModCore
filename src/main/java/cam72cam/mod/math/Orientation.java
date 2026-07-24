package cam72cam.mod.math;

import util.Matrix4;

/**
 * Represents an orthonormal local coordinate system, stored as a 3×3 rotation matrix.
 * <p>
 * The internal matrix is column‑major: column 0 = right, column 1 = up, column 2 = forward.
 * All rotation methods are implemented via {@link Matrix3d#rotateLocal} / {@link Matrix3d#rotateWorld},
 * which avoid building temporary vectors and give much better performance than per‑vector Rodrigues.
 * <p>
 * Instances are immutable – rotation methods return a new Orientation.
 *
 * @author DeepseaSaltyFish
 */
public class Orientation {
    private final Matrix3d internal;

    private Orientation(Matrix3d matrix) {
        this.internal = matrix;
    }

    private Orientation(Vec3d forward, Vec3d right, Vec3d up) {
        this(new Matrix3d(right, up, forward));
    }

    /**
     * Constructs an orientation from forward and right vectors.
     * The supplied vectors do not need to be perfectly orthogonal.
     */
    public Orientation(Vec3d forward, Vec3d right) {
        Vec3d r = right.subtract(forward.scale(right.dotProduct(forward)));
        Vec3d u = forward.crossProduct(r);
        this.internal = new Matrix3d(forward, r, u);
    }

    /**
     * Constructs an orientation from Euler angles (yaw → pitch → roll, degrees).
     */
    public static Orientation fromEuler(double yaw, double pitch, double roll) {
        Matrix4 mat = new Matrix4();
        mat.rotate(Math.toRadians(yaw),   0, 1, 0);
        mat.rotate(Math.toRadians(pitch), 1, 0, 0);
        mat.rotate(Math.toRadians(roll),  0, 0, 1);

        Vec3d right   = new Vec3d(mat.m00, mat.m10, mat.m20);
        Vec3d up      = new Vec3d(mat.m01, mat.m11, mat.m21);
        Vec3d forward = new Vec3d(mat.m02, mat.m12, mat.m22);

        return new Orientation(forward, right, up);
    }

    //Cached
    private Vec3d forward, right, up;

    public Vec3d forward() {
        if (forward == null ||
                (forward.x != internal.m02
                 || forward.y != internal.m12
                 || forward.z != internal.m22)
        ) {
            forward = new Vec3d(internal.m02, internal.m12, internal.m22);
        }
        return forward;
    }

    public Vec3d right() {
        if (right == null ||
                (right.x != internal.m00
                 || right.y != internal.m10
                 || right.z != internal.m20)
        ) {
            right = new Vec3d(internal.m00, internal.m10, internal.m20);
        }
        return right;
    }

    public Vec3d up() {
        if (up == null ||
                (up.x != internal.m01
                 || up.y != internal.m11
                 || up.z != internal.m21)
        ) {
            up = new Vec3d(internal.m01, internal.m11, internal.m21);
        }
        return up;
    }

    /**
     * Return the Euler representation of this Orientation in degrees, stored in Vec3d
     */
    public Vec3d toEuler() {
        Matrix4 m = toMatrix();
        double pitch = -Math.asin(m.m12);
        double yaw, roll;
        if (Math.abs(Math.cos(pitch)) > 1E-6) {
            yaw  = Math.atan2(m.m02, m.m22);
            roll = Math.atan2(m.m10, m.m11);
        } else {
            yaw  = Math.atan2(-m.m20, m.m00);
            roll = 0;
        }
        return new Vec3d(Math.toDegrees(yaw), Math.toDegrees(pitch), Math.toDegrees(roll));
    }

    /**
     * Convert this to a {@link Matrix3d}
     * <p>
     * In most cases you would need the method below
     */
    public Matrix3d toMatrix3d() {
        return internal.copy();
    }

    public Matrix4 toMatrix() {
        return new Matrix4(internal);
    }

    public Matrix4 toInverseMatrix() {
        return toMatrix().transpose();
    }

    public Quaternion toQuaternion() {
        return internal.toQuaternion();
    }

    /**
     * Rotates around a world‑space axis and returns a new Orientation.
     */
    public Orientation rotateWorld(Vec3d worldAxis, double angle) {
        Matrix3d rotated = internal.copy();
        rotated.rotateWorld(worldAxis, angle);
        return new Orientation(rotated);
    }

    /**
     * Rotates around a local axis and returns a new Orientation.
     * The axis is expressed in the current local frame.
     */
    public Orientation rotateLocal(Vec3d localAxis, double angle) {
        Matrix3d rotated = internal.copy();
        rotated.rotateLocal(localAxis, angle);
        return new Orientation(rotated);
    }

    public Orientation rotatePitch(double degrees) {
        return rotateLocal(Matrix3d.RIGHT, Math.toRadians(degrees));
    }

    public Orientation rotateYaw(double degrees) {
        return rotateLocal(Matrix3d.UP, Math.toRadians(degrees));
    }

    public Orientation rotateRoll(double degrees) {
        return rotateLocal(Matrix3d.FORWARD, Math.toRadians(degrees));
    }

    /**
     * Re-orthogonalizes the basis and returns a new Orientation.
     * Uses Gram–Schmidt on the stored matrix columns.
     */
    public Orientation normalize() {
        Vec3d f = forward().normalize();
        Vec3d r = right().subtract(f.scale(right().dotProduct(f))).normalize();
        Vec3d u = f.crossProduct(r).normalize();
        return new Orientation(f, r, u);
    }


    @Override
    public String toString() {
        return String.format("Orientation{forward=%s, right=%s, up=%s}", forward(), right(), up());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Orientation)) return false;
        Orientation o = (Orientation) obj;
        return internal.equals(o.internal);
    }

    @Override
    public int hashCode() {
        return forward().hashCode() + 31 * (right().hashCode() + 31 * up().hashCode());
    }
}