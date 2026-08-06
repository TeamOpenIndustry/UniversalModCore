package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.Facing;

import java.util.Collections;

public final class CapUVGenerator {

    private CapUVGenerator() {}

    public static void generate(
            Polygon polygon,
            Plane plane,
            Facing facing) {

        if(facing != null) {
            generateBlockUV(
                    polygon,
                    facing
            );
        } else {
            generatePlaneUV(
                    polygon,
                    PlaneBasis.fromPlane(plane)
            );
        }

        reverseWinding(polygon);
    }

    private static void generateBlockUV(
            Polygon polygon,
            Facing facing) {

        double minU = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;


        for (ClipVertex vertex : polygon.vertices) {

            Vec3d p = vertex.pos;

            double[] uv = project(p, facing);

            minU = Math.min(minU, uv[0]);
            maxU = Math.max(maxU, uv[0]);

            minV = Math.min(minV, uv[1]);
            maxV = Math.max(maxV, uv[1]);
        }


        double du = maxU - minU;
        double dv = maxV - minV;

        if (du < 1E-6)
            du = 1;

        if (dv < 1E-6)
            dv = 1;


        for (ClipVertex vertex : polygon.vertices) {

            double[] uv =
                    project(vertex.pos, facing);

            vertex.u =
                    (float)((uv[0] - minU) / du);

            vertex.v =
                    (float)((uv[1] - minV) / dv);
        }
    }

    private static double[] project(
            Vec3d p,
            Facing facing) {

        switch (facing) {

            case UP:
                return new double[]{p.x, p.z};

            case DOWN:
                return new double[]{p.x, -p.z};

            case NORTH:
                return new double[]{-p.x, -p.y};

            case SOUTH:
                return new double[]{p.x, -p.y};

            case WEST:
                return new double[]{p.z, -p.y};

            case EAST:
                return new double[]{-p.z, -p.y};

            default:
                return new double[]{0, 0};
        }
    }

    private static void reverseWinding(
            Polygon polygon) {

        Collections.reverse(
                polygon.vertices
        );
    }

    private static void generatePlaneUV(
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