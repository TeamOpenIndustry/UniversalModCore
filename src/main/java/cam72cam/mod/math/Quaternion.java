package cam72cam.mod.math;

import util.Matrix4;

/**
 * Representation for standard rotation in another format
 * <p>
 * Mainly a helper for some lerp related methods
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
    public static Quaternion fromAxisAndAngle(Vec3d axis, double radians) {
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

    public static Quaternion fromBasis(Vec3d forward, Vec3d right, Vec3d up) {
        forward = forward.normalize();
        right = right.normalize();
        up = up.normalize();

        double m00 = right.x, m01 = up.x, m02 = forward.x;
        double m10 = right.y, m11 = up.y, m12 = forward.y;
        double m20 = right.z, m21 = up.z, m22 = forward.z;

        double t = m00 + m11 + m22;
        double w, x, y, z;
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

    /**
     * Shortest rotation from vector a to vector b.
     */
    public static Quaternion fromAtoB(Vec3d a, Vec3d b) {
        a = a.normalize();
        b = b.normalize();
        double d = a.dotProduct(b);

        if (Math.abs(1 - d) < 1e-6) {
            // Almost 0°
            return IDENTITY;
        } else if (Math.abs(-1 - d) < 1e-6) {
            // 180° rotation around any orthogonal axis
            Vec3d axis;
            if (Math.abs(a.x) < 0.9) {
                axis = new Vec3d(1, 0, 0).crossProduct(a);
            } else {
                axis = new Vec3d(0, 1, 0).crossProduct(a);
            }
            if (axis.lengthSquared() < 1e-12) {
                axis = new Vec3d(0, 0, 1).crossProduct(a);
            }
            axis = axis.normalize();
            return new Quaternion(axis.x, axis.y, axis.z, 0);
        } else {
            Vec3d axis = a.crossProduct(b);
            double s = Math.sqrt((1 + d) * 2);
            double invS = 1.0 / s;
            return new Quaternion(axis.x * invS, axis.y * invS, axis.z * invS, s * 0.5);
        }
    }

    public Quaternion inverse() {
        return Quaternion.trusted(-x, -y, -z, w);
    }

    public Quaternion normalize() {
        double lenSq = x * x + y * y + z * z + w * w;
        if (Math.abs(lenSq - 1.0) < 1e-12)
            return this;
        double inv = 1.0 / Math.sqrt(lenSq);
        return new Quaternion(x * inv, y * inv, z * inv, w * inv);
    }

    public Quaternion hamiltonProduct(Quaternion q) {
        return new Quaternion(
                w * q.x + x * q.w + y * q.z - z * q.y,
                w * q.y - x * q.z + y * q.w + z * q.x,
                w * q.z + x * q.y - y * q.x + z * q.w,
                w * q.w - x * q.x - y * q.y - z * q.z
        );
    }

    public double dotProduct(Quaternion q) {
        return this.w * q.w + this.x * q.x + this.y * q.y + this.z * q.z;
    }

    public static Quaternion lerp(Quaternion from, Quaternion to, double t) {
        from = from.normalize();
        to = to.normalize();
        double cosOmega = from.x * to.x +  from.y * to.y + from.z * to.z + from.w * to.w;
        // Take shortest path
        double sign = 1.0;
        if (cosOmega < 0) {
            cosOmega = -cosOmega;
            sign = -1.0;
        }
        cosOmega = Math.min(cosOmega, 1);
        double scale0, scale1;
        if (1.0 - cosOmega < 1e-6) {
            // Nlerp interpolation for very close quaternions
            scale0 = 1.0 - t;
            scale1 = t * sign;
        } else {
            // Slerp for very close quaternions
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
        ).normalize();
    }

    public Matrix3 toMatrix3() {
        double xx = 2 * x * x, yy = 2 * y * y, zz = 2 * z * z;
        double xy = 2 * x * y, xz = 2 * x * z, yz = 2 * y * z;
        double wx = 2 * w * x, wy = 2 * w * y, wz = 2 * w * z;

        return new Matrix3(1 - yy - zz, xy - wz, xz + wy,
                           xy + wz, 1 - xx - zz, yz - wx,
                           xz - wy, yz + wx, 1 - xx - yy);
    }

    public Matrix4 toMatrix4() {
        return toMatrix3().toMatrix4();
    }

    public Vec3d apply(Vec3d orig) {
        // t = 2 * cross(q.xyz, orig)
        double tx = 2.0 * (y * orig.z - z * orig.y);
        double ty = 2.0 * (z * orig.x - x * orig.z);
        double tz = 2.0 * (x * orig.y - y * orig.x);
        return new Vec3d(
                orig.x + w * tx + (y * tz - z * ty),
                orig.y + w * ty + (z * tx - x * tz),
                orig.z + w * tz + (x * ty - y * tx)
        );
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
