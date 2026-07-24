package cam72cam.mod.math;

public class Matrix3d {
    static final Vec3d FORWARD = new Vec3d(0, 0, 1);
    static final Vec3d RIGHT = new Vec3d(1, 0, 0);
    static final Vec3d UP = new Vec3d(0, 1, 0);
    public double m00, m10, m20;
    public double m01, m11, m21;
    public double m02, m12, m22;

    public Matrix3d() {
        setIdentity();
    }

    public Matrix3d(double m00, double m01, double m02,
                    double m10, double m11, double m12,
                    double m20, double m21, double m22) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02;
        this.m10 = m10; this.m11 = m11; this.m12 = m12;
        this.m20 = m20; this.m21 = m21; this.m22 = m22;
    }

    public Matrix3d(Vec3d forward, Vec3d right, Vec3d up) {
        forward = forward.normalize();
        this.m02 = forward.x;
        this.m12 = forward.y;
        this.m22 = forward.z;
        right = right.normalize();
        this.m00 = right.x;
        this.m10 = right.y;
        this.m20 = right.z;
        up = up.normalize();
        this.m01 = up.x;
        this.m11 = up.y;
        this.m21 = up.z;
    }

    public static Matrix3d fromQuaternion(Quaternion q) {
        q.normalize();
        return fromAxisAngle(new Vec3d(q.x, q.y, q.z), q.w);
    }

    public static Matrix3d fromAxisAngle(Vec3d axis, double radians) {
        axis = axis.normalize();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        double t = 1 - c;
        double x = axis.x, y = axis.y, z = axis.z;
        return new Matrix3d(
                c + x*x*t,   x*y*t - z*s, x*z*t + y*s,
                y*x*t + z*s, c + y*y*t,   y*z*t - x*s,
                z*x*t - y*s, z*y*t + x*s, c + z*z*t
        );
    }

    public static Matrix3d fromXRot(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new Matrix3d(
                1, 0,  0,
                0, c, -s,
                0, s,  c
        );
    }

    public static Matrix3d fromYRot(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new Matrix3d(
                c, 0, s,
                0, 1, 0,
                -s, 0, c
        );
    }

    public static Matrix3d fromZRot(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new Matrix3d(
                c, -s, 0,
                s,  c, 0,
                0,  0, 1
        );
    }

    public Matrix3d copy() {
        return new Matrix3d(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    public Matrix3d setIdentity() {
        m00 = m11 = m22 = 1;
        m01 = m02 = m10 = m12 = m20 = m21 = 0;
        return this;
    }

    public Matrix3d multiply(Matrix3d other) {
        double t00 = other.m00 * m00 + other.m01 * m10 + other.m02 * m20;
        double t10 = other.m10 * m00 + other.m11 * m10 + other.m12 * m20;
        double t20 = other.m20 * m00 + other.m21 * m10 + other.m22 * m20;
        double t01 = other.m00 * m01 + other.m01 * m11 + other.m02 * m21;
        double t11 = other.m10 * m01 + other.m11 * m11 + other.m12 * m21;
        double t21 = other.m20 * m01 + other.m21 * m11 + other.m22 * m21;
        double t02 = other.m00 * m02 + other.m01 * m12 + other.m02 * m22;
        double t12 = other.m10 * m02 + other.m11 * m12 + other.m12 * m22;
        double t22 = other.m20 * m02 + other.m21 * m12 + other.m22 * m22;
        m00 = t00; m01 = t01; m02 = t02;
        m10 = t10; m11 = t11; m12 = t12;
        m20 = t20; m21 = t21; m22 = t22;
        return this;
    }

    public Matrix3d leftMultiply(Matrix3d other) {
        double t00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20;
        double t10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20;
        double t20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20;
        double t01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21;
        double t11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21;
        double t21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21;
        double t02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22;
        double t12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22;
        double t22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22;
        m00 = t00; m01 = t01; m02 = t02;
        m10 = t10; m11 = t11; m12 = t12;
        m20 = t20; m21 = t21; m22 = t22;
        return this;
    }

    public Matrix3d transpose() {
        return new Matrix3d(m00, m10, m20,
                            m01, m11, m21,
                            m02, m12, m22);
    }

    public void rotateWorld(Vec3d axis, double angle) {
        multiply(fromAxisAngle(axis, angle));
    }

    public void rotateLocal(Vec3d localAxis, double angle) {
        leftMultiply(fromAxisAngle(localAxis, angle));
    }

    public Quaternion toQuaternion() {
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

    @Override
    public String toString() {
        return String.format("[%7.4f %7.4f %7.4f]\n" + "[%7.4f %7.4f %7.4f]\n" + "[%7.4f %7.4f %7.4f]",
                               m00,  m01,  m02,          m10,  m11,  m12,          m20,  m21,  m22);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Matrix3d)) return false;
        Matrix3d o = (Matrix3d) obj;
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