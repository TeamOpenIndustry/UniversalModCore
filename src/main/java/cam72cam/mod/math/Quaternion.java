package cam72cam.mod.math;

/**
 * Internal helper for some lerp related methods
 *
 * @author DeepseaSaltyFish
 */
public class Quaternion {
    public static final Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    //                  w +xi+yj+zk
    public final double w, x, y, z;

    Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    Quaternion(Matrix3d m) {
        double t = m.m00 + m.m11 + m.m22;
        if (t > 0.0) {
            double s = Math.sqrt(t + 1.0) * 2.0;
            w = 0.25 * s;
            x = (m.m21 - m.m12) / s;
            y = (m.m02 - m.m20) / s;
            z = (m.m10 - m.m01) / s;
        } else if (m.m00 > m.m11 && m.m00 > m.m22) {
            double s = Math.sqrt(1.0 + m.m00 - m.m11 - m.m22) * 2.0;
            w = (m.m21 - m.m12) / s;
            x = 0.25 * s;
            y = (m.m01 + m.m10) / s;
            z = (m.m02 + m.m20) / s;
        } else if (m.m11 > m.m22) {
            double s = Math.sqrt(1.0 + m.m11 - m.m00 - m.m22) * 2.0;
            w = (m.m02 - m.m20) / s;
            x = (m.m01 + m.m10) / s;
            y = 0.25 * s;
            z = (m.m12 + m.m21) / s;
        } else {
            double s = Math.sqrt(1.0 + m.m22 - m.m00 - m.m11) * 2.0;
            w = (m.m10 - m.m01) / s;
            x = (m.m02 + m.m20) / s;
            y = (m.m12 + m.m21) / s;
            z = 0.25 * s;
        }
    }

    /**
     * Shortest rotation from vector a to vector b.
     */
    public static Quaternion fromAtoB(Vec3d a, Vec3d b) {
        a = a.normalize();
        b = b.normalize();
        double d = a.dotProduct(b);

        if (Math.abs(1 - d) < 1e-6) {
            return IDENTITY;
        } else if (Math.abs(-1 - d) < 1e-6) {
            // 180° rotation around any orthogonal axis
            Vec3d axis = Math.abs(a.x) < 0.9
                         ? new Vec3d(1, 0, 0).crossProduct(a).normalize()
                         : new Vec3d(0, 1, 0).crossProduct(a).normalize();
            return new Quaternion(axis.x, axis.y, axis.z, 0);
        } else {
            Vec3d axis = a.crossProduct(b);
            double s = Math.sqrt((1 + d) * 2);
            double invS = 1.0 / s;
            return new Quaternion(axis.x * invS, axis.y * invS, axis.z * invS, s * 0.5);
        }
    }

    public Quaternion normalize() {
        double lenSq = x * x + y * y + z * z + w * w;
        if (Math.abs(lenSq - 1.0) < 1e-12)
            return this;
        double inv = 1.0 / Math.sqrt(lenSq);
        return new Quaternion(x * inv, y * inv, z * inv, w * inv);
    }

    public Matrix3d toMatrix3d() {
        double xx = 2 * x * x, yy = 2 * y * y, zz = 2 * z * z;
        double xy = 2 * x * y, xz = 2 * x * z, yz = 2 * y * z;
        double wx = 2 * w * x, wy = 2 * w * y, wz = 2 * w * z;

        return new Matrix3d(1 - yy - zz, xy - wz, xz + wy,
                            xy + wz, 1 - xx - zz, yz - wx,
                            xz - wy, yz + wx, 1 - xx - yy);
    }

    public static Quaternion slerp(Quaternion from, Quaternion to, double t) {
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
