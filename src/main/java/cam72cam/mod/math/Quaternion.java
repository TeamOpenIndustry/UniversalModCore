package cam72cam.mod.math;

import util.Matrix4;

/**
 * Immutable unit quaternion representing a rotation in 3D space.
 * <p>
 * All factory methods return a normalized quaternion. Rotation composition
 * and vector rotation are implemented with optimal arithmetic.
 * <p>
 * Euler angle conversions use the same Yaw (Y) -> Pitch (X) -> Roll (Z)
 * order as Orientation.fromEuler().
 *
 * @author DeepseaSaltyFish
 */
public class Quaternion {
    public static final Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    //                  w +xi+yj+zk
    public final double w, x, y, z;

    private Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Builds and normalizes a rotation
     * */
    public static Quaternion of(double x, double y, double z, double w) {
        double len = Math.sqrt(x * x + y * y + z * z + w * w);
        return new Quaternion(x / len, y / len, z / len, w / len);
    }

    /**
     * Builds a rotation from an already normalized data source
     * */
    public static Quaternion trusted(double x, double y, double z, double w) {
        return new Quaternion(x, y, z, w);
    }

    /**
     * Builds a rotation from an axis and an angle
     * */
    public static Quaternion fromAxisAngle(Vec3d axis, double radians) {
        double half = radians * 0.5;
        double s = Math.sin(half);
        Vec3d n = axis.normalize();
        return new Quaternion(n.x * s, n.y * s, n.z * s, Math.cos(half));
    }

    /**
     * Builds a rotation from Euler angles in degrees.
     * <p>
     * Rotation order: Yaw -> Pitch -> Roll
     */
    public static Quaternion fromEuler(double yaw, double pitch, double roll) {
        double hy = Math.toRadians(yaw) * 0.5;
        double hp = Math.toRadians(pitch) * 0.5;
        double hr = Math.toRadians(roll) * 0.5;

        double cy = Math.cos(hy), sy = Math.sin(hy);
        double cp = Math.cos(hp), sp = Math.sin(hp);
        double cr = Math.cos(hr), sr = Math.sin(hr);

        return new Quaternion(
                cy * sp * cr + sy * cp * sr,
                sy * cp * cr - cy * sp * sr,
                cy * cp * sr + sy * sp * cr,
                cy * cp * cr - sy * sp * sr
        );
    }

    /**
     * Builds a quaternion from an orthonormal basis.
     */
    public static Quaternion fromBasis(Orientation orient) {
        return fromBasis(orient.forward(), orient.right(), orient.up());
    }
    public static Quaternion fromBasis(Vec3d forward, Vec3d right, Vec3d up) {
        double t = right.x + up.y + forward.z;
        double w, x, y, z;

        if (t > 0.0) {
            double s = Math.sqrt(t + 1.0) * 2.0;
            w = 0.25 * s;
            x = (up.z - forward.y) / s;
            y = (forward.x - right.z) / s;
            z = (right.y - up.x) / s;
        } else if (right.x > up.y && right.x > forward.z) {
            double s = Math.sqrt(1.0 + right.x - up.y - forward.z) * 2.0;
            w = (up.z - forward.y) / s;
            x = 0.25 * s;
            y = (up.x + right.y) / s;
            z = (forward.x + right.z) / s;
        } else if (up.y > forward.z) {
            double s = Math.sqrt(1.0 + up.y - right.x - forward.z) * 2.0;
            w = (forward.x - right.z) / s;
            x = (up.x + right.y) / s;
            y = 0.25 * s;
            z = (forward.y + up.z) / s;
        } else {
            double s = Math.sqrt(1.0 + forward.z - right.x - up.y) * 2.0;
            w = (right.y - up.x) / s;
            x = (forward.x + right.z) / s;
            y = (forward.y + up.z) / s;
            z = 0.25 * s;
        }
        return Quaternion.of(x, y, z, w);
    }

    /**
     * Shortest rotation from vector a to vector b.
     */
    public static Quaternion fromAtoB(Vec3d a, Vec3d b) {
        a = a.normalize();
        b = b.normalize();
        double d = a.dotProduct(b);

        if (d > 0.999999) {
            return IDENTITY;
        } else if (d < -0.999999) {
            // 180° rotation around any orthogonal axis
            Vec3d axis = Math.abs(a.x) < 0.9
                         ? new Vec3d(1, 0, 0).crossProduct(a).normalize()
                         : new Vec3d(0, 1, 0).crossProduct(a).normalize();
            return new Quaternion(axis.x, axis.y, axis.z, 0);
        } else {
            Vec3d axis = a.crossProduct(b);
            double s = Math.sqrt((1 + d) * 2);
            double invS = 1.0 / s;
            return new Quaternion(
                    axis.x * invS,
                    axis.y * invS,
                    axis.z * invS,
                    s * 0.5
            );
        }
    }

    public Quaternion multiply(Quaternion q) {
        return new Quaternion(
                w * q.x + x * q.w + y * q.z - z * q.y,
                w * q.y - x * q.z + y * q.w + z * q.x,
                w * q.z + x * q.y - y * q.x + z * q.w,
                w * q.w - x * q.x - y * q.y - z * q.z
        );
    }

    public Quaternion inverse() {
        return Quaternion.trusted(-x, -y, -z, w);
    }

    public Quaternion normalize() {
        double lenSq = x * x + y * y + z * z + w * w;
        if (Math.abs(lenSq - 1.0) < 1e-12)
            return this;
        double inv = 1.0 / Math.sqrt(lenSq);
        return Quaternion.trusted(x * inv, y * inv, z * inv, w * inv);
    }

    /**
     * Extracts Euler angles from the current quaternion.
     * @return A {@link Vec3d} containing the Euler rotation data as positions
     */
    public Vec3d toEuler() {
        Matrix4 m = toMatrix();
        double forwardY = m.m12;
        double pitch = -Math.asin(forwardY);
        double yaw, roll;
        if (Math.abs(Math.cos(pitch)) > 1e-6) {
            yaw = Math.atan2(m.m02, m.m22);
            roll = Math.atan2(m.m10, m.m11);
        } else {
            yaw = Math.atan2(-m.m20, m.m00);
            roll = 0;
        }
        return new Vec3d(Math.toDegrees(yaw), Math.toDegrees(pitch), Math.toDegrees(roll));
    }

    /**
     * Converts to a 4x4 rotation matrix (columns = right, up, forward).
     */
    public Matrix4 toMatrix() {
        double xx = 2*x*x, yy = 2*y*y, zz = 2*z*z;
        double xy = 2*x*y, xz = 2*x*z, yz = 2*y*z;
        double wx = 2*w*x, wy = 2*w*y, wz = 2*w*z;

        Matrix4 m = new Matrix4();
        m.m00 = 1 - yy - zz;
        m.m10 = xy + wz;
        m.m20 = xz - wy;
        m.m01 = xy - wz;
        m.m11 = 1 - xx - zz;
        m.m21 = yz + wx;
        m.m02 = xz + wy;
        m.m12 = yz - wx;
        m.m22 = 1 - xx - yy;
        return m;
    }

    public Orientation toOrientation() {
        Vec3d euler = toEuler();
        return Orientation.fromEuler(euler.x, euler.y, euler.z);
    }

    /**
     * Rotates a vector by this quaternion.
     * Uses the efficient Rodrigues‑like formula.
     */
    public Vec3d apply(Vec3d v) {
        // t = 2 * cross(q.xyz, v)
        double tx = 2.0 * (y*v.z - z*v.y);
        double ty = 2.0 * (z*v.x - x*v.z);
        double tz = 2.0 * (x*v.y - y*v.x);

        return new Vec3d(
                v.x + w*tx + (y*tz - z*ty),
                v.y + w*ty + (z*tx - x*tz),
                v.z + w*tz + (x*ty - y*tx)
        );
    }

    public static Quaternion slerp(Quaternion from, Quaternion to, double t) {
        double cosOmega = from.x * to.x +  from.y * to.y + from.z * to.z + from.w * to.w;
        // Take shortest path
        double sign = 1.0;
        if (cosOmega < 0) {
            cosOmega = -cosOmega;
            sign = -1.0;
        }
        double scale0, scale1;
        if (1.0 - cosOmega < 1e-6) {
            // Linear interpolation for very close quaternions
            scale0 = 1.0 - t;
            scale1 = t * sign;
        } else {
            double omega = Math.acos(cosOmega);
            double sinOmega = Math.sin(omega);
            scale0 = Math.sin((1.0 - t) * omega) / sinOmega;
            scale1 = Math.sin(t * omega) / sinOmega * sign;
        }
        return new Quaternion(
                scale0 * from.x + scale1 * to.x,
                scale0 * from.y + scale1 * to.y,
                scale0 * from.z + scale1 * to.z,
                scale0 * from.w + scale1 * to.w
        );

    }

    public double dotProduct(Quaternion other) {
        return x * other.x + y * other.y + z * other.z + w * other.w;
    }

    @Override
    public String toString() {
        return String.format("Quat[%.4f, %.4f, %.4f, %.4f]", x, y, z, w);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Quaternion) {
            if (this == obj) return true;
            Quaternion q = (Quaternion) obj;
            return Double.compare(this.w, q.w) == 0 &&
                   Double.compare(this.x, q.x) == 0 &&
                   Double.compare(this.y, q.y) == 0 &&
                   Double.compare(this.z, q.z) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        bits ^= Double.doubleToLongBits(y) * 31;
        bits ^= Double.doubleToLongBits(z) * 631;
        bits ^= Double.doubleToLongBits(w) * 1271;
        return (int) (bits ^ (bits >> 32));
    }
}
