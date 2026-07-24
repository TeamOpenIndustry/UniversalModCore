package cam72cam.mod.math;

import util.Matrix4;

/**
 * Representation for standard rotation.
 *
 * @author DeepseaSaltyFish
 */
public class Matrix3 {
    public static final Vec3d FORWARD = new Vec3d(0, 0, 1);
    public static final Vec3d RIGHT = new Vec3d(1, 0, 0);
    public static final Vec3d UP = new Vec3d(0, 1, 0);

    double m00, m10, m20;
    double m01, m11, m21;
    double m02, m12, m22;

    public Matrix3() {
        setIdentity();
    }

    public Matrix3(double m00, double m01, double m02,
                   double m10, double m11, double m12,
                   double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3(Matrix4 matrix4) {
        this(matrix4.m00, matrix4.m01, matrix4.m02,
             matrix4.m10, matrix4.m11, matrix4.m12,
             matrix4.m20, matrix4.m21, matrix4.m22);
    }

    /**
     * Constructs an orientation from forward and right vectors.
     * <p>
     * The supplied vectors do not need to be perfectly orthogonal.
     */
    public static Matrix3 fromBasis(Vec3d forward, Vec3d right) {
        Vec3d f = forward.normalize();
        Vec3d r = right.subtract(f.scale(right.dotProduct(f))).normalize();
        Vec3d u = f.crossProduct(r).normalize();

        return new Matrix3(r.x, u.x, f.x, r.y, u.y, f.y, r.z, u.z, f.z);
    }

    public static Matrix3 fromBasis(Vec3d forward, Vec3d right, Vec3d up) {
        forward = forward.normalize();
        right = right.normalize();
        up = up.normalize();
        return new Matrix3(right.x, up.x, forward.x, right.y, up.y, forward.y, right.z, up.z, forward.z);
    }

    /**
     * Builds a rotation from Euler angles in degrees.
     * <p>
     * Rotation order: Yaw -> Pitch -> Roll (YXZ)
     */
    public static Matrix3 fromEuler(double yaw, double pitch, double roll) {
        return new Matrix3().rotateLocal(Math.toRadians(yaw), 0, 1, 0)
                            .rotateLocal(Math.toRadians(pitch), 1, 0, 0)
                            .rotateLocal(Math.toRadians(roll), 0, 0, 1);
    }

    public static Matrix3 fromAxisAndAngle(Vec3d axis, double radians) {
        axis = axis.normalize();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        double t = 1 - c;
        double x = axis.x, y = axis.y, z = axis.z;
        return new Matrix3(
                c + x * x * t, x * y * t - z * s, x * z * t + y * s,
                y * x * t + z * s, c + y * y * t, y * z * t - x * s,
                z * x * t - y * s, z * y * t + x * s, c + z * z * t
        );
    }

    public static Matrix3 fromXRot(double radians) {
        return fromAxisAndAngle(RIGHT, radians);
    }

    public static Matrix3 fromYRot(double radians) {
        return fromAxisAndAngle(UP, radians);
    }

    public static Matrix3 fromZRot(double radians) {
        return fromAxisAndAngle(FORWARD, radians);
    }

    public Matrix3 copy() {
        return new Matrix3(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    //Cached
    private Vec3d forward = null, right = null, up = null;

    public Vec3d forward() {
        if (forward == null) {
            forward = new Vec3d(this.m02, this.m12, this.m22);
        }
        return forward;
    }

    public Vec3d right() {
        if (right == null) {
            right = new Vec3d(this.m00, this.m10, this.m20);
        }
        return right;
    }

    public Vec3d up() {
        if (up == null) {
            up = new Vec3d(this.m01, this.m11, this.m21);
        }
        return up;
    }

    public Matrix3 setIdentity() {
        m00 = m11 = m22 = 1;
        m01 = m02 = m10 = m12 = m20 = m21 = 0;
        forward = right = up = null;
        return this;
    }

    public Matrix3 multiply(Matrix3 other) {
        double t00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20;
        double t10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20;
        double t20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20;
        double t01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21;
        double t11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21;
        double t21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21;
        double t02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22;
        double t12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22;
        double t22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22;
        m00 = t00;
        m01 = t01;
        m02 = t02;
        m10 = t10;
        m11 = t11;
        m12 = t12;
        m20 = t20;
        m21 = t21;
        m22 = t22;
        forward = right = up = null;
        return this;
    }

    public Matrix3 leftMultiply(Matrix3 other) {
        double t00 = other.m00 * m00 + other.m01 * m10 + other.m02 * m20;
        double t10 = other.m10 * m00 + other.m11 * m10 + other.m12 * m20;
        double t20 = other.m20 * m00 + other.m21 * m10 + other.m22 * m20;
        double t01 = other.m00 * m01 + other.m01 * m11 + other.m02 * m21;
        double t11 = other.m10 * m01 + other.m11 * m11 + other.m12 * m21;
        double t21 = other.m20 * m01 + other.m21 * m11 + other.m22 * m21;
        double t02 = other.m00 * m02 + other.m01 * m12 + other.m02 * m22;
        double t12 = other.m10 * m02 + other.m11 * m12 + other.m12 * m22;
        double t22 = other.m20 * m02 + other.m21 * m12 + other.m22 * m22;
        m00 = t00;
        m01 = t01;
        m02 = t02;
        m10 = t10;
        m11 = t11;
        m12 = t12;
        m20 = t20;
        m21 = t21;
        m22 = t22;
        forward = right = up = null;
        return this;
    }

    /**
     * The transpose and invert of 3*3 matrix
     */
    public Matrix3 transpose() {
        double m01 = this.m10;
        double m02 = this.m20;
        double m10 = this.m01;
        double m12 = this.m21;
        double m20 = this.m02;
        double m21 = this.m12;

        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        forward = right = up = null;
        return this;
    }

    /**
     * Rotates around a local axis
     * <p>
     * The axis is expressed in the current local frame.
     */
    public Matrix3 rotateLocal(Quaternion q) {
        return multiply(q.toMatrix3());
    }

    public Matrix3 rotateLocal(double radians, double x, double y, double z) {
        return rotateLocal(new Vec3d(x, y, z), radians);
    }

    public Matrix3 rotateLocal(Vec3d localAxis, double radians) {
        return multiply(fromAxisAndAngle(localAxis, radians));
    }

    /**
     * Rotates around a world‑space axis
     */
    public Matrix3 rotateWorld(Quaternion q) {
        return leftMultiply(q.toMatrix3());
    }

    public Matrix3 rotateWorld(double radians, double x, double y, double z) {
        return rotateWorld(new Vec3d(x, y, z), radians);
    }

    public Matrix3 rotateWorld(Vec3d axis, double radians) {
        return leftMultiply(fromAxisAndAngle(axis, radians));
    }

    public Vec3d apply(Vec3d v) {
        return new Vec3d(
                m00 * v.x + m01 * v.y + m02 * v.z,
                m10 * v.x + m11 * v.y + m12 * v.z,
                m20 * v.x + m21 * v.y + m22 * v.z
        );
    }

    /**
     * Get the Euler angle represented by this Matrix in degrees
     * @return A Vec3d containing the angle (yaw -> x, pitch -> y, roll -> z);
     */
    public Vec3d toEuler() {
        double pitch = -Math.asin(m12);
        double yaw, roll;
        if (Math.abs(Math.cos(pitch)) > 1E-6) {
            yaw  = Math.atan2(m02, m22);
            roll = Math.atan2(m10, m11);
        } else {
            yaw  = Math.atan2(-m20, m00);
            roll = 0;
        }
        return new Vec3d(Math.toDegrees(yaw), Math.toDegrees(pitch), Math.toDegrees(roll));
    }

    public Quaternion toQuaternion() {
        double x, y, z, w;
        double t = m00 + m11 + m22;
        if (t > 0.0) {
            double s = Math.sqrt(t + 1.0) * 2.0;
            w = 0.25 * s;
            x = (m21 - m12) / s;
            y = (m02 - m20) / s;
            z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1.0 + m00 - m11 - m22) * 2.0;
            w = (m21 - m12) / s;
            x = 0.25 * s;
            y = (m01 + m10) / s;
            z = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0 + m11 - m00 - m22) * 2.0;
            w = (m02 - m20) / s;
            x = (m01 + m10) / s;
            y = 0.25 * s;
            z = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1.0 + m22 - m00 - m11) * 2.0;
            w = (m10 - m01) / s;
            x = (m02 + m20) / s;
            y = (m12 + m21) / s;
            z = 0.25 * s;
        }
        return Quaternion.trusted(x, y, z, w);
    }

    public Matrix4 toMatrix4() {
        Matrix4 matrix4 = new Matrix4();
        matrix4.m00 = this.m00;
        matrix4.m01 = this.m01;
        matrix4.m02 = this.m02;
        matrix4.m10 = this.m10;
        matrix4.m11 = this.m11;
        matrix4.m12 = this.m12;
        matrix4.m20 = this.m20;
        matrix4.m21 = this.m21;
        matrix4.m22 = this.m22;
        return matrix4;
    }


    @Override
    public String toString() {
        return String.format("[%7.4f %7.4f %7.4f]\n" + "[%7.4f %7.4f %7.4f]\n" + "[%7.4f %7.4f %7.4f]",
                             m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Matrix3)) return false;
        Matrix3 o = (Matrix3) obj;
        return Double.compare(this.m00, o.m00) == 0 &&
                Double.compare(this.m01, o.m01) == 0 &&
                Double.compare(this.m02, o.m02) == 0 &&
                Double.compare(this.m10, o.m10) == 0 &&
                Double.compare(this.m11, o.m11) == 0 &&
                Double.compare(this.m12, o.m12) == 0 &&
                Double.compare(this.m20, o.m20) == 0 &&
                Double.compare(this.m21, o.m21) == 0 &&
                Double.compare(this.m22, o.m22) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m00);
        bits = bits * 31 + Double.doubleToLongBits(m01);
        bits = bits * 31 + Double.doubleToLongBits(m02);
        bits = bits * 31 + Double.doubleToLongBits(m10);
        bits = bits * 31 + Double.doubleToLongBits(m11);
        bits = bits * 31 + Double.doubleToLongBits(m12);
        bits = bits * 31 + Double.doubleToLongBits(m20);
        bits = bits * 31 + Double.doubleToLongBits(m21);
        bits = bits * 31 + Double.doubleToLongBits(m22);
        return Long.hashCode(bits);
    }
}