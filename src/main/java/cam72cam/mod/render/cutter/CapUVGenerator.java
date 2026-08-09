package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.adapter.QuadTemplate;

import java.util.Collections;

public final class CapUVGenerator {

    private CapUVGenerator() {}

    public static void generate(Polygon polygon, QuadTemplate template) {

        Vec3d p0 = template.sourcePos[0];
        Vec3d p1 = template.sourcePos[1];
        Vec3d p3 = template.sourcePos[3];

        // position space basis
        Vec3d e1 = p1.subtract(p0);
        Vec3d e2 = p3.subtract(p0);

        double a00 = e1.dotProduct(e1);
        double a01 = e1.dotProduct(e2);
        double a11 = e2.dotProduct(e2);

        // 2x2 inverse determinant
        double det = a00 * a11 - a01 * a01;

        if (Math.abs(det) < 1E-8) {
            return;
        }

        for (ClipVertex vertex : polygon.vertices) {
            Vec3d d = vertex.pos.subtract(p0);

            double b0 = d.dotProduct(e1);
            double b1 = d.dotProduct(e2);

            // solve:
            //
            // [a00 a01][x] = [b0]
            // [a01 a11][y] = [b1]

            double x = (b0 * a11 - b1 * a01) / det;
            double y = (b1 * a00 - b0 * a01) / det;

            /*
             * x = p0 -> p1 direction
             * y = p0 -> p3 direction
             *
             * UV uses the same weight
             */

            vertex.u = (float) (
                    template.sourceU[0]
                            + x * (template.sourceU[1] - template.sourceU[0])
                            + y * (template.sourceU[3] - template.sourceU[0])
            );

            vertex.v = (float) (
                    template.sourceV[0]
                            + x * (template.sourceV[1] - template.sourceV[0])
                            + y * (template.sourceV[3] - template.sourceV[0])
            );
        }

        Collections.reverse(polygon.vertices);
    }
}