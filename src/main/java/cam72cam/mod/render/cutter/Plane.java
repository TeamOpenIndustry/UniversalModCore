package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

public class Plane {

    public final Vec3d normal;
    public final double d;
    public final Vec3d point;

    public Plane(Vec3d normal, double d) {
        this.normal = normal.normalize();
        this.d = d;
        this.point = this.normal.scale(-d);
    }

    public Plane(Vec3d point, Vec3d normal) {
        this.normal = normal.normalize();
        this.d = -this.normal.dotProduct(point);
        this.point = point;
    }

    public double distance(Vec3d p) {
        return normal.dotProduct(p) + d;
    }

    public Plane flip() {
        return new Plane(normal.scale(-1), -d);
    }
}