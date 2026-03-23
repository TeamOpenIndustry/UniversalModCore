package cam72cam.mod.render.cutter.adapter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.ClipVertex;
import cam72cam.mod.render.cutter.Plane;
import cam72cam.mod.render.cutter.PlaneBasis;
import cam72cam.mod.render.cutter.Polygon;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Collections;

public final class CapUVGenerator {

    private CapUVGenerator() {}

    public static void generate(
            Polygon polygon,
            Plane plane,
            TextureAtlasSprite sprite) {

        generateUV(
                polygon,
                sprite,
                PlaneBasis
                        .fromPlane(plane)
                        .rotateCCW()
        );

        reverseWinding(polygon);
        generateNormals(polygon, plane);
    }

    private static void reverseWinding(
            Polygon polygon) {

        Collections.reverse(
                polygon.vertices
        );
    }

    private static void generateUV(
            Polygon polygon,
            TextureAtlasSprite sprite,
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

            vertex.u = sprite.getU(u);
            vertex.v = sprite.getV(v);
        }
    }

    private static void generateNormals(
            Polygon polygon,
            Plane plane) {

        byte nx = (byte)Math.round(plane.normal.x * 127);
        byte ny = (byte)Math.round(plane.normal.y * 127);
        byte nz = (byte)Math.round(plane.normal.z * 127);

        for (ClipVertex v : polygon.vertices) {

            v.nx = nx;
            v.ny = ny;
            v.nz = nz;
        }
    }
}