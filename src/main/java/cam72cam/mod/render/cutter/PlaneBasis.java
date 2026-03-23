package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

public final class PlaneBasis {

    public final Vec3d u;
    public final Vec3d v;

    public PlaneBasis(Vec3d u, Vec3d v) {
        this.u = u;
        this.v = v;
    }

    public static PlaneBasis fromPlane(Plane plane) {

        Vec3d n = plane.normal.normalize();

        Vec3d helper;

        if (Math.abs(n.x) <= Math.abs(n.y)
                && Math.abs(n.x) <= Math.abs(n.z)) {

            helper = new Vec3d(1,0,0);

        } else if (Math.abs(n.y) <= Math.abs(n.z)) {

            helper = new Vec3d(0,1,0);

        } else {

            helper = new Vec3d(0,0,1);
        }

        // u = helper × normal
        Vec3d u = helper.crossProduct(n).normalize();

        // v = normal × u
        Vec3d v = n.crossProduct(u).normalize();

        return new PlaneBasis(u, v);
    }

    public PlaneBasis rotateCW() {
        return new PlaneBasis(
                v,
                u.scale(-1)
        );
    }

    public PlaneBasis rotateCCW() {
        return new PlaneBasis(
                v.scale(-1),
                u
        );
    }

    public PlaneBasis flipU() {
        return new PlaneBasis(
                u.scale(-1),
                v
        );
    }

    public PlaneBasis flipV() {
        return new PlaneBasis(
                u,
                v.scale(-1)
        );
    }

    public PlaneBasis flip() {

        return new PlaneBasis(
                u.scale(-1),
                v.scale(-1)
        );
    }
}