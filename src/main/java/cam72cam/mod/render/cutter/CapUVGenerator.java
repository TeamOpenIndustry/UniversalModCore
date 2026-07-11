package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

import java.util.Collections;

public final class CapUVGenerator {

    private CapUVGenerator() {}

    public static void generate(Polygon polygon, Plane plane) {

        generateUV(
                polygon,
                PlaneBasis
                        .fromPlane(plane)
                        .rotateCCW()
        );

        reverseWinding(polygon);
    }

    private static void reverseWinding(
            Polygon polygon) {

        Collections.reverse(
                polygon.vertices
        );
    }

    private static void generateUV(
            Polygon polygon,
            PlaneBasis basis) {

        double minU = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;

        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;

        for (ClipVertex vertex : polygon.vertices) {

            Vec3d p = vertex.pos;

            double u = p.dotProduct(basis.u);
            double v = p.dotProduct(basis.v);

            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);

            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }

        double du = maxU - minU;
        double dv = maxV - minV;

        if (du < 1E-6) du = 1;
        if (dv < 1E-6) dv = 1;

        for (ClipVertex vertex : polygon.vertices) {

            Vec3d p = vertex.pos;

            double lu = p.dotProduct(basis.u);
            double lv = p.dotProduct(basis.v);

            float u = (float)((lu - minU) / du);
            float v = (float)((lv - minV) / dv);

            vertex.u = u;
            vertex.v = v;
        }
    }
}