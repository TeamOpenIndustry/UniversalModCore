package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public final class CapBuilder {

    private CapBuilder() {}

    public static Polygon build(
            List<ClipVertex> intersections,
            Plane plane) {

        List<ClipVertex> vertices =
                VertexDeduplicator.deduplicate(
                        intersections);

        if (vertices.size() < 3) {
            return null;
        }

        Vec3d center = computeCenter(vertices);

        PlaneBasis basis =
                PlaneBasis.fromPlane(plane);

        vertices.sort(
                Comparator.comparingDouble(v -> {

                    Vec3d offset = v.pos.subtract(center);

                    double x = offset.dotProduct(basis.u);
                    double y = offset.dotProduct(basis.v);

                    return Math.atan2(y, x);
                })
        );

        Polygon polygon = new Polygon();
        polygon.vertices.addAll(vertices);

        return polygon;
    }

    private static Vec3d computeCenter(
            List<ClipVertex> vertices) {

        double x = 0;
        double y = 0;
        double z = 0;

        for (ClipVertex v : vertices) {
            x += v.pos.x;
            y += v.pos.y;
            z += v.pos.z;
        }

        double inv = 1.0 / vertices.size();

        return new Vec3d(
                x * inv,
                y * inv,
                z * inv
        );
    }
}