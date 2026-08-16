package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public final class PolygonQuadBuilder {

    private PolygonQuadBuilder() {}

    /**
     * Splits a convex polygon into quads using fan triangulation from the first vertex.
     * Triangles are represented as degenerate quads (last vertex duplicated).
     * Assumes input polygon is convex; for concave polygons, use ear clipping first.
     *
     * @param polygon the convex polygon to split
     * @return a list of quads (each as a Polygon with 4 vertices)
     */
    public static List<Polygon> build(Polygon polygon) {
        List<Polygon> result = new ArrayList<>();
        List<ClipVertex> verts = polygon.getVertices();
        int n = verts.size();
        if (n < 3) return result;

        ClipVertex first = verts.get(0);
        for (int i = 1; i < n - 1; i++) {
            ClipVertex a = first;
            ClipVertex b = verts.get(i);
            ClipVertex c = verts.get(i + 1);
            List<ClipVertex> quadVerts = new ArrayList<>(4);
            quadVerts.add(a);
            quadVerts.add(b);
            quadVerts.add(c);
            quadVerts.add(c);
            result.add(new Polygon(quadVerts, polygon.getNormal()));
        }
        return result;
    }
}