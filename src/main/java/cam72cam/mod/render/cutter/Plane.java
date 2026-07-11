package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

public class Plane {

    public final Vec3d normal;
    public final double d;

    public Plane(Vec3d normal, double d) {
        this.normal = normal.normalize();
        this.d = d;
    }

    public Plane(Vec3d point, Vec3d normal) {
        this.normal = normal.normalize();
        this.d = -this.normal.dotProduct(point);
    }

    public double distance(Vec3d p) {
        return normal.dotProduct(p) + d;
    }
}
