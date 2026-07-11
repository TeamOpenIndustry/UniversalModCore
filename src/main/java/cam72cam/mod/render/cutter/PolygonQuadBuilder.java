package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public final class PolygonQuadBuilder {

    private PolygonQuadBuilder() {
    }

    /**
     * Split an arbitrary polygon into quads.
     *
     * Result polygons always contain exactly 4 vertices.
     * Triangles are represented by duplicating the last vertex.
     */
    public static List<Polygon> build(Polygon polygon) {

        List<Polygon> result = new ArrayList<>();

        List<ClipVertex> verts = new ArrayList<>(polygon.vertices);

        while (verts.size() > 4) {

            Polygon quad = new Polygon();

            quad.vertices.add(verts.get(0));
            quad.vertices.add(verts.get(1));
            quad.vertices.add(verts.get(2));
            quad.vertices.add(verts.get(3));

            result.add(quad);

            if ((verts.size() & 1) == 0) {
                // even
                verts.remove(2);
                verts.remove(1);
            } else {
                // odd
                verts.remove(1);
            }
        }

        if (verts.size() == 4) {

            Polygon quad = new Polygon();
            quad.vertices.addAll(verts);
            result.add(quad);

        } else if (verts.size() == 3) {

            Polygon quad = new Polygon();

            quad.vertices.add(verts.get(0));
            quad.vertices.add(verts.get(1));
            quad.vertices.add(verts.get(2));
            quad.vertices.add(verts.get(2));

            result.add(quad);
        }

        return result;
    }
}